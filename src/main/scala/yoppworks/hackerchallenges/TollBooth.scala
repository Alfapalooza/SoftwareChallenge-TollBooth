package yoppworks.hackerchallenges

import java.util.Date

import akka.actor.{Actor, ActorRef, Props}
import yoppworks.hackerchallenges.TollBooth.LoggedVehicle

import akka.routing.RoundRobinPool

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

/**
  * The TollBooth regroups all the lanes
  *
  * Instructions:
  *
  * A ) Responsible to create the vehicles and dispatch them to each TollLane when receiving a NewVehicle message
  *
  * B ) The toll booth should log the enter and exit time of each vehicle. These statistics are important to monitor if each vehicle received a fast enough service.
  *     When receiving a AskStatistics message, Toll should respond with Statistics(vehicles: List[LoggedVehicle])
  */
object TollBooth {
  // Vehicle Message Protocol
  case class NewVehicle(vehicleId: String)
  case class LoggedVehicle(vehicleId: String, timeIn: Date, timeOut: Date)

  // Statistics Message Protocol
  case object AskStatistics
  case class Statistics(vehicles: List[LoggedVehicle])

  def props(paymentProcessor: ActorRef, gatesCount: Int = 1,
            driveToPaymentDuration: FiniteDuration = 100.milliseconds, exitDuration: FiniteDuration = 100.milliseconds,
            priceInCents: Int = 1000, failureRate: Double = 0.0) =
    Props(classOf[TollBooth], paymentProcessor, gatesCount, driveToPaymentDuration, exitDuration, priceInCents, failureRate)
}

class TollBooth(paymentProcessor: ActorRef, gatesCount: Int, driveToPaymentDuration: FiniteDuration, exitDuration: FiniteDuration, priceInCents: Int, failureRate: Double) extends Actor {
  private val timeLogs =
    ArrayBuffer[LoggedVehicle]()

  private val carsInTransit: mutable.Map[String, Date] =
    mutable.Map[String, Date]()

  private val tollLanes =
    context.actorOf(
      RoundRobinPool(gatesCount)
        .props(
          TollLane.props(
            paymentProcessor,
            self,
            driveToPaymentDuration,
            exitDuration,
            priceInCents,
            failureRate)
        ), "router")

  override def receive: Receive = {
    case TollBooth.NewVehicle(vehicleId) =>
      carsInTransit += (vehicleId -> new Date())
      tollLanes ! TollLane.SendPayment(vehicleId, PriceCents(priceInCents))

    case TollLane.ExitAck(vehicleId) =>
      val timeIn =
        carsInTransit(vehicleId)

      timeLogs += LoggedVehicle(vehicleId, timeIn, new Date())
      carsInTransit -= vehicleId

    case TollBooth.AskStatistics =>
      sender() ! TollBooth.Statistics(timeLogs.toList)
  }
}
