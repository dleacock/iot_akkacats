package app

import actors.{NotifierActor, PersistentDevice}
import akka.Done
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.persistence.typed.PersistenceId
import notifier.{HttpNotifier, Notifier}
import service.{DefaultIotDeviceService, IotDeviceService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object IotApp {

  def main(args: Array[String]): Unit =
    ActorSystem[Nothing](appBehavior(), "iot-app")

  private def appBehavior(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[_] = context.system

      val service: IotDeviceService = initService

      service.registerDevice(UUID.randomUUID().toString)

      Behaviors.empty
    }
  }

  private def initService(implicit system: ActorSystem[_]): IotDeviceService = {
    val sharding = ClusterSharding(system)

    // TODO how is ClusterSharding knowing what ID to use to map to actor?
    // TODO is this the right location for it?
    sharding.init(Entity(typeKey = PersistentDevice.TypeKey) { entityContext =>
      PersistentDevice(
        PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
      )
    })

    val notifier = new Notifier[Done] {
      override def sendNotification: Future[Done] = {
        system.log.info("Sending notification")
        Future.successful(Done)
      }

      override def getType: String = "dummy"
    }

    val notifierActor: Behavior[NotifierActor.NotifierMessage] = NotifierActor(notifier)

    new DefaultIotDeviceService(sharding, notifierActor)

  }
}
