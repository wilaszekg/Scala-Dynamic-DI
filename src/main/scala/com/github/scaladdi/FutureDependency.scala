package com.github.scaladdi

import scala.concurrent.Future

trait FutureDependency[Args, T] {
  def apply(args: Args): Future[T]
}


object FutureDependency {

  private def create[T, Args](f: Args => Future[T]): FutureDependency[Args, T] = new FutureDependency[Args, T] {
    override def apply(args: Args): Future[T] = f(args)
  }

  def apply[A, R](f: A => Future[R]): FutureDependency[Tuple1[A], R] =
    new FutureDependency[Tuple1[A], R] {
      override def apply(args: Tuple1[A]): Future[R] = f(args._1)
    }

  def apply[A, B, R](f: (A, B) => Future[R]): FutureDependency[(A, B), R] = create(f.tupled)

  def apply[A, B, C, R](f: (A, B, C) => Future[R]): FutureDependency[(A, B, C), R] = create(f.tupled)

  def apply[A, B, C, D, R](f: (A, B, C, D) => Future[R]): FutureDependency[(A, B, C, D), R] = create(f.tupled)

  def apply[A, B, C, D, E, R](f: (A, B, C, D, E) => Future[R]): FutureDependency[(A, B, C, D, E), R] = create(f.tupled)

  def apply[A, B, C, D, E, F, R](f: (A, B, C, D, E, F) => Future[R]): FutureDependency[(A, B, C, D, E, F), R] = create(f.tupled)
}
