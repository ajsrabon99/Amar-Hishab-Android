package com.ajyra.amarhishab.network

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()

    data class Error(
        val messageEn: String,
        val messageBn: String,
        val code: Int? = null,
        val isAuthError: Boolean = false
    ) : NetworkResult<Nothing>() {
        fun getLocalizedMessage(isBengali: Boolean): String = if (isBengali) messageBn else messageEn
    }

    object Loading : NetworkResult<Nothing>()
}

object NetworkErrorParser {
    fun parse(throwable: Throwable, isLoginAttempt: Boolean = false): NetworkResult.Error {
        val msg = throwable.message ?: ""
        val localized = throwable.localizedMessage ?: "Unknown error"

        if (msg.contains("401", ignoreCase = true) || msg.contains("Unauthorized", ignoreCase = true)) {
            return if (isLoginAttempt) {
                NetworkResult.Error(
                    messageEn = "Backend authentication failed (401 Unauthorized): The Google token was rejected by the server.",
                    messageBn = "সার্ভারে গুগল প্রমাণীকরণ ব্যর্থ হয়েছে (৪০১ Unauthorized): টোকেনটি সার্ভার গ্রহণ করেনি।",
                    code = 401,
                    isAuthError = true
                )
            } else {
                NetworkResult.Error(
                    messageEn = "Your session has expired. Please sign in again.",
                    messageBn = "আপনার সেশনের মেয়াদ শেষ হয়েছে। অনুগ্রহ করে পুনরায় লগইন করুন।",
                    code = 401,
                    isAuthError = true
                )
            }
        }
        if (msg.contains("404", ignoreCase = true) || msg.contains("Not Found", ignoreCase = true)) {
            return if (isLoginAttempt) {
                NetworkResult.Error(
                    messageEn = "Authentication endpoint not found on server (404 Not Found): The backend API route is missing.",
                    messageBn = "সার্ভারে প্রমাণীকরণ এন্ডপয়েন্ট পাওয়া যায়নি (৪০৪ Not Found): ব্যাকএন্ড এপিআই রুট অনুপস্থিত।",
                    code = 404,
                    isAuthError = false
                )
            } else {
                NetworkResult.Error(
                    messageEn = "Requested resource not found on the server.",
                    messageBn = "সার্ভারে কাঙ্ক্ষিত তথ্য পাওয়া যায়নি।",
                    code = 404,
                    isAuthError = false
                )
            }
        }
        if (msg.contains("429", ignoreCase = true)) {
            return NetworkResult.Error(
                messageEn = "Too many requests. Please wait a moment and try again.",
                messageBn = "অতিরিক্ত রিকোয়েস্ট পাঠানো হয়েছে। কিছুক্ষণ পর পুনরায় চেষ্টা করুন।",
                code = 429,
                isAuthError = false
            )
        }
        if (msg.contains("500", ignoreCase = true) ||
            msg.contains("502", ignoreCase = true) ||
            msg.contains("503", ignoreCase = true)
        ) {
            return NetworkResult.Error(
                messageEn = "Amar Hishab server is currently unavailable or waking up. Please try again shortly.",
                messageBn = "আমার হিসাব সার্ভার এই মুহূর্তে চালিত হচ্ছে। অনুগ্রহ করে একটু পর পুনরায় চেষ্টা করুন।",
                code = 500,
                isAuthError = false
            )
        }
        if (msg.contains("timeout", ignoreCase = true) ||
            msg.contains("SocketTimeout", ignoreCase = true)
        ) {
            return NetworkResult.Error(
                messageEn = "Connection timed out. The server might be waking up from sleep. Retrying recommended.",
                messageBn = "সার্ভারের সংযোগে সময় লেগেছে (টাইমআউট)। সার্ভার চালু হচ্ছে, অনুগ্রহ করে আবার চেষ্টা করুন।",
                code = -1,
                isAuthError = false
            )
        }
        if (msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("ConnectException", ignoreCase = true)
        ) {
            return NetworkResult.Error(
                messageEn = "No internet connection. Operating in secure offline mode.",
                messageBn = "ইন্টারনেট সংযোগ নেই। নিরাপদে অফলাইন মোডে সংরক্ষিত হচ্ছে।",
                code = -2,
                isAuthError = false
            )
        }
        return NetworkResult.Error(
            messageEn = "An unexpected error occurred: $localized",
            messageBn = "একটি অপ্রত্যাশিত ত্রুটি ঘটেছে। অনুগ্রহ করে আবার চেষ্টা করুন।",
            code = -3,
            isAuthError = false
        )
    }
}
