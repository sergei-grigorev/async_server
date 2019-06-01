package com.github.spgrigorev.scodec.laws

import cats.Eq
import cats.kernel.laws.discipline._
import com.github.spgrigorev.scodec.data.Buffer
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.typelevel.discipline.Laws

trait BufferTests[B] extends Laws {
  def laws: BufferLaws[B]

  def algebra(implicit arbBuffer: Arbitrary[B],
              arbBufferSize: Arbitrary[NonNegInt],
              arbNonEmptySize: Arbitrary[PosInt],
              eqBoolean: Eq[Boolean],
              eqInt: Eq[NonNegInt]) =
    new SimpleRuleSet(
      name = "Buffer",
      "newly allocated buffer's isFull flag is not defined" ->
        forAll(laws.newNonEmptyAllocatedIsNotFull _),
      "newly allocated buffer's start position is zero" ->
        forAll(laws.newAllocatedStartPositionIsZero _),
      "empty buffer is always full" ->
        delay(laws.zeroSizeBufferIsFullAlways()),
      "compaction shifts start position" ->
        // todo: for real life tests position should be in range [0, capacity]
        forAll(laws.compactionShiftsStartPosition _),
      "increasing capacity doesn't shift start position" ->
        forAll(laws.increaseCapacityDoesntShiftStartPosition _),
      "increasing capacity clears isFull flag" ->
        forAll(laws.increaseCapacityAlwaysClearsIsFullFlag _)
    )
}

object BufferTests {
  def apply[B](instance: Buffer[B]): BufferTests[B] = new BufferTests[B] {
    override val laws: BufferLaws[B] = BufferLaws(instance)
  }
}
