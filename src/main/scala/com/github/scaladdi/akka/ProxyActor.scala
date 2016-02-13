package com.github.scaladdi.akka

import akka.actor._
import akka.pattern.pipe
import com.github.scaladdi.{FindAligned, FutureDependencies}
import shapeless.HList

import scala.reflect.ClassTag

class ProxyActor[Dependencies <: HList, Required <: HList : ClassTag]
(d: => FutureDependencies[_, Dependencies],
  create: Required => Props,
  dependenciesRetriesMax: Option[Int],
  dependencyError: Throwable => Any)
  (implicit alignDeps: FindAligned[Dependencies, Required]) extends Actor with Stash {

  import scala.concurrent.ExecutionContext.Implicits.global

  private var failedDependencyTries = 0

  override def preStart(): Unit = {
    super.preStart()
    getDependencies()
  }

  private def getDependencies() =
    d.result.map(alignDeps).recover { case t => DependencyError(t) } pipeTo self

  override def receive: Receive = {
    case deps: Required =>
      unstashAll()
      val proxied = context.actorOf(create(deps))
      context.watch(proxied)
      context become work(proxied)

    case DependencyError(t) =>
      failedDependencyTries += 1
      dependenciesRetriesMax match {
        case None => getDependencies()
        case Some(max) if max > failedDependencyTries => getDependencies()
        case _ => context.parent ! dependencyError(t)
      }
    case _ => stash()
  }

  private def work(proxied: ActorRef): Receive = {
    case Terminated(`proxied`) => context.stop(self)
    case any => if (sender == proxied) {
      context.parent.forward(any)
    } else {
      proxied.forward(any)
    }
  }

  private case class DependencyError(t: Throwable)

}