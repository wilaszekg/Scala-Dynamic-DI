package com.github.wilaszekg.scaladdi

import cats.implicits._
import com.github.wilaszekg.sequencebuilder._
import shapeless.{::, HList, HNil}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class Dependencies[DepMs <: HList, DepValues <: HList : ClassTag](builder: SequenceBuilder[DepMs, DepValues, Future]) {

  def requires[F, Args <: HList, FutArgs <: HList, T](dependency: Dependency[F, Args, T])
                                                     (implicit sequenced: IsSequence.Aux[Future, Args, FutArgs],
                                                      align: FindAligned[DepMs, FutArgs],
                                                      tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepMs, T :: DepValues] = {
    new Dependencies(dependency match {
      case futureDependency: FutureDependency[F, Args, T] =>
        import futureDependency.fnFromHList
        builder.bind(futureDependency.f)
      case functionDependency: FunctionDependency[F, Args, T] =>
        import functionDependency.funProduct
        builder.map(functionDependency.f)
    })
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