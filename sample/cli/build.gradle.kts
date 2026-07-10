plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ksp)
    application
}

application {
    mainClass = "MainKt"
}

sourceSets {
    main {
        kotlin.srcDir("main")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
    ksp(project(":ksp"))
}
