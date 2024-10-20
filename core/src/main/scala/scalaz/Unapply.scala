package scalaz

import scala.annotation._
import Leibniz.refl

/**
 * Represents a type `MA` that has been destructured into as a type constructor `M[_]`
 * applied to type `A`, along with a corresponding type class instance `TC[M]`.
 *
 * The implicit conversions in the companion object provide a means to obtain type class
 * instances for partially applied type constructors, in lieu of direct compiler support
 * as described in [[https://issues.scala-lang.org/browse/SI-2712 SI-2712]].
 *
 * {{{
 * // Directly depending on Applicative[G]
 * def traverse[G[_], B](f: A => G[B])(implicit G: Applicative[G]): G[F[B]] =
 *   G.traverse(self)(f)
 *
 * // Indirect lookup of the Applicative instance
 * def traverseI[GB](f: A => GB)(implicit G: Unapply[Applicative, GB]): G.M[F[G.A]] /*G[F[B]*/ = {
 *   G.TC.traverse(self)(a => G(f(a)))
 * }
 *
 * // Deforested version of traverseI
 * def traverseI2[GB](f: A => GB)(implicit G: Unapply[Applicative, GB]): G.M[F[G.A]] /*G[F[B]*/ = {
 *   G.TC.traverse(self)(G.leibniz.onF(f))
 * }
 *
 * // Old usage
 * def stateTraverse1 {
 *   import scalaz._, Scalaz._
 *   import State.{State, stateMonad}
 *   val ls = List(1, 2, 3)
 *   val traverseOpt: Option[List[Int]] = ls.traverse(a => Some(a))
 *   val traverseState: State[Int, List[Int]] = ls.traverse[State[Int, *], Int](a => State((x: Int) => (x + 1, a)))
 * }
 *
 * // New usage
 * def stateTraverse2 {
 *   import scalaz._, Scalaz._
 *   val ls = List(1, 2, 3)
 *   val traverseOpt: Option[List[Int]] = ls.traverseI(a => some(a))
 *   val traverseState = ls.traverseI(a => State((x: Int) => (x + 1, a)))
 * }
 *
 * }}}
 *
 * Credits to Miles Sabin.
 */
@implicitNotFound("Implicit not found: scalaz.Unapply[${TC}, ${MA}]. Unable to unapply type `${MA}` into a type constructor of kind `M[_]` that is classified by the type class `${TC}`. Check that the type class is defined by compiling `implicitly[${TC}[type constructor]]` and review the implicits in object Unapply, which only cover common type 'shapes.'")
trait Unapply[TC[_[_]], MA] {

  /** The type constructor */
  type M[_]

  /** The type that `M` was applied to */
  type A

  /** The instance of the type class */
  def TC: TC[M]

  /** Evidence that MA === M[A] */
  def leibniz: MA === M[A]

  /** Compatibility. */
  @inline final def apply(ma: MA): M[A] = leibniz(ma)
}

sealed abstract class Unapply_4 {
  // /** Unpack a value of type `A0` into type `[a]A0`, given a instance of `TC` */
  implicit def unapplyA[TC[_[_]], A0](implicit TC0: TC[λ[α => A0]]): Unapply[TC, A0] {
    type M[X] = A0
    type A = A0
  } =
    new Unapply[TC, A0] {
      type M[X] = A0
      type A = A0
      def TC = TC0
      def leibniz: A0 === M[A] = refl
    }
}

sealed abstract class Unapply_3 extends Unapply_4 {
  /**Unpack a value of type `M0[F[_], A0, A0, B0]` into types `[a]M0[F, a, a, B0]` and `A0`, given an instance of `TC` */
  implicit def unapplyMFABC1and2[TC[_[_]], F[_], M0[F[_], _, _, _], A0, B0](implicit TC0: TC[λ[α => M0[F, α, α, B0]]]): Unapply[TC, M0[F, A0, A0, B0]] {
    type M[X] = M0[F, X, X, B0]
    type A = A0
  } =
    new Unapply[TC, M0[F, A0, A0, B0]] {
      type M[X] = M0[F, X, X, B0]
      type A = A0
      def TC = TC0
      def leibniz: M0[F, A0, A0, B0] === M[A] = refl
    }
}

sealed abstract class Unapply_2 extends Unapply_3 {
  /**Unpack a value of type `M0[F[_], A0, B0]` into types `[a]M0[F, a, B0]` and `A0`, given an instance of `TC` */
  implicit def unapplyMFAB1[TC[_[_]], F[_], M0[F[_], _, _], A0, B0](implicit TC0: TC[M0[F, *, B0]]): Unapply[TC, M0[F, A0, B0]] {
    type M[X] = M0[F, X, B0]
    type A = A0
  } =
    new Unapply[TC, M0[F, A0, B0]] {
      type M[X] = M0[F, X, B0]
      type A = A0
      def TC = TC0
      def leibniz: M0[F, A0, B0] === M[A] = refl
    }
}

sealed abstract class Unapply_0 extends Unapply_2 {
  /**Unpack a value of type `M0[A0, B0]` into types `[a]M0[a, B0]` and `A0`, given an instance of `TC` */
  implicit def unapplyMAB1[TC[_[_]], M0[_, _], A0, B0](implicit TC0: TC[M0[*, B0]]): Unapply[TC, M0[A0, B0]] {
    type M[X] = M0[X, B0]
    type A = A0
  } =
    new Unapply[TC, M0[A0, B0]] {
      type M[X] = M0[X, B0]
      type A = A0
      def TC = TC0
      def leibniz: M0[A0, B0] === M[A] = refl
    }
}

object Unapply extends Unapply_0 {
  type AuxA[TC[_[_]], MA, A0] = Unapply[TC, MA] {
    type A = A0
  }

  /** Fetch a well-typed `Unapply` for the given typeclass and type. */
  def apply[TC[_[_]], MA](implicit U: Unapply[TC, MA]): U.type {
    type M[A] = U.M[A]
    type A = U.A
  } = U

  /** Unpack a value of type `M0[A0]` into types `M0` and `A0`, given a instance of `TC` */
  implicit def unapplyMA[TC[_[_]], M0[_], A0](implicit TC0: TC[M0]): Unapply[TC, M0[A0]] {
    type M[X] = M0[X]
    type A = A0
  } =
    new Unapply[TC, M0[A0]] {
      type M[X] = M0[X]
      type A = A0
      def TC = TC0
      def leibniz: M0[A0] === M[A] = refl
    }

  // TODO More!
}

trait Unapply2[TC[_[_, _]], MAB] {

  /** The type constructor */
  type M[_, _]

  /** The first type that `M` was applied to */
  type A

  /** The second type that `M` was applied to */
  type B

  /** The instance of the type class */
  def TC: TC[M]

  /** Evidence that MAB === M[A, B] */
  def leibniz: MAB === M[A, B]

  /** Compatibility. */
  @inline final def apply(ma: MAB): M[A, B] = leibniz(ma)
}

object Unapply2 {
  /** Fetch a well-typed `Unapply2` for the given typeclass and type. */
  def apply[TC[_[_, _]], MAB](implicit U: Unapply2[TC, MAB]): U.type {
    type M[X, Y] = U.M[X, Y]
    type A = U.A
    type B = U.B
  } = U

  /**Unpack a value of type `M0[A0, B0]` into types `M0`, `A`, and 'B', given an instance of `TC` */
  implicit def unapplyMAB[TC[_[_, _]], M0[_, _], A0, B0](implicit TC0: TC[M0]): Unapply2[TC, M0[A0, B0]] {
    type M[X, Y] = M0[X, Y]
    type A = A0
    type B = B0
  } =
    new Unapply2[TC, M0[A0, B0]] {
      type M[X, Y] = M0[X, Y]
      type A = A0
      type B = B0
      def TC = TC0
      def leibniz: M0[A0, B0] === M[A, B] = refl
    }

  /**Unpack a value of type `M0[A0, F0, B0]` into types `M0`, `A`, `F0`, and 'B', given an instance of `TC` */
  implicit def unapplyMAFB[TC[_[_, _]], M0[_, _[_], _], A0, F0[_], B0](implicit TC0: TC[M0[*, F0, *]]): Unapply2[TC, M0[A0, F0, B0]] {
    type M[X, Y] = M0[X, F0, Y]
    type A = A0
    type B = B0
  } =
    new Unapply2[TC, M0[A0, F0, B0]] {
      type M[X, Y] = M0[X, F0, Y]
      type A = A0
      type B = B0
      def TC = TC0
      def leibniz: M0[A0, F0, B0] === M[A, B] = refl
    }
}

trait Unapply21[TC[_[_, _], _], MAB]{
  type M[_, _]
  type A
  type B
  def TC: TC[M, A]

  def leibniz: MAB === M[A, B]
  @inline final def apply(mabc: MAB): M[A, B] = leibniz(mabc)
}

object Unapply21 {
  /** Fetch a well-typed `Unapply21` for the given typeclass and type. */
  def apply[TC[_[_, _], _], MAB](implicit U: Unapply21[TC, MAB]): U.type {
    type M[X, Y] = U.M[X, Y]
    type A = U.A
    type B = U.B
  } = U

  implicit def unapply210MFABC[TC[_[_, _], _], F[_,_], M0[_[_], _, _], A0, B0, C](implicit TC0: TC[λ[(α, β) => M0[F[α, *], C, β]], A0]): Unapply21[TC, M0[F[A0, *], C, B0]]{
    type M[X, Y] = M0[F[X, *], C, Y]
    type A = A0
    type B = B0
  } =
    new Unapply21[TC, M0[F[A0, *], C, B0]]{
      type M[X, Y] = M0[F[X, *], C, Y]
      type A = A0
      type B = B0

      def TC = TC0
      def leibniz: M0[F[A0, *], C, B0] === M[A, B] = refl
    }
}
