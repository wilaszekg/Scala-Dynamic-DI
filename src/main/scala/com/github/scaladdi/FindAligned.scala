package com.github.scaladdi

import shapeless.ops.hlist.Align
import shapeless.{::, HList}

trait FindAligned[L <: HList, M <: HList] extends (L => M) with Serializable {
  def apply(l: L): M
}

object FindAligned {
  implicit def fromAlign[L <: HList, M <: HList](implicit align: Align[L, M]) = new FindAligned[L, M] {
    override def apply(l: L): M = align(l)
  }

  implicit def alignTail[H, TL <: HList, M <: HList](implicit ar: FindAligned[TL, M]) = new FindAligned[H :: TL, M] {
    override def apply(l: H :: TL): M = ar(l.tail)
  }

  implicit def fromTails[H, T1 <: HList, T2 <: HList](implicit ar: FindAligned[T1, T2]) = new FindAligned[H :: T1, H :: T2] {
    override def apply(l: H :: T1): H :: T2 = l.head :: ar(l.tail)
  }

}

object FindAlignedOps {

  implicit class WithAlignFilter[L <: HList](l: L) {
    def findAligned[M <: HList](implicit alignF: FindAligned[L, M]): M =
      alignF(l)
  }

}
