package yoppworks.hackerchallenges

import akka.actor.{Actor, Props}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

/**
  * Actor which handle the payment
  *
  * Instructions:
  *
  * A ) There is a "paymentDuration" between receive the payment and payment written in payments logs.
  * The actor receive a payment and should return an AcceptedPayment message after paymentDuration.
  * It can process multiple payment in parallel
  *
  * B ) A consumer can send a PaymentProcessor.AskPaymentHistory message to the logs of payments as response (PaymentHistory as response)
  */
object PaymentProcessor {
  // Payment Message Protocol
  case class Payment(vehicleId: String, priceCents: PriceCents)
  case class AcceptedPayment(vehicleId: String)

  // Payment History Message Protocol
  case object AskPaymentHistory
  case class PaymentHistory(payments: List[Payment])

  def props(paymentDuration: FiniteDuration = 50.milliseconds) =
    Props(classOf[PaymentProcessor], paymentDuration)
}

class PaymentProcessor(paymentDuration: FiniteDuration) extends Actor {
  val payments: ArrayBuffer[PaymentProcessor.Payment] =
    ArrayBuffer[PaymentProcessor.Payment]() // logs payments here

  override def receive: Receive = {
    case payment: PaymentProcessor.Payment =>
      val currSender =
        sender()

      context
        .system
        .scheduler
        .scheduleOnce(Duration.fromNanos(paymentDuration.toNanos)) {
          payments += payment
          currSender ! PaymentProcessor.AcceptedPayment(payment.vehicleId)
        }(ThreadPools.Scheduler)

    case PaymentProcessor.AskPaymentHistory =>
      sender() ! PaymentProcessor.PaymentHistory(payments.toList)
  }
}

