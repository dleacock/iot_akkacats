package app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import notifier.HttpNotifier

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object IotApp {

  def main(args: Array[String]): Unit = {
    println("*** Running ***")

    implicit val system: ActorSystem[_] =
      ActorSystem(Behaviors.empty, "IotApp")
    implicit val ec: ExecutionContext = system.executionContext

    val notifier = new HttpNotifier("http://www.google.ca")
    notifier.sendNotification.onComplete {
      case Success(value) => system.log.info(s"Success: App got back $value")
      case Failure(exception) =>
        system.log.info(s"Failure: App got ${exception.getMessage}")
    }


  }
}
