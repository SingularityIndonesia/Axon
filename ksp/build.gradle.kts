plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.nmcp)
    `maven-publish`
    signing
}

group = "com.singularity-universe.axon"
version = "1.0.0-alpha3"

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

nmcp {
    publishAllPublicationsToCentralPortal {
        username = providers.gradleProperty("AXON_SONATYPE_USERNAME")
        password = providers.gradleProperty("AXON_SONATYPE_PASSWORD")
        publishingType = "AUTOMATIC"
    }
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
    sign(publishing.publications["mavenKotlin"])
}
