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
            url = uri("https://artifact.bytedance.com/repository/Volcengine") 
            // 或者尝试这个地址
            // url = uri("https://artifact.bytedance.com/repository/maven-public")
        }
    }
}

rootProject.name = "TodoAPP"
include(":app")
