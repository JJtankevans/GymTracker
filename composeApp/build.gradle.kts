import java.util.Properties

// App de iPhone (Compose Multiplatform). Só tem alvos iOS: no Linux/Windows o Gradle configura e pula;
// quem compila é o Mac do GitHub (.github/workflows/ios.yml). Usa os ViewModels e repositórios do :shared.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

// Chaves do Supabase: local.properties (no PC) ou variáveis de ambiente (secrets no CI). Viram um objeto Kotlin gerado.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val supabaseUrl: String = (localProps.getProperty("SUPABASE_URL") ?: System.getenv("SUPABASE_URL") ?: "").trim()
val supabaseKey: String = (localProps.getProperty("SUPABASE_KEY") ?: System.getenv("SUPABASE_KEY") ?: "").trim()

val generateAppConfig by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/appConfig")
    val url = supabaseUrl
    val key = supabaseKey
    inputs.property("url", url)
    inputs.property("key", key)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("com/vitoroliveira/gymtracker/ios/AppConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.vitoroliveira.gymtracker.ios
            |
            |/** Gerado pelo Gradle (composeApp/build.gradle.kts). Não editar. */
            |internal object AppConfig {
            |    const val SUPABASE_URL = "$url"
            |    const val SUPABASE_KEY = "$key"
            |}
            |""".trimMargin()
        )
    }
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppConfig)
            dependencies {
                implementation(project(":shared"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.material3.mp)
                implementation(libs.koin.compose.viewmodel)   // traz o ViewModel multiplataforma (base dos ViewModels do :shared)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}
