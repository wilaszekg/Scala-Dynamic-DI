package com.github.wilaszekg.scaladdi

import cats.Monad
import cats.implicits.{toFlatMapOps, toFunctorOps}
import shapeless.ops.function.FnToProduct
import shapeless.{::, HList, HNil}

import scala.language.higherKinds
import scala.reflect.ClassTag

class SequenceBuilder[DepMs <: HList, DepValues <: HList : ClassTag, M[_] : Monad](dependencies: => DepMs)
                                                                                  (implicit toMSequence: IsSequence.Aux[M, DepValues, DepMs]) {

  import FindAlignedOps._

  def map[F, Args <: HList, T, FutReq <: HList](f: F)
                                               (implicit funProduct: FnToProduct.Aux[F, Args => T],
                                                sequenced: IsSequence.Aux[M, Args, FutReq],
                                                align: FindAligned[DepMs, FutReq],
                                                tNotInDeps: NotIn[T, DepValues]): SequenceBuilder[M[T] :: DepMs, T :: DepValues, M] = {
    new SequenceBuilder({
      val materialised = dependencies
      sequenced.hsequence(materialised.findAligned[FutReq])
        .map(args => funProduct(f)(args)) :: materialised
    })
  }

  def bind[F, Args <: HList, T, FutReq <: HList](f: F)
                                                (implicit fnToM: FnFromHListToM[F, Args, T, M],
                                                 sequenced: IsSequence.Aux[M, Args, FutReq],
                                                 align: FindAligned[DepMs, FutReq],
                                                 tNotInDeps: NotIn[T, DepValues]): SequenceBuilder[M[T] :: DepMs, T :: DepValues, M] = {
    new SequenceBuilder({
      val materialised = dependencies
      sequenced.hsequence(materialised.findAligned[FutReq])
        .flatMap(args => fnToM(f)(args)) :: materialised
    })
  }

  def add[T](m: M[T])
            (implicit tNotInDeps: NotIn[T, DepValues]): SequenceBuilder[M[T] :: DepMs, T :: DepValues, M] = {
    new SequenceBuilder(m :: dependencies)
  }

  def pure[T](value: T)
             (implicit tNotInDeps: NotIn[T, DepValues]): SequenceBuilder[M[T] :: DepMs, T :: DepValues, M] = {
    add(implicitly[Monad[M]].pure(value))
  }

  def result: M[DepValues] = toMSequence.hsequence(dependencies)

}

object SequenceBuilder {

  def apply[M[_] : Monad](): SequenceBuilder[HNil, HNil, M] = {
    new SequenceBuilder(HNil)
  }
}
