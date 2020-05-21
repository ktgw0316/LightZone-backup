javafx {
    modules = listOf("javafx.controls", "javafx.swing")
}
dependencies {
    "implementation"(project(":lightcrafts"))
    "implementation"("javax.help:javahelp:2.0.05")
}
application {
    mainClassName = "com.lightcrafts.platform.linux.LinuxLauncher"
}
