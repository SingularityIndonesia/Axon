plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

application {
    mainClass = "com.singularity_universe.axon.sample.MainKt"
}

dependencies {
    implementation(project(":library"))
    implementation(libs.kotlinx.coroutines.core)
}
