package yoppworks.hackerchallenges

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object ThreadPools {
  lazy val Scheduler: ExecutionContextExecutor =
    ExecutionContext.global
}
