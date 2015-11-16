package com.github.scaladdi

import akka.actor.Props
import akka.util.Timeout
import shapeless.{::, HList, HNil}

import scala.reflect.ClassTag

trait DynConfig[T, Args <: HList]

import scala.concurrent.{ExecutionContext, Future}

class FutureDependencies[Futures <: HList, Values <: HList, DepFutures <: HList, DepValues <: HList : ClassTag](val dependencies: Futures)
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

  def requires[T: ClassTag, Req <: HList, FutReq <: HList](actorDep: ActorDep[Req, T])
    (implicit toFutu: IsHListOfFutures[FutReq, Req],
      align: AlignReduce[Futures, FutReq],
      timeout: Timeout) = {

    import akka.pattern.ask
    val d = toFutu.hsequence(dependencies.alignReduce[FutReq]).flatMap { req =>
      actorDep.who ? actorDep.question(req)
    }.collect { case t: T => t }

    new FutureDependencies[Future[T] :: Futures,
      T :: Values,
      Future[T] :: DepFutures,
      T :: DepValues](d :: dependencies)
  }

  def isGiven[T](future: Future[T]) = {
    new FutureDependencies[Future[T] :: Futures, T :: Values, DepFutures, DepValues](future :: dependencies)
  }

  def result: Future[DepValues] = toDepFuture.hsequence(dependencies.alignReduce[DepFutures])

  def props(fun: DepValues => Props, dependencyError: Throwable => Any = defaultError) =
    Props(new ProxyActor(this, fun, dependencyError))

  private def defaultError(t: Throwable) = DynamicConfigurationFailure(t)
}

case class DynamicConfigurationFailure(t: Throwable)

object FutureDependencies {

  def deps(implicit ec: ExecutionContext): FutureDependencies[HNil, HNil, HNil, HNil] = {
    new FutureDependencies[HNil, HNil, HNil, HNil](HNil)
  }
}