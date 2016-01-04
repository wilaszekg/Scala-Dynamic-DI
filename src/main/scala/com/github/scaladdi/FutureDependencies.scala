package com.github.scaladdi

import akka.actor.Props
import akka.util.Timeout
import shapeless.ops.function.FnToProduct
import shapeless.syntax.std.function._
import shapeless.{Generic, ::, HList, HNil}

import scala.reflect.ClassTag


import scala.concurrent.{ExecutionContext, Future}

class FutureDependencies[DepFutures <: HList, DepValues <: HList : ClassTag](val dependencies: DepFutures)
  (implicit val toDepFuture: IsHListOfFutures[DepFutures, DepValues],
    ec: ExecutionContext) {

  import AlignReduceOps._

  def requires[T, Args, Req <: HList, FutReq <: HList](dependency: DynConfig[T, Args])
    (implicit genArgs: Generic.Aux[Args, Req],
      toFutu: IsHListOfFutures[FutReq, Req],
      align: AlignReduce[DepFutures, FutReq],
      tNotInDeps: NotIn[T, DepValues]): FutureDependencies[Future[T] :: DepFutures, T :: DepValues] = {

    new FutureDependencies(toFutu.hsequence(dependencies.alignReduce[FutReq]).map(args => dependency.apply(genArgs from args)) :: dependencies)
  }

  def requires[T: ClassTag, Req <: HList, FutReq <: HList](actorDep: ActorDep[Req, T])
    (implicit toFutu: IsHListOfFutures[FutReq, Req],
      align: AlignReduce[DepFutures, FutReq],
      timeout: Timeout, tNotInDeps: NotIn[T, DepValues]): FutureDependencies[Future[T] :: DepFutures, T :: DepValues] = {

    import akka.pattern.ask
    val d = toFutu.hsequence(dependencies.alignReduce[FutReq]).flatMap { req =>
      actorDep.who ? actorDep.question(req)
    }.collect { case t: T => t }

    new FutureDependencies(d :: dependencies)
  }

  def isGiven[T](future: Future[T])(implicit tNotInDeps: NotIn[T, DepValues]): FutureDependencies[Future[T] :: DepFutures, T :: DepValues] = {
    new FutureDependencies(future :: dependencies)
  }

  def result: Future[DepValues] = toDepFuture.hsequence(dependencies)

  def props[F, Req <: HList : ClassTag](fun: F, dependencyError: Throwable => Any = defaultError)
    (implicit funOfDeps: FnToProduct.Aux[F, Req => Props],
      align: AlignReduce[DepValues, Req]) =
    Props(new ProxyActor(this, fun.toProduct, dependencyError))

  private def defaultError(t: Throwable) = DynamicConfigurationFailure(t)
}

case class DynamicConfigurationFailure(t: Throwable)

object FutureDependencies {

  def deps(implicit ec: ExecutionContext): FutureDependencies[HNil, HNil] = {
    new FutureDependencies(HNil)
  }
}
