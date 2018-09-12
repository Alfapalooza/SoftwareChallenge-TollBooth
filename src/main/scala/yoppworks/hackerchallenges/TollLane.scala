package yoppworks.hackerchallenges

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.concurrent.duration.Duration

/**
  * A single TollLane
  *
  * Instructions:
  * Only one vehicle can be present in the lane at a time,
  *
  * A ) Vehicle enter the gate and drive to the barrier/payment station => should simulate a duration of driveToPaymentDuration before next step
  *
  * B ) Vehicle pay by sending TollGate.SendPayment message to the paymentProcessor, then it should wait the payment confirmation (PaymentProcessor.AcceptedPayment message)
  *
  * C ) Once payment is accepted, send Gate.OpenGate message to "gate" actor and it will respond with a Gate.OpenedGate message meaning the vehicle can move forward
  *
  * D ) Vehicle exit the gate => should simulate a exitDuration + It should send a ExitAck message to the Toll to confirm the vehicle exited properly
  *
  * F ) Despite having a brand new software using Akka, the hardware is old and unreliable. The gate has a failureRate, when you send a OpenGate message,
  *     The gate may fail (throw an Exception). You have to recover properly when it happens: In this simulation, to repair the gate you have to restart the gate actor.
  *     Note: Once recovery is done, the vehicle should be able to continue its progress.
  */
object TollLane{
  // TollLane Message Protocol
  case class SendPayment(vehicleId: String, cents: PriceCents)
  case class ExitAck(vehicleId: String) // confirm that the vehicle exited properly

  def props(paymentProcessor: ActorRef, tollBooth: ActorRef, driveToPaymentDuration: Duration, exitDuration: Duration, priceInCents: Int, failureRate: Double) =
    Props(classOf[TollLane], paymentProcessor, tollBooth, driveToPaymentDuration, exitDuration, priceInCents, failureRate)
}

class TollLane(paymentProcessor: ActorRef, tollBooth: ActorRef, driveToPaymentDuration: Duration, exitDuration: Duration, priceInCents: Int, failureRate: Double) extends Actor with ActorLogging {
  val gate: ActorRef =
    context.actorOf(Gate.props(failureRate))

  def receive: Receive = {
    case TollLane.SendPayment(vehicleId, cents) =>
      context
        .system
        .scheduler
        .scheduleOnce(Duration.fromNanos(driveToPaymentDuration.toNanos)) {
          paymentProcessor ! PaymentProcessor.Payment(vehicleId, cents)
        }(ThreadPools.Scheduler)

    case PaymentProcessor.AcceptedPayment(vehicleId) =>
      gate ! Gate.OpenTheGate(vehicleId)

    case Gate.OpenedGate(vehicleId) =>
      tollBooth ! TollLane.ExitAck(vehicleId)
  }
}
