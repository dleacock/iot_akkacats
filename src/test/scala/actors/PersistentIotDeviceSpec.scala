package actors

import actors.PersistentIotDevice.Command._
import actors.PersistentIotDevice.Response._
import actors.PersistentIotDevice._
import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.util.Success

class PersistentIotDeviceSpec
  extends ScalaTestWithActorTestKit(
    ConfigFactory.parseString("akka.actor.allow-java-serialization = on")
      .withFallback(EventSourcedBehaviorTestKit.config))
    with AnyWordSpecLike
    with BeforeAndAfterEach
    with LogCapturing {

  private val id: String = UUID.randomUUID().toString
  private val name: String = "test_device"
  private val probeWaitDuration = FiniteDuration(500, TimeUnit.MILLISECONDS)

  private val eventSourcedTestKit: EventSourcedBehaviorTestKit[Command, Event, State] =
    EventSourcedBehaviorTestKit[Command, Event, State](
      system,
      PersistentIotDevice(id, name))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "A Device" must {
    val device = Device(id, name)
    val alertMessage = "alert_message"

    "be created in inactive state" in {
      val result = eventSourcedTestKit.runCommand[Response](replyTo => GetDeviceState(replyTo))
      result.reply shouldBe GetDeviceStateResponse(Some(Inactive(device)))
      result.stateOfType[Inactive].device shouldBe device
    }

    "not become in an alerting state without initialization" in {
      val testProbe = testKit.createTestProbe[Response]("test_probe")
      val result = eventSourcedTestKit.runCommand(AlertDevice(alertMessage, testProbe.ref))

      testProbe.expectNoMessage(probeWaitDuration)
      result.hasNoEvents shouldBe true
    }

    "start monitoring once initialized" in {
      val result = eventSourcedTestKit.runCommand[Response](replyTo => InitializeDevice(replyTo))
      result.reply shouldBe DeviceInitializedResponse()
      result.stateOfType[Monitoring].device shouldBe device
    }

    "become alerting when device is alerted after being initialized" in {
      eventSourcedTestKit.runCommand[Response](replyTo => InitializeDevice(replyTo))

      val result = eventSourcedTestKit.runCommand[Response](replyTo => AlertDevice(alertMessage, replyTo))
      result.reply shouldBe DeviceAlertedResponse(Success(device))
      result.stateOfType[Alerting].device shouldBe device
    }

    "stop alerting and go back to monitoring" in {
      eventSourcedTestKit.runCommand[Response](replyTo => InitializeDevice(replyTo))
      eventSourcedTestKit.runCommand[Response](replyTo => AlertDevice(alertMessage, replyTo))

      val result = eventSourcedTestKit.runCommand[Response](replyTo => StopAlert(replyTo))
      result.reply shouldBe DeviceStopAlertResponse(Success(device))
      result.stateOfType[Monitoring].device shouldBe device
    }

    "not become disabled without an alert being stopping first" in {
      eventSourcedTestKit.runCommand[Response](replyTo => InitializeDevice(replyTo))
      eventSourcedTestKit.runCommand[Response](replyTo => AlertDevice(alertMessage, replyTo))

      val testProbe = testKit.createTestProbe[Response]("test_probe")
      val result = eventSourcedTestKit.runCommand(DisableDevice(testProbe.ref))

      testProbe.expectNoMessage(probeWaitDuration)
      result.hasNoEvents shouldBe true
    }
  }
}
