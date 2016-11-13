package com.github.wilaszekg.scaladdi

import com.github.wilaszekg.sequencebuilder.FnFromHListToM
import shapeless.HList
import shapeless.ops.function.FnToProduct

import scala.concurrent.Future

sealed private[scaladdi] trait Dependency[F, Args <: HList, T]

private[scaladdi] case class FunctionDependency[F, Args <: HList, T](f: F)
                                                                    (implicit val funProduct: FnToProduct.Aux[F, Args => T]) extends Dependency[F, Args, T] {

}

private[scaladdi] case class FutureDependency[F, Args <: HList, T](f: F)
                                                                  (implicit val fnFromHList: FnFromHListToM[F, Args, T, Future]) extends Dependency[F, Args, T] {

}

object Dependency extends DependencyLowerPriorityForFunctionDependency {

  implicit def futureDependency[F, Args <: HList, T](f: F)
                                                    (implicit fnToM: FnFromHListToM[F, Args, T, Future]): FutureDependency[F, Args, T] =
    FutureDependency(f)
}

trait DependencyLowerPriorityForFunctionDependency {
  implicit def functionDependency[F, Args <: HList, T](f: F)
                                                      (implicit funProduct: FnToProduct.Aux[F, Args => T]): FunctionDependency[F, Args, T] =
    FunctionDependency(f)
}