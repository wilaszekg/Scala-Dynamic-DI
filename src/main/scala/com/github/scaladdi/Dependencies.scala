package com.github.scaladdi

import shapeless.{::, Generic, HList, HNil}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class Dependencies[DepFutures <: HList, DepValues <: HList : ClassTag](dependencies: => FutureDependencies[DepFutures, DepValues])
  (implicit val toDepFuture: IsHListOfFutures[DepFutures, DepValues], ec: ExecutionContext) {

  def requires[T, Args, Req <: HList, FutReq <: HList](dependency: FunctionDependency[T, Args])
    (implicit genArgs: Generic.Aux[Args, Req],
      toFutu: IsHListOfFutures[FutReq, Req],
      align: FindAligned[DepFutures, FutReq],
      tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepFutures, T :: DepValues] = {

    new Dependencies(dependencies.requires(dependency))
  }

  def requires[T, Args, Req <: HList, FutReq <: HList](dependency: FutureDependency[Args, T])
    (implicit genArgs: Generic.Aux[Args, Req],
      toFutu: IsHListOfFutures[FutReq, Req],
      align: FindAligned[DepFutures, FutReq],
      tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepFutures, T :: DepValues] = {

    new Dependencies(dependencies.requires(dependency))
  }

  def withFuture[T](future: => Future[T])(implicit tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepFutures, T :: DepValues] = {
    new Dependencies(dependencies.withFuture(future))
  }

  def withVal[T](value: => T)(implicit tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepFutures, T :: DepValues] = {
    withFuture(Future.successful(value))
  }

  def run: FutureDependencies[DepFutures, DepValues] = dependencies

}

object Dependencies {

  def apply()(implicit ec: ExecutionContext): Dependencies[HNil, HNil] = {
    new Dependencies(FutureDependencies())
  }
}
