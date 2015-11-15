package com.github.scaladdi

import shapeless.{::, HList, HNil}
trait DynConfig[T, Args <: HList]
import scala.concurrent.{ExecutionContext, Future}

class FutureDependencies[Futures <: HList, Values <: HList, DepFutures <: HList, DepValues <: HList](val dependencies: Futures)
  (implicit toFuture: IsHListOfFutures[Futures, Values],
    val toDepFuture: IsHListOfFutures[DepFutures, DepValues],
    alignToDeps: AlignReduce[Futures, DepFutures],
    ec: ExecutionContext) {

  type TempDepFut = DepFutures
  import AlignReduceOps._

  def requires[T, Req <: HList, FutReq <: HList](dependency: DynConfig[T, Req])
    (implicit construct: Req => T,
      toFutu: IsHListOfFutures[FutReq, Req],
      align: AlignReduce[Futures, FutReq]) = {

    new FutureDependencies[Future[T] :: Futures,
      T :: Values,
      Future[T] :: DepFutures,
      T :: DepValues](toFutu.hsequence(dependencies.alignReduce[FutReq]).map(construct(_)) :: dependencies)
  }

  def isGiven[T](future: Future[T]) = {
    new FutureDependencies[Future[T] :: Futures, T :: Values, DepFutures, DepValues](future :: dependencies)
  }

  def result: Future[DepValues] = toDepFuture.hsequence(dependencies.alignReduce[DepFutures])
}

object FutureDependencies {

  def it(implicit ec: ExecutionContext): FutureDependencies[HNil, HNil, HNil, HNil] = {
    new FutureDependencies[HNil, HNil, HNil, HNil](HNil)
  }
}