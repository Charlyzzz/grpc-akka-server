package live.integration

import live.SubRequest

import scala.util.{Failure, Success}

object LiveConsumer extends GrpcIntegrationClient {

  val respuestas = client.subscribe(SubRequest("topic"))

  val done = respuestas.runFold(0)((numero, evento) => {
    println(s"Msg #$numero: ${ evento.message }")
    numero + 1
  })

  done.onComplete {
    case Success(_) =>
      println("stream finalizado")
    case Failure(e) =>
      println(s"Error: $e")
  }
}
