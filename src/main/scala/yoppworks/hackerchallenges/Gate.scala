package yoppworks.hackerchallenges

import akka.actor.{Actor, ActorLogging, Props}
import scala.util.Random

/**
  * The gate which forbid vehicles to go forward until they pay
  *
  * Instructions: This implementation is completed and doesn't require change
  */
object Gate {
  // Open Gate Message Protocol
  case class OpenTheGate(vehicleId: String)
  case class OpenedGate(vehicleId: String)

  // Exceptions
  case class GateBrokeDown(msg: String) extends RuntimeException

  def props(failureRate: Double) =
    Props(classOf[Gate], failureRate)
}
case class Gate(failureRate: Double) extends Actor with ActorLogging {
  override def receive: Receive =  {
    case Gate.OpenTheGate(vehicleId: String) =>
      if(Random.nextDouble() < failureRate) {
        log.error("The gate broke down!")
        throw Gate.GateBrokeDown("The gate broke down!")
      } else {
        sender() ! Gate.OpenedGate(vehicleId)
      }
  }
}
