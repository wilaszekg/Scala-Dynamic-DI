package com.github.scaladdi

import akka.actor.ActorRef
import shapeless._
import shapeless.ops.function.FnToProduct
import shapeless.syntax.std.function._


case class ActorDep[Question <: HList, R](who: ActorRef, question: Question => Any) {
  type Response = R
}

object ActorDep {
  def apply[F, Question <: HList, R](who: ActorRef, question: F, c: Class[R])(implicit fnToProduct: FnToProduct.Aux[F, Question => Any]) =
    ActorDep[Question, R](who, question.toProduct)
}