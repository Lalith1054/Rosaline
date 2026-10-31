package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.max

data class ReleaseUpdateInfo(
    val tagName: String,
    val releaseName: String,
    val body: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long,
    val publishedAt: String,
    val isNewerThanInstalled: Boolean,
    val installedVersionDisplay: String,
    val latestVersionDisplay: String
)

sealed class UpdateCheckResult {
    data class Success(
        val info: ReleaseUpdateInfo,
        val resolvedRepo: String
    ) : UpdateCheckResult()

    data class Error(
        val message: String
    ) : UpdateCheckResult()
}

object UpdateChecker {

    /**
     * Parses user input such as:
     * - "owner/repo"
     * - "https://github.com/owner/repo"
     * - "https://github.com/owner/repo/releases"
     * - "owner" (GitHub username only)
     */
    fun parseRepoInput(raw: String): String {
        var cleaned = raw.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("github.com/")
            .removePrefix("api.github.com/repos/")
            .trim('/')

        if (cleaned.endsWith(".git")) {
            cleaned = cleaned.removeSuffix(".git")
        }

        val segments = cleaned.split("/").filter { it.isNotBlank() }
        return when {
            segments.size >= 2 -> "${segments[0]}/${segments[1]}"
            segments.size == 1 -> segments[0]
            else -> ""
        }
    }

    /**
     * Checks the GitHub Releases API for the latest release of the given repository,
     * compares the release version against the currently installed build, and extracts
     * the signed APK download asset URL if present.
     */
    suspend fun checkForUpdates(
        rawRepoInput: String,
        installedVersionName: String,
        installedRunNumber: String
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val parsed = parseRepoInput(rawRepoInput)
            if (parsed.isEmpty()) {
                return@withContext UpdateCheckResult.Error(
                    "Please enter your GitHub repository (e.g. username/Rosaline) to check for updates."
                )
            }

            // If user entered only a GitHub username without "/repo", auto-discover their Rosaline repo
            var resolvedRepo = if (!parsed.contains("/")) {
                discoverRepoForUser(parsed) ?: "$parsed/Rosaline"
            } else {
                parsed
            }

            var releaseJson = fetchLatestReleaseJson(resolvedRepo)
            if (releaseJson == null && resolvedRepo.contains("/")) {
                val owner = resolvedRepo.substringBefore("/")
                val discovered = discoverRepoForUser(owner)
                if (!discovered.isNullOrBlank() && !discovered.equals(resolvedRepo, ignoreCase = true)) {
                    resolvedRepo = discovered
                    releaseJson = fetchLatestReleaseJson(resolvedRepo)
                }
            }

            if (releaseJson == null) {
                return@withContext UpdateCheckResult.Error(
                    "No releases found at github.com/$resolvedRepo/releases. Tap 'Set Release Repo' to enter your GitHub repository (owner/repo) or publish a release on GitHub."
                )
            }

            val tagName = releaseJson.optString("tag_name", "").trim()
            val releaseName = releaseJson.optString("name", "").trim().ifEmpty { tagName }
            val body = releaseJson.optString("body", "").trim()
            val htmlUrl = releaseJson.optString(
                "html_url",
                "https://github.com/$resolvedRepo/releases"
            )
            val publishedAt = releaseJson.optString("published_at", "")

            var apkDownloadUrl: String? = null
            var apkFileName: String? = null
            var apkSizeBytes = 0L

            val assetsArray = releaseJson.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    if (name.lowercase(Locale.US).endsWith(".apk") && downloadUrl.isNotBlank()) {
                        apkDownloadUrl = downloadUrl
                        apkFileName = name
                        apkSizeBytes = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val effectiveInstalledVersion = if (installedRunNumber.isNotBlank()) {
                "$installedVersionName.$installedRunNumber"
            } else {
                installedVersionName
            }

            val remoteVersionCandidate = tagName.ifEmpty { releaseName }
            val isNewer = isRemoteVersionNewer(
                remoteVersion = remoteVersionCandidate,
                installedVersion = effectiveInstalledVersion,
                hasRunNumberInstalled = installedRunNumber.isNotBlank()
            )

            val installedDisplay = if (installedRunNumber.isNotBlank()) {
                "v$installedVersionName (build #$installedRunNumber)"
            } else {
                "v$installedVersionName"
            }

            val latestDisplay = if (tagName.isNotBlank()) {
                if (tagName.startsWith("v", ignoreCase = true)) tagName else "v$tagName"
            } else {
                releaseName
            }

            UpdateCheckResult.Success(
                info = ReleaseUpdateInfo(
                    tagName = tagName,
                    releaseName = releaseName,
                    body = body,
                    htmlUrl = htmlUrl,
                    apkDownloadUrl = apkDownloadUrl,
                    apkFileName = apkFileName,
                    apkSizeBytes = apkSizeBytes,
                    publishedAt = publishedAt,
                    isNewerThanInstalled = isNewer,
                    installedVersionDisplay = installedDisplay,
                    latestVersionDisplay = latestDisplay
                ),
                resolvedRepo = resolvedRepo
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                "Unable to reach GitHub Releases: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    private fun discoverRepoForUser(username: String): String? {
        val url = "https://api.github.com/users/$username/repos?per_page=50&sort=updated"
        val response = httpGet(url) ?: return null
        return try {
            val array = JSONArray(response)
            var fallbackWithRosaline: String? = null
            for (i in 0 until array.length()) {
                val repoObj = array.optJSONObject(i) ?: continue
                val fullName = repoObj.optString("full_name", "")
                val name = repoObj.optString("name", "").lowercase(Locale.US)
                val desc = repoObj.optString("description", "").lowercase(Locale.US)
                if (name == "rosaline" || name.contains("rosaline") || name.contains("lastdrop")) {
                    return fullName
                }
                if (fallbackWithRosaline == null && desc.contains("rosaline")) {
                    fallbackWithRosaline = fullName
                }
            }
            fallbackWithRosaline
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchLatestReleaseJson(repoSlug: String): JSONObject? {
        // 1. Try /releases/latest first
        val latestUrl = "https://api.github.com/repos/$repoSlug/releases/latest"
        val latestResponse = httpGet(latestUrl)
        if (!latestResponse.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(latestResponse)
                if (obj.has("tag_name") || obj.has("html_url")) {
                    return obj
                }
            }
        }

        // 2. Fallback to /releases list (handles prereleases or multiple tags)
        val listUrl = "https://api.github.com/repos/$repoSlug/releases?per_page=5"
        val listResponse = httpGet(listUrl) ?: return null
        return try {
            val array = JSONArray(listResponse)
            if (array.length() > 0) array.optJSONObject(0) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Rosaline-Android-Updater")
            }
            val code = connection.responseCode
            if (code in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compares remote version tag (e.g. "v1.3", "v1.2.4") against installed version (e.g. "1.2" or "1.2.2").
     * If installed build does not have a GitHub Actions run number (e.g. "1.2") and the remote tag is "v1.2.<run>",
     * only treat as newer if the major/minor version is greater (e.g. 1.3 > 1.2) unless installed also tracks run_number.
     */
    fun isRemoteVersionNewer(
        remoteVersion: String,
        installedVersion: String,
        hasRunNumberInstalled: Boolean
    ): Boolean {
        val remoteParts = extractVersionParts(remoteVersion)
        val installedParts = extractVersionParts(installedVersion)
        if (remoteParts.isEmpty() || installedParts.isEmpty()) {
            return remoteVersion.trim() != installedVersion.trim()
        }

        val compareDepth = if (!hasRunNumberInstalled && installedParts.size == 2 && remoteParts.size >= 3) {
            // Compare major.minor (e.g. 1.2 vs 1.3) when local build has no run_number suffix
            2
        } else {
            max(remoteParts.size, installedParts.size)
        }

        for (i in 0 until compareDepth) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = installedParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    internal fun extractVersionParts(raw: String): List<Int> {
        val match = Regex("""(\d+(?:\.\d+)*)""").find(raw) ?: return emptyList()
        return match.value.split(".").mapNotNull { it.toIntOrNull() }
    }
}
