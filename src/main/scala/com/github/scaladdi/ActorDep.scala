package com.github.scaladdi

import akka.actor.ActorRef
import shapeless._

case class ActorDep[Question <: HList, R](who: ActorRef, question: Question => Any) {
  type Response = R
}
