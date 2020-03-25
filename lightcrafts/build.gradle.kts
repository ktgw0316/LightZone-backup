val MAKE = "make"

dependencies {
    "implementation"(files("lib/substance-lite.jar"))
    "implementation"(files("lib/laf-widget-.jar"))
}
tasks {
    register<Exec> ("coprocesses") {
        commandLine(MAKE, "-C", "coprocesses", "-j", "-s")
    }
    register<Exec> ("cleanCoprocesses") {
        commandLine(MAKE, "-C", "coprocesses", "-j", "-s", "clean")
    }
    getByName("build") {
        dependsOn("coprocesses")
    }
    getByName("clean") {
        dependsOn("cleanCoprocesses")
    }
}
