package com.aiassistant.scheduler.executor;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.aiassistant.exceptions.MessagingException;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Handler for sending email messages
 */
public class EmailActionHandler implements ActionHandler {
    
    private static final String TAG = "EmailActionHandler";
    
    // Email service types
    public enum EmailServiceType {
        GMAIL,
        OUTLOOK,
        YAHOO,
        CUSTOM,
        DEVICE_MAILER
    }
    
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    
    // SMTP server configurations for common services
    private final Map<EmailServiceType, Map<String, String>> serviceConfigs;
    
    /**
     * Constructor
     * 
     * @param context Android context
     */
    public EmailActionHandler(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.serviceConfigs = initializeServiceConfigs();
    }
    
    /**
     * Initialize service configurations for common email providers
     */
    private Map<EmailServiceType, Map<String, String>> initializeServiceConfigs() {
        Map<EmailServiceType, Map<String, String>> configs = new HashMap<>();
        
        // Gmail config
        Map<String, String> gmailConfig = new HashMap<>();
        gmailConfig.put("host", "smtp.gmail.com");
        gmailConfig.put("port", "587");
        gmailConfig.put("auth", "true");
        gmailConfig.put("starttls", "true");
        configs.put(EmailServiceType.GMAIL, gmailConfig);
        
        // Outlook config
        Map<String, String> outlookConfig = new HashMap<>();
        outlookConfig.put("host", "smtp-mail.outlook.com");
        outlookConfig.put("port", "587");
        outlookConfig.put("auth", "true");
        outlookConfig.put("starttls", "true");
        configs.put(EmailServiceType.OUTLOOK, outlookConfig);
        
        // Yahoo config
        Map<String, String> yahooConfig = new HashMap<>();
        yahooConfig.put("host", "smtp.mail.yahoo.com");
        yahooConfig.put("port", "587");
        yahooConfig.put("auth", "true");
        yahooConfig.put("starttls", "true");
        configs.put(EmailServiceType.YAHOO, yahooConfig);
        
        return configs;
    }
    
    @Override
    public boolean executeAction(Map<String, Object> params) {
        try {
            // Extract parameters
            String subject = (String) params.get("subject");
            String body = (String) params.get("body");
            String htmlBody = (String) params.get("htmlBody");
            
            Object recipientsObj = params.get("recipients");
            List<String> recipients = convertToStringList(recipientsObj);
            
            // Validate recipients
            if (recipients == null || recipients.isEmpty()) {
                Log.e(TAG, "At least one recipient is required");
                return false;
            }
            
            // Method-specific parameters
            String serviceTypeStr = (String) params.get("serviceType");
            EmailServiceType serviceType = getServiceType(serviceTypeStr);
            
            // Check sending method
            if (serviceType == EmailServiceType.DEVICE_MAILER) {
                // Use device's email client
                try {
                    // Create intent for sending email
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    
                    // Set email data
                    emailIntent.setType("message/rfc822"); // Email MIME type
                    
                    // Set recipients
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients.toArray(new String[0]));
                    
                    // Set subject
                    if (!TextUtils.isEmpty(subject)) {
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                    }
                    
                    // Set body
                    if (!TextUtils.isEmpty(htmlBody)) {
                        emailIntent.putExtra(Intent.EXTRA_TEXT, android.text.Html.fromHtml(htmlBody, 
                                android.text.Html.FROM_HTML_MODE_LEGACY));
                    } else if (!TextUtils.isEmpty(body)) {
                        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                    }
                    
                    // Create chooser for email clients
                    Intent chooser = Intent.createChooser(emailIntent, "Send email using");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    // Launch email client
                    context.startActivity(chooser);
                    
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Error sending email with device mailer", e);
                    return false;
                }
            } else {
                // For SMTP implementation, we need username and password
                // which should be handled in the callback version
                Log.e(TAG, "SMTP email implementation requires callback for security reasons");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in executeAction", e);
            return false;
        }
    }
    
    @Override
    public void executeAction(Map<String, Object> parameters, ActionCallback callback) {
        // Extract parameters
        String subject = (String) parameters.get("subject");
        String body = (String) parameters.get("body");
        String htmlBody = (String) parameters.get("htmlBody");
        
        Object recipientsObj = parameters.get("recipients");
        List<String> recipients = convertToStringList(recipientsObj);
        
        Object ccObj = parameters.get("cc");
        List<String> cc = convertToStringList(ccObj);
        
        Object bccObj = parameters.get("bcc");
        List<String> bcc = convertToStringList(bccObj);
        
        Object attachmentsObj = parameters.get("attachments");
        List<String> attachments = convertToStringList(attachmentsObj);
        
        // Method-specific parameters
        String serviceTypeStr = (String) parameters.get("serviceType");
        EmailServiceType serviceType = getServiceType(serviceTypeStr);
        
        // Check sending method
        if (serviceType == EmailServiceType.DEVICE_MAILER) {
            // Use device's email client
            sendWithDeviceMailer(subject, body, htmlBody, recipients, cc, bcc, attachments, callback);
        } else {
            // Use SMTP
            String username = (String) parameters.get("username");
            String password = (String) parameters.get("password");
            String smtpHost = (String) parameters.get("smtpHost");
            String smtpPort = (String) parameters.get("smtpPort");
            String fromName = (String) parameters.get("fromName");
            Boolean useAuth = (Boolean) parameters.get("useAuth");
            Boolean useStartTLS = (Boolean) parameters.get("useStartTLS");
            
            // Use default service configuration if not specified
            if (serviceType != EmailServiceType.CUSTOM) {
                Map<String, String> serviceConfig = serviceConfigs.get(serviceType);
                if (TextUtils.isEmpty(smtpHost)) {
                    smtpHost = serviceConfig.get("host");
                }
                if (TextUtils.isEmpty(smtpPort)) {
                    smtpPort = serviceConfig.get("port");
                }
                if (useAuth == null) {
                    useAuth = Boolean.parseBoolean(serviceConfig.get("auth"));
                }
                if (useStartTLS == null) {
                    useStartTLS = Boolean.parseBoolean(serviceConfig.get("starttls"));
                }
            }
            
            // Validate SMTP parameters
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                callback.onError("Email username and password are required");
                return;
            }
            
            if (TextUtils.isEmpty(smtpHost) || TextUtils.isEmpty(smtpPort)) {
                callback.onError("SMTP host and port are required");
                return;
            }
            
            // Send email with SMTP
            sendWithSMTP(username, password, smtpHost, smtpPort, fromName, useAuth, useStartTLS,
                    subject, body, htmlBody, recipients, cc, bcc, attachments, callback);
        }
    }
    
    /**
     * Convert an object to a list of strings
     */
    private List<String> convertToStringList(Object obj) {
        List<String> result = new ArrayList<>();
        
        if (obj == null) {
            return result;
        }
        
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
        } else if (obj instanceof String) {
            String str = (String) obj;
            if (!TextUtils.isEmpty(str)) {
                // Split by commas or semicolons
                result.addAll(Arrays.asList(str.split("[,;]")));
                // Trim each entry
                for (int i = 0; i < result.size(); i++) {
                    result.set(i, result.get(i).trim());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Get email service type from string
     */
    private EmailServiceType getServiceType(String serviceTypeStr) {
        if (TextUtils.isEmpty(serviceTypeStr)) {
            return EmailServiceType.DEVICE_MAILER; // Default
        }
        
        try {
            return EmailServiceType.valueOf(serviceTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If not a valid enum value, try common names
            if ("gmail".equalsIgnoreCase(serviceTypeStr)) {
                return EmailServiceType.GMAIL;
            } else if ("outlook".equalsIgnoreCase(serviceTypeStr) || 
                    "hotmail".equalsIgnoreCase(serviceTypeStr)) {
                return EmailServiceType.OUTLOOK;
            } else if ("yahoo".equalsIgnoreCase(serviceTypeStr)) {
                return EmailServiceType.YAHOO;
            } else if ("device".equalsIgnoreCase(serviceTypeStr) || 
                    "app".equalsIgnoreCase(serviceTypeStr) ||
                    "intent".equalsIgnoreCase(serviceTypeStr)) {
                return EmailServiceType.DEVICE_MAILER;
            } else {
                return EmailServiceType.CUSTOM;
            }
        }
    }
    
    /**
     * Send email using the device's email client
     */
    private void sendWithDeviceMailer(String subject, String body, String htmlBody,
                                      List<String> recipients, List<String> cc, List<String> bcc,
                                      List<String> attachments, ActionCallback callback) {
        try {
            // Create intent for sending email
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            
            // Multiple attachments
            if (attachments != null && !attachments.isEmpty() && attachments.size() > 1) {
                emailIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            }
            
            // Set email data
            emailIntent.setType("message/rfc822"); // Email MIME type
            
            // Set recipients
            if (recipients != null && !recipients.isEmpty()) {
                emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients.toArray(new String[0]));
            } else {
                callback.onError("At least one recipient is required");
                return;
            }
            
            // Set subject
            if (!TextUtils.isEmpty(subject)) {
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            }
            
            // Set body
            if (!TextUtils.isEmpty(htmlBody)) {
                emailIntent.putExtra(Intent.EXTRA_TEXT, android.text.Html.fromHtml(htmlBody, 
                        android.text.Html.FROM_HTML_MODE_LEGACY));
            } else if (!TextUtils.isEmpty(body)) {
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
            }
            
            // Set CC
            if (cc != null && !cc.isEmpty()) {
                emailIntent.putExtra(Intent.EXTRA_CC, cc.toArray(new String[0]));
            }
            
            // Set BCC
            if (bcc != null && !bcc.isEmpty()) {
                emailIntent.putExtra(Intent.EXTRA_BCC, bcc.toArray(new String[0]));
            }
            
            // Add attachments
            if (attachments != null && !attachments.isEmpty()) {
                if (attachments.size() == 1) {
                    // Single attachment
                    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(attachments.get(0)));
                } else {
                    // Multiple attachments
                    ArrayList<Uri> uris = new ArrayList<>();
                    for (String attachment : attachments) {
                        uris.add(Uri.parse(attachment));
                    }
                    emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                }
            }
            
            // Create chooser for email clients
            Intent chooser = Intent.createChooser(emailIntent, "Send email using");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Launch email client
            context.startActivity(chooser);
            
            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("method", "device_mailer");
            result.put("recipientCount", recipients.size());
            
            callback.onComplete(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending email with device mailer", e);
            callback.onError("Error sending email: " + e.getMessage());
        }
    }
    
    /**
     * Send email using SMTP
     */
    private void sendWithSMTP(String username, String password, String smtpHost, String smtpPort,
                              String fromName, Boolean useAuth, Boolean useStartTLS,
                              String subject, String body, String htmlBody,
                              List<String> recipients, List<String> cc, List<String> bcc,
                              List<String> attachments, ActionCallback callback) {
        // Validate parameters
        if (recipients == null || recipients.isEmpty()) {
            callback.onError("At least one recipient is required");
            return;
        }
        
        // Execute in background
        executorService.execute(() -> {
            try {
                // Set properties
                Properties props = new Properties();
                props.put("mail.smtp.host", smtpHost);
                props.put("mail.smtp.port", smtpPort);
                props.put("mail.smtp.auth", useAuth != null ? useAuth.toString() : "true");
                props.put("mail.smtp.starttls.enable", useStartTLS != null ? useStartTLS.toString() : "true");
                
                // Create session
                Session session;
                if (useAuth != null && useAuth) {
                    session = Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });
                } else {
                    session = Session.getInstance(props);
                }
                
                // Create message
                MimeMessage message = new MimeMessage(session);
                
                // Set from address
                if (!TextUtils.isEmpty(fromName)) {
                    message.setFrom(new InternetAddress(username, fromName));
                } else {
                    message.setFrom(new InternetAddress(username));
                }
                
                // Set recipients
                for (String recipient : recipients) {
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                }
                
                // Set CC
                if (cc != null && !cc.isEmpty()) {
                    for (String ccAddress : cc) {
                        message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccAddress));
                    }
                }
                
                // Set BCC
                if (bcc != null && !bcc.isEmpty()) {
                    for (String bccAddress : bcc) {
                        message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccAddress));
                    }
                }
                
                // Set subject
                if (!TextUtils.isEmpty(subject)) {
                    message.setSubject(subject);
                }
                
                // Create multipart message
                Multipart multipart = new MimeMultipart();
                
                // Add body part
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                if (!TextUtils.isEmpty(htmlBody)) {
                    messageBodyPart.setContent(htmlBody, "text/html");
                } else {
                    messageBodyPart.setText(body != null ? body : "");
                }
                multipart.addBodyPart(messageBodyPart);
                
                // Add attachments
                if (attachments != null && !attachments.isEmpty()) {
                    for (String attachmentPath : attachments) {
                        try {
                            File file = new File(attachmentPath);
                            if (file.exists() && file.isFile()) {
                                MimeBodyPart attachmentPart = new MimeBodyPart();
                                FileDataSource source = new FileDataSource(file);
                                attachmentPart.setDataHandler(new DataHandler(source));
                                attachmentPart.setFileName(file.getName());
                                multipart.addBodyPart(attachmentPart);
                            } else {
                                Log.w(TAG, "Attachment file not found: " + attachmentPath);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error adding attachment: " + attachmentPath, e);
                        }
                    }
                }
                
                // Set content
                message.setContent(multipart);
                
                // Send message
                Transport.send(message);
                
                // Prepare result
                final Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("method", "smtp");
                result.put("service", smtpHost);
                result.put("recipientCount", recipients.size());
                
                // Return result on main thread
                mainHandler.post(() -> callback.onComplete(result));
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending email with SMTP", e);
                final String errorMessage = e.getMessage();
                
                mainHandler.post(() -> {
                    try {
                        throw new MessagingException("Failed to send email: " + errorMessage, 
                                MessagingException.ERROR_CONNECTION_FAILED, e);
                    } catch (MessagingException ex) {
                        callback.onError(ex.getMessage());
                    }
                });
            }
        });
    }
    
    @Override
    public void cancel() {
        // SMTP sending can't really be cancelled once it's started
    }
    
    @Override
    public String getHandlerType() {
        return "email";
    }
}