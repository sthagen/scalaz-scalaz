package scalaz

import Id._

/** A universally quantified function, usually written as `F ~> G`,
  * for symmetry with `A => B`.
  *
  * Can be used to encode first-class functor transformations in the
  * same way functions encode first-class concrete value morphisms;
  * for example, `sequence` from [[scalaz.Traverse]] and `cosequence`
  * from [[scalaz.Distributive]] give rise to `([a]T[A[a]]) ~>
  * ([a]A[T[a]])`, for varying `A` and `T` constraints.
  */
trait NaturalTransformation[F[_], G[_]] {
  self =>
  def apply[A](fa: F[A]): G[A]

  def compose[E[_]](f: E ~> F): E ~> G = new (E ~> G) {
    def apply[A](ea: E[A]) = self(f(ea))
  }

  def andThen[H[_]](f: G ~> H): F ~> H =
    f compose self

  /**
    * Combines this [[scalaz.NaturalTransformation]] with another one to create one
    * that can transform [[scalaz.Coproduct]].
    *
    * The current NaturalTransformation will be used to transform the Left (`F`) value of
    * the [[scalaz.Coproduct]] while the other one will be used to transform the Right (`H`) value.
    */
  def or[H[_]](hg: H ~> G): Coproduct[F, H, *] ~> G =
    new (Coproduct[F, H, *] ~> G) {
      def apply[A](a: Coproduct[F, H, A]) = a.fold(self, hg)
    }

  import LiskovF._

  def widen[GG[_]](implicit ev: GG >~~> G): F ~> GG =
    new NaturalTransformation[F, GG] {
      def apply[A](a: F[A]) = ev(self(a))
    }

  def narrow[FF[_]](implicit ev: FF <~~< F): FF ~> G =
    new NaturalTransformation[FF, G] {
      def apply[A](a: FF[A]) = self(ev(a))
    }
}

trait NaturalTransformations {
  /** A function type encoded as a natural transformation by adding a
    * phantom parameter.
    */
  type ->[A, B] = λ[α => A] ~> λ[α => B]

  /** `refl` specialized to [[scalaz.Id.Id]]. */
  def id: Id ~> Id =
    new (Id ~> Id) {
      def apply[A](a: A) = a
    }

  /** A universally quantified identity function */
  def refl[F[_]]: F ~> F =
    new (F ~> F) {
      def apply[A](fa: F[A]) = fa
    }
}

object NaturalTransformation extends NaturalTransformations {
  /**
   * Construct a natural transformation over a coproduct from its parts.
   * Useful for combining Free interpreters.
   */
  def or[F[_], G[_], H[_]](fg: F ~> G, hg: H ~> G): Coproduct[F, H, *] ~> G =
    new (Coproduct[F, H, *] ~> G) {
      def apply[A](a: Coproduct[F, H, A]) = a.fold(fg, hg)
    }

  /**
   * Like Hoist, for Functors, when we already know how to transform `F ~> G`.
   */
  def liftMap[F[_], G[_], H[_]: Functor](in: F ~> G): λ[α => H[F[α]]] ~> λ[α => H[G[α]]] =
    new ~>[λ[α => H[F[α]]], λ[α => H[G[α]]]] {
      def apply[A](fa: H[F[A]]) =
        Functor[H].map(fa)(in.apply)
    }
}

/** A function universally quantified over two parameters. */
trait BiNaturalTransformation[-F[_, _], +G[_, _]] {
  self =>
  def apply[A, B](f: F[A, B]): G[A, B]

  def compose[E[_, _]](f: BiNaturalTransformation[E, F]): BiNaturalTransformation[E,G] =
    new BiNaturalTransformation[E, G] {
      def apply[A, B](eab: E[A, B]): G[A, B] = self(f(eab))
    }
}

/** A constrained natural transformation */
trait ConstrainedNaturalTransformation[F[_], G[_], E[_]] {
  def apply[A: E](f: F[A]): G[A]
}

/** A constrained transformation natural in both sides of a bifunctor */
trait BiConstrainedNaturalTransformation[F[_,_], G[_,_], C[_], E[_]] {
  def apply[A: C, B: E](f: F[A,B]): G[A,B]
}

trait DiNaturalTransformation[F[_,_], G[_,_]] {
  def apply[A](f: F[A,A]): G[A,A]
}

// TODO needed, or just use type lambdas?
//type Thunk[A] = () => A
//
trait Konst[A] {
  type Apply[B] = A
}
