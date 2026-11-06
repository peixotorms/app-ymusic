import org.gradle.api.Project

/** Git revision included in the app's build metadata. */
fun Project.gitCommit(): String =
    runCatching {
        val process = ProcessBuilder("git", "rev-parse", "--short=10", "HEAD")
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().readText().trim().takeIf {
            process.waitFor() == 0 && it.isNotEmpty()
        }
    }.getOrNull() ?: "unknown"
