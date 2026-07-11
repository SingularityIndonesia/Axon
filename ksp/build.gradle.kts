plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.nmcp)
    `maven-publish`
    signing
}

group = "com.singularity_universe.axon"
version = "1.0.0"

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":core"))

    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    testImplementation(libs.kotlin.test)
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenKotlin") {
            artifactId = "ksp"
            from(components["java"])
            pom {
                name = "Axon KSP"
                description = "KSP annotation processor for the Axon framework. Resolves the dependency graph and generates Axon.init()."
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
}

signing {
    useInMemoryPgpKeys(
        (project.findProperty("AXON_SIGNING_KEY") as String?) ?: System.getenv("AXON_SIGNING_KEY") ?: "",
        (project.findProperty("AXON_SIGNING_PASSWORD") as String?) ?: System.getenv("AXON_SIGNING_PASSWORD") ?: ""
    )
    sign(publishing.publications["mavenKotlin"])
}
