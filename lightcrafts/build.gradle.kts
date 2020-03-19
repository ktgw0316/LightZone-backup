plugins {
    java
    id("org.openjfx.javafxplugin") version "0.0.8"
//    kotlin("jvm") version "1.3.70"
}

group = "com.lightcrafts"
version = "4.2.0"

repositories {
    maven(url = "http://download.osgeo.org/webdav/geotools/")
    maven(url = "https://jitpack.io/")
    mavenCentral()
}

dependencies {
    annotationProcessor("org.jetbrains", "annotations", "17.0.0")
    annotationProcessor("org.projectlombok", "lombok", "1.18.4")
    compileOnly("com.github.krnl0x0", "gjavah", "0.2.0.1")
    compileOnly("org.jetbrains", "annotations", "17.0.0")
    compileOnly("org.projectlombok", "lombok", "1.18.4")
    implementation(files("lib/substance-lite.jar", "lib/laf-widget-.jar"))
    implementation("javax.media", "jai_core", "1.1.3")
    implementation("javax.media", "jai_codec", "1.1.3")
    implementation("javax.xml.bind", "jaxb-api", "2.4.0-b180830.0359")
    implementation("com.github.jiconfont", "jiconfont-swing", "1.0.0")
    implementation("com.github.jiconfont", "jiconfont-font_awesome", "4.5.0.3")
    implementation("com.github.jiconfont", "jiconfont-google_material_design_icons", "2.2.0.1")
    implementation("de.dimaki", "refuel", "0.1.1")
    implementation("org.ejml", "ejml-simple", "0.37.1")
    implementation("org.slf4j", "slf4j-simple", "1.7.25")
    implementation("org.glassfish.jaxb", "jaxb-runtime", "2.4.0-b180830.0438")
//    implementation(kotlin("stdlib-jdk8"))
//    testCompile("junit", "junit", "4.12")
}

javafx {
    modules = listOf("javafx.controls", "javafx.swing")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}

val NATIVE_SRC_JNI_INCLUDE_DIR = "javah"

/**
 * Generates header files for native methods.
 */
tasks.withType<JavaCompile> {
    doLast {
        val cp = (sourceSets["main"].runtimeClasspath + sourceSets["main"].output).asPath
        val cs = sourceSets["main"].output.asFileTree.matching { include("**/*.class") }.asPath
	    project.exec {
		    commandLine = listOf(
                "sh", "tools/bin/gjavah.sh",
                "-d", NATIVE_SRC_JNI_INCLUDE_DIR,
                "-classpath", cp, cs
            )
        }
    }
}

//tasks {
//    compileKotlin {
//        kotlinOptions.jvmTarget = "1.8"
//    }
//    compileTestKotlin {
//        kotlinOptions.jvmTarget = "1.8"
//    }
//}
