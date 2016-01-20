package com.github.scaladdi

import shapeless.{<:!<, HNil, HList, ::}

trait NotIn[T, L <: HList]

object NotIn {
  implicit def notInHNil[T] = new NotIn[T, HNil] {}

  implicit def notInList[H, T, L <: HList](implicit tIsNotH: <:!<[T, H], hIsNotT: <:!<[T, H], notInTail: NotIn[T, L]) =
    new NotIn[T, H :: L] {}
}