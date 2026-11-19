package com.sukisu.nova.folkpatch

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Встроенная аппаратная защита от бутлупа (Kitsune Bootloop Guard).
 *
 * Не является модулем Magisk/KernelSU.
 * Работает на этапе ранней инициализации (post-fs-data.d) до монтирования и запуска модулей.
 *
 * Механизмы защиты:
 * 1. Аппаратный счетчик загрузок (.boot_count) — если ядро перезагружается 2-3 раза без достижения
 *    sys.boot_completed, скрипт автоматически отключает неисправный модуль и сбрасывает счетчик.
 * 2. Сторожевой таймер (Watchdog) — отслеживает падения Zygote (Zygote crashloop) и зависание анимации
 *    загрузки (таймаут 100 сек). При обнаружении сбоя мгновенно отключает виновный модуль и перезагружает систему.
 * 3. Точечная изоляция — отключается именно последний активированный/установленный модуль (.last_active_module).
 *    Если модуль неизвестен или сбои продолжаются, включается безопасный режим (Safe Mode, отключение всех модулей).
 * 4. Уведомление пользователя — при входе в KitsuneSU после спасения от бутлупа выводится диалог с информацией.
 */
object KitsuneBootloopGuard {
    private const val TAG = "KitsuneBootloopGuard"
    private const val PREFS_NAME = "kitsune_bootloop_prefs"
    private const val KEY_ENABLED = "bootloop_guard_enabled"

    private const val KSU_DIR = "/data/adb/ksu"
    private const val POST_FS_DATA_SCRIPT = "/data/adb/post-fs-data.d/00_kitsune_bootloop_guard.sh"
    private const val SERVICE_SCRIPT = "/data/adb/service.d/00_kitsune_bootloop_guard.sh"
    private const val WATCHDOG_SCRIPT = "/data/adb/ksu/kitsune_watchdog.sh"
    private const val LAST_ACTIVE_FILE = "/data/adb/ksu/.last_active_module"
    private const val RESCUE_FILE = "/data/adb/ksu/.bootloop_rescued"
    private const val DISABLE_GUARD_FILE = "/data/adb/ksu/.disable_guard"

    data class RescueIncident(
        val moduleId: String,
        val timestamp: Long,
        val mode: String, // "single", "all", "watchdog", "watchdog_all"
    )

    fun isEnabled(context: Context): Boolean {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_ENABLED, true)
    }

    suspend fun setEnabled(context: Context, enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_ENABLED, enabled).apply()

        if (enabled) {
            Shell.cmd("rm -f $DISABLE_GUARD_FILE").exec()
            installOrUpdateGuard()
        } else {
            Shell.cmd("mkdir -p $KSU_DIR && touch $DISABLE_GUARD_FILE").exec()
        }
        true
    }

    /**
     * Запоминает ID последнего включенного или установленного модуля,
     * чтобы при возникновении бутлупа отключить именно его.
     */
    fun recordLastModifiedModule(moduleId: String) {
        if (moduleId.isBlank()) return
        try {
            Shell.cmd("mkdir -p $KSU_DIR && echo -n '$moduleId' > $LAST_ACTIVE_FILE && chmod 644 $LAST_ACTIVE_FILE").submit()
            Log.d(TAG, "Recorded last active module: $moduleId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record last active module", e)
        }
    }

    /**
     * Проверяет, происходило ли автоматическое спасение системы от бутлупа.
     * Если да, возвращает информацию об инциденте и очищает флаг.
     */
    suspend fun checkAndConsumeRescueIncident(): RescueIncident? = withContext(Dispatchers.IO) {
        try {
            val res = Shell.cmd("cat $RESCUE_FILE 2>/dev/null").exec()
            val out = res.out.joinToString("").trim()
            if (out.isNotEmpty()) {
                Shell.cmd("rm -f $RESCUE_FILE").exec()
                val parts = out.split("|")
                val modId = parts.getOrNull(0) ?: "unknown"
                val ts = parts.getOrNull(1)?.toLongOrNull() ?: System.currentTimeMillis()
                val mode = parts.getOrNull(2) ?: "single"
                return@withContext RescueIncident(modId, ts, mode)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check rescue incident", e)
        }
        null
    }

    /**
     * Разворачивает скрипты защиты в post-fs-data.d и service.d.
     * Запускается при старте приложения KitsuneSU.
     */
    suspend fun installOrUpdateGuard(): Boolean = withContext(Dispatchers.IO) {
        try {
            val postFsScript = buildPostFsDataScript()
            val watchdogScript = buildWatchdogScript()
            val serviceScript = buildServiceScript()

            val commands = listOf(
                "mkdir -p /data/adb/post-fs-data.d /data/adb/service.d $KSU_DIR",
                "cat << 'EOF' > $POST_FS_DATA_SCRIPT\n$postFsScript\nEOF",
                "chmod 755 $POST_FS_DATA_SCRIPT",
                "cat << 'EOF' > $WATCHDOG_SCRIPT\n$watchdogScript\nEOF",
                "chmod 755 $WATCHDOG_SCRIPT",
                "cat << 'EOF' > $SERVICE_SCRIPT\n$serviceScript\nEOF",
                "chmod 755 $SERVICE_SCRIPT",
            )

            val result = Shell.cmd(*commands.toTypedArray()).exec()
            if (result.isSuccess) {
                Log.d(TAG, "Kitsune Bootloop Guard scripts installed successfully.")
                return@withContext true
            } else {
                Log.w(TAG, "Failed to install bootloop guard: ${result.err}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error installing bootloop guard scripts", e)
        }
        false
    }

    private fun buildPostFsDataScript(): String {
        return """
#!/system/bin/sh
# KitsuneSU Built-in Bootloop Guard (post-fs-data stage)
# Runs BEFORE any modules mount or execute
set +e

GUARD_DIR="/data/adb/ksu"
MODULES_DIR="/data/adb/modules"
COUNTER_FILE="${'$'}GUARD_DIR/.boot_count"
LAST_MOD_FILE="${'$'}GUARD_DIR/.last_active_module"
RESCUE_FILE="${'$'}GUARD_DIR/.bootloop_rescued"
DISABLED_GUARD="${'$'}GUARD_DIR/.disable_guard"

# Check if protection was manually disabled
[ -f "${'$'}DISABLED_GUARD" ] && exit 0

mkdir -p "${'$'}GUARD_DIR" 2>/dev/null

# Read and increment boot counter
COUNT=0
if [ -f "${'$'}COUNTER_FILE" ]; then
    VAL=${'$'}(cat "${'$'}COUNTER_FILE" 2>/dev/null)
    case "${'$'}VAL" in
        ''|*[!0-9]*) COUNT=0 ;;
        *) COUNT=${'$'}VAL ;;
    esac
fi

COUNT=${'$'}((COUNT + 1))
echo "${'$'}COUNT" > "${'$'}COUNTER_FILE" 2>/dev/null
sync

# Threshold: if COUNT >= 3, previous boots failed before sys.boot_completed
if [ "${'$'}COUNT" -ge 3 ]; then
    TARGET=""
    if [ -f "${'$'}LAST_MOD_FILE" ]; then
        TARGET=${'$'}(cat "${'$'}LAST_MOD_FILE" 2>/dev/null)
    fi

    # Fallback to newest directory if last active module is not recorded
    if [ -z "${'$'}TARGET" ] && [ -d "${'$'}MODULES_DIR" ]; then
        TARGET=${'$'}(ls -t "${'$'}MODULES_DIR" 2>/dev/null | head -n 1)
    fi

    RESCUED=0
    if [ -n "${'$'}TARGET" ] && [ -d "${'$'}MODULES_DIR/${'$'}TARGET" ]; then
        touch "${'$'}MODULES_DIR/${'$'}TARGET/disable" 2>/dev/null
        echo "${'$'}TARGET|${'$'}(date +%s)|single" > "${'$'}RESCUE_FILE" 2>/dev/null
        RESCUED=1
    fi

    # If single target failed or COUNT >= 4, enter Safe Mode and disable all modules
    if [ "${'$'}RESCUED" -eq 0 ] || [ "${'$'}COUNT" -ge 4 ]; then
        if [ -d "${'$'}MODULES_DIR" ]; then
            for mod in "${'$'}MODULES_DIR"/*; do
                [ -d "${'$'}mod" ] && touch "${'$'}mod/disable" 2>/dev/null
            done
        fi
        touch "${'$'}MODULES_DIR/.disable" 2>/dev/null
        echo "ALL_MODULES|${'$'}(date +%s)|all" > "${'$'}RESCUE_FILE" 2>/dev/null
    fi

    # Reset counter and sync
    echo 0 > "${'$'}COUNTER_FILE" 2>/dev/null
    sync
fi

# Launch background watchdog daemon for Zygote crashes or boot hangs
if [ -f "${'$'}GUARD_DIR/kitsune_watchdog.sh" ]; then
    /system/bin/sh "${'$'}GUARD_DIR/kitsune_watchdog.sh" >/dev/null 2>&1 &
fi
exit 0
        """.trimIndent()
    }

    private fun buildWatchdogScript(): String {
        return """
#!/system/bin/sh
# KitsuneSU Early Boot Watchdog Daemon
set +e

GUARD_DIR="/data/adb/ksu"
MODULES_DIR="/data/adb/modules"
LAST_MOD_FILE="${'$'}GUARD_DIR/.last_active_module"
RESCUE_FILE="${'$'}GUARD_DIR/.bootloop_rescued"
PID_FILE="${'$'}GUARD_DIR/.watchdog_pid"
COUNTER_FILE="${'$'}GUARD_DIR/.boot_count"
DISABLED_GUARD="${'$'}GUARD_DIR/.disable_guard"

[ -f "${'$'}DISABLED_GUARD" ] && exit 0

echo $$ > "${'$'}PID_FILE" 2>/dev/null

MAX_WAIT=100
ELAPSED=0
ZYGOTE_CRASHES=0
PREV_ZYGOTE_PID=""

trigger_rescue() {
    TARGET=""
    if [ -f "${'$'}LAST_MOD_FILE" ]; then
        TARGET=${'$'}(cat "${'$'}LAST_MOD_FILE" 2>/dev/null)
    fi
    if [ -z "${'$'}TARGET" ] && [ -d "${'$'}MODULES_DIR" ]; then
        TARGET=${'$'}(ls -t "${'$'}MODULES_DIR" 2>/dev/null | head -n 1)
    fi

    if [ -n "${'$'}TARGET" ] && [ -d "${'$'}MODULES_DIR/${'$'}TARGET" ]; then
        touch "${'$'}MODULES_DIR/${'$'}TARGET/disable" 2>/dev/null
        echo "${'$'}TARGET|${'$'}(date +%s)|watchdog" > "${'$'}RESCUE_FILE" 2>/dev/null
    else
        if [ -d "${'$'}MODULES_DIR" ]; then
            for mod in "${'$'}MODULES_DIR"/*; do
                [ -d "${'$'}mod" ] && touch "${'$'}mod/disable" 2>/dev/null
            done
        fi
        touch "${'$'}MODULES_DIR/.disable" 2>/dev/null
        echo "ALL_MODULES|${'$'}(date +%s)|watchdog_all" > "${'$'}RESCUE_FILE" 2>/dev/null
    fi

    echo 0 > "${'$'}COUNTER_FILE" 2>/dev/null
    rm -f "${'$'}PID_FILE" 2>/dev/null
    sync
    /system/bin/reboot 2>/dev/null
    exit 0
}

while [ "${'$'}ELAPSED" -lt "${'$'}MAX_WAIT" ]; do
    BOOT_COMPLETED=${'$'}(getprop sys.boot_completed 2>/dev/null)
    if [ "${'$'}BOOT_COMPLETED" = "1" ]; then
        echo 0 > "${'$'}COUNTER_FILE" 2>/dev/null
        rm -f "${'$'}PID_FILE" 2>/dev/null
        exit 0
    fi

    # Check Zygote stability
    ZYGOTE_PID=${'$'}(pidof zygote64 2>/dev/null)
    [ -z "${'$'}ZYGOTE_PID" ] && ZYGOTE_PID=${'$'}(pidof zygote 2>/dev/null)
    if [ -n "${'$'}ZYGOTE_PID" ]; then
        if [ -n "${'$'}PREV_ZYGOTE_PID" ] && [ "${'$'}ZYGOTE_PID" != "${'$'}PREV_ZYGOTE_PID" ]; then
            ZYGOTE_CRASHES=${'$'}((ZYGOTE_CRASHES + 1))
            if [ "${'$'}ZYGOTE_CRASHES" -ge 3 ]; then
                trigger_rescue
            fi
        fi
        PREV_ZYGOTE_PID="${'$'}ZYGOTE_PID"
    fi

    sleep 2
    ELAPSED=${'$'}((ELAPSED + 2))
done

# Timeout reached (100 seconds without boot completion)
BOOT_COMPLETED=${'$'}(getprop sys.boot_completed 2>/dev/null)
if [ "${'$'}BOOT_COMPLETED" != "1" ]; then
    trigger_rescue
fi

rm -f "${'$'}PID_FILE" 2>/dev/null
exit 0
        """.trimIndent()
    }

    private fun buildServiceScript(): String {
        return """
#!/system/bin/sh
# KitsuneSU Built-in Bootloop Guard (late service stage)
set +e

GUARD_DIR="/data/adb/ksu"
COUNTER_FILE="${'$'}GUARD_DIR/.boot_count"
PID_FILE="${'$'}GUARD_DIR/.watchdog_pid"

# Wait until boot completed
while [ "${'$'}(getprop sys.boot_completed 2>/dev/null)" != "1" ]; do
    sleep 2
done

# Reset boot counter to 0 upon successful boot
echo 0 > "${'$'}COUNTER_FILE" 2>/dev/null

# Clean up watchdog PID file
if [ -f "${'$'}PID_FILE" ]; then
    WPID=${'$'}(cat "${'$'}PID_FILE" 2>/dev/null)
    [ -n "${'$'}WPID" ] && kill -9 "${'$'}WPID" 2>/dev/null
    rm -f "${'$'}PID_FILE" 2>/dev/null
fi

exit 0
        """.trimIndent()
    }
}

