package com.github.scaladdi.akka

import akka.actor.{SupervisorStrategy, Props}
import com.github.scaladdi.{Dependencies, DynamicConfigurationFailure, FindAligned, Dependencies$}
import shapeless.HList
import shapeless.ops.function.FnToProduct
import shapeless.syntax.std.function._

import scala.reflect.ClassTag

class ProxyProps[F, Req <: HList : ClassTag](fun: F,
  dependenciesTriesMax: Option[Int] = None,
  reConfigureAfterTerminated: Boolean = true,
  supervisionStrategy: SupervisorStrategy = SupervisorStrategy.defaultStrategy,
  dependencyError: Throwable => Any = ProxyProps.defaultError)
  (implicit funOfDeps: FnToProduct.Aux[F, Req => Props]) {

  def from[FD <: HList, D <: HList](dependencies: => Dependencies[FD, D])(implicit align: FindAligned[D, Req]): Props =
    Props(new ProxyActor(dependencies, fun.toProduct, dependenciesTriesMax, supervisionStrategy, reConfigureAfterTerminated, dependencyError))

}

object ProxyProps {
  private def defaultError(t: Throwable) = DynamicConfigurationFailure(t)

}