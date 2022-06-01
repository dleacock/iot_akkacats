package service

import akka.Done

import scala.concurrent.Future

trait IotDeviceService {
  def registerDevice(): Future[Either[String, Done]]
  def processDeviceEvent(): Future[Either[String, Done]]
  def retrieveDevice(): Future[Either[String, Done]]
}
