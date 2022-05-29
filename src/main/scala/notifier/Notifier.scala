package notifier

import scala.concurrent.Future

trait Notifier {
  def sendNotification: Future[String]
  def getType: String
}
