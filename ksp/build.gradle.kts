plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":core"))
}
