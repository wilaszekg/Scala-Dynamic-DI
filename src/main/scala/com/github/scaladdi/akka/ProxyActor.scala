package com.github.scaladdi.akka

import akka.actor._
import akka.pattern.pipe
import com.github.scaladdi.{FindAligned, FutureDependencies}
import shapeless.HList

import scala.reflect.ClassTag

class ProxyActor[Dependencies <: HList, Required <: HList : ClassTag]
(d: => FutureDependencies[_, Dependencies], create: Required => Props, dependencyError: Throwable => Any)
  (implicit alignDeps: FindAligned[Dependencies, Required]) extends Actor with Stash {

  import scala.concurrent.ExecutionContext.Implicits.global

  private case class DependencyError(t: Throwable)

  override def preStart(): Unit = {
    super.preStart()
    d.result.map(alignDeps).recover { case t => DependencyError(t) } pipeTo self
  }

  override def receive: Receive = {
    case deps: Required =>
      unstashAll()
      val proxied = context.actorOf(create(deps))
      context.watch(proxied)
      context become work(proxied)
    case this.DependencyError(t) => context.parent ! dependencyError(t)
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
}