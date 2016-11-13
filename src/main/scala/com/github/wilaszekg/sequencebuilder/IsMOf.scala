package com.github.wilaszekg.sequencebuilder

import scala.language.higherKinds

trait IsMOf[F, T, M[_]] {
  def apply(f: F): M[T]
}

object IsMOf {
  implicit def isMOf[T, M[_]]: IsMOf[M[T], T, M] = new IsMOf[M[T], T, M] {
    override def apply(f: M[T]): M[T] = f
  }
}