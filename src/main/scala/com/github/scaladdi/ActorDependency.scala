package com.github.scaladdi

import akka.actor.ActorRef
import akka.util.Timeout
import shapeless._
import shapeless.ops.function.FnToProduct
import shapeless.ops.hlist.Tupler
import shapeless.syntax.std.function._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag


case class ActorDependency[Question, R: ClassTag](who: ActorRef, question: Question => Any)
  (implicit timeout: Timeout, ec: ExecutionContext) extends FutureDependency[R, Question] {
  type Response = R

  override def apply(args: Question): Future[R] = {
    import akka.pattern.ask
    who ? question(args) collect { case r: R => r }
  }
}

object ActorDependency {
  def apply[F, Question <: HList, R: ClassTag, Q <: Any, P <: Product](who: ActorRef, question: F, c: Class[R])
    (implicit fnToProduct: FnToProduct.Aux[F, Question => Q],
      tupl: Tupler.Aux[Question, P],
      gen: Generic.Aux[P, Question],
      timeout: Timeout, ec: ExecutionContext) =
    ActorDependency[P, R](who, (tuple: P) => question.toProduct(gen.to(tuple)))
}