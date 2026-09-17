package com.ajyra.amarhishab.presentation.viewmodel

sealed class DeepLinkParseResult {
    data class Success(val code: String) : DeepLinkParseResult()
    data class AuthError(val error: String, val description: String?) : DeepLinkParseResult()
    object MissingCode : DeepLinkParseResult()
    object InvalidUri : DeepLinkParseResult()
}

object DeepLinkValidator {
    const val EXPECTED_SCHEME = "amarhishab"
    const val EXPECTED_HOST = "auth"
    const val EXPECTED_PATH = "/callback"

    fun validateAndExtractCode(
        scheme: String?,
        host: String?,
        path: String?,
        error: String?,
        errorDescription: String?,
        codeParam: String?,
        fragment: String?
    ): DeepLinkParseResult {
        if (scheme == null || !scheme.equals(EXPECTED_SCHEME, ignoreCase = true)) {
            return DeepLinkParseResult.InvalidUri
        }

        val rawHost = host?.lowercase().orEmpty()
        val rawPath = path.orEmpty()
        val normalizedPath = if (rawPath.endsWith("/") && rawPath.length > 1) rawPath.dropLast(1) else rawPath

        val isValidCallback = (rawHost == EXPECTED_HOST && (normalizedPath == EXPECTED_PATH || normalizedPath.isEmpty())) ||
                (rawHost == "callback" && (normalizedPath.isEmpty() || normalizedPath == "/"))

        if (!isValidCallback) {
            return DeepLinkParseResult.InvalidUri
        }

        if (!error.isNullOrBlank()) {
            return DeepLinkParseResult.AuthError(error, errorDescription)
        }

        var extractedCode = codeParam?.trim()
        if (extractedCode.isNullOrBlank() && !fragment.isNullOrBlank()) {
            val fragParams = fragment.split("&").associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (if (parts.size > 1) parts[1] else "")
            }
            extractedCode = (fragParams["code"] ?: fragParams["auth_code"])?.trim()
        }

        return if (!extractedCode.isNullOrBlank()) {
            DeepLinkParseResult.Success(extractedCode)
        } else {
            DeepLinkParseResult.MissingCode
        }
    }
}
