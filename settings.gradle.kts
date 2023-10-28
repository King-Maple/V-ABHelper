pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven(url="https://maven.aliyun.com/repository/public/")
        maven(url="https://jitpack.io")
        maven(url="https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url="https://api.xposed.info/")
        maven(url="https://s01.oss.sonatype.org/content/repositories/releases")
    }
}

rootProject.name = "系统更新助手"
include(":app")
 