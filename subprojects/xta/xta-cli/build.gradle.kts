plugins {
    id("java-common")
    id("cli-tool")
}

dependencies {
    implementation(project(":theta-xta"))
    implementation(project(":theta-xta-learning"))
    implementation(project(":theta-xta-analysis"))
    implementation(project(":theta-solver-z3"))
    implementation(project(":theta-solver"))
    implementation(project(":theta-common"))
    implementation(project(":theta-core"))
    implementation(project(":theta-analysis"))

    implementation("de.learnlib.distribution:learnlib-distribution:0.18.0")
    implementation("net.automatalib.distribution:automata-distribution:0.12.0")
}

application {
    mainClassName = "hu.bme.mit.theta.xta.cli.XtaCli"
}
