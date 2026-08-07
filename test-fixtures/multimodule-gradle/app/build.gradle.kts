plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation("com.google.guava:guava:32.1.3-jre")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}
