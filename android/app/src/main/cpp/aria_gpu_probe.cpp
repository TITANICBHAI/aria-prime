// ─────────────────────────────────────────────────────────────────────────────
// aria_gpu_probe.cpp — Deep runtime probe for OpenCL and Vulkan backends.
//
// PURPOSE
// ───────
// After months of silent GPU failures the team needed one place that answers
// definitively at runtime:
//   1. Is libOpenCL.so present at /vendor/lib64/ (or any vendor path)?
//   2. How many OpenCL platforms/devices are there, and what are their caps?
//   3. Is libvulkan.so (Android native Vulkan loader) reachable?
//   4. How many Vulkan physical devices are there and what are their limits?
//   5. Do both backends survive a round-trip context creation without crashing?
//
// APPROACH
// ────────
// Both libraries are opened with dlopen() at runtime — never linked at
// compile time here.  That means:
//   • This file compiles on ANY build machine (no OpenCL or Vulkan SDK needed).
//   • On a device without libOpenCL.so the probe returns a clean JSON error
//     instead of crashing.
//   • The Kotlin side (DiagnosticsScreen, SettingsScreen) can display the full
//     GPU capability report without the user needing to open logcat.
//
// JNI ENTRY POINTS
// ────────────────
//   nativeProbeGpu()        → full JSON string (call once at startup)
//   nativeGetGpuCapJson()   → same result, cached after first call
//
// Called from LlamaEngine.kt or a dedicated GpuProbe.kt companion object.
// ─────────────────────────────────────────────────────────────────────────────

#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <string>
#include <sstream>
#include <cstring>
#include <cstdint>
#include <vector>
#include <atomic>
#include <pthread.h>

#include "aria_gpu_logger.h"

// ── OpenCL type aliases — avoid including CL/cl.h (would pull in the stub) ───
// We resolve all symbols dynamically via dlopen, so we only need the types.
using cl_int            = int32_t;
using cl_uint           = uint32_t;
using cl_ulong          = uint64_t;
using cl_platform_id    = void*;
using cl_device_id      = void*;
using cl_device_type    = uint64_t;
using cl_platform_info  = uint32_t;
using cl_device_info    = uint32_t;

// Selected CL_DEVICE_INFO constants (from CL/cl.h — copied to avoid header dep)
static constexpr cl_device_info kCL_DEVICE_TYPE               = 0x1000;
static constexpr cl_device_info kCL_DEVICE_VENDOR_ID          = 0x1001;
static constexpr cl_device_info kCL_DEVICE_MAX_COMPUTE_UNITS  = 0x1002;
static constexpr cl_device_info kCL_DEVICE_MAX_CLOCK_FREQUENCY= 0x100C;
static constexpr cl_device_info kCL_DEVICE_GLOBAL_MEM_SIZE    = 0x101F;
static constexpr cl_device_info kCL_DEVICE_LOCAL_MEM_SIZE     = 0x1023;
static constexpr cl_device_info kCL_DEVICE_MAX_WORK_GROUP_SIZE= 0x1004;
static constexpr cl_device_info kCL_DEVICE_NAME               = 0x102B;
static constexpr cl_device_info kCL_DEVICE_VERSION            = 0x102F;
static constexpr cl_device_info kCL_DEVICE_OPENCL_C_VERSION   = 0x103D;
static constexpr cl_device_info kCL_DRIVER_VERSION            = 0x102D;
static constexpr cl_device_info kCL_DEVICE_EXTENSIONS         = 0x1030;
static constexpr cl_device_info kCL_DEVICE_MAX_WORK_ITEM_SIZES= 0x1005;
static constexpr cl_device_info kCL_DEVICE_MAX_WORK_ITEM_DIMENSIONS = 0x1003;
static constexpr cl_device_info kCL_DEVICE_IMAGE_SUPPORT      = 0x1016;
static constexpr cl_device_info kCL_DEVICE_HALF_FP_CONFIG     = 0x101A;
static constexpr cl_device_info kCL_DEVICE_SINGLE_FP_CONFIG   = 0x101B;
static constexpr cl_platform_info kCL_PLATFORM_NAME           = 0x0902;
static constexpr cl_platform_info kCL_PLATFORM_VERSION        = 0x0901;
static constexpr cl_platform_info kCL_PLATFORM_VENDOR         = 0x0903;
static constexpr cl_platform_info kCL_PLATFORM_EXTENSIONS     = 0x0904;
static constexpr cl_device_type   kCL_DEVICE_TYPE_ALL         = 0xFFFFFFFF;

// ── Vulkan type aliases — avoid including vulkan.h ───────────────────────────
using VkResult                = int32_t;
using VkPhysicalDevice        = void*;
using VkInstance              = void*;
using VkStructureType         = int32_t;

static constexpr VkStructureType kVK_STRUCTURE_TYPE_APPLICATION_INFO      = 0;
static constexpr VkStructureType kVK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO  = 1;
static constexpr VkResult        kVK_SUCCESS                               = 0;

struct VkApplicationInfo {
    VkStructureType sType;
    const void*     pNext;
    const char*     pApplicationName;
    uint32_t        applicationVersion;
    const char*     pEngineName;
    uint32_t        engineVersion;
    uint32_t        apiVersion;
};

struct VkInstanceCreateInfo {
    VkStructureType          sType;
    const void*              pNext;
    uint32_t                 flags;
    const VkApplicationInfo* pApplicationInfo;
    uint32_t                 enabledLayerCount;
    const char* const*       ppEnabledLayerNames;
    uint32_t                 enabledExtensionCount;
    const char* const*       ppEnabledExtensionNames;
};

struct VkPhysicalDeviceProperties {
    uint32_t apiVersion;
    uint32_t driverVersion;
    uint32_t vendorID;
    uint32_t deviceID;
    uint32_t deviceType;        // VkPhysicalDeviceType enum
    char     deviceName[256];
    uint8_t  pipelineCacheUUID[16];
    uint8_t  limits[504];       // VkPhysicalDeviceLimits — opaque blob for our purposes
    uint8_t  sparseProperties[20];
};

struct VkMemoryHeap {
    uint64_t size;
    uint32_t flags;
};

struct VkMemoryType {
    uint32_t propertyFlags;
    uint32_t heapIndex;
};

struct VkPhysicalDeviceMemoryProperties {
    uint32_t     memoryTypeCount;
    VkMemoryType memoryTypes[32];
    uint32_t     memoryHeapCount;
    VkMemoryHeap memoryHeaps[16];
};

struct VkQueueFamilyProperties {
    uint32_t queueFlags;
    uint32_t queueCount;
    uint32_t timestampValidBits;
    uint32_t minImageTransferGranularity[3];
};
static constexpr uint32_t kVK_QUEUE_COMPUTE_BIT  = 0x00000002;
static constexpr uint32_t kVK_QUEUE_GRAPHICS_BIT = 0x00000001;
static constexpr uint32_t kVK_API_VERSION_1_1     = (1u << 22) | (1u << 12) | 0u;

// ── Function pointer typedefs (resolved via dlsym) ───────────────────────────
// OpenCL
using Fn_clGetPlatformIDs  = cl_int (*)(cl_uint, cl_platform_id*, cl_uint*);
using Fn_clGetPlatformInfo = cl_int (*)(cl_platform_id, cl_platform_info, size_t, void*, size_t*);
using Fn_clGetDeviceIDs    = cl_int (*)(cl_platform_id, cl_device_type, cl_uint, cl_device_id*, cl_uint*);
using Fn_clGetDeviceInfo   = cl_int (*)(cl_device_id, cl_device_info, size_t, void*, size_t*);
// Vulkan
using Fn_vkCreateInstance             = VkResult (*)(const VkInstanceCreateInfo*, const void*, VkInstance*);
using Fn_vkDestroyInstance            = void     (*)(VkInstance, const void*);
using Fn_vkEnumeratePhysicalDevices   = VkResult (*)(VkInstance, uint32_t*, VkPhysicalDevice*);
using Fn_vkGetPhysicalDeviceProperties= void     (*)(VkPhysicalDevice, VkPhysicalDeviceProperties*);
using Fn_vkGetPhysicalDeviceMemoryProperties = void (*)(VkPhysicalDevice, VkPhysicalDeviceMemoryProperties*);
using Fn_vkGetPhysicalDeviceQueueFamilyProperties = void (*)(VkPhysicalDevice, uint32_t*, VkQueueFamilyProperties*);

// ── JSON escape helper ────────────────────────────────────────────────────────
static std::string json_escape(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 8);
    for (char c : s) {
        if (c == '"')  { out += "\\\""; }
        else if (c == '\\') { out += "\\\\"; }
        else if (c == '\n') { out += "\\n"; }
        else if (c == '\r') { out += "\\r"; }
        else if (c == '\t') { out += "\\t"; }
        else { out += c; }
    }
    return out;
}

// ── OpenCL probe ─────────────────────────────────────────────────────────────

static std::string probe_opencl() {
    GpuPhaseLogger ph("OpenCL probe", ARIA_TAG_OCL);
    std::ostringstream j;

    // Try every known vendor path for libOpenCL.so on Android
    static const char* kOCL_PATHS[] = {
        "/vendor/lib64/libOpenCL.so",
        "/vendor/lib/libOpenCL.so",
        "/system/lib64/libOpenCL.so",
        "/system/lib/libOpenCL.so",
        "/system/vendor/lib64/libOpenCL.so",
        "libOpenCL.so",
        nullptr
    };

    void* lib = nullptr;
    const char* loaded_from = nullptr;
    for (int i = 0; kOCL_PATHS[i]; ++i) {
        lib = dlopen(kOCL_PATHS[i], RTLD_NOW | RTLD_LOCAL);
        if (lib) { loaded_from = kOCL_PATHS[i]; break; }
        ARIA_OCL_DEBUG("dlopen(%s): %s", kOCL_PATHS[i], dlerror());
    }

    j << "\"opencl\":{";
    if (!lib) {
        ARIA_OCL_WARN("libOpenCL.so not found on any vendor path — GPU backend UNAVAILABLE");
        j << "\"available\":false,"
          << "\"error\":\"libOpenCL.so not found in /vendor/lib64 or any known path\","
          << "\"platforms\":[]"
          << "}";
        return j.str();
    }

    ARIA_OCL_INFO("libOpenCL.so loaded from: %s", loaded_from);
    j << "\"available\":true,"
      << "\"library\":\"" << json_escape(loaded_from) << "\",";

    auto* GetPlatformIDs  = reinterpret_cast<Fn_clGetPlatformIDs>(dlsym(lib,  "clGetPlatformIDs"));
    auto* GetPlatformInfo = reinterpret_cast<Fn_clGetPlatformInfo>(dlsym(lib, "clGetPlatformInfo"));
    auto* GetDeviceIDs    = reinterpret_cast<Fn_clGetDeviceIDs>(dlsym(lib,    "clGetDeviceIDs"));
    auto* GetDeviceInfo   = reinterpret_cast<Fn_clGetDeviceInfo>(dlsym(lib,   "clGetDeviceInfo"));

    if (!GetPlatformIDs || !GetPlatformInfo || !GetDeviceIDs || !GetDeviceInfo) {
        ARIA_OCL_ERR(-1, "Required OpenCL symbols not resolved in libOpenCL.so");
        j << "\"error\":\"missing required cl* symbols\",\"platforms\":[]}";
        dlclose(lib);
        return j.str();
    }

    cl_uint n_platforms = 0;
    cl_int  rc = GetPlatformIDs(0, nullptr, &n_platforms);
    if (rc != 0 || n_platforms == 0) {
        ARIA_OCL_ERR(rc, "clGetPlatformIDs returned 0 platforms — driver present but not functional");
        j << "\"error\":\"" << aria_ocl_error_string(rc) << " (no platforms)\","
          << "\"platform_count\":0,\"platforms\":[]}";
        dlclose(lib);
        return j.str();
    }

    ARIA_OCL_INFO("Platform count: %u", n_platforms);
    std::vector<cl_platform_id> platforms(n_platforms);
    GetPlatformIDs(n_platforms, platforms.data(), nullptr);

    j << "\"platform_count\":" << n_platforms << ",\"platforms\":[";

    auto getStr = [&](auto fn, void* obj, cl_uint key) -> std::string {
        size_t sz = 0;
        if (fn(obj, key, 0, nullptr, &sz) != 0 || sz == 0) return "";
        std::string buf(sz, '\0');
        fn(obj, key, sz, &buf[0], nullptr);
        while (!buf.empty() && buf.back() == '\0') buf.pop_back();
        return buf;
    };

    for (cl_uint pi = 0; pi < n_platforms; ++pi) {
        if (pi > 0) j << ",";
        cl_platform_id plat = platforms[pi];

        std::string plat_name    = getStr(GetPlatformInfo, plat, kCL_PLATFORM_NAME);
        std::string plat_version = getStr(GetPlatformInfo, plat, kCL_PLATFORM_VERSION);
        std::string plat_vendor  = getStr(GetPlatformInfo, plat, kCL_PLATFORM_VENDOR);
        std::string plat_exts    = getStr(GetPlatformInfo, plat, kCL_PLATFORM_EXTENSIONS);

        ARIA_OCL_INFO("Platform[%u]: name='%s' version='%s' vendor='%s'",
                      pi, plat_name.c_str(), plat_version.c_str(), plat_vendor.c_str());

        j << "{\"name\":\"" << json_escape(plat_name) << "\","
          << "\"version\":\"" << json_escape(plat_version) << "\","
          << "\"vendor\":\"" << json_escape(plat_vendor) << "\","
          << "\"extensions_snippet\":\"" << json_escape(plat_exts.substr(0, 200)) << "\",";

        cl_uint n_devices = 0;
        rc = GetDeviceIDs(plat, kCL_DEVICE_TYPE_ALL, 0, nullptr, &n_devices);
        if (rc != 0 || n_devices == 0) {
            ARIA_OCL_ERR(rc, "clGetDeviceIDs: no devices on platform[%u]", pi);
            j << "\"device_count\":0,\"devices\":[]}";
            continue;
        }

        std::vector<cl_device_id> devices(n_devices);
        GetDeviceIDs(plat, kCL_DEVICE_TYPE_ALL, n_devices, devices.data(), nullptr);
        ARIA_OCL_INFO("  Device count: %u", n_devices);
        j << "\"device_count\":" << n_devices << ",\"devices\":[";

        auto getDevStr = [&](cl_device_id dev, cl_device_info key) -> std::string {
            size_t sz = 0;
            if (GetDeviceInfo(dev, key, 0, nullptr, &sz) != 0 || sz == 0) return "";
            std::string buf(sz, '\0');
            GetDeviceInfo(dev, key, sz, &buf[0], nullptr);
            while (!buf.empty() && buf.back() == '\0') buf.pop_back();
            return buf;
        };

        for (cl_uint di = 0; di < n_devices; ++di) {
            if (di > 0) j << ",";
            cl_device_id dev = devices[di];

            std::string dev_name       = getDevStr(dev, kCL_DEVICE_NAME);
            std::string dev_version    = getDevStr(dev, kCL_DEVICE_VERSION);
            std::string driver_version = getDevStr(dev, kCL_DRIVER_VERSION);
            std::string cl_c_version   = getDevStr(dev, kCL_DEVICE_OPENCL_C_VERSION);
            std::string extensions     = getDevStr(dev, kCL_DEVICE_EXTENSIONS);

            cl_uint  max_cu    = 0; GetDeviceInfo(dev, kCL_DEVICE_MAX_COMPUTE_UNITS,   sizeof(max_cu),   &max_cu,   nullptr);
            cl_uint  max_freq  = 0; GetDeviceInfo(dev, kCL_DEVICE_MAX_CLOCK_FREQUENCY, sizeof(max_freq), &max_freq, nullptr);
            cl_ulong gmem      = 0; GetDeviceInfo(dev, kCL_DEVICE_GLOBAL_MEM_SIZE,     sizeof(gmem),     &gmem,     nullptr);
            cl_ulong lmem      = 0; GetDeviceInfo(dev, kCL_DEVICE_LOCAL_MEM_SIZE,      sizeof(lmem),     &lmem,     nullptr);
            size_t   wg_size   = 0; GetDeviceInfo(dev, kCL_DEVICE_MAX_WORK_GROUP_SIZE, sizeof(wg_size),  &wg_size,  nullptr);
            cl_uint  img_supp  = 0; GetDeviceInfo(dev, kCL_DEVICE_IMAGE_SUPPORT,       sizeof(img_supp), &img_supp, nullptr);
            cl_uint  wi_dims   = 0; GetDeviceInfo(dev, kCL_DEVICE_MAX_WORK_ITEM_DIMENSIONS, sizeof(wi_dims), &wi_dims, nullptr);
            cl_device_type dev_type = 0;
            GetDeviceInfo(dev, kCL_DEVICE_TYPE, sizeof(dev_type), &dev_type, nullptr);

            const char* type_str = "UNKNOWN";
            if (dev_type & 4) type_str = "GPU";
            else if (dev_type & 2) type_str = "CPU";
            else if (dev_type & 8) type_str = "ACCELERATOR";
            else if (dev_type & 1) type_str = "DEFAULT";

            ARIA_OCL_INFO("  Device[%u]: '%s' type=%s CUs=%u freq=%u MHz"
                          " gmem=%llu MB wg_size=%zu driver='%s'",
                          di, dev_name.c_str(), type_str, max_cu, max_freq,
                          (unsigned long long)(gmem >> 20), wg_size,
                          driver_version.c_str());
            ARIA_OCL_INFO("    cl_version='%s' cl_c='%s' images=%u",
                          dev_version.c_str(), cl_c_version.c_str(), img_supp);

            j << "{\"name\":\""           << json_escape(dev_name) << "\","
              << "\"type\":\""            << type_str << "\","
              << "\"cl_version\":\""      << json_escape(dev_version) << "\","
              << "\"cl_c_version\":\""    << json_escape(cl_c_version) << "\","
              << "\"driver_version\":\""  << json_escape(driver_version) << "\","
              << "\"max_compute_units\":" << max_cu << ","
              << "\"max_clock_mhz\":"     << max_freq << ","
              << "\"global_mem_mb\":"     << (gmem >> 20) << ","
              << "\"local_mem_kb\":"      << (lmem >> 10) << ","
              << "\"max_work_group_size\":" << wg_size << ","
              << "\"image_support\":"     << (img_supp ? "true" : "false") << ","
              << "\"extensions_snippet\":\"" << json_escape(extensions.substr(0, 300)) << "\""
              << "}";
        }
        j << "]}";
    }
    j << "]}";

    dlclose(lib);
    return j.str();
}

// ── Vulkan probe ──────────────────────────────────────────────────────────────

static std::string probe_vulkan() {
    GpuPhaseLogger ph("Vulkan probe", ARIA_TAG_VK);
    std::ostringstream j;

    static const char* kVK_PATHS[] = {
        "/system/lib64/libvulkan.so",
        "/system/lib/libvulkan.so",
        "libvulkan.so",
        nullptr
    };

    void* lib = nullptr;
    const char* loaded_from = nullptr;
    for (int i = 0; kVK_PATHS[i]; ++i) {
        lib = dlopen(kVK_PATHS[i], RTLD_NOW | RTLD_LOCAL);
        if (lib) { loaded_from = kVK_PATHS[i]; break; }
        ARIA_VK_DEBUG("dlopen(%s): %s", kVK_PATHS[i], dlerror());
    }

    j << "\"vulkan\":{";
    if (!lib) {
        ARIA_VK_WARN("libvulkan.so not found — Vulkan UNAVAILABLE on this device");
        j << "\"available\":false,"
          << "\"error\":\"libvulkan.so not found\","
          << "\"devices\":[]"
          << "}";
        return j.str();
    }

    ARIA_VK_INFO("libvulkan.so loaded from: %s", loaded_from);

    auto* vkCreateInstance = reinterpret_cast<Fn_vkCreateInstance>(
        dlsym(lib, "vkCreateInstance"));
    auto* vkDestroyInstance = reinterpret_cast<Fn_vkDestroyInstance>(
        dlsym(lib, "vkDestroyInstance"));
    auto* vkEnumeratePhysicalDevices = reinterpret_cast<Fn_vkEnumeratePhysicalDevices>(
        dlsym(lib, "vkEnumeratePhysicalDevices"));
    auto* vkGetPhysicalDeviceProperties = reinterpret_cast<Fn_vkGetPhysicalDeviceProperties>(
        dlsym(lib, "vkGetPhysicalDeviceProperties"));
    auto* vkGetPhysicalDeviceMemoryProperties = reinterpret_cast<Fn_vkGetPhysicalDeviceMemoryProperties>(
        dlsym(lib, "vkGetPhysicalDeviceMemoryProperties"));
    auto* vkGetPhysicalDeviceQueueFamilyProperties = reinterpret_cast<Fn_vkGetPhysicalDeviceQueueFamilyProperties>(
        dlsym(lib, "vkGetPhysicalDeviceQueueFamilyProperties"));

    if (!vkCreateInstance || !vkDestroyInstance || !vkEnumeratePhysicalDevices
        || !vkGetPhysicalDeviceProperties || !vkGetPhysicalDeviceMemoryProperties) {
        ARIA_VK_ERR(-1, "Required vk* symbols not resolved — loader present but incomplete");
        j << "\"available\":false,"
          << "\"library\":\"" << json_escape(loaded_from) << "\","
          << "\"error\":\"vk symbols not resolved\","
          << "\"devices\":[]"
          << "}";
        dlclose(lib);
        return j.str();
    }

    j << "\"available\":true,"
      << "\"library\":\"" << json_escape(loaded_from) << "\",";

    // Create a minimal instance (no layers, no extensions)
    VkApplicationInfo app_info{};
    app_info.sType              = kVK_STRUCTURE_TYPE_APPLICATION_INFO;
    app_info.pNext              = nullptr;
    app_info.pApplicationName   = "aria-probe";
    app_info.applicationVersion = 1;
    app_info.pEngineName        = "aria";
    app_info.engineVersion      = 1;
    app_info.apiVersion         = kVK_API_VERSION_1_1;

    VkInstanceCreateInfo ci{};
    ci.sType                   = kVK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ci.pNext                   = nullptr;
    ci.flags                   = 0;
    ci.pApplicationInfo        = &app_info;
    ci.enabledLayerCount       = 0;
    ci.ppEnabledLayerNames     = nullptr;
    ci.enabledExtensionCount   = 0;
    ci.ppEnabledExtensionNames = nullptr;

    VkInstance instance = nullptr;
    VkResult rc = vkCreateInstance(&ci, nullptr, &instance);
    if (rc != kVK_SUCCESS || !instance) {
        ARIA_VK_ERR(rc, "vkCreateInstance failed — Vulkan loader present but driver non-functional");
        j << "\"instance_create_result\":\"" << aria_vk_result_string(rc) << "\","
          << "\"error\":\"vkCreateInstance failed\","
          << "\"devices\":[]"
          << "}";
        dlclose(lib);
        return j.str();
    }

    ARIA_VK_INFO("VkInstance created successfully");
    j << "\"instance_create_result\":\"VK_SUCCESS\",";

    uint32_t dev_count = 0;
    rc = vkEnumeratePhysicalDevices(instance, &dev_count, nullptr);
    if (rc != kVK_SUCCESS || dev_count == 0) {
        ARIA_VK_ERR(rc, "vkEnumeratePhysicalDevices: 0 devices");
        j << "\"device_count\":0,\"devices\":[]}";
        vkDestroyInstance(instance, nullptr);
        dlclose(lib);
        return j.str();
    }

    std::vector<VkPhysicalDevice> devs(dev_count);
    vkEnumeratePhysicalDevices(instance, &dev_count, devs.data());
    ARIA_VK_INFO("Physical device count: %u", dev_count);
    j << "\"device_count\":" << dev_count << ",\"devices\":[";

    for (uint32_t di = 0; di < dev_count; ++di) {
        if (di > 0) j << ",";
        VkPhysicalDeviceProperties props{};
        vkGetPhysicalDeviceProperties(devs[di], &props);

        uint32_t api_major = (props.apiVersion >> 22) & 0x3FF;
        uint32_t api_minor = (props.apiVersion >> 12) & 0x3FF;
        uint32_t api_patch =  props.apiVersion        & 0xFFF;

        ARIA_VK_INFO("PhysicalDevice[%u]: '%s' type=%s vendor=%s (0x%04x)"
                     " api=%u.%u.%u driver=0x%08x",
                     di, props.deviceName,
                     aria_vk_device_type_string((int)props.deviceType),
                     aria_vk_vendor_string(props.vendorID),
                     props.vendorID,
                     api_major, api_minor, api_patch,
                     props.driverVersion);

        VkPhysicalDeviceMemoryProperties mem{};
        vkGetPhysicalDeviceMemoryProperties(devs[di], &mem);

        uint64_t device_local_mb = 0;
        uint64_t host_visible_mb = 0;
        for (uint32_t hi = 0; hi < mem.memoryHeapCount; ++hi) {
            // VK_MEMORY_HEAP_DEVICE_LOCAL_BIT = 1
            if (mem.memoryHeaps[hi].flags & 1)
                device_local_mb += mem.memoryHeaps[hi].size >> 20;
            else
                host_visible_mb += mem.memoryHeaps[hi].size >> 20;
        }
        ARIA_VK_INFO("  Memory: device-local=%llu MB host-visible=%llu MB",
                     (unsigned long long)device_local_mb,
                     (unsigned long long)host_visible_mb);

        bool has_compute = false;
        if (vkGetPhysicalDeviceQueueFamilyProperties) {
            uint32_t qf_count = 0;
            vkGetPhysicalDeviceQueueFamilyProperties(devs[di], &qf_count, nullptr);
            if (qf_count > 0) {
                std::vector<VkQueueFamilyProperties> qfams(qf_count);
                vkGetPhysicalDeviceQueueFamilyProperties(devs[di], &qf_count, qfams.data());
                for (auto& qf : qfams) {
                    if (qf.queueFlags & kVK_QUEUE_COMPUTE_BIT) { has_compute = true; break; }
                }
            }
        }
        ARIA_VK_INFO("  Compute queue: %s", has_compute ? "YES" : "NO");

        j << "{\"name\":\""          << json_escape(props.deviceName) << "\","
          << "\"type\":\""           << aria_vk_device_type_string((int)props.deviceType) << "\","
          << "\"vendor\":\""         << aria_vk_vendor_string(props.vendorID) << "\","
          << "\"vendor_id\":"        << props.vendorID << ","
          << "\"device_id\":"        << props.deviceID << ","
          << "\"api_version\":\""    << api_major << "." << api_minor << "." << api_patch << "\","
          << "\"driver_version\":"   << props.driverVersion << ","
          << "\"device_local_mb\":"  << device_local_mb << ","
          << "\"host_visible_mb\":"  << host_visible_mb << ","
          << "\"has_compute_queue\":" << (has_compute ? "true" : "false")
          << "}";
    }
    j << "]}";

    vkDestroyInstance(instance, nullptr);
    dlclose(lib);
    return j.str();
}

// ── Cached result ─────────────────────────────────────────────────────────────

static std::string  g_probe_cache;
static pthread_once_t g_probe_once = PTHREAD_ONCE_INIT;

static void run_probe_once() {
    ARIA_GPU_SECTION("GPU backend probe — ARIA startup");
    std::ostringstream j;
    j << "{" << probe_opencl() << "," << probe_vulkan() << "}";
    g_probe_cache = j.str();
    ARIA_GPU_INFO("Probe complete — JSON length %zu chars", g_probe_cache.size());
}

// ── JNI entry points ──────────────────────────────────────────────────────────

extern "C" {

// Call once at startup (or any time) — result is cached.
// Kotlin signature: external fun nativeProbeGpu(): String
JNIEXPORT jstring JNICALL
Java_com_ariaagent_mobile_core_ai_LlamaEngine_nativeProbeGpu(
    JNIEnv* env, jobject /*thiz*/)
{
    pthread_once(&g_probe_once, run_probe_once);
    return env->NewStringUTF(g_probe_cache.c_str());
}

// Force a fresh probe (e.g., after toggling a setting).
// Kotlin signature: external fun nativeReprobeGpu(): String
JNIEXPORT jstring JNICALL
Java_com_ariaagent_mobile_core_ai_LlamaEngine_nativeReprobeGpu(
    JNIEnv* env, jobject /*thiz*/)
{
    g_probe_once = PTHREAD_ONCE_INIT;
    pthread_once(&g_probe_once, run_probe_once);
    return env->NewStringUTF(g_probe_cache.c_str());
}

} // extern "C"
