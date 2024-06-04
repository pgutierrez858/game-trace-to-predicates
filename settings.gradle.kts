plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "predicate-compiler"
include("src:main:java")
findProject(":src:main:java")?.name = "java"
include("src:main:java:predcompiiler.compilation")
findProject(":src:main:java:predcompiiler.compilation")?.name = "predcompiiler.compilation"
