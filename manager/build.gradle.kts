plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.agp.library) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
}

extra["androidMinSdkVersion"] = 24
extra["androidTargetSdkVersion"] = 37
extra["androidCompileSdkVersion"] = 37
extra["androidCompileSdkVersionMinor"] = 0
extra["androidBuildToolsVersion"] = "37.0.0"
extra["androidCompileNdkVersion"] = libs.versions.ndk.get()
extra["androidSourceCompatibility"] = JavaVersion.VERSION_21
extra["androidTargetCompatibility"] = JavaVersion.VERSION_21
extra["managerVersionCode"] = getVersionCode()
extra["managerVersionName"] = getVersionName()

fun getGitCommitCount(): Int {
    return runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
        process.inputStream.bufferedReader().use { it.readText().trim().toInt() }
    }.getOrDefault(4002)
}

fun getGitDescribe(): String {
    return runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags", "--always", "--abbrev=0"))
        val out = process.inputStream.bufferedReader().use { it.readText().trim() }
        out.ifBlank { "3.0.0-KitsuneSU" }
    }.getOrDefault("3.0.0-KitsuneSU")
}

fun getVersionCode(): Int {
    val commitCount = getGitCommitCount()
    val major = 3
    val end = 4001
    return (major * 10000 + commitCount - end).coerceAtLeast(31000)
}

fun getVersionName(): String {
    return "3.1.0-KitsuneSU"
}