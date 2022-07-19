package app

import actors.PersistentDevice
import actors.PersistentDevice.Command
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import http.{IotDeviceServiceWebServer, Routes}
import service.DefaultIotDeviceService

import scala.concurrent.ExecutionContext.Implicits.global

object IotApp {

  def main(args: Array[String]): Unit =
    ActorSystem[Command](behavior(), "iot-app")

  private def behavior(): Behavior[Command] = {
    Behaviors.setup { context =>
      val system: ActorSystem[_] = context.system
      val sharding = ClusterSharding(system)

      sharding.init(Entity(PersistentDevice.TypeKey) { entityContext =>
        PersistentDevice(entityContext.entityId)
      })

      val iotDeviceService = new DefaultIotDeviceService(sharding)

      IotDeviceServiceWebServer.start(
        routes = new Routes(iotDeviceService).routes,
        port = 1234,
        system
      )

      Behaviors.empty
    }
  }
}
