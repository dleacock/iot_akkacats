package notifier

import scala.concurrent.Future

class ConsoleLoggingNotifier extends Notifier {
  override def getType: String = "ConsoleLogging"
  override def sendNotification: Future[String] = {
    println("Sending notification... sent")
    Future.successful("Done")
  }
}