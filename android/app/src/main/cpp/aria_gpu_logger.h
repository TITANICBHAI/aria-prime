// ─────────────────────────────────────────────────────────────────────────────
// aria_gpu_logger.h — Exhaustive logging for OpenCL and Vulkan backends.
//
// WHY THIS FILE EXISTS
// ─────────────────────
// After months of silent, hard-to-diagnose GPU failures we need structured,
// verbose, timestamp-tagged logs at every GPU decision point so a bug report
// or ADB pull can answer:
//   • Which backend was probed / selected / rejected and why?
//   • What OpenCL platform/device was found (version, CUs, driver string)?
//   • What Vulkan physical device was found (vendor, API version, memory)?
//   • What error code did clBuildProgram / vkCreateDevice return?
//   • Did we fall back to CPU? Was it because of OOM, missing driver, or
//     a failed kernel compilation?
//   • How long did each phase take (probe → select → load → first inference)?
//
// USAGE
// ─────
//   #include "aria_gpu_logger.h"
//
//   // Annotated macros — always use these instead of raw LOGI/LOGE.
//   ARIA_OCL_INFO("clBuildProgram succeeded on device '%s'", dev_name);
//   ARIA_OCL_ERR(CL_BUILD_PROGRAM_FAILURE, "kernel compile failed");
//   ARIA_VK_INFO("Physical device: %s (driver 0x%08x)", name, driver_ver);
//   ARIA_VK_ERR(VK_ERROR_DEVICE_LOST, "vkQueueSubmit");
//   ARIA_GPU_INFO("backend selected: %s", chosen);
//   ARIA_GPU_WARN("No GPU backend found — falling back to CPU");
//
//   // RAII phase timer (logs start and end with wall-clock elapsed):
//   {
//     GpuPhaseLogger _ph("OpenCL probe");
//     // ... probe code ...
//   }  // → logcat: "[ARIA-GPU] OpenCL probe → 42 ms"
//
// ─────────────────────────────────────────────────────────────────────────────

#pragma once

#include <android/log.h>
#include <chrono>
#include <string>

// ── Log tags ────────────────────────────────────────────────────────────────
#define ARIA_TAG_OCL  "ARIA-OpenCL"
#define ARIA_TAG_VK   "ARIA-Vulkan"
#define ARIA_TAG_GPU  "ARIA-GPU"

// ── Base macros ──────────────────────────────────────────────────────────────
// Include function name + line so grep of logcat immediately points to the site.
#define ARIA_OCL_INFO(fmt, ...) \
    __android_log_print(ANDROID_LOG_INFO,  ARIA_TAG_OCL, "[%s:%d] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define ARIA_OCL_WARN(fmt, ...) \
    __android_log_print(ANDROID_LOG_WARN,  ARIA_TAG_OCL, "[%s:%d] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define ARIA_OCL_DEBUG(fmt, ...) \
    __android_log_print(ANDROID_LOG_DEBUG, ARIA_TAG_OCL, "[%s:%d] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)

#define ARIA_VK_INFO(fmt, ...) \
    __android_log_print(ANDROID_LOG_INFO,  ARIA_TAG_VK,  "[%s:%d] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define ARIA_VK_WARN(fmt, ...) \
    __android_log_print(ANDROID_LOG_WARN,  ARIA_TAG_VK,  "[%s:%d] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define ARIA_VK_DEBUG(fmt, ...) \
    __android_log_print(ANDROID_LOG_DEBUG, ARIA_TAG_VK,  "[%s:%d] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)

#define ARIA_GPU_INFO(fmt, ...) \
    __android_log_print(ANDROID_LOG_INFO,  ARIA_TAG_GPU, "[%s:%d] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define ARIA_GPU_WARN(fmt, ...) \
    __android_log_print(ANDROID_LOG_WARN,  ARIA_TAG_GPU, "[%s:%d] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define ARIA_GPU_ERR(fmt, ...) \
    __android_log_print(ANDROID_LOG_ERROR, ARIA_TAG_GPU, "[%s:%d] " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)

// ── Error-annotating macros ──────────────────────────────────────────────────
// Log an OpenCL error code with its human-readable name alongside the message.
#define ARIA_OCL_ERR(cl_err_code, msg, ...) \
    __android_log_print(ANDROID_LOG_ERROR, ARIA_TAG_OCL, \
        "[%s:%d] " msg " → %s (%d)", \
        __FUNCTION__, __LINE__, ##__VA_ARGS__, \
        aria_ocl_error_string(cl_err_code), (int)(cl_err_code))

// Log a Vulkan result code with its human-readable name.
#define ARIA_VK_ERR(vk_result, msg, ...) \
    __android_log_print(ANDROID_LOG_ERROR, ARIA_TAG_VK, \
        "[%s:%d] " msg " → %s (%d)", \
        __FUNCTION__, __LINE__, ##__VA_ARGS__, \
        aria_vk_result_string(vk_result), (int)(vk_result))

// ── Separator helpers — makes log sections scannable in a wall of logcat ────
#define ARIA_GPU_SECTION(name) \
    __android_log_print(ANDROID_LOG_INFO, ARIA_TAG_GPU, \
        "══════════════ %s ══════════════", name)

// ── RAII phase timer ─────────────────────────────────────────────────────────
// Logs entry and exit of a named phase with wall-clock milliseconds elapsed.
// Use for: probe, platform init, device create, kernel build, model load, etc.
class GpuPhaseLogger {
public:
    explicit GpuPhaseLogger(const char* phase_name, const char* tag = ARIA_TAG_GPU)
        : name_(phase_name), tag_(tag),
          start_(std::chrono::steady_clock::now())
    {
        __android_log_print(ANDROID_LOG_INFO, tag_, "┌─ %s: start", name_);
    }

    ~GpuPhaseLogger() {
        auto end = std::chrono::steady_clock::now();
        long long ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start_).count();
        __android_log_print(ANDROID_LOG_INFO, tag_, "└─ %s: done (%lld ms)", name_, ms);
    }

    // Mark a sub-milestone without ending the phase.
    void milestone(const char* label) const {
        auto now = std::chrono::steady_clock::now();
        long long ms = std::chrono::duration_cast<std::chrono::milliseconds>(now - start_).count();
        __android_log_print(ANDROID_LOG_INFO, tag_, "   %s [+%lld ms]: %s", name_, ms, label);
    }

private:
    const char*   name_;
    const char*   tag_;
    std::chrono::steady_clock::time_point start_;
};

// ── OpenCL error code → string ───────────────────────────────────────────────
// Covers all standard OpenCL 3.0 error codes.
// Called by ARIA_OCL_ERR macro; also available directly for diagnostic dumps.
inline const char* aria_ocl_error_string(int err) {
    switch (err) {
        case    0: return "CL_SUCCESS";
        case   -1: return "CL_DEVICE_NOT_FOUND";
        case   -2: return "CL_DEVICE_NOT_AVAILABLE";
        case   -3: return "CL_COMPILER_NOT_AVAILABLE";
        case   -4: return "CL_MEM_OBJECT_ALLOCATION_FAILURE";
        case   -5: return "CL_OUT_OF_RESOURCES";
        case   -6: return "CL_OUT_OF_HOST_MEMORY";
        case   -7: return "CL_PROFILING_INFO_NOT_AVAILABLE";
        case   -8: return "CL_MEM_COPY_OVERLAP";
        case   -9: return "CL_IMAGE_FORMAT_MISMATCH";
        case  -10: return "CL_IMAGE_FORMAT_NOT_SUPPORTED";
        case  -11: return "CL_BUILD_PROGRAM_FAILURE";
        case  -12: return "CL_MAP_FAILURE";
        case  -13: return "CL_MISALIGNED_SUB_BUFFER_OFFSET";
        case  -14: return "CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST";
        case  -15: return "CL_COMPILE_PROGRAM_FAILURE";
        case  -16: return "CL_LINKER_NOT_AVAILABLE";
        case  -17: return "CL_LINK_PROGRAM_FAILURE";
        case  -18: return "CL_DEVICE_PARTITION_FAILED";
        case  -19: return "CL_KERNEL_ARG_INFO_NOT_AVAILABLE";
        case  -30: return "CL_INVALID_VALUE";
        case  -31: return "CL_INVALID_DEVICE_TYPE";
        case  -32: return "CL_INVALID_PLATFORM";
        case  -33: return "CL_INVALID_DEVICE";
        case  -34: return "CL_INVALID_CONTEXT";
        case  -35: return "CL_INVALID_QUEUE_PROPERTIES";
        case  -36: return "CL_INVALID_COMMAND_QUEUE";
        case  -37: return "CL_INVALID_HOST_PTR";
        case  -38: return "CL_INVALID_MEM_OBJECT";
        case  -39: return "CL_INVALID_IMAGE_FORMAT_DESCRIPTOR";
        case  -40: return "CL_INVALID_IMAGE_SIZE";
        case  -41: return "CL_INVALID_SAMPLER";
        case  -42: return "CL_INVALID_BINARY";
        case  -43: return "CL_INVALID_BUILD_OPTIONS";
        case  -44: return "CL_INVALID_PROGRAM";
        case  -45: return "CL_INVALID_PROGRAM_BUILD";
        case  -46: return "CL_INVALID_KERNEL_NAME";
        case  -47: return "CL_INVALID_KERNEL_DEFINITION";
        case  -48: return "CL_INVALID_KERNEL";
        case  -49: return "CL_INVALID_ARG_INDEX";
        case  -50: return "CL_INVALID_ARG_VALUE";
        case  -51: return "CL_INVALID_ARG_SIZE";
        case  -52: return "CL_INVALID_KERNEL_ARGS";
        case  -53: return "CL_INVALID_WORK_DIMENSION";
        case  -54: return "CL_INVALID_WORK_GROUP_SIZE";
        case  -55: return "CL_INVALID_WORK_ITEM_SIZE";
        case  -56: return "CL_INVALID_GLOBAL_OFFSET";
        case  -57: return "CL_INVALID_EVENT_WAIT_LIST";
        case  -58: return "CL_INVALID_EVENT";
        case  -59: return "CL_INVALID_OPERATION";
        case  -60: return "CL_INVALID_GL_OBJECT";
        case  -61: return "CL_INVALID_BUFFER_SIZE";
        case  -62: return "CL_INVALID_MIP_LEVEL";
        case  -63: return "CL_INVALID_GLOBAL_WORK_SIZE";
        case  -64: return "CL_INVALID_PROPERTY";
        case  -65: return "CL_INVALID_IMAGE_DESCRIPTOR";
        case  -66: return "CL_INVALID_COMPILER_OPTIONS";
        case  -67: return "CL_INVALID_LINKER_OPTIONS";
        case  -68: return "CL_INVALID_DEVICE_PARTITION_COUNT";
        case  -69: return "CL_INVALID_PIPE_SIZE";
        case  -70: return "CL_INVALID_DEVICE_QUEUE";
        case  -71: return "CL_INVALID_SPEC_ID";
        case  -72: return "CL_MAX_SIZE_RESTRICTION_EXCEEDED";
        // Vendor extension codes (Mali-specific range)
        case -1000: return "CL_PLATFORM_NOT_FOUND_KHR";
        case -1001: return "CL_INVALID_D3D10_DEVICE_KHR";
        case -1002: return "CL_INVALID_D3D10_RESOURCE_KHR";
        case -1003: return "CL_D3D10_RESOURCE_ALREADY_ACQUIRED_KHR";
        case -1004: return "CL_D3D10_RESOURCE_NOT_ACQUIRED_KHR";
        default:
            return (err > 0) ? "CL_EXTENSION_SUCCESS(>0)" : "CL_UNKNOWN_ERROR";
    }
}

// ── Vulkan result → string ───────────────────────────────────────────────────
// Covers VkResult values used by Vulkan 1.0–1.3 and common extensions.
inline const char* aria_vk_result_string(int result) {
    switch (result) {
        case  0: return "VK_SUCCESS";
        case  1: return "VK_NOT_READY";
        case  2: return "VK_TIMEOUT";
        case  3: return "VK_EVENT_SET";
        case  4: return "VK_EVENT_RESET";
        case  5: return "VK_INCOMPLETE";
        case -1: return "VK_ERROR_OUT_OF_HOST_MEMORY";
        case -2: return "VK_ERROR_OUT_OF_DEVICE_MEMORY";
        case -3: return "VK_ERROR_INITIALIZATION_FAILED";
        case -4: return "VK_ERROR_DEVICE_LOST";
        case -5: return "VK_ERROR_MEMORY_MAP_FAILED";
        case -6: return "VK_ERROR_LAYER_NOT_PRESENT";
        case -7: return "VK_ERROR_EXTENSION_NOT_PRESENT";
        case -8: return "VK_ERROR_FEATURE_NOT_PRESENT";
        case -9: return "VK_ERROR_INCOMPATIBLE_DRIVER";
        case -10: return "VK_ERROR_TOO_MANY_OBJECTS";
        case -11: return "VK_ERROR_FORMAT_NOT_SUPPORTED";
        case -12: return "VK_ERROR_FRAGMENTED_POOL";
        case -13: return "VK_ERROR_UNKNOWN";
        // Vulkan 1.1
        case -1000069000: return "VK_ERROR_OUT_OF_POOL_MEMORY";
        case -1000072003: return "VK_ERROR_INVALID_EXTERNAL_HANDLE";
        // Vulkan 1.2
        case -1000161000: return "VK_ERROR_FRAGMENTATION";
        case -1000257000: return "VK_ERROR_INVALID_OPAQUE_CAPTURE_ADDRESS";
        // Vulkan 1.3
        case  1000297001: return "VK_PIPELINE_COMPILE_REQUIRED";
        // KHR surface / swapchain
        case -1000000000: return "VK_ERROR_SURFACE_LOST_KHR";
        case -1000000001: return "VK_ERROR_NATIVE_WINDOW_IN_USE_KHR";
        case  1000001003: return "VK_SUBOPTIMAL_KHR";
        case -1000001004: return "VK_ERROR_OUT_OF_DATE_KHR";
        // Validation
        case -1000011001: return "VK_ERROR_VALIDATION_FAILED_EXT";
        case -1000012000: return "VK_ERROR_INVALID_SHADER_NV";
        // Pipeline cache
        case -1000298000: return "VK_ERROR_PIPELINE_COMPILE_REQUIRED_EXT";
        default:
            return (result > 0) ? "VK_INCOMPLETE_OR_EXTENSION" : "VK_UNKNOWN_ERROR";
    }
}

// ── Vulkan physical device type → string ─────────────────────────────────────
inline const char* aria_vk_device_type_string(int type) {
    switch (type) {
        case 0: return "OTHER";
        case 1: return "INTEGRATED_GPU";
        case 2: return "DISCRETE_GPU";
        case 3: return "VIRTUAL_GPU";
        case 4: return "CPU";
        default: return "UNKNOWN";
    }
}

// ── Vendor ID → human-readable string ────────────────────────────────────────
inline const char* aria_vk_vendor_string(uint32_t vendor_id) {
    switch (vendor_id) {
        case 0x1002: return "AMD";
        case 0x1010: return "ImgTec";
        case 0x10DE: return "NVIDIA";
        case 0x13B5: return "ARM (Mali)";
        case 0x5143: return "Qualcomm (Adreno)";
        case 0x8086: return "Intel";
        case 0x106B: return "Apple";
        default:     return "Unknown vendor";
    }
}
