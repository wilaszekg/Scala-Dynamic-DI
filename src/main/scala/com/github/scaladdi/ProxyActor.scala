package com.github.scaladdi

import akka.actor.{ActorRef, Stash, Props, Actor}
import akka.pattern.pipe
import shapeless.HList

import scala.reflect.ClassTag

/*class ProxyActor[Futures <: HList, Values <: HList, DepFutures <: HList, DepValues <: HList, AD <: HList, FAD <: HList, Responses <: HList, Failures <: HList]
(d: ActorDependencies[Futures, Values, DepFutures, DepValues, AD, FAD, Responses, Failures]) extends Actor {*/
class ProxyActor[Dependencies <: HList, Required <: HList : ClassTag]
(d: FutureDependencies[_, Dependencies], create: Required => Props, dependencyError: Throwable => Any)
  (implicit alignDeps: AlignReduce[Dependencies, Required]) extends Actor with Stash {

  import scala.concurrent.ExecutionContext.Implicits.global

  private case class DependencyError(t: Throwable)

  override def preStart(): Unit = {
    super.preStart()
    d.result.map(alignDeps).recover { case t => DependencyError(t) } pipeTo self
  }

  override def receive: Receive = {
    case deps: Required =>
      unstashAll()
      context become work(context.actorOf(create(deps)))
    case this.DependencyError(t) => context.parent ! dependencyError(t)
    case _ => stash()
  }

  private def work(proxied: ActorRef): Receive = {
    case any => if (sender == proxied) {
      context.parent ! any
    } else {
      proxied.forward(any)
    }
  }
}