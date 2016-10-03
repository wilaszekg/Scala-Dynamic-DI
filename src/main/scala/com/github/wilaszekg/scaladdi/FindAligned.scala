package com.github.wilaszekg.scaladdi

import shapeless.HList
import shapeless.ops.hlist.RemoveAll

import scala.annotation.implicitNotFound

@implicitNotFound("""Implicit not found: com.github.wilaszekg.scaladdi.FindAligned[${L},${M}]
Not all types from ${M} can be found in ${L}""")
trait FindAligned[L <: HList, M <: HList] extends (L => M) with Serializable {
  def apply(l: L): M
}

object FindAligned {

  implicit def findAligned[L <: HList, M <: HList, X <: HList]
  (implicit removeAll: RemoveAll.Aux[L, M, (M, X)]): FindAligned[L, M] = new FindAligned[L, M] {
    override def apply(l: L): M =
      removeAll(l)._1
  }

}

object FindAlignedOps {

  implicit class WithAlignFilter[L <: HList](l: L) {
    def findAligned[M <: HList](implicit alignF: FindAligned[L, M]): M =
      alignF(l)
  }

}
