package scalaz
package scalacheck

import org.scalacheck._
import Prop.forAll
import Scalaz._

/**
 * Scalacheck properties that should hold for instances of type classes defined in Scalaz Core.
 */
object ScalazProperties {
  private def newProperties(name: String)(f: Properties => Unit): Properties = {
    val p = new Properties(name)
    f(p)
    p
  }

  object equal {
    def commutativity[A](implicit A: Equal[A], arb: Arbitrary[A]): Prop = forAll(A.equalLaw.commutative _)

    def reflexive[A](implicit A: Equal[A], arb: Arbitrary[A]): Prop = forAll(A.equalLaw.reflexive _)

    def transitive[A](implicit A: Equal[A], arb: Arbitrary[A]): Prop = forAll(A.equalLaw.transitive _)

    def naturality[A](implicit A: Equal[A], arb: Arbitrary[A]): Prop = forAll(A.equalLaw.naturality _)

    def laws[A](implicit A: Equal[A], arb: Arbitrary[A]): Properties =
      newProperties("equal") { p =>
        p.property("commutativity") = commutativity[A]
        p.property("reflexive") = reflexive[A]
        p.property("transitive") = transitive[A]
        p.property("naturality") = naturality[A]
      }
  }

  object order {
    def antisymmetric[A](implicit A: Order[A], arb: Arbitrary[A]): Prop =
      forAll(A.orderLaw.antisymmetric _)

    def transitiveOrder[A](implicit A: Order[A], arb: Arbitrary[A]): Prop = forAll(A.orderLaw.transitiveOrder _)

    def orderAndEqualConsistent[A](implicit A: Order[A], arb: Arbitrary[A]): Prop = forAll(A.orderLaw.orderAndEqualConsistent _)

    import scala.math.{Ordering => SOrdering}

    def scalaOrdering[A: Order: SOrdering: Arbitrary]: Prop = forAll((a1: A, a2: A) => Order[A].order(a1, a2) == Ordering.fromInt(SOrdering[A].compare(a1, a2)))

    def laws[A](implicit A: Order[A], arb: Arbitrary[A]): Properties =
      newProperties("order") { p =>
        p.include(equal.laws[A])
        p.property("antisymmetric") = antisymmetric[A]
        p.property("transitive order") = transitiveOrder[A]
        p.property("order and equal consistent") = orderAndEqualConsistent[A]
      }
  }

  object `enum` {
    def succpred[A](implicit A: Enum[A], arb: Arbitrary[A]): Prop = forAll(A.enumLaw.succpred _)

    def predsucc[A](implicit A: Enum[A], arb: Arbitrary[A]): Prop = forAll(A.enumLaw.predsucc _)

    def minmaxpred[A](implicit A: Enum[A]): Prop = A.enumLaw.minmaxpred

    def minmaxsucc[A](implicit A: Enum[A]): Prop = A.enumLaw.minmaxsucc

    private[this] val smallInt = Gen.choose(-100, 100)

    def succn[A](implicit A: Enum[A], arb: Arbitrary[A]): Prop = forAll((x: A) => forAll(smallInt)(A.enumLaw.succn(x, _)))

    def predn[A](implicit A: Enum[A], arb: Arbitrary[A]): Prop = forAll((x: A) => forAll(smallInt)(A.enumLaw.predn(x, _)))

    def succorder[A](implicit A: Enum[A], arb: Arbitrary[A]): Prop = forAll(A.enumLaw.succorder _)

    def predorder[A](implicit A: Enum[A], arb: Arbitrary[A]): Prop = forAll(A.enumLaw.predorder _)

    def laws[A](implicit A: Enum[A], arb: Arbitrary[A]): Properties =
      newProperties("enum") { p =>
        p.include(order.laws[A])
        p.property("predecessor then successor is identity") = succpred[A]
        p.property("successor then predecessor is identity") = predsucc[A]
        p.property("predecessor of the min is the max") = minmaxpred[A]
        p.property("successor of the max is the min") = minmaxsucc[A]
        p.property("n-successor is n-times successor") = succn[A]
        p.property("n-predecessor is n-times predecessor") = predn[A]
        p.property("successor is greater or equal") = succorder[A]
        p.property("predecessor is less or equal") = predorder[A]
      }
  }

  object semigroup {
    import ScalazArbitrary.Arbitrary_Maybe

    def associative[A](implicit A: Semigroup[A], eqa: Equal[A], arb: Arbitrary[A]): Prop = forAll(A.semigroupLaw.associative _)

    def unfoldlSumOptConsistency[A, S](implicit A: Semigroup[A], eqa: Equal[A], aa: Arbitrary[A], as: Arbitrary[S], cs: Cogen[S]): Prop =
      forAll(A.semigroupLaw.unfoldlSumOptConsistency[S] _)

    def unfoldrSumOptConsistency[A, S](implicit A: Semigroup[A], eqa: Equal[A], aa: Arbitrary[A], as: Arbitrary[S], cs: Cogen[S]): Prop =
      forAll(A.semigroupLaw.unfoldrSumOptConsistency[S] _)

    def laws[A](implicit A: Semigroup[A], eqa: Equal[A], arb: Arbitrary[A]): Properties =
      newProperties("semigroup") { p =>
        p.property("associative") = associative[A]
        p.property("unfoldlSumOpt consistency") = unfoldlSumOptConsistency[A, Int]
        p.property("unfoldrSumOpt consistency") = unfoldrSumOptConsistency[A, Int]
      }
  }

  object monoid {
    def leftIdentity[A](implicit A: Monoid[A], eqa: Equal[A], arb: Arbitrary[A]): Prop = forAll(A.monoidLaw.leftIdentity _)

    def rightIdentity[A](implicit A: Monoid[A], eqa: Equal[A], arb: Arbitrary[A]): Prop = forAll(A.monoidLaw.rightIdentity _)

    def laws[A](implicit A: Monoid[A], eqa: Equal[A], arb: Arbitrary[A]): Properties =
      newProperties("monoid") { p =>
        p.include(semigroup.laws[A])
        p.property("left identity") = leftIdentity[A]
        p.property("right identity") = rightIdentity[A]
      }
  }

  object band {
    def idempotency[A: Equal: Arbitrary](implicit A: Band[A]): Prop =
      forAll(A.bandLaw.idempotency _)

    def laws[A: Equal: Arbitrary](implicit A: Band[A]): Properties =
      newProperties("band") { p =>
        p.include(semigroup.laws[A])
        p.property("idempotency") = idempotency[A]
      }
  }

  object semilattice {
    def commutative[A](implicit A: SemiLattice[A], eq: Equal[A], arb: Arbitrary[A]): Prop = forAll(A.semiLatticeLaw.commutative _)

    def laws[A: Equal: Arbitrary](implicit A: SemiLattice[A]): Properties =
      newProperties("semilattice") { p =>
        p.include(band.laws[A])
        p.property("commutative") = commutative[A]
    }
  }

  object reducer {
    import ScalazArbitrary.Arbitrary_Maybe

    def consCorrectness[C, M](implicit R: Reducer[C, M], ac: Arbitrary[C], am: Arbitrary[M], eqm: Equal[M]): Prop =
      forAll(R.reducerLaw.consCorrectness _)

    def snocCorrectness[C, M](implicit R: Reducer[C, M], ac: Arbitrary[C], am: Arbitrary[M], eqm: Equal[M]): Prop =
      forAll(R.reducerLaw.snocCorrectness _)

    def unfoldlOptConsistency[C, M, S](implicit R: Reducer[C, M], ac: Arbitrary[C], as: Arbitrary[S], cs: Cogen[S], eqm: Equal[M]): Prop =
      forAll(R.reducerLaw.unfoldlOptConsistency[S] _)

    def unfoldrOptConsistency[C, M, S](implicit R: Reducer[C, M], ac: Arbitrary[C], as: Arbitrary[S], cs: Cogen[S], eqm: Equal[M]): Prop =
      forAll(R.reducerLaw.unfoldrOptConsistency[S] _)

    def laws[C: Arbitrary, M: Arbitrary: Equal](implicit R: Reducer[C, M]): Properties =
      newProperties("reducer") { p =>
        p.property("cons correctness") = consCorrectness[C, M]
        p.property("snoc correctness") = snocCorrectness[C, M]
        p.property("unfoldlOpt consistency") = unfoldlOptConsistency[C, M, Int]
        p.property("unfoldrOpt consistency") = unfoldrOptConsistency[C, M, Int]
      }
  }

  object invariantFunctor {
    def identity[F[_], X](implicit F: InvariantFunctor[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]): Prop =
      forAll(F.invariantFunctorLaw.invariantIdentity[X] _)

    def composite[F[_], X, Y, Z](implicit F: InvariantFunctor[F], af: Arbitrary[F[X]], axy: Arbitrary[(X => Y)],
                                   ayz: Arbitrary[(Y => Z)], ayx: Arbitrary[(Y => X)], azy: Arbitrary[(Z => Y)], ef: Equal[F[Z]]): Prop =
      forAll(F.invariantFunctorLaw.invariantComposite[X, Y, Z] _)

    def laws[F[_]](implicit F: InvariantFunctor[F], af: Arbitrary[F[Int]], axy: Arbitrary[(Int => Int)],
                   ef: Equal[F[Int]]): Properties =
      newProperties("invariantFunctor") { p =>
        p.property("identity") = identity[F, Int]
        p.property("composite") = composite[F, Int, Int, Int]
      }
  }

  object functor {
    def identity[F[_], X](implicit F: Functor[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]): Prop =
      forAll(F.functorLaw.identity[X] _)

    def composite[F[_], X, Y, Z](implicit F: Functor[F], af: Arbitrary[F[X]], axy: Arbitrary[(X => Y)],
                                   ayz: Arbitrary[(Y => Z)], ef: Equal[F[Z]]): Prop =
      forAll(F.functorLaw.composite[X, Y, Z] _)

    def laws[F[_]](implicit F: Functor[F], af: Arbitrary[F[Int]], axy: Arbitrary[(Int => Int)],
                   ef: Equal[F[Int]]): Properties =
      newProperties("functor") { p =>
        p.include(invariantFunctor.laws[F])
        p.property("identity") = identity[F, Int]
        p.property("composite") = composite[F, Int, Int, Int]
      }
  }

  object profunctor {
    def identity[M[_,_], A, B](implicit M: Profunctor[M], mba: Arbitrary[M[A, B]], ef: Equal[M[A,B]]): Prop =
      forAll(M.profunctorLaw.identity[A, B] _)

    def compose[M[_,_], A, B, C, D, E, F](implicit M: Profunctor[M], mab: Arbitrary[M[A, D]], fba: Arbitrary[(B => A)], fcb: Arbitrary[(C => B)], fde: Arbitrary[(D => E)], fef: Arbitrary[(E => F)], e: Equal[M[C, F]]): Prop =
      forAll(M.profunctorLaw.composite[A, B, C, D, E, F] _ )

    def laws[M[_,_]](implicit F: Profunctor[M], af: Arbitrary[M[Int, Int]], itf: Arbitrary[(Int => Int)], e: Equal[M[Int, Int]]): Properties =
      newProperties("profunctor") { p =>
        p.property("identity") = identity[M, Int, Int]
        p.property("composite") = compose[M, Int, Int, Int, Int, Int, Int]
      }
  }

  object strong {
    def firstIsSwappedSecond[M[_,_], A, B, C](implicit M: Strong[M], mba: Arbitrary[M[A, B]], eq: Equal[M[(A,C),(B,C)]]): Prop =
      forAll(M.strongLaw.firstIsSwappedSecond[A, B, C] _)

    def secondIsSwappedFirst[M[_,_], A, B, C](implicit M: Strong[M], mba: Arbitrary[M[A, B]], eq: Equal[M[(C,A),(C,B)]]): Prop =
      forAll(M.strongLaw.secondIsSwappedFirst[A, B, C] _)

    def mapfstEqualsFirstAndThenMapsnd[M[_,_], A, B, C](implicit M: Strong[M], mba: Arbitrary[M[A, B]], eq: Equal[M[(A,C),B]]): Prop =
      forAll(M.strongLaw.mapfstEqualsFirstAndThenMapsnd[A, B, C] _)

    def mapfstEqualsSecondAndThenMapsnd[M[_,_], A, B, C](implicit M: Strong[M], mba: Arbitrary[M[A, B]], eq: Equal[M[(C,A),B]]): Prop =
      forAll(M.strongLaw.mapfstEqualsSecondAndThenMapsnd[A, B, C] _)

    def dinaturalityFirst[M[_,_], A, B, C, D](implicit M: Strong[M], mba: Arbitrary[M[A, B]], cd: Arbitrary[C => D], eq: Equal[M[(A,C),(B,D)]]): Prop =
      forAll(M.strongLaw.dinaturalityFirst[A, B, C, D] _)

    def dinaturalitySecond[M[_,_], A, B, C, D](implicit M: Strong[M], mba: Arbitrary[M[A, B]], cd: Arbitrary[C => D], eq: Equal[M[(C,A), (D,B)]]): Prop =
      forAll(M.strongLaw.dinaturalitySecond[A, B, C, D] _)

    def firstFirstIsDimap[M[_,_], A, B, C, D](implicit M: Strong[M], mba: Arbitrary[M[A, B]], eq: Equal[M[((A,C),D),((B,C),D)]]): Prop =
      forAll(M.strongLaw.firstFirstIsDimap[A, B, C, D] _)

    def secondSecondIsDimap[M[_,_], A, B, C, D](implicit M: Strong[M], mba: Arbitrary[M[A, B]], eq: Equal[M[(D,(C,A)),(D,(C,B))]]): Prop =
      forAll(M.strongLaw.secondSecondIsDimap[A, B, C, D] _)

    def laws[M[_,_]](implicit
         F: Strong[M],
         af: Arbitrary[M[Int, Int]],
         eq0: Equal[M[Int,Int]],
         eq1: Equal[M[(Int,Int), (Int,Int)]],
         eq2: Equal[M[(Int,Int), Int]],
         eq3: Equal[M[((Int,Int),Int),((Int,Int),Int)]],
         eq4: Equal[M[(Int,(Int,Int)),(Int,(Int,Int))]]): Properties =
      newProperties("strong") { p =>
        p.include(ScalazProperties.profunctor.laws[M])
        p.property("firstIsSwappedSecond") = firstIsSwappedSecond[M, Int, Int, Int]
        p.property("secondIsSwappedFirst") = secondIsSwappedFirst[M, Int, Int, Int]
        p.property("mapfstEqualsFirstAndThenMapsnd") = mapfstEqualsFirstAndThenMapsnd[M, Int, Int, Int]
        p.property("dinaturalityFirst") = dinaturalityFirst[M, Int, Int, Int, Int]
        p.property("dinaturalitySecond") = dinaturalitySecond[M, Int, Int, Int, Int]
        p.property("firstFirstIsDimap") = firstFirstIsDimap[M, Int, Int, Int, Int]
        p.property("secondSecondIsDimap") = secondSecondIsDimap[M, Int, Int, Int, Int]
      }
  }

  object align {
    def collapse[F[_], A](implicit F: Align[F], E: Equal[F[A \&/ A]], A: Arbitrary[F[A]]): Prop =
      forAll(F.alignLaw.collapse[A] _)
    def laws[F[_]](implicit F: Align[F], af: Arbitrary[F[Int]],
                   e: Equal[F[Int]], ef: Equal[F[Int \&/ Int]]): Properties =
      newProperties("align") { p =>
        p.include(functor.laws[F])
        p.property("collapse") = collapse[F, Int]
      }
  }

  object apply {self =>
    def composition[F[_], X, Y, Z](implicit ap: Apply[F], afx: Arbitrary[F[X]], au: Arbitrary[F[Y => Z]],
                                   av: Arbitrary[F[X => Y]], e: Equal[F[Z]]): Prop = forAll(ap.applyLaw.composition[X, Y, Z] _)

    def laws[F[_]](implicit F: Apply[F], af: Arbitrary[F[Int]],
                   aff: Arbitrary[F[Int => Int]], e: Equal[F[Int]]): Properties =
      newProperties("apply") { p =>
        implicit val r: Reducer[F[Int], F[Int]] = F.liftReducer(Reducer.identityReducer[Int])

        p.include(functor.laws[F])
        p.include(reducer.laws[F[Int], F[Int]])
        p.property("composition") = self.composition[F, Int, Int, Int]
      }
  }

  object applicative {
    def identity[F[_], X](implicit f: Applicative[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]): Prop =
      forAll(f.applicativeLaw.identityAp[X] _)

    def homomorphism[F[_], X, Y](implicit ap: Applicative[F], ax: Arbitrary[X], af: Arbitrary[X => Y], e: Equal[F[Y]]): Prop =
      forAll(ap.applicativeLaw.homomorphism[X, Y] _)

    def interchange[F[_], X, Y](implicit ap: Applicative[F], ax: Arbitrary[X], afx: Arbitrary[F[X => Y]], e: Equal[F[Y]]): Prop =
      forAll(ap.applicativeLaw.interchange[X, Y] _)

    def mapApConsistency[F[_], X, Y](implicit ap: Applicative[F], ax: Arbitrary[F[X]], afx: Arbitrary[X => Y], e: Equal[F[Y]]): Prop =
      forAll(ap.applicativeLaw.mapLikeDerived[X, Y] _)

    def laws[F[_]](implicit F: Applicative[F], af: Arbitrary[F[Int]],
                   aff: Arbitrary[F[Int => Int]], e: Equal[F[Int]]): Properties =
      newProperties("applicative") { p =>
        p.include(ScalazProperties.apply.laws[F])
        p.property("identity") = applicative.identity[F, Int]
        p.property("homomorphism") = applicative.homomorphism[F, Int, Int]
        p.property("interchange") = applicative.interchange[F, Int, Int]
        p.property("map consistent with ap") = applicative.mapApConsistency[F, Int, Int]
      }
  }

  object applicativeError{
    def raisedErrorsHandled[F[_], E, A](implicit A: ApplicativeError[F, E], eq: Equal[F[A]], ae: Arbitrary[E], afea: Arbitrary[E => F[A]]): Prop =
      forAll(A.applicativeErrorLaws.raisedErrorsHandled[A] _)

    def laws[F[_], E](implicit A: ApplicativeError[F, E], am: Arbitrary[F[Int]], afap: Arbitrary[F[Int => Int]], aeq: Equal[F[Int]], ae: Arbitrary[E], afea: Arbitrary[E => F[Int]]): Properties =
      newProperties("applicative error"){ p =>
        p.include(applicative.laws[F])
        p.property("raisedErrorsHandled") = raisedErrorsHandled[F, E, Int]
      }
  }

  object alt {
    def laws[F[_]](implicit F: Applicative[F], af: Arbitrary[F[Int]],
                   aff: Arbitrary[F[Int => Int]], e: Equal[F[Int]]): Properties =
      newProperties("alt") { p =>
        p.include(applicative.laws[F])
      }
  }

  object bind {
    def associativity[M[_], X, Y, Z](implicit M: Bind[M], amx: Arbitrary[M[X]], af: Arbitrary[(X => M[Y])],
                                     ag: Arbitrary[(Y => M[Z])], emz: Equal[M[Z]]): Prop =
      forAll(M.bindLaw.associativeBind[X, Y, Z] _)

    def bindApConsistency[M[_], X, Y](implicit M: Bind[M], amx: Arbitrary[M[X]],
                                      af: Arbitrary[M[X => Y]], emy: Equal[M[Y]]): Prop =
      forAll(M.bindLaw.apLikeDerived[X, Y] _)

    def laws[M[_]](implicit a: Bind[M], am: Arbitrary[M[Int]],
                   af: Arbitrary[Int => M[Int]], ag: Arbitrary[M[Int => Int]], e: Equal[M[Int]]): Properties =
      newProperties("bind") { p =>
        p.include(ScalazProperties.apply.laws[M])

        p.property("associativity") = bind.associativity[M, Int, Int, Int]
        p.property("ap consistent with bind") = bind.bindApConsistency[M, Int, Int]
      }
  }

  object bindRec {
    def tailrecBindConsistency[M[_], X](implicit M: BindRec[M], ax: Arbitrary[X], af: Arbitrary[X => M[X]],
                                        emx: Equal[M[X]]): Prop =
      forAll(M.bindRecLaw.tailrecBindConsistency[X] _)

    def laws[M[_]](implicit a: BindRec[M], am: Arbitrary[M[Int]],
                   af: Arbitrary[Int => M[Int]], ag: Arbitrary[M[Int => Int]], e: Equal[M[Int]]): Properties =
      newProperties("bindRec") { p =>
        p.include(bind.laws[M])
        p.property("tailrecM is consistent with bind") = bindRec.tailrecBindConsistency[M, Int]
      }
  }

  object monad {
    def rightIdentity[M[_], X](implicit M: Monad[M], e: Equal[M[X]], a: Arbitrary[M[X]]): Prop =
      forAll(M.monadLaw.rightIdentity[X] _)

    def leftIdentity[M[_], X, Y](implicit am: Monad[M], emy: Equal[M[Y]], ax: Arbitrary[X], af: Arbitrary[(X => M[Y])]): Prop =
      forAll(am.monadLaw.leftIdentity[X, Y] _)

    def laws[M[_]](implicit a: Monad[M], am: Arbitrary[M[Int]],
                   af: Arbitrary[Int => M[Int]], ag: Arbitrary[M[Int => Int]], e: Equal[M[Int]]): Properties =
      newProperties("monad") { p =>
        p.include(applicative.laws[M])
        p.include(bind.laws[M])
        p.property("right identity") = monad.rightIdentity[M, Int]
        p.property("left identity") = monad.leftIdentity[M, Int, Int]
      }
  }

  object cobind {
    def cobindAssociative[F[_], A, B, C, D](implicit F: Cobind[F], D: Equal[D], fa: Arbitrary[F[A]],
                                            f: Arbitrary[F[A] => B], g: Arbitrary[F[B] => C], h: Arbitrary[F[C] => D]): Prop =
      forAll(F.cobindLaw.cobindAssociative[A, B, C, D] _)

    def laws[F[_]](implicit a: Cobind[F], f: Arbitrary[F[Int] => Int], am: Arbitrary[F[Int]], e: Equal[F[Int]]): Properties =
      newProperties("cobind") { p =>
        p.include(functor.laws[F])
        p.property("cobind associative") = cobindAssociative[F, Int, Int, Int, Int]
      }
  }

  object comonad {
    def cobindLeftIdentity[F[_], A](implicit F: Comonad[F], F0: Equal[F[A]], fa: Arbitrary[F[A]]): Prop =
      forAll(F.comonadLaw.cobindLeftIdentity[A] _)

    def cobindRightIdentity[F[_], A, B](implicit F: Comonad[F], F0: Equal[B], fa: Arbitrary[F[A]], f: Arbitrary[F[A] => B]): Prop =
      forAll(F.comonadLaw.cobindRightIdentity[A, B] _)

    def laws[F[_]](implicit a: Comonad[F], am: Arbitrary[F[Int]],
                   af: Arbitrary[F[Int] => Int], e: Equal[F[Int]]): Properties =
      newProperties("comonad") { p =>
        p.include(cobind.laws[F])
        p.property("cobind left identity") = cobindLeftIdentity[F, Int]
        p.property("cobind right identity") = cobindRightIdentity[F, Int, Int]
      }
  }

  object density {
    def densityIsLeftKan[F[_], A, B](implicit F: Density[F, A], F0: Equal[B], fa: Arbitrary[F[A]], fab: Arbitrary[F[A] => B]): Prop =
      forAll(F.densityLaw.densityIsLeftKan[A,B] _)

    def leftKanIsDensity[F[_], A, B](implicit F: Density[F, A], F0: Equal[F[A]], fa: Arbitrary[F[A]], fab: Arbitrary[F[A] => B]): Prop =
      forAll(F.densityLaw.leftKanIsDensity[A,B] _)

    def laws[F[_]](implicit a: Density[F, Int], am: Arbitrary[F[Int]],
                   af: Arbitrary[F[Int] => Int], e: Equal[F[Int]]): Properties =
      newProperties("density") { p =>
        p.property("density is left kan") = densityIsLeftKan[F, Int, Int]
        p.property("left kan is density") = leftKanIsDensity[F, Int, Int]
      }
  }

  private def resizeProp(p: Prop, max: Int): Prop = new PropFromFun(
    params => p(params.withSize(params.size % (max + 1)))
  )

  object traverse {
    def identityTraverse[F[_], X, Y](implicit f: Traverse[F], afx: Arbitrary[F[X]], axy: Arbitrary[X => Y], ef: Equal[F[Y]]): Prop =
      forAll(f.traverseLaw.identityTraverse[X, Y] _)

    def purity[F[_], G[_], X](implicit f: Traverse[F], afx: Arbitrary[F[X]], G: Applicative[G], ef: Equal[G[F[X]]]): Prop =
      forAll(f.traverseLaw.purity[G, X] _)

    def sequentialFusion[F[_], N[_], M[_], A, B, C](implicit fa: Arbitrary[F[A]], amb: Arbitrary[A => M[B]], bnc: Arbitrary[B => N[C]],
                                                      F: Traverse[F], N: Applicative[N], M: Applicative[M], MN: Equal[M[N[F[C]]]]): Prop =
      forAll(F.traverseLaw.sequentialFusion[N, M, A, B, C] _)

    def naturality[F[_], N[_], M[_], A](nat: (M ~> N))
                                       (implicit fma: Arbitrary[F[M[A]]], F: Traverse[F], N: Applicative[N], M: Applicative[M], NFA: Equal[N[F[A]]]): Prop =
      forAll(F.traverseLaw.naturality[N, M, A](nat) _)

    def parallelFusion[F[_], N[_], M[_], A, B](implicit fa: Arbitrary[F[A]], amb: Arbitrary[A => M[B]], anb: Arbitrary[A => N[B]],
                                               F: Traverse[F], N: Applicative[N], M: Applicative[M], MN: Equal[(M[F[B]], N[F[B]])]): Prop =
      forAll(F.traverseLaw.parallelFusion[N, M, A, B] _)

    def laws[F[_]](implicit fa: Arbitrary[F[Int]], F: Traverse[F], EF: Equal[F[Int]]): Properties =
      newProperties("traverse") { p =>
        p.include(functor.laws[F])
        p.include(foldable.laws[F])
        p.property("identity traverse") = identityTraverse[F, Int, Int]

        import std.list._, std.option._, std.stream._

        p.property("purity.option") = purity[F, Option, Int]
        p.property("purity.stream") = purity[F, Stream, Int]

        p.property("sequential fusion") = resizeProp(sequentialFusion[F, Option, List, Int, Int, Int], 3)
        // TODO naturality, parallelFusion
      }
  }

  object bifoldable {
    def leftFMConsistent[F[_, _], A, B](implicit F: Bifoldable[F], afa: Arbitrary[F[A, B]], ea: Equal[A], eb: Equal[B]): Prop =
      forAll(F.bifoldableLaw.leftFMConsistent[A, B] _)

    def rightFMConsistent[F[_, _], A, B](implicit F: Bifoldable[F], afa: Arbitrary[F[A, B]], ea: Equal[A], eb: Equal[B]): Prop =
      forAll(F.bifoldableLaw.rightFMConsistent[A, B] _)

    def laws[F[_, _]](implicit fa: Arbitrary[F[Int, Int]], F: Bifoldable[F]): Properties =
      newProperties("bifoldable") { p =>
        p.property("consistent left bifold") = leftFMConsistent[F, Int, Int]
        p.property("consistent right bifold") = rightFMConsistent[F, Int, Int]
        implicit val left = F.leftFoldable[Int]
        implicit val right = F.rightFoldable[Int]
        p.include(foldable.laws[F[*, Int]])
        p.include(foldable.laws[F[Int, *]])
      }
  }

  object bitraverse {
    def laws[F[_, _]](implicit fa: Arbitrary[F[Int,Int]], F: Bitraverse[F], EF: Equal[F[Int, Int]]): Properties =
      newProperties("bitraverse") { p =>
        p.include(bifoldable.laws[F])
        implicit val left = F.leftTraverse[Int]
        implicit val right = F.rightTraverse[Int]
        p.include(traverse.laws[F[*, Int]])
        p.include(traverse.laws[F[Int, *]])
      }
  }

  object plus {
    def associative[F[_], X](implicit f: Plus[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]): Prop =
      forAll(f.plusLaw.associative[X] _)

    def laws[F[_]](implicit F: Plus[F], afx: Arbitrary[F[Int]], ef: Equal[F[Int]]): Properties =
      newProperties("plus") { p =>
        p.include(semigroup.laws[F[Int]](F.semigroup[Int], implicitly, implicitly))
        p.property("associative") = associative[F, Int]
      }
  }

  object plusEmpty {
    def leftPlusIdentity[F[_], X](implicit f: PlusEmpty[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]): Prop =
      forAll(f.plusEmptyLaw.leftPlusIdentity[X] _)

    def rightPlusIdentity[F[_], X](implicit f: PlusEmpty[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]): Prop =
      forAll(f.plusEmptyLaw.rightPlusIdentity[X] _)

    def laws[F[_]](implicit F: PlusEmpty[F], afx: Arbitrary[F[Int]], ef: Equal[F[Int]]): Properties =
      newProperties("plusEmpty") { p =>
        p.include(plus.laws[F])
        p.include(monoid.laws[F[Int]](F.monoid[Int], implicitly, implicitly))
        p.property("left plus identity") = leftPlusIdentity[F, Int]
        p.property("right plus identity") = rightPlusIdentity[F, Int]
      }
  }

  object isEmpty {
    def emptyIsEmpty[F[_], X](implicit f: IsEmpty[F]):Prop =
      f.isEmptyLaw.emptyIsEmpty[X]

    def emptyPlusIdentity[F[_], X](implicit f: IsEmpty[F], afx: Arbitrary[F[X]]): Prop =
      forAll(f.isEmptyLaw.emptyPlusIdentity[X] _)

    def laws[F[_]](implicit F: IsEmpty[F], afx: Arbitrary[F[Int]], ef: Equal[F[Int]]): Properties =
      newProperties("isEmpty") { p =>
        p.include(plusEmpty.laws[F])
        p.property("empty is empty") = emptyIsEmpty[F, Int]
        p.property("empty plus identity") =  emptyPlusIdentity[F, Int]
      }
  }

  object monadPlus {
    def emptyMap[F[_], X](implicit f: MonadPlus[F], afx: Arbitrary[X => X], ef: Equal[F[X]]): Prop =
      forAll(f.monadPlusLaw.emptyMap[X] _)

    def leftZero[F[_], X](implicit F: MonadPlus[F], afx: Arbitrary[X => F[X]], ef: Equal[F[X]]): Prop =
      forAll(F.monadPlusLaw.leftZero[X] _)

    def rightZero[F[_], X](implicit F: MonadPlus[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]): Prop =
      forAll(F.strongMonadPlusLaw.rightZero[X] _)

    def laws[F[_]](implicit F: MonadPlus[F], afx: Arbitrary[F[Int]], afy: Arbitrary[F[Int => Int]], ef: Equal[F[Int]]): Properties =
      newProperties("monad plus") { p =>
        p.include(monad.laws[F])
        p.include(plusEmpty.laws[F])
        p.property("empty map") = emptyMap[F, Int]
        p.property("left zero") = leftZero[F, Int]
      }
    def strongLaws[F[_]](implicit F: MonadPlus[F], afx: Arbitrary[F[Int]], afy: Arbitrary[F[Int => Int]], ef: Equal[F[Int]]): Properties =
      newProperties("monad plus") { p =>
        p.include(laws[F])
        p.property("right zero") = rightZero[F, Int]
      }
  }

  object foldable {
    def leftFMConsistent[F[_], A](implicit F: Foldable[F], afa: Arbitrary[F[A]], ea: Equal[A]): Prop =
      forAll(F.foldableLaw.leftFMConsistent[A] _)

    def rightFMConsistent[F[_], A](implicit F: Foldable[F], afa: Arbitrary[F[A]], ea: Equal[A]): Prop =
      forAll(F.foldableLaw.rightFMConsistent[A] _)

    def laws[F[_]](implicit fa: Arbitrary[F[Int]], F: Foldable[F]): Properties =
      newProperties("foldable") { p =>
        p.property("consistent left fold") = leftFMConsistent[F, Int]
        p.property("consistent right fold") = rightFMConsistent[F, Int]
      }
  }

  object foldable1 {
    def leftFM1Consistent[F[_], A](implicit F: Foldable1[F], fa: Arbitrary[F[A]], ea: Equal[A]): Prop =
      forAll(F.foldable1Law.leftFM1Consistent[A] _)

    def rightFM1Consistent[F[_], A](implicit F: Foldable1[F], fa: Arbitrary[F[A]], ea: Equal[A]): Prop =
      forAll(F.foldable1Law.rightFM1Consistent[A] _)

    def laws[F[_]](implicit fa: Arbitrary[F[Int]],
                   F: Foldable1[F]): Properties =
      newProperties("foldable1") { p =>
        p.include(foldable.laws[F])
        p.property("consistent left fold1") = leftFM1Consistent[F, Int]
        p.property("consistent right fold1") = rightFM1Consistent[F, Int]
      }
  }

  object traverse1 {
    def identityTraverse1[F[_], X, Y](implicit f: Traverse1[F], afx: Arbitrary[F[X]], axy: Arbitrary[X => Y], ef: Equal[F[Y]]): Prop =
      forAll(f.traverse1Law.identityTraverse1[X, Y] _)

    def sequentialFusion1[F[_], N[_], M[_], A, B, C](implicit fa: Arbitrary[F[A]], amb: Arbitrary[A => M[B]], bnc: Arbitrary[B => N[C]],
                                                      F: Traverse1[F], N: Apply[N], M: Apply[M], MN: Equal[M[N[F[C]]]]): Prop =
      forAll(F.traverse1Law.sequentialFusion1[N, M, A, B, C] _)

    def naturality1[F[_], N[_], M[_], A](nat: (M ~> N))
                                       (implicit fma: Arbitrary[F[M[A]]], F: Traverse1[F], N: Apply[N], M: Apply[M], NFA: Equal[N[F[A]]]): Prop =
      forAll(F.traverse1Law.naturality1[N, M, A](nat) _)

    def parallelFusion1[F[_], N[_], M[_], A, B](implicit fa: Arbitrary[F[A]], amb: Arbitrary[A => M[B]], anb: Arbitrary[A => N[B]],
                                               F: Traverse1[F], N: Apply[N], M: Apply[M], MN: Equal[(M[F[B]], N[F[B]])]): Prop =
      forAll(F.traverse1Law.parallelFusion1[N, M, A, B] _)

    def laws[F[_]](implicit fa: Arbitrary[F[Int]], F: Traverse1[F], EF: Equal[F[Int]]): Properties =
      newProperties("traverse1") { p =>
        p.include(traverse.laws[F])
        p.include(foldable1.laws[F])
        p.property("identity traverse1") = identityTraverse1[F, Int, Int]

        import std.list._, std.option._

        p.property("sequential fusion (1)") = resizeProp(sequentialFusion1[F, Option, List, Int, Int, Int], 3)
        // TODO naturality1, parallelFusion1
      }
  }

  object zip {
    def zipPreservation[F[_], X](implicit F: Zip[F], FF: Functor[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]): Prop =
      forAll(F.zipLaw.zipPreservation[X] _)

    def zipSymmetric[F[_], X, Y](implicit F: Zip[F], FF: Functor[F], afx: Arbitrary[F[X]], afy: Arbitrary[F[Y]], ef: Equal[F[X]]): Prop =
      forAll(F.zipLaw.zipSymmetric[X, Y] _)

    def laws[F[_]](implicit fa: Arbitrary[F[Int]], F: Zip[F], FF: Functor[F], EF: Equal[F[Int]]): Properties =
      newProperties("zip") { p =>
        p.property("preserves structure") = zipPreservation[F, Int]
        p.property("symmetry") = zipSymmetric[F, Int, Int]
      }
  }

  object contravariant {
    def identity[F[_], X](implicit F: Contravariant[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]): Prop =
      forAll(F.contravariantLaw.identity[X] _)

    def composite[F[_], X, Y, Z](implicit F: Contravariant[F], af: Arbitrary[F[Z]], axy: Arbitrary[(X => Y)],
                                   ayz: Arbitrary[(Y => Z)], ef: Equal[F[X]]): Prop =
      forAll(F.contravariantLaw.composite[Z, Y, X] _)

    def laws[F[_]](implicit F: Contravariant[F], af: Arbitrary[F[Int]], axy: Arbitrary[(Int => Int)],
                   ef: Equal[F[Int]]): Properties =
      newProperties("contravariant") { p =>
        p.include(invariantFunctor.laws[F])
        p.property("identity") = identity[F, Int]
        p.property("composite") = composite[F, Int, Int, Int]
      }
  }

  object divide {
    def composition[F[_], A](implicit F: Divide[F], A: Arbitrary[F[A]], E: Equal[F[A]]): Prop =
      forAll(F.divideLaw.composition[A] _)

    def laws[F[_]](implicit F: Divide[F], af: Arbitrary[F[Int]], axy: Arbitrary[Int => Int],
                   ef: Equal[F[Int]]): Properties =
      newProperties("divide") { p =>
        p.include(contravariant.laws[F])
        p.property("composition") = composition[F, Int]
      }
  }

  object divisible {
    def rightIdentity[F[_], A](implicit F: Divisible[F], A: Arbitrary[F[A]], E: Equal[F[A]]): Prop =
      forAll(F.divisibleLaw.rightIdentity[A] _)

    def leftIdentity[F[_], A](implicit F: Divisible[F], A: Arbitrary[F[A]], E: Equal[F[A]]): Prop =
      forAll(F.divisibleLaw.leftIdentity[A] _)

    def laws[F[_]](implicit F: Divisible[F], af: Arbitrary[F[Int]], axy: Arbitrary[Int => Int],
                   ef: Equal[F[Int]]): Properties =
      newProperties("divisible") { p =>
        p.include(divide.laws[F])
        p.property("right identity") = rightIdentity[F, Int]
        p.property("left identity") = leftIdentity[F, Int]
      }
  }

  object decidable {
    def laws[F[_]](implicit
                     F: Decidable[F],
                     af: Arbitrary[F[Int]],
                     axy: Arbitrary[Int => Int],
                     ef: Equal[F[Int]]): Properties =
      newProperties("decidable") { p =>
        p.include(divisible.laws[F])
      }
  }

  object compose {
    def associative[=>:[_, _], A, B, C, D](implicit ab: Arbitrary[A =>: B], bc: Arbitrary[B =>: C],
                                           cd: Arbitrary[C =>: D], C: Compose[=>:], E: Equal[A =>: D]): Prop =
      forAll(C.composeLaw.associative[A, B, C, D] _)

    def laws[=>:[_, _]](implicit C: Compose[=>:], AB: Arbitrary[Int =>: Int], E: Equal[Int =>: Int]): Properties =
      newProperties("compose") { p =>
        p.property("associative") = associative[=>:, Int, Int, Int, Int]
        p.include(semigroup.laws[Int =>: Int](C.semigroup[Int], implicitly, implicitly))
      }
  }

  object category {
    def leftIdentity[=>:[_, _], A, B](implicit ab: Arbitrary[A =>: B], C: Category[=>:], E: Equal[A =>: B]): Prop =
      forAll(C.categoryLaw.leftIdentity[A, B] _)

    def rightIdentity[=>:[_, _], A, B](implicit ab: Arbitrary[A =>: B], C: Category[=>:], E: Equal[A =>: B]): Prop =
      forAll(C.categoryLaw.rightIdentity[A, B] _)

    def laws[=>:[_, _]](implicit C: Category[=>:], AB: Arbitrary[Int =>: Int], E: Equal[Int =>: Int]): Properties =
      newProperties("category") { p =>
        p.include(compose.laws[=>:])
        p.property("left identity") = leftIdentity[=>:, Int, Int]
        p.property("right identity") = rightIdentity[=>:, Int, Int]
        p.include(monoid.laws[Int =>: Int](C.monoid[Int], implicitly, implicitly))
      }
  }

  object associative {
    def leftRight[=>:[_, _], X, Y, Z](implicit F: Associative[=>:], af: Arbitrary[X =>: (Y =>: Z)], ef: Equal[X =>: (Y =>: Z)]): Prop =
      forAll(F.associativeLaw.leftRight[X, Y, Z] _)

    def rightLeft[=>:[_, _], X, Y, Z](implicit F: Associative[=>:], af: Arbitrary[(X =>: Y) =>: Z], ef: Equal[(X =>: Y) =>: Z]): Prop =
      forAll(F.associativeLaw.rightLeft[X, Y, Z] _)

    def laws[=>:[_, _]](implicit F: Associative[=>:],
                        al: Arbitrary[(Int =>: Int) =>: Int], ar: Arbitrary[Int =>: (Int =>: Int)],
                        el: Equal[(Int =>: Int) =>: Int], er: Equal[Int =>: (Int =>: Int)]): Properties =
      newProperties("associative") { p =>
        p.property("left and then right reassociation is identity") = leftRight[=>:, Int, Int, Int]
        p.property("right and then left reassociation is identity") = rightLeft[=>:, Int, Int, Int]
      }
  }

  object bifunctor {
    def laws[F[_, _]](implicit F: Bifunctor[F], E: Equal[F[Int, Int]], af: Arbitrary[F[Int, Int]],
                      axy: Arbitrary[(Int => Int)]): Properties =
      newProperties("bifunctor") { p =>
        p.include(functor.laws[F[*, Int]](F.leftFunctor[Int], implicitly, implicitly, implicitly))
        p.include(functor.laws[F[Int, *]](F.rightFunctor[Int], implicitly, implicitly, implicitly))
      }
  }

  object lens {
    def identity[A, B](l: Lens[A, B])(implicit A: Arbitrary[A], EA: Equal[A]): Prop = forAll(l.lensLaw.identity _)
    def retention[A, B](l: Lens[A, B])(implicit A: Arbitrary[A], B: Arbitrary[B], EB: Equal[B]): Prop = forAll(l.lensLaw.retention _)
    def doubleSet[A, B](l: Lens[A, B])(implicit A: Arbitrary[A], B: Arbitrary[B], EB: Equal[A]): Prop = forAll(l.lensLaw.doubleSet _)

    def laws[A, B](l: Lens[A, B])(implicit A: Arbitrary[A], B: Arbitrary[B], EA: Equal[A], EB: Equal[B]): Properties =
      newProperties("lens") { p =>
        p.property("identity") = identity[A, B](l)
        p.property("retention") = retention[A, B](l)
        p.property("doubleSet") = doubleSet[A, B](l)
      }
  }

  object monadError {
    def errorsRaised[F[_], E, A](implicit me: MonadError[F, E], eq: Equal[F[A]], ae: Arbitrary[E], aa: Arbitrary[A]): Prop =
      forAll(me.monadErrorLaw.errorsRaised[A] _)
    def errorsStopComputation[F[_], E, A](implicit me: MonadError[F, E], eq: Equal[F[A]], ae: Arbitrary[E], aa: Arbitrary[A]): Prop =
      forAll(me.monadErrorLaw.errorsStopComputation[A] _)

    def laws[F[_], E](implicit me: MonadError[F, E], am: Arbitrary[F[Int]], afap: Arbitrary[F[Int => Int]], aeq: Equal[F[Int]], ae: Arbitrary[E], afea: Arbitrary[E => F[Int]]): Properties =
      newProperties("monad error"){ p =>
        p.include(monad.laws[F])
        p.include(applicativeError.laws[F, E])
        p.property("errorsRaised") = errorsRaised[F, E, Int]
        p.property("errorsStopComputation") = errorsStopComputation[F, E, Int]
      }
  }

  object monadTrans {
    def identity[F[_[_], _], G[_], A](implicit F: MonadTrans[F], G: Monad[G], A: Arbitrary[A], Eq: Equal[F[G, A]]): Prop =
      forAll(F.monadTransLaw.identity[G, A] _)
    def composition[F[_[_], _], G[_], A, B](implicit F: MonadTrans[F], G: Monad[G], GA: Arbitrary[G[A]], AGB: Arbitrary[A => G[B]], Eq: Equal[F[G, B]]): Prop =
      forAll(F.monadTransLaw.composition[G, A, B] _)

    def laws[F[_[_], _], G[_]](implicit F: MonadTrans[F], G: Monad[G], AGI: Arbitrary[G[Int]], Eq: Equal[F[G, Int]]): Properties =
      newProperties("monadTrans") { p =>
        p.property("identity") = identity[F, G, Int]
        p.property("composition") = composition[F, G, Int, Int]
      }
  }
}
