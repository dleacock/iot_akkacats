package notifier

import akka.Done

import scala.concurrent.Future

object HttpNotifier extends Notifier[Done]{
  override def sendNotification: Future[Done] = ???

  override def getType: String = "HttpNotifier"

  // TODO inject in akka http
  def apply(): Unit ={
  }
}
