plugins {
    id("dev.architectury.loom") version "1.11-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "2.1.1"
}

// 加载器由节点 gradle.properties 的 loom.platform 决定（当前节点为 forge）
val loader = property("loom.platform").toString()
val mcVersion = property("vers.mcVersion").toString()
// 预先取出 mod.id（Project 作用域）：runConfigs 的 lambda 里 property(String) 会命中
// RunConfigSettings.property 而非 Project.property，故在外层取好。
val modId = property("mod.id").toString()

// runData（Forge datagen）会走完整 mod-loading、加载 classpath 上所有 mod；KubeJS 等重型测试 mod 会拉起
// 非 daemon 后台线程，使 datagen 跑完（All providers took…）后 JVM 不退出，runData 卡住不自动结束。
// 故跑 runData 时整体排除这些纯测试 modLocalRuntime（datagen 只需要本 mod + Forge + TacZ）。
val isDatagenRun = gradle.startParameter.taskNames.any { it.contains("runData", ignoreCase = true) }

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
    // Architectury API（KubeJS 的必需前置）——官方 maven，版本定位比 CurseForge 更直接
    maven("https://maven.architectury.dev/") {
        name = "Architectury"
        content { includeGroup("dev.architectury") }
    }
    // Modrinth（JEI/Oculus/Embeddium 等开发期测试 mod；用版本字符串定位，比 CurseForge fileId 稳定）
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
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

        // MixinExtras：必须用 -common（类直接在 jar 内）。-forge 变体把类 JiJ 内嵌在
        // META-INF/jars/MixinExtras-*.jar，Loom dev 不解压 → com.llamalad7...Operation 找不到。
        "forgeRuntimeLibrary"("io.github.llamalad7:mixinextras-common:0.3.6")

        // TacZ 通过 JiJ 内嵌以下库；Loom dev 不解压内嵌 jar，需从 TacZ jar 抽到 libs/ 再显式补齐，
        // 否则 TacZ 构造/加载期 NoClassDefFoundError（luaj / bcel / commons-math3）。参见 skill ref 11。
        // 注意：本共享脚本应用于节点工程（versions/1.20.1-forge），相对 files("libs/..") 会指向
        // versions/1.20.1-forge/libs（不存在）→ jar 不进 classpath。必须用 rootProject.files 指向根 libs/。
        "forgeRuntimeLibrary"(rootProject.files("libs/luaj-core-3.0.8-figura.jar"))
        "forgeRuntimeLibrary"(rootProject.files("libs/luaj-jse-3.0.8-figura.jar"))
        "forgeRuntimeLibrary"(rootProject.files("libs/bcel-6.6.1.jar"))
        "forgeRuntimeLibrary"(rootProject.files("libs/commons-math3-3.6.1.jar"))

        // luaj 编译期可见（运行期已由 TacZ JiJ + 上面的 forgeRuntimeLibrary 提供，不打包）——
        // 为在 Java 里引用 org.luaj.vm2.* 以及 TacZ ScriptManager.getScript 返回的 LuaTable（弹药效果 Lua 脚本系统）。
        "compileOnly"(rootProject.files("libs/luaj-core-3.0.8-figura.jar"))
        "compileOnly"(rootProject.files("libs/luaj-jse-3.0.8-figura.jar"))

        // simplebedrockmodel 是 mod（TacZ 枪械客户端渲染依赖），需 Loom 重映射并按 mod 加载；
        // 它内嵌的 mae 是纯动画数学库（同样从 JiJ 抽到 libs/ 补齐）。
        "modRuntimeOnly"(rootProject.files("libs/simplebedrockmodel-2.2.2-forge+mc1.20.1.jar"))
        "forgeRuntimeLibrary"(rootProject.files("libs/mae-1.1.2.jar"))

        // 开发期测试辅助 mod：仅本地 dev 运行期加载（modLocalRuntime = 不参与编译、不写入发布依赖，
        // 下游使用者无需安装）。用于验证本 mod 的弹药/口径伤害：
        //   DamageRender —— 在世界内实体头顶显示所受伤害数值；
        //   Powerful Dummy —— 可定制属性的测试假人，用于 DPS 检测；
        //   KubeJS / Rhino / Architectury —— 脚本/测试用（KubeJS 1.20.1 Forge 硬前置 Rhino + Architectury API）。
        // 经 CurseMaven 引入，坐标 = 任意名-项目ID:文件ID（文件均为 Forge 1.20.1 变体）。
        // 【runData 排除】跑 datagen 时整体不加载：这些重型 mod 会在 datagen 的 mod-loading 里拉起非 daemon
        // 线程，让 datagen 跑完后 JVM 不退出（runData 卡死）。runClient/runServer 不受影响（isDatagenRun=false）。
        if (!isDatagenRun) {
            "modLocalRuntime"("curse.maven:damagerender-1263626:8431126")   // DamageRender-1.20.1-Forge-1.3.3
            "modLocalRuntime"("curse.maven:powerful-dummy-1276893:7638171") // powerful_dummy-20-0.0.9 (Forge 1.20.1)

            // KubeJS 硬前置 Rhino + Architectury API（各自作为独立 mod 引入）；如需在 Java 里写 KubeJS 集成，
            // 把 kubejs 改成 modImplementation（编译期）。Architectury 锁 9.1.12（kubejs build.26 POM 的兼容版本）。
            "modLocalRuntime"("dev.architectury:architectury-forge:9.1.12") { isTransitive = false }
            "modLocalRuntime"("curse.maven:rhino-416294:6186971")   // rhino-forge-2001.2.3-build.10 (1.20.1 Forge)
            "modLocalRuntime"("curse.maven:kubejs-238086:8020595")  // kubejs-forge-2001.6.5-build.26 (1.20.1 Forge)

            // 兼容性测试 mod（Modrinth，用版本字符串定位）；modLocalRuntime = 仅 dev 运行期、不参与编译、不写入发布依赖。
            // 只在非 datagen 运行加载。JEI —— 物品/配方查看。
            "modLocalRuntime"("maven.modrinth:jei:15.20.0.106")           // jei-1.20.1-forge-15.20.0.106
            // 注意：Oculus（Iris 光影移植）+ 其硬前置 Embeddium（Sodium 移植）在本项目 Forge 47.4.0 下启动即 FATAL 崩溃，故禁用：
            //   Embeddium 0.3.31（1.20.1-Forge 末版，2024-08）的 features.render.gui.debug.ForgeGuiMixin 注入 ForgeGui#renderLines
            //   的描述符与 47.4.0 不符（期望 (...CallbackInfo)V，实际多两个 ArrayList 局部变量），抛 InvalidInjectionException。
            //   1.20.1-Forge 无更新 Embeddium，Oculus 又硬依赖它，故此 Forge 版本下 Oculus 光影链不可用。
            // "modLocalRuntime"("maven.modrinth:embeddium:0.3.31+mc1.20.1")
            // "modLocalRuntime"("maven.modrinth:oculus:1.20.1-1.8.0")
        }
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

// ---------------------------------------------------------------------------------------------------
// CurseForge publishing (me.modmuss50.mod-publish-plugin). Uploads this node's remapped jar;
// the root stonecutter.gradle.kts `publishAllVersions` task releases every loader node at once.
// Secrets/id are read lazily (only when a publish task runs), so a normal build is unaffected:
//   - CURSEFORGE_TOKEN env var (CI), or user-level ~/.gradle/gradle.properties curseforge.token (never commit);
//   - curseforge.projectId (numeric, from the CurseForge About Project page) in gradle.properties (blank for now).
// Usage:
//   ./gradlew publishAllVersions                         # every loader node
//   ./gradlew :1.20.1-forge:publishMods                  # a single node
//   ./gradlew publishAllVersions -Ppublish.dryRun=true   # full flow, uploads nothing (see scripts/dryrun.ps1)
// ---------------------------------------------------------------------------------------------------
publishMods {
    // Architectury Loom's final (remapped) artifact - not the raw jar task output.
    file = tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar").flatMap { it.archiveFile }
    version = project.version.toString() // e.g. 0.1.0+1.20.1
    displayName = "${property("mod.name")} ${property("mod.version")} - MC $mcVersion ($loader)"
    modLoaders.add(loader)
    type = STABLE

    // Exercise the whole flow without uploading: -Ppublish.dryRun=true
    dryRun = providers.gradleProperty("publish.dryRun").map { it.toBoolean() }.orElse(false)

    changelog = providers.environmentVariable("CHANGELOG")
        .orElse(providers.provider { rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() })
        .orElse("See the project page.")

    curseforge {
        projectId = providers.gradleProperty("curseforge.projectId")
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            .orElse(providers.gradleProperty("curseforge.token"))
        minecraftVersions.add(mcVersion)
        javaVersions.add(JavaVersion.toVersion(javaVersion))
        // TacZ add-on: needed on both client and server (the plugin requires at least one).
        client = true
        server = true
    }
}
