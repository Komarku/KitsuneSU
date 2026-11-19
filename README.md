# KitsuneSU 🦊

<div align="center">

![KitsuneSU Banner](manager/app/src/main/res/drawable/kitsune_logo_hero.png)

**Универсальный модульный рут-менеджер для Android с двойным движком и защитой Kitsune Shield.**

[![Release](https://img.shields.io/badge/Release-v3.1.0-orange.svg)](https://github.com/Komarku/KitsuneSU/releases)
[![Android](https://img.shields.io/badge/Android-12%20--%2016-green.svg)](https://github.com/Komarku/KitsuneSU)
[![KernelPatch](https://img.shields.io/badge/KernelPatch-0.13.9-blue.svg)](https://github.com/Komarku/KitsuneSU)
[![License](https://img.shields.io/badge/License-GPL--3.0-purple.svg)](LICENSE)

</div>

---

## 🌟 Основные возможности

### 1. 🦊 Двойной движок рутирования (Dual Engine)
Выбирайте наиболее подходящий способ для вашего устройства:
* **Kitsune (`init_boot`)**:
  * Работает через модули ядра LKM / GKI (Android 12 – 16).
  * Статически скомпилированный `ksuinit` с поддержкой страниц памяти 16 КБ — **полная защита от бутлупов**.
* **Kitsunepatch (`boot`)**:
  * Встроенный движок KernelPatch 0.13.9.
  * Поддержка SuperKey, PathHide на уровне ядра и прямое вшивание модулей KPM в образ ядра.

### 2. 🛡️ Kitsune Shield & Play Integrity Express
* **Абсолютная безопасность (Zero-Brick)**:
  * Не модифицирует системные разделы `/system` и `/vendor`.
  * Все подмены статуса загрузчика (`ro.boot.verifiedbootstate=green`, `ro.boot.flash.locked=1`) выполняются исключительно в оперативной памяти (RAM) через встроенный `libresetprop.so -n`.
* **Очистка следов GMS в 1 клик**:
  * Удаление кэша DroidGuard и Google Play Services для сброса негативных вердиктов аттестации.
  * Изоляция пространств монтирования и скрытие su-бинарников.

### 3. 💎 Современный интерфейс
* Чистый дизайн на русском языке без стороннего мусора и донатов.
* Постоянная удобная нижняя панель навигации (Главная, Суперпользователь, Модули, Настройки).
* Режим чистого чёрного экрана AMOLED и полупрозрачный стиль Liquid Glass.
* Встроенный выбор логотипов приложения.

### 4. 🔄 Обновления по воздуху (OTA)
* Встроенный менеджер обновлений, привязанный к официальным релизам репозитория [Komarku/KitsuneSU](https://github.com/Komarku/KitsuneSU).

---

## 📥 Установка

1. Скачайте свежий APK из раздела [Releases](https://github.com/Komarku/KitsuneSU/releases) и установите на телефон.
2. Откройте **KitsuneSU**:
   - **Способ 1 (init_boot)**: Нажмите «Установка» $\rightarrow$ «Выбрать файл и пропатчить» $\rightarrow$ выберите `init_boot.img` от вашей прошивки $\rightarrow$ прошейте полученный образ через Fastboot:
     ```bash
     fastboot flash init_boot "kernelsu_patched_xxx.img"
     ```
   - **Способ 2 (boot / Kitsunepatch)**: Перейдите в «Патчер boot» $\rightarrow$ выберите `boot.img` $\rightarrow$ задайте свой SuperKey $\rightarrow$ нажмите «Пропатчить boot.img».

---

## 🛠️ Сборка из исходников

```bash
# Клонирование репозитория
git clone https://github.com/Komarku/KitsuneSU.git
cd KitsuneSU/manager

# Сборка релизного APK
./gradlew assembleRelease
```
Готовый APK будет сохранён в `manager/app/build/outputs/apk/release/`.

---

## 📜 Лицензия
KitsuneSU распространяется под лицензией [GPL-3.0](LICENSE).

