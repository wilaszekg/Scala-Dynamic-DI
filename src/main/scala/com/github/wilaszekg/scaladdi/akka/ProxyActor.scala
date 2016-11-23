package com.github.wilaszekg.scaladdi.akka

import akka.actor._
import akka.pattern.pipe
import com.github.wilaszekg.scaladdi.Dependencies
import com.github.wilaszekg.scaladdi.akka.ProxyActor.RunDependencies
import com.github.wilaszekg.sequencebuilder.FindAligned
import shapeless.HList

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

class ProxyActor[Deps <: HList, Required <: HList : ClassTag](d: Dependencies[_, Deps],
                                                              create: Required => Props,
                                                              dependenciesTriesMax: Option[Int],
                                                              dependenciesTriesRetryDelay: FiniteDuration,
                                                              supervision: SupervisorStrategy,
                                                              reConfigureAfterTerminated: Boolean,
                                                              dependencyError: Throwable => Any)
  (implicit alignDeps: FindAligned[Deps, Required]) extends Actor with Stash {

  import scala.concurrent.ExecutionContext.Implicits.global


  override def supervisorStrategy: SupervisorStrategy = supervision

  private var failedDependencyTries = 0

  override def preStart(): Unit = {
    super.preStart()
    runDependencies()
  }

  private def runDependencies() =
    d.result.map(alignDeps).recover { case t => DependencyError(t) } pipeTo self

  override def receive = configure

  private def configure: Receive = {
    case deps: Required =>
      unstashAll()
      val proxied = context.actorOf(create(deps))
      context.watch(proxied)
      context become work(proxied)

    case DependencyError(t) =>
      failedDependencyTries += 1
      dependenciesTriesMax match {
        case None => scheduleDependenciesRun()
        case Some(max) if max > failedDependencyTries => scheduleDependenciesRun()
        case _ =>
          context.parent ! dependencyError(t)
          context stop self
      }

    case RunDependencies =>
      runDependencies()

    case _ => stash()
  }

  private def work(proxied: ActorRef): Receive = {
    case Terminated(`proxied`) =>
      if (reConfigureAfterTerminated) reConfigure()
      else context.stop(self)

    case any =>
      if (sender == proxied) {
        context.parent.forward(any)
      } else {
        proxied.forward(any)
      }
  }

  private def scheduleDependenciesRun() = context.system.scheduler.scheduleOnce(dependenciesTriesRetryDelay, self, RunDependencies)

  private def reConfigure() = {
    context become configure
    failedDependencyTries = 0
    runDependencies()
  }

  private case class DependencyError(t: Throwable)

}

object ProxyActor {
  private case object RunDependencies
}