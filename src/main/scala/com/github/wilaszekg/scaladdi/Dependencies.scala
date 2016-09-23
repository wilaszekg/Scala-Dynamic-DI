package com.github.wilaszekg.scaladdi

import shapeless.{::, Generic, HList, HNil}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class Dependencies[DepFutures <: HList, DepValues <: HList : ClassTag](dependencies: => DepFutures)
                                                                      (implicit val toDepFuture: IsHListOfFutures[DepFutures, DepValues], ec: ExecutionContext) {

  import FindAlignedOps._

  def requires[T, Args, Req <: HList, FutReq <: HList](dependency: FunctionDependency[Args, T])
                                                      (implicit genArgs: Generic.Aux[Args, Req],
                                                       toFutu: IsHListOfFutures[FutReq, Req],
                                                       align: FindAligned[DepFutures, FutReq],
                                                       tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepFutures, T :: DepValues] = {
    new Dependencies({
      val materialised = dependencies
      toFutu.hsequence(materialised.findAligned[FutReq])
        .map(args => dependency.apply(genArgs from args)) :: materialised
    })
  }

  def requires[T, Args, Req <: HList, FutReq <: HList](dependency: FutureDependency[Args, T])
                                                      (implicit genArgs: Generic.Aux[Args, Req],
                                                       toFutu: IsHListOfFutures[FutReq, Req],
                                                       align: FindAligned[DepFutures, FutReq],
                                                       tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepFutures, T :: DepValues] = {
    new Dependencies({
      val materialised = dependencies
      toFutu.hsequence(materialised.findAligned[FutReq])
        .flatMap(args => dependency.apply(genArgs from args)) :: materialised
    })
  }

  def withFuture[T](future: Future[T])(implicit tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepFutures, T :: DepValues] = {
    new Dependencies(future :: dependencies)
  }

  def withVal[T](value: T)(implicit tNotInDeps: NotIn[T, DepValues]): Dependencies[Future[T] :: DepFutures, T :: DepValues] = {
    withFuture(Future.successful(value))
  }

  def result: Future[DepValues] = toDepFuture.hsequence(dependencies)

}

object Dependencies {

  def apply()(implicit ec: ExecutionContext): Dependencies[HNil, HNil] = {
    new Dependencies(HNil)
  }
}
