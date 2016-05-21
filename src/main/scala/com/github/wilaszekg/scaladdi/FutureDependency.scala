package com.github.wilaszekg.scaladdi

import shapeless.{Generic, HList}
import shapeless.ops.function.FnToProduct
import shapeless.ops.hlist.Tupler

import scala.concurrent.Future
import scala.language.higherKinds

trait FutureDependency[Args, T] {
  def apply(args: Args): Future[T]
}


object FutureDependency {

  trait IsFuture[F, T] {
    def apply(f: F): Future[T]
  }

  implicit def isFuture[T] = new IsFuture[Future[T], T] {
    override def apply(f: Future[T]): Future[T] = f
  }

  def apply[Args <: HList, FX, X, F, P <: Product](f: F)
                                              (implicit funProduct: FnToProduct.Aux[F, Args => FX],
                                               futurize: IsFuture[FX, X],
                                               tupl: Tupler.Aux[Args, P],
                                               gen: Generic.Aux[P, Args]): FutureDependency[P, X] =
    new FutureDependency[P, X] {
      override def apply(args: P): Future[X] = futurize(funProduct(f)(gen.to(args)))
    }

}
