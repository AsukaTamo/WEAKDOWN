pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // CameraX 1.6.2 的 AAR 在镜像上偶发超时；只对 androidx.camera 组走 Google 官方源，
        // 其余依赖继续用阿里云镜像，避免扩大网络影响面。
        maven {
            url = uri("https://dl.google.com/dl/android/maven2")
            content { includeGroup("androidx.camera") }
        }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        google()
        mavenCentral()
    }
}

rootProject.name = "CourseApp"
include(":app")
