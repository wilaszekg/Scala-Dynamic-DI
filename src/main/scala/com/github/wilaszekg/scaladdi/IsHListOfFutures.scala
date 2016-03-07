package com.github.wilaszekg.scaladdi

import shapeless._

import scala.concurrent.{ExecutionContext, Future}

trait IsHListOfFutures[In <: HList, Out <:HList ] {
  def hsequence(l : In)(implicit ec: ExecutionContext): Future[Out]
}

object IsHListOfFutures {
  def apply[In <: HList, Out <: HList](implicit isHzippable: IsHListOfFutures[In, Out]): IsHListOfFutures[In, Out] = isHzippable

  implicit object HNilIsListOfFutures extends IsHListOfFutures[HNil, HNil] {
    override def hsequence(l : HNil)(implicit ec: ExecutionContext): Future[HNil] = Future.successful(HNil)
  }

  implicit def hconsIsHListOfFutures[H, In <: HList, Out <: HList]
  (implicit ev: IsHListOfFutures[In, Out]): IsHListOfFutures[Future[H] :: In, H :: Out] = new IsHListOfFutures[Future[H] :: In, H :: Out] {

    override def hsequence(l : Future[H] :: In)(implicit ec: ExecutionContext): Future[H :: Out] = {
      val head = l.head
      val tail = l.tail
      head.flatMap(h => ev.hsequence(tail).map(h :: _))
    }
  }
}