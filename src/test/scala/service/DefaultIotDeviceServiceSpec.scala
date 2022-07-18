package service
import actors.PersistentDevice
import actors.PersistentDevice.Command
import actors.PersistentDevice.Command.InitializeDevice
import akka.Done
import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.IdiomaticMockito
import org.mockito.MockitoSugar.{verify, when}
import org.mockito.captor.{ArgCaptor, Captor}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, EitherValues}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// TODO do I need all these traits?
class DefaultIotDeviceServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with IdiomaticMockito
    with BeforeAndAfterEach
    with ScalaFutures
    with EitherValues {

  type ActorRefF[T] = ActorRef[T] => Command

  private val mockClusterSharding = mock[ClusterSharding]
  private val mockReplyTo = mock[ActorRef[Done]]
  private val mockEntityRef = mock[EntityRef[Command]]
  private val id = UUID.randomUUID().toString

  private val service = new DefaultIotDeviceService(mockClusterSharding)

  override def beforeEach(): Unit = reset(mockClusterSharding)

  // TODO decouple the command from the replyTo

  "registerDevice" should {
    "register the device" in {
      val command = InitializeDevice(mockReplyTo)

      when(mockClusterSharding.entityRefFor(PersistentDevice.TypeKey, id))
        .thenReturn(mockEntityRef)

      when(mockEntityRef.ask(any[ActorRefF[Done]])(any[Timeout]))
        .thenReturn(Future(Done))

      val result: Future[Either[String, Done]] = service.registerDevice(id)

      result.futureValue shouldBe Right(Done)

      assertCommand(command)

    }
  }

  "processDeviceEvent" should {}

  "retrieveDevice" should {}

  private def assertCommand[T](command: Command): Unit = {
    val argCaptor: Captor[ActorRefF[T]] = ArgCaptor[ActorRefF[T]]

    verify(mockEntityRef).ask(argCaptor.capture)(any[Timeout])

    val testRef = mock[ActorRef[T]]

    val captured: ActorRefF[T] = argCaptor.value
    captured(testRef) shouldBe command

  }

}
