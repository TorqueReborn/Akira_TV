package com.ghostreborn.akira.features.home.services

class CaptchaRequiredException(
    val statusCode: Int? = null,
    val errorCode: String? = "ERROR_NEED_CAPTCHA",
    val rawResponse: String? = null,
    override val message: String = "Security verification required (CAPTCHA)."
) : Exception(message)
