package com.github.scaladdi

import shapeless.ops.hlist.Align
import shapeless.{::, HList}

trait AlignReduce[L <: HList, M <: HList] extends (L => M) with Serializable {
  def apply(l: L): M
}

object AlignReduce {
  implicit def fromAlign[L <: HList, M <: HList](implicit align: Align[L, M]) = new AlignReduce[L, M] {
    override def apply(l: L): M = align(l)
  }

  implicit def alignTail[H, TL <: HList, M <: HList](implicit ar: AlignReduce[TL, M]) = new AlignReduce[H :: TL, M] {
    override def apply(l: H :: TL): M = ar(l.tail)
  }

  implicit def fromTails[H, T1 <: HList, T2 <: HList](implicit ar: AlignReduce[T1, T2]) = new AlignReduce[H :: T1, H :: T2] {
    override def apply(l: H :: T1): H :: T2 = l.head :: ar(l.tail)
  }

}

object AlignReduceOps {

  implicit class WithAlignFilter[L <: HList](l: L) {
    def alignReduce[M <: HList](implicit alignF: AlignReduce[L, M]): M =
      alignF(l)
  }

}
