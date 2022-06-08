package playground.clustering

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object EchoActor {
  sealed trait Command
  case class Echo(word: String)(val replyTo: ActorRef[Echoing]) extends Command
  case class Echoing(word: String, numberOfTimes: Int) extends Command
  case class Echoed(word: String)

  case class EchoedWords(words: Set[String]) {
    def add(word: String): EchoedWords = copy(words = words + word)

    def totalEchos: Int = words.size
  }

  private val commandHandler: (EchoedWords, Command) => Effect[Echoed, EchoedWords] = {
    (_, command) =>
      command match {
        case echo @ Echo(word) =>
          Effect.persist(Echoed(word))
          .thenRun(state => echo.replyTo ! Echoing(echo.word, state.totalEchos))
      }
  }

  private val eventHandler: (EchoedWords, Echoed) => EchoedWords = {
    (state, event) => state.add(event.word)
  }

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("EchoActor")

  def apply(entityId: String, persistenceId: PersistenceId): Behavior[Command] = {
    Behaviors.setup{ context =>
      context.log.info(s"Starting EchoActor $entityId")
      EventSourcedBehavior(
        persistenceId,
        emptyState = EchoedWords(Set.empty),
        commandHandler,
        eventHandler
      )
    }
  }
}



class ClusterShardingPG2 {

}
