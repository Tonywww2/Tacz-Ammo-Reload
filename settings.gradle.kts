rootProject.name = "tacz_caliber_ammo"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.architectury.dev")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

stonecutter {
    kotlinController = true
    create(rootProject) {
        // 节点名与逻辑版本必须分离，否则版本谓词会把 "1.20.1-forge" 当成 SemVer 预发布
        version("1.20.1-forge", "1.20.1")
        version("1.21.1-neoforge", "1.21.1")
        // 活动节点由 stonecutter.gradle.kts 指定；vcsVersion = 提交前 Reset 回到的版本
        vcsVersion = "1.20.1-forge"
    }
}
