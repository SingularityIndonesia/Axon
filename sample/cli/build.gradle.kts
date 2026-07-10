plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

application {
    mainClass = "com.singularity_universe.axon.sample.MainKt"
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
}
