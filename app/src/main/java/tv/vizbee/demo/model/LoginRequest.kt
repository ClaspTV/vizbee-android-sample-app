package tv.vizbee.demo.model

data class LoginRequest(
    val email: String,
    val pwd: String,
    val deviceId: String
)
