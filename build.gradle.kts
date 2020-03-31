// import org.gradle.nativeplatform.platform.NativePlatform
// import org.gradle.nativeplatform.platform.OperatingSystem

plugins {
//    id("kotlin") version "1.3.70" apply false
    id("org.openjfx.javafxplugin") version "0.0.8" apply false
}

subprojects {
    apply(plugin = "java")
//    apply(plugin = "kotlin")
    apply(plugin = "org.openjfx.javafxplugin")
    group = "com.lightcrafts"
    version = "4.2.0"
    repositories {
        maven(url = "https://download.osgeo.org/webdav/geotools/")
        maven(url = "https://jitpack.io/")
        mavenCentral()
    }
    dependencies {
        "annotationProcessor"("org.jetbrains:annotations:17.0.0")
        "annotationProcessor"("org.projectlombok:lombok:1.18.4")
        "compileOnly"("org.jetbrains:annotations:17.0.0")
        "compileOnly"("org.projectlombok:lombok:1.18.4")
        "implementation"(files("${project.rootDir}/lightcrafts/lib/substance-lite.jar"))
        "implementation"(files("${project.rootDir}/lightcrafts/lib/laf-widget-.jar"))
        "implementation"("javax.media:jai_core:1.1.3")
        "implementation"("javax.media:jai_codec:1.1.3")
        "implementation"("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
        "implementation"("com.github.jiconfont:jiconfont-swing:1.0.0")
        "implementation"("com.github.jiconfont:jiconfont-font_awesome:4.5.0.3")
        "implementation"("com.github.jiconfont:jiconfont-google_material_design_icons:2.2.0.1")
        "implementation"("de.dimaki:refuel:0.1.1")
        "implementation"("org.ejml:ejml-simple:0.37.1")
        "implementation"("org.slf4j:slf4j-simple:1.7.25")
        "implementation"("org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438")
//        "implementation"(kotlin("stdlib-jdk8", "1.3.70"))
//        "testImplementation"("junit:junit:4.12")
    }
    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_11
    }
    // val os = NativePlatform.getOperatingSystem
    // val MAKE = when(os) {
    //     isFreeBSD, isSolaris -> "gmake"
    //     else -> "make"
    // }
    val MAKE = "make"
    tasks {
//        compileKotlin {
//            kotlinOptions.jvmTarget = "1.8"
//        }
//        compileTestKotlin {
//            kotlinOptions.jvmTarget = "1.8"
//        }
        register<Exec> ("products") {
            commandLine(MAKE, "-C", "products", "-j")
        }
        register<Exec> ("cleanProducts") {
            commandLine(MAKE, "-C", "products", "-j", "-s", "clean")
        }
        register<Exec> ("jni") {
            dependsOn("classes")
            commandLine(MAKE, "-C", "jnisrc")
        }
        register<Exec> ("cleanJni") {
            commandLine(MAKE, "-C", "jnisrc", "-j", "-s", "clean")
        }
        getByName("build") {
            dependsOn("jni", "products")
        }
        getByName("clean") {
            dependsOn("cleanProducts", "cleanJni")
        }
    }
}

