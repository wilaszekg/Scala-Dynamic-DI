package com.github.wilaszekg.scaladdi

import cats.implicits._
import com.github.wilaszekg.sequencebuilder._
import shapeless.ops.function.FnToProduct
import shapeless.{::, HList, HNil}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class Dependencies[DepMs <: HList, DepValues <: HList : ClassTag](builder: SequenceBuilder[DepMs, DepValues, Future]) {

  def requires[F, T, Args <: HList, FutArgs <: HList](dependency: FunctionDependency[F])
                                                     (implicit funProduct: FnToProduct.Aux[F, Args => T],
                                                      sequenced: IsSequence.Aux[Future, Args, FutArgs],
                                                      align: FindAligned[DepMs, FutArgs],
                                                      tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepMs, T :: DepValues] = {
    new Dependencies(builder.map(dependency.function))
  }

  def requires[F, Args <: HList, FutArgs <: HList, T](dependency: FutureDependency[F])
                                                     (implicit fnToM: FnFromHListToM[F, Args, T, Future],
                                                      sequenced: IsSequence.Aux[Future, Args, FutArgs],
                                                      align: FindAligned[DepMs, FutArgs],
                                                      tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepMs, T :: DepValues] = {
    new Dependencies(builder.bind(dependency.function))
  }

  def withFuture[T](m: Future[T])
                   (implicit tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepMs, T :: DepValues] = {
    new Dependencies(builder.add(m))
  }

  def withVal[T](value: T)
                (implicit tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepMs, T :: DepValues] = {
    new Dependencies(builder.pure(value))
  }

  def result: Future[DepValues] = builder.result
}

object Dependencies {

  def apply()(implicit ec: ExecutionContext): Dependencies[HNil, HNil] =
    new Dependencies[HNil, HNil](SequenceBuilder[Future]())
}