package com.dariopellegrini.kdone.middlewares

import com.dariopellegrini.kdone.exceptions.MiddlewareInputException
import com.dariopellegrini.kdone.extensions.respondWithException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*

fun Route.middlewares(endpoint: String,
                      middlewares: Array<out suspend (MiddlewareInput) -> Any>,
                      method: HttpMethod = HttpMethod.Get,
                      responseClosure: ((MiddlewareInput) -> Unit)? = null,
                      exceptionClosure: ((Exception) -> Unit)? = null) {
    install(DoubleReceive)

    val body: PipelineInterceptor<Unit, ApplicationCall> = {
        try {
            var currentStatus: Any = Unit
            middlewares.forEach {
//                if (call.response.isCommitted) return@forEach
                val result = it(MiddlewareInput(call, currentStatus))
                currentStatus = result
            }
            responseClosure?.invoke(MiddlewareInput(call, currentStatus))
//            if (!call.response.isCommitted) {
//                responseClosure?.invoke(MiddlewareInput(call, currentStatus))
//            }
        } catch (e: Exception) {
            exceptionClosure?.let {
                it(e)
            } ?: run {
                call.respondWithException(e)
            }
        }
    }

    route(endpoint, method) { handle(body) }
}

class Wrapper<T>(val base: T)
val Route.middlewares: Wrapper<Route> get() = Wrapper(this)

fun Wrapper<Route>.get(endpoint: String,
                       vararg middlewares: suspend (MiddlewareInput) -> Any,
                       responseClosure: ((MiddlewareInput) -> Unit)? = null,
                       exceptionClosure: ((Exception) -> Unit)? = null) {
    base.middlewares(endpoint, middlewares, HttpMethod.Get, responseClosure, exceptionClosure)
}

fun Wrapper<Route>.post(endpoint: String,
                       vararg middlewares: suspend (MiddlewareInput) -> Any,
                       responseClosure: ((MiddlewareInput) -> Unit)? = null,
                       exceptionClosure: ((Exception) -> Unit)? = null) {
    base.middlewares(endpoint, middlewares, HttpMethod.Post, responseClosure, exceptionClosure)
}

fun Wrapper<Route>.patch(endpoint: String,
                       vararg middlewares: suspend (MiddlewareInput) -> Any,
                       responseClosure: ((MiddlewareInput) -> Unit)? = null,
                       exceptionClosure: ((Exception) -> Unit)? = null) {
    base.middlewares(endpoint, middlewares, HttpMethod.Patch, responseClosure, exceptionClosure)
}

fun Wrapper<Route>.delete(endpoint: String,
                       vararg middlewares: suspend (MiddlewareInput) -> Any,
                       responseClosure: ((MiddlewareInput) -> Unit)? = null,
                       exceptionClosure: ((Exception) -> Unit)? = null) {
    base.middlewares(endpoint, middlewares, HttpMethod.Delete, responseClosure, exceptionClosure)
}

data class MiddlewareInput(val call: ApplicationCall, val body: Any) {
    inline fun <reified T : Any> get(): T =
        (body as? T) ?: throw MiddlewareInputException("Error casting $body to ${T::class.simpleName}")
}