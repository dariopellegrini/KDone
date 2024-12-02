package auth

import com.dariopellegrini.kdone.auth.UserAuth
import com.dariopellegrini.kdone.exceptions.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import org.bson.types.ObjectId
import org.litote.kmongo.Id
import org.litote.kmongo.id.toId
import java.net.URLDecoder


// For JWT
val ApplicationCall.userAuth: UserAuth
    get() = (authentication.principal as? UserAuth)?.copy() ?: throw NotAuthorizedException()


val ApplicationCall.userAuthOrNull: UserAuth?
    get() = (authentication.principal as? UserAuth)?.copy()

inline infix fun <T> T?.guard(block: T?.() -> Nothing): T {
    return this ?: block()
}

//@Throws(Exception::class)
//// reified: pass to have that class type
//inline fun <reified T> Gson.fromJson(jsonString: String): T {
//    return this.fromJson<T>(jsonString, object: TypeToken<T>() {}.type)
//}
//
//@Throws(Exception::class)
//inline fun <reified T> Gson.fromJson(jsonObject: JsonObject): T {
//    return this.fromJson<T>(jsonObject, object: TypeToken<T>() {}.type)
//}
//
//@Throws(Exception::class)
//inline fun <reified T> Gson.fromJson(map: Map<String, Any>): T {
//    return this.fromJson<T>(map.toString(), object: TypeToken<T>() {}.type)
//}

fun <T>String.mongoId(): Id<T> = ObjectId(this).toId()
