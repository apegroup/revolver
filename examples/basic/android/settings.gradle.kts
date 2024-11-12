pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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
            url = uri("https://maven.pkg.github.com/apegroup/revolver/")
            content {
                includeGroup("com.umain")
            }
            credentials {
                username = System.getenv("GH_USERNAME") ?: ""
                password = System.getenv("GH_TOKEN") ?: ""
            }
        }
    }
}

rootProject.name = "Basic Android Integration"
include(":app")
