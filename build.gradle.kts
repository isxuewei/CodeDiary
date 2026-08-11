plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.8.0"
}

group = "fun.xuewei"
version = "26.08.12"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2021.3.3")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* 不依赖 git4idea，沙箱可正常启动 */))
}

dependencies {
    // 排除 JGit 自带的 slf4j，避免与 IDEA 平台的 SLF4J 类加载冲突
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r") {
        exclude(group = "org.slf4j")
    }
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    test {
        useJUnitPlatform()
    }

    buildSearchableOptions {
        enabled = false
    }

    runIde {
        // 沙箱 IDE 视为本地环境，生成请求转发到本机启动的服务端。
        systemProperty("codediary.environment", "local")
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("253.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
