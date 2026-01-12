import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val roomVersion = "2.7.0-alpha13"
val sqliteVersion = "2.5.0-alpha13"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    // KSP plugin – wersja jest w settings.gradle.kts
    id("com.google.devtools.ksp")
}

kotlin {
    jvm() // Bez dodatkowych konfiguracji — KSP działa automatycznie

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)

            // ROOM MULTIPLATFORM + SQLITE BUNDLED (DZIAŁA NA DESKTOP)
            implementation("androidx.room:room-common:$roomVersion")
            implementation("androidx.room:room-runtime:$roomVersion")
            implementation("androidx.sqlite:sqlite-bundled:$sqliteVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.json:json:20240303")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

dependencies {
    // Room Compiler -> KSP
    add("kspJvm", "androidx.room:room-compiler:$roomVersion")
}

ksp {
    // Schemat bazy danych w Github
    arg("room.schemaLocation", "$projectDir/schemas")
}

compose.desktop {
    application {
        mainClass = "com.example.homebudget.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "HomeBudget"
            packageVersion = "1.0.4"

            windows {
                menu = true // Start Menu
                shortcut = true // Skrót na pulpicie
                dirChooser = true
                // Ikona aplikacji
                iconFile.set(
                    project.file("src/desktopMain/resources/icon.ico")
                )
            }
        }
    }
}
