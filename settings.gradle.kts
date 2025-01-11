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
            url = uri("https://artifact.bytedance.com/repository/releases")
            content {
                // 只从这个仓库获取火山引擎的包
                includeGroup("com.volcengine")
            }
        }
    }
}

rootProject.name = "TodoAPP"
include(":app")
