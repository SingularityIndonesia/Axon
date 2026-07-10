plugins {
    alias(libs.plugins.kotlinJvm)
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
}
