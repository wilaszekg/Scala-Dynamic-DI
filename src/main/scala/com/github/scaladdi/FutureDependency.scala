package com.github.scaladdi

import shapeless.Generic
import shapeless.ops.function.FnToProduct

import scala.concurrent.Future

trait FutureDependency[T, Args] {
  def apply(args: Args): Future[T]
}

object FutureDependency {
  def apply[Args, T, F, P](f: F)(implicit gen: Generic.Aux[P, Args], funProduct: FnToProduct.Aux[F, Args => Future[T]]): FutureDependency[T, P] =
    new FutureDependency[T, P] {
      override def apply(args: P): Future[T] = funProduct.apply(f)(gen.to(args))
    }
}
