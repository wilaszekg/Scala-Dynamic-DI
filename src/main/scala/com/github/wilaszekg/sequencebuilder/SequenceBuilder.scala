package com.github.wilaszekg.sequencebuilder

import cats.Monad
import cats.implicits.{toFlatMapOps, toFunctorOps}
import shapeless.ops.function.FnToProduct
import shapeless.{::, HList, HNil}

import scala.language.higherKinds
import scala.reflect.ClassTag

class SequenceBuilder[ML <: HList, L <: HList : ClassTag, M[_] : Monad](dependencies: => ML)
                                                                       (implicit toMSequence: IsSequence.Aux[M, L, ML]) {

  import FindAlignedOps._

  def map[F, Args <: HList, T, MArgs <: HList](f: F)
                                              (implicit funProduct: FnToProduct.Aux[F, Args => T],
                                               sequenced: IsSequence.Aux[M, Args, MArgs],
                                               align: FindAligned[ML, MArgs],
                                               tNotInDeps: NotIn[T, L]): SequenceBuilder[M[T] :: ML, T :: L, M] = {
    new SequenceBuilder({
      val materialised = dependencies
      sequenced.hsequence(materialised.findAligned[MArgs])
        .map(args => funProduct(f)(args)) :: materialised
    })
  }

  def bind[F, Args <: HList, T, MArgs <: HList](f: F)
                                               (implicit fnToM: FnFromHListToM[F, Args, T, M],
                                                sequenced: IsSequence.Aux[M, Args, MArgs],
                                                align: FindAligned[ML, MArgs],
                                                tNotInDeps: NotIn[T, L]): SequenceBuilder[M[T] :: ML, T :: L, M] = {
    new SequenceBuilder({
      val materialised = dependencies
      sequenced.hsequence(materialised.findAligned[MArgs])
        .flatMap(args => fnToM(f)(args)) :: materialised
    })
  }

  def add[T](m: M[T])
            (implicit tNotInDeps: NotIn[T, L]): SequenceBuilder[M[T] :: ML, T :: L, M] = {
    new SequenceBuilder(m :: dependencies)
  }

  def pure[T](value: T)
             (implicit tNotInDeps: NotIn[T, L]): SequenceBuilder[M[T] :: ML, T :: L, M] = {
    add(implicitly[Monad[M]].pure(value))
  }

  def result: M[L] = toMSequence.hsequence(dependencies)

}

object SequenceBuilder {

  def apply[M[_] : Monad](): SequenceBuilder[HNil, HNil, M] = {
    new SequenceBuilder(HNil)
  }
}
