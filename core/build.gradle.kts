import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.nmcp)
    `maven-publish`
    signing
}

group = "com.singularity-universe.axon"
version = "1.0.0-alpha1"

kotlin {
    jvm()
    androidLibrary {
        namespace = "com.singularity_universe.axon"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "Axon Core"
            description = "The backbone for business applications built around Intent → Process → Result."
            url = "https://github.com/SingularityIndonesia/Axon"
            licenses {
                license {
                    name = "Apache License 2.0"
                    url = "https://github.com/SingularityIndonesia/Axon/blob/main/LICENSE"
                }
            }
            developers {
                developer {
                    id = "SingularityIndonesia"
                    name = "Singularity Indonesia"
                    email = "singularity.indonesia@gmail.com"
                }
            }
            scm {
                connection = "scm:git:git://github.com/SingularityIndonesia/Axon.git"
                developerConnection = "scm:git:ssh://github.com/SingularityIndonesia/Axon.git"
                url = "https://github.com/SingularityIndonesia/Axon"
            }
        }
    }
}

signing {
    sign(publishing.publications)
}
