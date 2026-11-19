# KitsuneSU

<img align='right' src='kitsune_logo.png' width='220px' alt="KitsuneSU logo">

[Русский](../README.md) | **English**

A unified kernel-based root solution for Android with dual root engines (**Kitsune** `init_boot` + **Kitsunepatch** `boot`), built-in hardware bootloop protection (**Kitsune Bootloop Guard**), and a modern interface.

[![Latest Release](https://img.shields.io/badge/Release-v3.1.0-orange.svg?logo=github)](https://github.com/Komarku/KitsuneSU/releases)
[![Android](https://img.shields.io/badge/Android-12%20--%2016-green.svg?logo=android)](https://github.com/Komarku/KitsuneSU)
[![KernelPatch](https://img.shields.io/badge/KernelPatch-0.13.9-blue.svg)](https://github.com/Komarku/KitsuneSU)
[![License: GPL 3.0 or later](https://img.shields.io/badge/License-GPL%203.0%20or%20later-purple.svg?logo=gnu)](/LICENSE)

---

## 🌟 Features

1. **Dual Root Engine**:
   - **Kitsune (`init_boot`)**: GKI 2.0 / LKM module integration for modern Android devices.
   - **Kitsunepatch (`boot`)**: Full KernelPatch 0.13.9 engine with SuperKey authorization and embedded KPM support directly in `boot.img`.
2. **Built-in Bootloop Guard (Kitsune Bootloop Guard)**:
   - **Not a module!** Integrated directly into the early initialization phase (`post-fs-data.d`) before any module loads or mounts.
   - **Hardware Boot Counter (`.boot_count`)**: Automatically records failed boots and repeated restarts.
   - **Watchdog Daemon**: Monitors Zygote crash loops and boot animation hangs (100-second failsafe timeout).
   - **Precise Fault Isolation**: Automatically disables the faulty module (`disable` flag) without wiping userdata or causing bootloops. Seamlessly falls back to Safe Mode upon critical crashes.
   - **In-App Recovery Alert**: Alerts the user on next manager startup regarding which module was rescued.
3. **Static `ksuinit` (16KB Page-Size)**:
   - Full compatibility with Android 14, 15, and 16 devices utilizing 16 KB memory pages. Completely prevents dynamic linker crashes during early boot.
4. **App Profile (Granular Root Isolation)**:
   - Per-application permission management: run root applications in isolated namespaces ("keep root access in a cage").
5. **Built-in Zygisk (NyaZygisk)**:
   - Native Zygisk support out-of-the-box without requiring external modules.
6. **Kitsunepatch Settings**:
   - Dedicated management screen for KernelPatch 0.13.9: SuperKey, PathHide, NetIsolate, and KPM.
7. **Dual UI (Material 3 Expressive & Miuix)**:
   - Clean UI, Liquid Glass effect support, AMOLED pure black theme, and persistent bottom navigation bar.
8. **OTA Updates**:
   - Direct built-in updates from official GitHub releases.

---

## 📱 Compatibility

- **GKI 2.0**: Linux kernels 5.10, 5.15, 6.1, 6.6 and newer.
- **Standard `boot` partition**: Legacy or vendor kernels supported via **Kitsunepatch** mode (KernelPatch 0.13.9).
- **Architecture**: `arm64-v8a`.

---

## 📊 Kernel Compatibility Status

| Scenario | Installation Method | Notes |
|---|---|---|
| GKI devices (kernel 5.10+) | **Kitsune** mode (`init_boot`) | Recommended LKM mode |
| Devices without `init_boot` | **Kitsunepatch** mode (`boot`) | KernelPatch 0.13.9 with SuperKey authentication |
| KPM features required | Requires `CONFIG_KPM=y` | KernelPatch KPM module support |

---

## 📥 Installation

1. Install the APK from **[Releases](https://github.com/Komarku/KitsuneSU/releases)** on your device.
2. Open **KitsuneSU**:
   - **Method 1 (`init_boot` mode)**: Tap "Install" $\rightarrow$ select your stock `init_boot.img` $\rightarrow$ flash via Fastboot:
     ```bash
     fastboot flash init_boot kernelsu_patched_xxx.img
     ```
   - **Method 2 (`boot` / Kitsunepatch mode)**: Open "Boot Patcher" $\rightarrow$ select `boot.img` $\rightarrow$ set SuperKey $\rightarrow$ tap "Patch boot.img" $\rightarrow$ flash via Fastboot:
     ```bash
     fastboot flash boot kitsunepatch_patched_xxx.img
     ```

---

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/Komarku/KitsuneSU.git
cd KitsuneSU/manager

# Build release APK
./gradlew assembleRelease
```
The output APK will be placed in `manager/app/build/outputs/apk/release/`.

---

## 📜 License
KitsuneSU is licensed under [GPL-3.0 or later](LICENSE).
