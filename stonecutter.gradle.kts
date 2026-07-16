plugins { id("dev.kikugie.stonecutter") }

// 当前编辑/链接的活动版本，必须恰好赋值一次
stonecutter active "1.20.1-forge"

stonecutter parameters {
    // 从节点名取加载器：1.20.1-forge -> forge
    val loader = current.project.substringAfterLast('-')
    // 启用版本化注释常量：//? if forge { ... } / //? if neoforge { ... }
    constants { match(loader, "forge", "neoforge") }
}

// ---------------------------------------------------------------------------------------------------
// One command publishes every loader node: run publishMods (CurseForge upload, see build.gradle.kts)
// on each Stonecutter node, independent of the active node.   ./gradlew publishAllVersions
// ---------------------------------------------------------------------------------------------------
tasks.register("publishAllVersions") {
    group = "publishing"
    description = "Builds and publishes every Minecraft/loader version to CurseForge."
    dependsOn(stonecutter.tasks.named("publishMods").map { it.values })
}

// Upload each loader's artifact serially to avoid CurseForge API rate limits.
stonecutter.tasks.order("publishCurseforge")
