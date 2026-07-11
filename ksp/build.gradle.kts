plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":core"))

    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    testImplementation(libs.kotlin.test)
}
