plugins {
    id("org.jetbrains.intellij") version "0.4.18"
    kotlin("jvm") version "1.3.72"
}

group = "flyinwind"
version = "0.1"

repositories {
    mavenLocal()
    mavenCentral()
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2020.1.3"
    setPlugins("Lombook Plugin:0.30-2020.1","java")
    sandboxDirectory = "${rootProject.projectDir}/idea-sandbox/idea-${version}"
//    setPlugins()
//    plugins = arrayOf("java")
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
//    compileTestKotlin {
//        kotlinOptions.jvmTarget = "1.8"
//    }
}
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
//tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
//    changeNotes("""
//      Add change notes here.<br>
//      <em>most HTML tags may be used</em>""")
//}