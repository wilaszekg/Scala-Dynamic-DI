package com.github.wilaszekg.scaladdi.akka

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import shapeless._
import shapeless.ops.function.{FnFromProduct, FnToProduct}
import shapeless.syntax.std.function._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object ActorDependency {
  def apply[F, Args <: HList, R: ClassTag, Q, RF](who: ActorRef, question: F, c: Class[R])
                                                 (implicit fnToProduct: FnToProduct.Aux[F, Args => Q],
                                                  resultFunction: FnFromProduct.Aux[Args => Future[R], RF],
                                                  rfToProduct: FnToProduct[RF],
                                                  timeout: Timeout, ec: ExecutionContext): RF = {
    val f: Args => Future[R] = (args: Args) => (who ? question.toProduct(args)).mapTo[R]
    f.fromProduct
  }
}