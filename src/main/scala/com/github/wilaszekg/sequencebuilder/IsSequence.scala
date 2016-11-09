package com.github.wilaszekg.sequencebuilder

import cats.Monad
import cats.implicits._
import shapeless._

import scala.language.{higherKinds, implicitConversions}


trait IsSequence[M[_], Out <: HList] {
  type In <: HList

  def hsequence(l: In)(implicit monad: Monad[M]): M[Out]
}

object IsSequence {

  type Aux[M[_], Out <: HList, In0 <: HList] = IsSequence[M, Out] {type In = In0}

  def apply[Out <: HList, In <: HList, M[_] : Monad](implicit isHzippable: Aux[M, Out, In]): Aux[M, Out, In] = isHzippable

  implicit def hNilIsSequence[M[_]]: Aux[M, HNil, HNil] = new IsSequence[M, HNil] {
    type In = HNil

    override def hsequence(l: HNil)(implicit monad: Monad[M]): M[HNil] = monad.pure(HNil)
  }

  implicit def hconsIsSequence[M[_], H, Out0 <: HList, In0 <: HList]
  (implicit ev: IsSequence.Aux[M, Out0, In0]): Aux[M, H :: Out0, M[H] :: In0] = new IsSequence[M, H :: Out0] {

    type In = M[H] :: In0

    override def hsequence(l: M[H] :: In0)(implicit monad: Monad[M]): M[H :: Out0] = {
      val head = l.head
      val tail = l.tail
      head.flatMap(h => ev.hsequence(tail).map(h :: _))
    }
  }

}
