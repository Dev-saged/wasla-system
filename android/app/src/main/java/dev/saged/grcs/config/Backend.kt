package dev.saged.grcs.config

/** إعدادات الخادم العامة (مفتاح العميل عام بطبيعته؛ الحماية في قواعد Firestore). */
object Backend {
    const val PROJECT_ID = "gaza-camps-grcs"
    const val API_KEY = "AIzaSyAfSFBhTJvrTlvAvLUAvL3PriYQHqSHgDk"
    const val WORKER = BrandConfig.ADMIN_API
    const val FIRESTORE = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"
    const val IDENTITY = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=$API_KEY"
}
