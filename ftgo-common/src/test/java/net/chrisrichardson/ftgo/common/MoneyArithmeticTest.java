package net.chrisrichardson.ftgo.common;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoneyArithmeticTest {

  private final Money ten = new Money(10);
  private final Money fifteen = new Money(15);

  @Test
  public void shouldWriteWholeAmountsWithTwoDecimals() {
    assertEquals("0.00", Money.ZERO.asString());
    assertEquals("10.00", ten.asString());
    assertEquals("1500.00", new Money(1500).asString());
  }

  @Test
  public void shouldPadShortFractionsToTwoDecimals() {
    assertEquals("12.50", new Money("12.5").asString());
    assertEquals("0.10", new Money("0.1").asString());
  }

  @Test
  public void shouldKeepAmountsThatAlreadyHaveTwoDecimals() {
    assertEquals("12.34", new Money("12.34").asString());
    assertEquals("99.99", new Money("99.99").asString());
  }

  @Test
  public void shouldRoundLongFractionsHalfUp() {
    assertEquals("12.35", new Money("12.345").asString());
    assertEquals("12.34", new Money("12.344").asString());
    assertEquals("0.01", new Money("0.005").asString());
    assertEquals("0.00", new Money("0.004").asString());
  }

  @Test
  public void shouldWriteNegativeAmounts() {
    assertEquals("-3.50", new Money("-3.5").asString());
    assertEquals("-12.35", new Money("-12.345").asString());
  }

  @Test
  public void shouldNeverUseScientificNotation() {
    assertEquals("100000000000.00", new Money("1E+11").asString());
    assertEquals("0.00", new Money("1E-9").asString());
  }

  @Test
  public void shouldReadBackWhatItWrites() {
    Money original = new Money("12.35");
    assertEquals(original, new Money(original.asString()));
  }

  @Test
  public void shouldReportCentsFromTheExactAmount() {
    assertEquals(Long.valueOf(1235), new Money("12.35").asLong());
    assertEquals(Long.valueOf(1000), ten.asLong());
  }

  @Test
  public void shouldSubtract() {
    assertEquals(new Money(5), fifteen.subtract(ten));
    assertEquals(new Money(-5), ten.subtract(fifteen));
  }

  @Test
  public void shouldNegate() {
    assertEquals(new Money(-10), ten.negate());
    assertEquals(ten, ten.negate().negate());
  }

  @Test
  public void shouldCompareStrictly() {
    assertTrue(ten.isLessThan(fifteen));
    assertFalse(fifteen.isLessThan(ten));
    assertFalse(ten.isLessThan(new Money(10)));
  }

  @Test
  public void shouldTellZeroFromNonZero() {
    assertTrue(Money.ZERO.isZero());
    assertTrue(new Money("0.00").isZero());
    assertFalse(new Money("0.01").isZero());
    assertFalse(new Money("-0.01").isZero());
  }

  @Test
  public void shouldTellNegativeFromPositive() {
    assertTrue(new Money("-0.01").isNegative());
    assertFalse(Money.ZERO.isNegative());
    assertFalse(ten.isNegative());
  }

  @Test
  public void shouldPickTheSmallerAndLargerAmount() {
    assertEquals(ten, ten.min(fifteen));
    assertEquals(ten, fifteen.min(ten));
    assertEquals(fifteen, ten.max(fifteen));
    assertEquals(fifteen, fifteen.max(ten));
  }

  @Test
  public void shouldTakeAPercentage() {
    assertEquals("1.50", ten.percentage(15).asString());
    assertEquals("0.00", ten.percentage(0).asString());
    assertEquals("10.00", ten.percentage(100).asString());
    assertEquals("20.00", ten.percentage(200).asString());
  }

  @Test
  public void shouldRoundAPercentageToTheCent() {
    assertEquals("0.33", new Money("3.33").percentage(10).asString());
    assertEquals("0.34", new Money("3.35").percentage(10).asString());
  }

  @Test
  public void shouldAllocateEvenlyWhenTheAmountDivides() {
    assertEquals(Arrays.asList("5.00", "5.00"), strings(ten.allocate(2)));
  }

  @Test
  public void shouldGiveExtraCentsToTheEarlierParts() {
    assertEquals(Arrays.asList("3.34", "3.33", "3.33"), strings(ten.allocate(3)));
    assertEquals(Arrays.asList("0.01", "0.01", "0.00"), strings(new Money("0.02").allocate(3)));
  }

  @Test
  public void shouldAllocateIntoOnePartUnchanged() {
    assertEquals(Collections.singletonList("12.35"), strings(new Money("12.35").allocate(1)));
  }

  @Test
  public void shouldAllocateWithoutLosingOrInventingMoney() {
    Money amount = new Money("100.01");
    assertEquals(amount.asString(), Money.sum(amount.allocate(7)).asString());
    assertEquals(7, amount.allocate(7).size());
  }

  @Test
  public void shouldSumAList() {
    assertEquals(new Money(25), Money.sum(Arrays.asList(ten, fifteen)));
    assertEquals(Money.ZERO, Money.sum(Collections.<Money>emptyList()));
  }

  private static List<String> strings(List<Money> amounts) {
    return amounts.stream().map(Money::asString).collect(java.util.stream.Collectors.toList());
  }
}
