package net.chrisrichardson.ftgo.common;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//@Embeddable
//@Access(AccessType.FIELD)
public class Money {

  public static Money ZERO = new Money(0);

  private BigDecimal amount;

  private Money() {
  }

  public Money(BigDecimal amount) {
    this.amount = amount;
  }

  public Money(String s) {
    this.amount = new BigDecimal(s);
  }

  public Money(int i) {
    this.amount = new BigDecimal(i);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    Money money = (Money) o;

    return new EqualsBuilder()
            .append(amount, money.amount)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(amount)
            .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("amount", amount)
            .toString();
  }


  public Money add(Money delta) {
    return new Money(amount.add(delta.amount));
  }

  public boolean isGreaterThanOrEqual(Money other) {
    return amount.compareTo(other.amount) >= 0;
  }

  /** The amount written with exactly two decimal places, rounded half up, e.g. {@code 12.50}. */
  public String asString() {
    return rounded().toPlainString();
  }

  private BigDecimal rounded() {
    return amount.setScale(currencyScale(), RoundingMode.HALF_UP);
  }

  private static int currencyScale() {
    return 2;
  }

  public Money subtract(Money delta) {
    return new Money(amount.subtract(delta.amount));
  }

  public Money negate() {
    return new Money(amount.negate());
  }

  public boolean isLessThan(Money other) {
    return amount.compareTo(other.amount) < 0;
  }

  public boolean isZero() {
    return amount.signum() == 0;
  }

  public boolean isNegative() {
    return amount.signum() < 0;
  }

  /** This amount scaled by a whole percentage, rounded to two decimal places. */
  public Money percentage(int percent) {
    BigDecimal scaled = amount.multiply(new BigDecimal(percent)).divide(new BigDecimal(100), currencyScale(), RoundingMode.HALF_UP);
    return new Money(scaled);
  }

  /**
   * Splits the amount into the given number of parts that add back up to the whole. Parts
   * differ by at most one cent; the earlier parts take the extra cents.
   */
  public List<Money> allocate(int parts) {
    long cents = rounded().movePointRight(currencyScale()).longValueExact();
    long share = cents / parts;
    long remainder = cents % parts;
    return IntStream.range(0, parts)
            .mapToObj(index -> new Money(BigDecimal.valueOf(share + (index < remainder ? 1 : 0), currencyScale())))
            .collect(Collectors.toList());
  }

  /** The smaller of the two amounts. */
  public Money min(Money other) {
    return isLessThan(other) ? this : other;
  }

  /** The larger of the two amounts. */
  public Money max(Money other) {
    return isLessThan(other) ? other : this;
  }

  /** The total of the given amounts, or {@link #ZERO} when there are none. */
  public static Money sum(List<Money> amounts) {
    return amounts.stream().reduce(new Money(0), Money::add);
  }

  public Money multiply(int x) {
    return new Money(amount.multiply(new BigDecimal(x)));
  }

  public Long asLong() {
    return multiply(100).amount.longValue();
  }
}
