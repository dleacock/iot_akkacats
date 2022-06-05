package service

import akka.Done

import scala.concurrent.Future

trait IotDeviceService {
  // TODO use ADT for param
  def registerDevice(id: String): Future[Either[String, Done]]
  def processDeviceEvent(): Future[Either[String, Done]]
  def retrieveDevice(): Future[Either[String, Done]]
}
