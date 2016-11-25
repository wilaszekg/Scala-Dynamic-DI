package com.github.wilaszekg.scaladdi.akka

import akka.actor.{Props, SupervisorStrategy}
import com.github.wilaszekg.scaladdi.Dependencies
import com.github.wilaszekg.sequencebuilder.FindAligned
import shapeless.HList
import shapeless.ops.function.FnToProduct
import shapeless.syntax.std.function._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag

class ProxyProps[F, Req <: HList : ClassTag](fun: F,
  dependenciesTriesMax: Option[Int] = None,
  dependenciesTriesRetryDelay: FiniteDuration = 1 second,
  reConfigureAfterTerminated: Boolean = true,
  supervisionStrategy: SupervisorStrategy = SupervisorStrategy.defaultStrategy,
  dependencyError: Throwable => Any = ProxyProps.defaultError)
  (implicit funOfDeps: FnToProduct.Aux[F, Req => Props]) {

  def from[FD <: HList, D <: HList](dependencies: Dependencies[FD, D])(implicit align: FindAligned[D, Req]): Props =
    Props(new ProxyActor(dependencies, fun.toProduct, dependenciesTriesMax, dependenciesTriesRetryDelay, supervisionStrategy, reConfigureAfterTerminated, dependencyError))

}

object ProxyProps {
  private def defaultError(t: Throwable) = DynamicConfigurationFailure(t)

}