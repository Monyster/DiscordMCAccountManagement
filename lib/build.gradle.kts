plugins {
    java
    eclipse  // Add this for better Eclipse integration
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    jar {
        archiveBaseName.set("ExamplePlugin")
        archiveVersion.set("1.0.5")
        destinationDirectory.set(file("E:\\MinecraftDev\\server\\plugins"))
    }
}