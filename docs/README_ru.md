# KitsuneSU

<img align='right' src='kitsune_logo.png' width='220px' alt="KitsuneSU logo">

[English](../README.md) | **Русский**

Универсальный модульный рут-менеджер для Android с двойным движком (**Kitsune** `init_boot` + **Kitsunepatch** `boot`), встроенной защитой от бутлупа (**Kitsune Bootloop Guard**) и современным интерфейсом.

[![Latest Release](https://img.shields.io/badge/Release-v3.1.0-orange.svg?logo=github)](https://github.com/Komarku/KitsuneSU/releases)
[![Android](https://img.shields.io/badge/Android-12%20--%2016-green.svg?logo=android)](https://github.com/Komarku/KitsuneSU)
[![KernelPatch](https://img.shields.io/badge/KernelPatch-0.13.9-blue.svg)](https://github.com/Komarku/KitsuneSU)
[![License: GPL 3.0 or later](https://img.shields.io/badge/License-GPL%203.0%20or%20later-purple.svg?logo=gnu)](/LICENSE)

---

## 🌟 Возможности (Features)

1. **Двойной движок рутирования (Dual Root Engine)**:
   - **Kitsune (`init_boot`)**: интеграция LKM/GKI модулей для современных устройств на базе GKI 2.0.
   - **Kitsunepatch (`boot`)**: полноценный движок KernelPatch 0.13.9 с авторизацией по SuperKey и поддержкой KPM-модулей прямо в `boot.img`.
2. **Встроенная защита от бутлупа (Kitsune Bootloop Guard)**:
   - **Не является модулем!** Интегрирована непосредственно в ранний этап инициализации (`post-fs-data.d`) до запуска и монтирования модулей.
   - **Аппаратный счетчик загрузок (`.boot_count`)**: автоматически фиксирует сбои при перезагрузках ядра.
   - **Сторожевой таймер (Watchdog daemon)**: отслеживает циклические падения Zygote (Zygote crashloop) и зависание анимации загрузки (таймаут 100 сек).
   - **Точечная изоляция**: автоматически отключает неисправный модуль (`disable`), предотвращая бутлуп и окирпичивание устройства. При критических сбоях откатывается в Safe Mode.
   - **Уведомление при старте**: при открытии приложения система сообщает, какой модуль был автоматически изолирован.
3. **Статически скомпилированный `ksuinit` (16KB)**:
   - Полная совместимость с устройствами Android 14, 15 и 16 с поддержкой страниц памяти 16 КБ. Никаких сбоев динамического компоновщика.
4. **App Profile (Изоляция прав root)**:
   - Тонкий контроль доступа: предоставление прав только выбранным приложениям с запуском в изолированном пространстве имен.
5. **Встроенный Zygisk**:
   - Поддержка Zygisk прямо из коробки без необходимости установки сторонних модулей.
6. **Настройки Kitsunepatch**:
   - Выделенное меню управления параметрами KernelPatch 0.13.9: SuperKey, PathHide, NetIsolate и управление KPM.
7. **Двойной интерфейс (Dual UI: Material 3 Expressive & Miuix)**:
   - Полная кастомизация плашки статуса: формы M3 (Hero, Squircle, Cut-Corner, Pill), декоративные искры/кольца, иконки статуса и настраиваемый бейдж с галочкой.
   - Чистый русский интерфейс, поддержка эффекта Liquid Glass, глубокий чёрный фон AMOLED и постоянная нижняя панель навигации.
8. **Обновления по воздуху (OTA)**:
   - Встроенный механизм проверки и загрузки обновлений напрямую из официальных релизов репозитория.

---

## 📱 Совместимость (Compatibility)

- **GKI 2.0**: полная поддержка ядер версии 5.10, 5.15, 6.1, 6.6 и новее.
- **Стандартный раздел `boot`**: устройства на старых или нестандартных ядрах поддерживаются через режим **Kitsunepatch** (KernelPatch 0.13.9).
- **Архитектура**: `arm64-v8a`.

---

## 📊 Таблица совместимости ядра

| Сценарий | Метод установки | Примечание |
|---|---|---|
| Устройства GKI (ядро 5.10+) | Режим **Kitsune** (`init_boot`) | Рекомендуемый режим LKM |
| Устройства без `init_boot` | Режим **Kitsunepatch** (`boot`) | KernelPatch 0.13.9 с авторизацией по SuperKey |
| Использование модулей KPM | Требуется `CONFIG_KPM=y` | Поддержка KernelPatch KPM |

---

## 📥 Установка (Installation)

1. Установите APK из раздела **[Releases](https://github.com/Komarku/KitsuneSU/releases)** на ваше устройство.
2. Откройте **KitsuneSU**:
   - **Способ 1 (через `init_boot`)**: нажмите «Установка» $\rightarrow$ выберите файл `init_boot.img` от вашей прошивки $\rightarrow$ прошейте полученный файл через Fastboot:
     ```bash
     fastboot flash init_boot kernelsu_patched_xxx.img
     ```
   - **Способ 2 (через `boot` / Kitsunepatch)**: перейдите в «Патчер boot» $\rightarrow$ выберите `boot.img` $\rightarrow$ задайте SuperKey $\rightarrow$ нажмите «Пропатчить boot.img» $\rightarrow$ прошейте через Fastboot:
     ```bash
     fastboot flash boot kitsunepatch_patched_xxx.img
     ```

---

## 🛠️ Сборка из исходников (Building from Source)

```bash
# Клонирование репозитория
git clone https://github.com/Komarku/KitsuneSU.git
cd KitsuneSU/manager

# Сборка релизного APK
./gradlew assembleRelease
```
Готовый файл APK будет находиться в каталоге `manager/app/build/outputs/apk/release/`.

---

## 📜 Лицензия
KitsuneSU распространяется под лицензией [GPL-3.0 or later](LICENSE).

