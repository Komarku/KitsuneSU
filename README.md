# KitsuneSU

<img align='right' src='docs/kitsune_logo.png' width='220px' alt="KitsuneSU logo">

**Русский** | [English](./docs/README_en.md)

Универсальный модульный рут-менеджер для Android с двойным движком (**Kitsune** `init_boot` + **Kitsunepatch** `boot`) и интеллектуальной системой защиты **Kitsune Shield**.

[![Latest Release](https://img.shields.io/badge/Release-v3.1.0-orange.svg?logo=github)](https://github.com/Komarku/KitsuneSU/releases)
[![Android](https://img.shields.io/badge/Android-12%20--%2016-green.svg?logo=android)](https://github.com/Komarku/KitsuneSU)
[![KernelPatch](https://img.shields.io/badge/KernelPatch-0.13.9-blue.svg)](https://github.com/Komarku/KitsuneSU)
[![License: GPL 3.0 or later](https://img.shields.io/badge/License-GPL%203.0%20or%20later-purple.svg?logo=gnu)](/LICENSE)

---

## 🌟 Возможности (Features)

1. **Двойной движок рутирования (Dual Root Engine)**:
   - **Kitsune (`init_boot`)**: интеграция LKM/GKI модулей для современных устройств.
   - **Kitsunepatch (`boot`)**: полноценный движок KernelPatch 0.13.9 с авторизацией по SuperKey и вшиванием KPM-модулей прямо в `boot.img`.
2. **Kitsune Shield (Play Integrity Express)**:
   - 100% безопасный RAM-антидетект загрузчика (`ro.boot.verifiedbootstate=green`, `ro.boot.flash.locked=1`) без риска бутлупа и изменения системных разделов.
   - Очистка кэша Google Play Services (DroidGuard) в один тап.
   - Автоматическая изоляция пространств монтирования (Mount Hide) и скрытие su-бинарников.
3. **Статически скомпилированный `ksuinit` (16KB)**:
   - Полная совместимость с устройствами Android 14, 15 и 16 с поддержкой страниц памяти 16 КБ. Никаких бутлупов из-за динамического компоновщика.
4. **App Profile (Изоляция прав root)**:
   - Тонкий контроль доступа: предоставление прав только выбранным приложениям или запуск в изолированном окружении.
5. **Встроенный Zygisk**:
   - Поддержка Zygisk прямо из коробки без необходимости ставить сторонние модули.
6. **Настройки Kitsunepatch**:
   - Выделенное меню управления параметрами KernelPatch: SuperKey, PathHide, NetIsolate и управление KPM.
7. **Современный интерфейс Liquid Glass & AMOLED**:
   - Чистый русский интерфейс, полупрозрачные карточки, эффект стекла и глубокий чёрный фон. Постоянная нижняя панель навигации.
8. **Обновления по воздуху (OTA)**:
   - Встроенный механизм проверки и загрузки обновлений напрямую из официальных релизов репозитория.

---

## 📱 Совместимость (Compatibility)

- **GKI 2.0**: полная поддержка ядер версии 5.10, 5.15, 6.1, 6.6 и новее.
- **Два метода установки**:
  - Устройства с разделом `init_boot` (Android 13+): режим **Kitsune**.
  - Устройства со стандартным разделом `boot`: режим **Kitsunepatch** (KP 0.13.9).
- **Архитектура**: `arm64-v8a`.

---

## 📥 Установка (Installation)

1. Установите APK из раздела **[Releases](https://github.com/Komarku/KitsuneSU/releases)** на ваше устройство.
2. Откройте **KitsuneSU**:
   - **Способ 1 (через `init_boot`)**: нажмите «Установка» $\rightarrow$ выберите файл `init_boot.img` от вашей прошивки $\rightarrow$ прошейте полученный файл через Fastboot:
     ```bash
     fastboot flash init_boot kernelsu_patched_xxx.img
     ```
   - **Способ 2 (через `boot` / Kitsunepatch)**: перейдите в «Патчер boot» $\rightarrow$ выберите `boot.img` $\rightarrow$ задайте SuperKey $\rightarrow$ нажмите «Пропатчить boot.img».

---

## 🛠️ Сборка из исходников (Building)

```bash
# Клонирование репозитория
git clone https://github.com/Komarku/KitsuneSU.git
cd KitsuneSU/manager

# Сборка релизного APK
./gradlew assembleRelease
```
Готовый файл APK будет находиться в `manager/app/build/outputs/apk/release/`.

---

## 📜 Лицензия
KitsuneSU распространяется под лицензией [GPL-3.0 or later](LICENSE).
