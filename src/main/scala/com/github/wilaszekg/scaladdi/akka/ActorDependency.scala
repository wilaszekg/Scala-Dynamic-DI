package com.github.wilaszekg.scaladdi.akka

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.github.wilaszekg.scaladdi.FutureDependency
import shapeless._
import shapeless.ops.function.{FnFromProduct, FnToProduct}
import shapeless.syntax.std.function._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

// TODO: try to get rid of fnToProduct and let it infere result function type
object ActorDependency {
  def apply[F, Args <: HList, R: ClassTag, Q, RF](who: ActorRef, question: F, c: Class[R])
                                                 (implicit fnToProduct: FnToProduct.Aux[F, Args => Q],
                                                  resultFunction: FnFromProduct.Aux[Args => Future[R], RF],
                                                  rfToProduct: FnToProduct[RF],
                                                  timeout: Timeout, ec: ExecutionContext): FutureDependency[RF] = {
    val f: Args => Future[R] = (args: Args) => (who ? question.toProduct(args)).mapTo[R]
    FutureDependency(f.fromProduct)
  }
}