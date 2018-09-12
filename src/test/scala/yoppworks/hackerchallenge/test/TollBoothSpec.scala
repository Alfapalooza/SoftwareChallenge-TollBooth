package yoppworks.hackerchallenge.test

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import yoppworks.hackerchallenges.PaymentProcessor.{Payment, PaymentHistory}
import yoppworks.hackerchallenges.{PaymentProcessor, PriceCents, TollBooth}

import scala.concurrent.duration._

/**
  * Tests which validate the Toll behavior,
  *
  * Instructions:
  * There is a single test as example, but it is not enough, complete the tests to have a solid validation
  */
class TollBoothSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
  val driveToPaymentDuration: FiniteDuration =
    100.milliseconds

  val paymentDuration: FiniteDuration =
    50.milliseconds

  val exitDuration: FiniteDuration =
    100.milliseconds

  val expectedDurationForOneVhl: FiniteDuration =
    driveToPaymentDuration + paymentDuration + exitDuration + 20.milliseconds // 20 millis as margin

  "A TollLane" must {
    "process a single vehicle at time" in {
      val paymentProc =
        system.actorOf(PaymentProcessor.props(paymentDuration))

      val toll =
        system.actorOf(TollBooth.props(paymentProc, 1, driveToPaymentDuration, exitDuration))

      // send 2 vehicles
      for (i <- 1 to 2) {
        toll ! TollBooth.NewVehicle("vhl-" + i)
      }

      // check if lane process it one vhl at a time
      Thread.sleep((expectedDurationForOneVhl + 30.milliseconds).toMillis)

      // after waiting the duration expected for 1 vhl, should have processed only 1
      paymentProc ! PaymentProcessor.AskPaymentHistory
      expectMsg(PaymentHistory(List(Payment("vhl-" + 1, PriceCents(1000)))))
      Thread.sleep((expectedDurationForOneVhl + 30.milliseconds).toMillis)

      // after second wait the second vhl should be processed too
      paymentProc ! PaymentProcessor.AskPaymentHistory
      expectMsgPF() {
        case PaymentHistory(payments) if payments.length == 2 =>
          ()
      }

      system.stop(paymentProc)
      system.stop(toll)
    }
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}