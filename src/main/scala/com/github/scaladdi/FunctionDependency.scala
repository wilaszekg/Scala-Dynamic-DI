package com.github.scaladdi

import shapeless.ops.hlist.Tupler
import shapeless.{HList, Generic}
import shapeless.ops.function.FnToProduct

trait FunctionDependency[T, Args] {
  def apply(args: Args): T
}

object FunctionDependency {
  def apply[Args <: HList, T, F, P <: Product](f: F)
    (implicit funProduct: FnToProduct.Aux[F, Args => T],
      tupl: Tupler.Aux[Args, P],
      gen: Generic.Aux[P, Args]): FunctionDependency[T, P] =
    new FunctionDependency[T, P] {
      override def apply(args: P): T = funProduct.apply(f)(gen.to(args))
    }
}