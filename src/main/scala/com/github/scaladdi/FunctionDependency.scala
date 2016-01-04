package com.github.scaladdi

import shapeless.Generic
import shapeless.ops.function.FnToProduct

trait FunctionDependency[T, Args] {
  def apply(args: Args): T
}

object FunctionDependency {
  def apply[Args, T, F, P](f: F)(implicit gen: Generic.Aux[P, Args], funProduct: FnToProduct.Aux[F, Args => T]): FunctionDependency[T, P] =
    new FunctionDependency[T, P] {
      override def apply(args: P): T = funProduct.apply(f)(gen.to(args))
    }
}