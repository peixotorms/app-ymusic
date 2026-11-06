package io.ymusic.ui

import io.ymusic.remoteconfig.RemoteConfigData

/** Project links stay with this fork even if a service supplies upstream defaults. */
internal class AppLinks(@Suppress("UNUSED_PARAMETER") config: RemoteConfigData?) {
    private val repository = "https://github.com/peixotorms/app-ymusic"
    val website: String = repository
    val telegram: String = repository
    val feedbackEmail: String = ""
    val feedbackIssue: String = "$repository/issues/new"
    val sourceCode: String = repository
    val terms: String = "$repository/blob/master/TERMS.md"
    val changelog: String = "$repository/blob/master/CHANGELOG.md"
}
