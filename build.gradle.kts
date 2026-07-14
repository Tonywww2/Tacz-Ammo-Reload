plugins {
    id("dev.architectury.loom") version "1.11-SNAPSHOT"
}

// 加载器由节点 gradle.properties 的 loom.platform 决定（当前节点为 forge）
val loader = property("loom.platform").toString()
val mcVersion = property("vers.mcVersion").toString()
// 预先取出 mod.id（Project 作用域）：runConfigs 的 lambda 里 property(String) 会命中
// RunConfigSettings.property 而非 Project.property，故在外层取好。
val modId = property("mod.id").toString()

group = property("mod.group").toString()
version = "${property("mod.version")}+$mcVersion"
base.archivesName = "${property("mod.id")}-$loader"

// 1.20.1 -> Java 17；>=1.20.6 -> Java 21（为将来多版本预留）
val javaVersion = if (stonecutter.eval(mcVersion, ">=1.20.6")) 21 else 17

loom {
    silentMojangMappingsLicense()
    // Mixin（Loom 内建），Loom 生成 refmap。useLegacyMixinAp 是配置 defaultRefmapName 的前置。
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName = "${property("mod.id")}.refmap.json"
    }
    // Forge 开发环境需显式注册 mixin 配置——Loom 的 runServer/runClient 不会自动识别
    // mods.toml 的 [[mixins]]，缺了这行 mixin 在 dev 下静默不加载（NeoForge 原生识别）。
    if (loader == "forge") {
        forge { mixinConfig("${property("mod.id")}.mixins.json") }
        // Forge datagen 运行配置（GatherDataEvent）。输出到独立的 src/generated/resources
        // （与手写 src/main/resources 分开！Forge datagen 会修剪 --output 下未生成的文件）。
        runConfigs.create("data") {
            data()
            programArgs(
                "--mod", modId,
                "--all",
                "--output", rootProject.file("src/generated/resources").absolutePath,
                "--existing", rootProject.file("src/main/resources").absolutePath,
            )
        }
    }
    // 只给活动节点生成 IDE 运行配置，run 目录集中到根
    if (stonecutter.current.isActive) {
        runConfigs.all {
            ideConfigGenerated(true)
            runDir("../../run")
        }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.minecraftforge.net/")
    // CurseForge（TacZ 等仅上架 CurseForge 的 mod，经 CurseMaven 引入）
    maven {
        name = "CurseMaven"
        url = uri("https://cursemaven.com")
        content { includeGroup("curse.maven") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())

    if (loader == "neoforge") {
        "neoForge"("net.neoforged:neoforge:${property("vers.deps.fml")}")
    } else {
        "forge"("net.minecraftforge:forge:$mcVersion-${property("vers.deps.fml")}")

        // TacZ（Timeless and Classics Zero）——本 mod 的前置/编译依赖。
        // 用户给的 ForgeGradle 写法 implementation fg.deobf(...) 在 Architectury Loom 下等价于
        // modImplementation（Loom 自动反混淆/重映射 mod 依赖）；curse.maven 坐标需上面的 CurseMaven 仓库。
        "modImplementation"("curse.maven:timeless-and-classics-zero-1028108:8141310")
    }
}

tasks {
    // 关键：用 Stonecutter 预处理后的源码构建，而非原始源码。
    // Loom 1.11 惰性注册 createMinecraftArtifacts（晚于本块求值），故用 configureEach
    // 惰性挂依赖，避免配置期 named() 抛 UnknownTaskException。
    configureEach {
        if (name == "createMinecraftArtifacts") {
            dependsOn("stonecutterGenerate")
        }
    }

    processResources {
        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "group" to project.property("mod.group"),
            "authors" to project.property("mod.authors"),
            "description" to project.property("mod.description"),
            "license" to project.property("mod.license"),
        )
        inputs.properties(props)
        // Forge 读 META-INF/mods.toml，NeoForge 读 META-INF/neoforge.mods.toml；两者都做占位展开，
        // 再按当前加载器排除另一个，避免多余元数据进包。
        filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) { expand(props) }
        exclude(if (loader == "neoforge") "META-INF/mods.toml" else "META-INF/neoforge.mods.toml")
    }
    withType<JavaCompile> { options.release = javaVersion }
}

java {
    withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

// 将 datagen 生成的资源（根 src/generated/resources）并入主源集，随构建打包。
// 生成目录与手写 src/main/resources 分开；两者不得有同名文件（否则 processResources 重复）。
sourceSets["main"].resources.srcDir(rootProject.file("src/generated/resources"))
