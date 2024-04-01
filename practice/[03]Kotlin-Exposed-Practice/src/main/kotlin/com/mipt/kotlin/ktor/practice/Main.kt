import com.mipt.kotlin.ktor.practice.api.commentsApi
import com.mipt.kotlin.ktor.practice.commentsModule
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {

    embeddedServer(Netty, port = 8080) {

        configureServer()

        commentsApi()
    }.start(wait = true)

}

fun Application.configureServer() {
    install(Koin) {
        modules(commentsModule)
    }
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }
}