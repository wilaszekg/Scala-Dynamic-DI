package com.github.wilaszekg.sequencebuilder

import shapeless.HList
import shapeless.ops.function.FnToProduct

import scala.language.higherKinds

trait FnFromHListToM[F, Args <: HList, T, M[_]] {
  def apply(fun: F): Args => M[T]
}

object FnFromHListToM {

  implicit def apply[Args <: HList, MT, T, F, M[_]](implicit funProduct: FnToProduct.Aux[F, Args => MT],
                                                    wrap: IsMOf[MT, T, M]): FnFromHListToM[F, Args, T, M] =
    new FnFromHListToM[F, Args, T, M] {
      override def apply(fun: F): Args => M[T] = funProduct(fun) andThen wrap.apply
    }
}
