package actors

object Device {

  sealed trait Command
  sealed trait Event
  sealed trait Response

  // TODO use case class for state
  case class Device(id: String, state: String)

}
