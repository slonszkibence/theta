plugins {
    id("java-common")
    id("antlr-grammar")
}

dependencies {
    implementation(project(":theta-xta"))
    implementation(project(":theta-common"))
    implementation(project(":theta-core"))
    implementation(project(":theta-analysis"))
    implementation(project(":theta-xta-analysis"))

    implementation("de.learnlib.distribution:learnlib-distribution:0.18.0")
    implementation("net.automatalib.distribution:automata-distribution:0.12.0")
}