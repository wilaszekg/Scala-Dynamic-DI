package com.github.scaladdi.akka

import akka.actor.Props
import com.github.scaladdi.{DynamicConfigurationFailure, FindAligned, FutureDependencies}
import shapeless.HList
import shapeless.ops.function.FnToProduct
import shapeless.syntax.std.function._

import scala.reflect.ClassTag

class ProxyProps[F, Req <: HList : ClassTag](fun: F,
  dependenciesRetriesMax: Option[Int] = None,
  dependencyError: Throwable => Any = ProxyProps.defaultError)
  (implicit funOfDeps: FnToProduct.Aux[F, Req => Props]) {

  def from[FD <: HList, D <: HList](dependencies: => FutureDependencies[FD, D])(implicit align: FindAligned[D, Req]): Props =
    Props(new ProxyActor(dependencies, fun.toProduct, dependenciesRetriesMax, dependencyError))

}

object ProxyProps {
  private def defaultError(t: Throwable) = DynamicConfigurationFailure(t)

}