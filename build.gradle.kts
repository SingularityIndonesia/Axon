plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.nmcp) apply false
    alias(libs.plugins.nmcp.aggregation)
}

nmcpAggregation {
    centralPortal {
        username.set(
            (project.findProperty("AXON_SONATYPE_USERNAME") as String?)
                ?: System.getenv("AXON_SONATYPE_USERNAME")
                ?: error("AXON_SONATYPE_USERNAME is not set")
        )
        password.set(
            (project.findProperty("AXON_SONATYPE_PASSWORD") as String?)
                ?: System.getenv("AXON_SONATYPE_PASSWORD")
                ?: error("AXON_SONATYPE_PASSWORD is not set")
        )
        publishingType.set("AUTOMATIC")
    }
}

dependencies {
    nmcpAggregation(project(":core"))
    nmcpAggregation(project(":ksp"))
}
