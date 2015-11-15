package com.github.scaladdi

import akka.actor.Actor
import akka.pattern.pipe

/*class ProxyActor[Futures <: HList, Values <: HList, DepFutures <: HList, DepValues <: HList, AD <: HList, FAD <: HList, Responses <: HList, Failures <: HList]
(d: ActorDependencies[Futures, Values, DepFutures, DepValues, AD, FAD, Responses, Failures]) extends Actor {*/
class ProxyActor(d: ActorDependencies[_, _, _, _, _, _, _, _]) extends Actor {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def preStart(): Unit = {
    super.preStart()
    d.futureDeps.result pipeTo self
  }

  override def receive: Receive = {
    case _ =>
  }
}