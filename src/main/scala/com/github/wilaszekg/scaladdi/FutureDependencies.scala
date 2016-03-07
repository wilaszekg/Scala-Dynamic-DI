package com.github.wilaszekg.scaladdi

import shapeless.{::, Generic, HList, HNil}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class FutureDependencies[DepFutures <: HList, DepValues <: HList : ClassTag](dependencies: DepFutures)
  (implicit val toDepFuture: IsHListOfFutures[DepFutures, DepValues], ec: ExecutionContext) {

  import FindAlignedOps._

  def requires[T, Args, Req <: HList, FutReq <: HList](dependency: FunctionDependency[Args, T])
    (implicit genArgs: Generic.Aux[Args, Req],
      toFutu: IsHListOfFutures[FutReq, Req],
      align: FindAligned[DepFutures, FutReq],
      tNotInDeps: NotIn[T, DepValues]): FutureDependencies[Future[T] :: DepFutures, T :: DepValues] = {

    new FutureDependencies(toFutu.hsequence(dependencies.findAligned[FutReq]).map(args => dependency.apply(genArgs from args)) :: dependencies)
  }

  def requires[T, Args, Req <: HList, FutReq <: HList](dependency: FutureDependency[Args, T])
    (implicit genArgs: Generic.Aux[Args, Req],
      toFutu: IsHListOfFutures[FutReq, Req],
      align: FindAligned[DepFutures, FutReq],
      tNotInDeps: NotIn[T, DepValues]): FutureDependencies[Future[T] :: DepFutures, T :: DepValues] = {

    new FutureDependencies(toFutu.hsequence(dependencies.findAligned[FutReq]).flatMap(args => dependency.apply(genArgs from args)) :: dependencies)
  }

  def withFuture[T](future: Future[T])(implicit tNotInDeps: NotIn[T, DepValues]): FutureDependencies[Future[T] :: DepFutures, T :: DepValues] = {
    new FutureDependencies(future :: dependencies)
  }

  def withVal[T](value: T)(implicit tNotInDeps: NotIn[T, DepValues]): FutureDependencies[Future[T] :: DepFutures, T :: DepValues] = {
    withFuture(Future.successful(value))
  }

  def result: Future[DepValues] = toDepFuture.hsequence(dependencies)

}

case class DynamicConfigurationFailure(t: Throwable)

object FutureDependencies {

  def apply()(implicit ec: ExecutionContext): FutureDependencies[HNil, HNil] = {
    new FutureDependencies(HNil)
  }
}
