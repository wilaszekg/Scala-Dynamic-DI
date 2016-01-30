package com.github.scaladdi

import scala.concurrent.Future

trait FutureDependency[T, Args] {
  def apply(args: Args): Future[T]
}


object FutureDependency {

  private def create[T, Args](f: Args => Future[T]): FutureDependency[T, Args] = new FutureDependency[T, Args] {
    override def apply(args: Args): Future[T] = f(args)
  }

  def apply[A, R](f: A => Future[R]): FutureDependency[R, Tuple1[A]] =
    new FutureDependency[R, Tuple1[A]] {
      override def apply(args: Tuple1[A]): Future[R] = f(args._1)
    }

  def apply[A, B, R](f: (A, B) => Future[R]): FutureDependency[R, (A, B)] = create(f.tupled)

  def apply[A, B, C, R](f: (A, B, C) => Future[R]): FutureDependency[R, (A, B, C)] = create(f.tupled)

  def apply[A, B, C, D, R](f: (A, B, C, D) => Future[R]): FutureDependency[R, (A, B, C, D)] = create(f.tupled)

  def apply[A, B, C, D, E, R](f: (A, B, C, D, E) => Future[R]): FutureDependency[R, (A, B, C, D, E)] = create(f.tupled)

  def apply[A, B, C, D, E, F, R](f: (A, B, C, D, E, F) => Future[R]): FutureDependency[R, (A, B, C, D, E, F)] = create(f.tupled)
}
