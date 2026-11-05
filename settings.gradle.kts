// Auto-detect and configure Android SDK location across machines (PC, laptop, container)
val localPropsFile = file("local.properties")
val configuredSdk = if (localPropsFile.exists()) {
    val p = java.util.Properties()
    localPropsFile.inputStream().use { p.load(it) }
    p.getProperty("sdk.dir")
} else null

if (configuredSdk == null || !file(configuredSdk).exists()) {
    val candidates = listOfNotNull(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        "/opt/android-sdk",
        "${System.getProperty("user.home")}/Android/Sdk",
        "/home/jakob/Android/Sdk",
        "/home/pn/android-sdk"
    )
    val foundSdk = candidates.firstOrNull { file(it).exists() }
    if (foundSdk != null) {
        val p = java.util.Properties()
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { p.load(it) }
        }
        p.setProperty("sdk.dir", foundSdk)
        localPropsFile.outputStream().use { p.store(it, null) }
    }
}

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "oledoro"
include(":app")
include(":common")
include(":desktopApp")
