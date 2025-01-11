pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { 
            url = uri("https://artifact.bytedance.com/repository/maven-releases/")
            credentials {
                username = "15937159166"
                password = "wmy@1990"
            }
        }
    }
}

rootProject.name = "TodoAPP"
include(":app")
