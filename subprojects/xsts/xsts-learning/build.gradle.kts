plugins {
    id("java-common")
}

dependencies {
    implementation(project(":theta-analysis"))
    implementation(project(":theta-common"))
    implementation(project(":theta-core"))
    implementation(project(":theta-solver"))
    implementation(project(":theta-xsts"))
    implementation(project(":theta-xsts-analysis"))
    testImplementation(project(":theta-solver-z3"))

    implementation("de.learnlib.distribution:learnlib-distribution:0.18.0")
    implementation("net.automatalib.distribution:automata-distribution:0.12.0")
}
