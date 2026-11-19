# KitsuneSU

<img align='right' src='kitsune_logo.png' width='220px' alt="KitsuneSU logo">

[Русский](../README.md) | **English**

A unified kernel-based root solution for Android combining **Kitsune** (`init_boot`) and **Kitsunepatch** (`boot`) with built-in **Kitsune Shield**.

[![Latest Release](https://img.shields.io/badge/Release-v3.1.0-orange.svg?logo=github)](https://github.com/Komarku/KitsuneSU/releases)
[![Android](https://img.shields.io/badge/Android-12%20--%2016-green.svg?logo=android)](https://github.com/Komarku/KitsuneSU)
[![KernelPatch](https://img.shields.io/badge/KernelPatch-0.13.9-blue.svg)](https://github.com/Komarku/KitsuneSU)
[![License: GPL 3.0 or later](https://img.shields.io/badge/License-GPL%203.0%20or%20later-purple.svg?logo=gnu)](/LICENSE)

---

## 🌟 Features

1. **Dual Root Engine**:
   - **Kitsune (`init_boot`)**: GKI / LKM module integration for modern devices.
   - **Kitsunepatch (`boot`)**: Full KernelPatch 0.13.9 engine with SuperKey and embedded KPM modules.
2. **Kitsune Shield (Play Integrity Express)**:
   - 100% safe RAM-only bootloader spoofing (`verifiedbootstate=green`, `flash.locked=1`) without risking bootloops or bricks.
   - One-tap Google Play Services (DroidGuard) cache purging.
   - Root mount namespace isolation and su binary hiding.
3. **Static `ksuinit` (16KB Page-Size)**:
   - Eliminates early-init dynamic linker crashes on Android 14/15/16.
4. **App Profile**:
   - Per-app permission sandboxing and root access restriction.
5. **Built-in Zygisk**:
   - Out-of-the-box Zygisk support.
6. **Kitsunepatch Settings**:
   - Dedicated management screen for SuperKey, PathHide, NetIsolate, and KPM.
7. **Liquid Glass & Pure AMOLED Design**:
   - Frosted glass cards, dark AMOLED palette, permanent bottom navigation bar.
8. **OTA Updates**:
   - Integrated update checker linked to official GitHub Releases.

---

## 📱 Compatibility

- **GKI 2.0**: Linux kernels 5.10, 5.15, 6.1, 6.6 and newer.
- **Architectures**: `arm64-v8a`.

---

## 📜 License
KitsuneSU is licensed under [GPL-3.0 or later](LICENSE).
