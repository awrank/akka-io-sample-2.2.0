import akka.actor.{ActorSystem, Props}

/**
 * サーバを起動する
 */
object Main extends App {
  val system = ActorSystem("Sample")
  val server = system.actorOf(Props[Server], "Server")
  system.awaitTermination
}
