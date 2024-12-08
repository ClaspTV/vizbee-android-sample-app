package tv.vizbee.demo.model

import tv.vizbee.config.controller.IDUtils

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String = IDUtils.getMyDeviceID()
)
