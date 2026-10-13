package net.chrisrichardson.ftgo.common;

import io.eventuate.common.json.mapper.JSonMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import static net.chrisrichardson.ftgo.common.MoneySerializationTest.MoneyContainer;
import static org.junit.Assert.assertEquals;

/** Amounts travel between services as JSON strings; pin the text each amount is written as. */
public class MoneyJsonFormatTest {

  @BeforeClass
  public static void initialize() {
    CommonJsonMapperInitializer.registerMoneyModule();
  }

  private static String json(Money price) {
    return JSonMapper.toJson(new MoneyContainer(price));
  }

  @Test
  public void shouldWriteAWholeAmountWithTwoDecimals() {
    assertEquals("{\"price\":\"10.00\"}", json(new Money(10)));
  }

  @Test
  public void shouldWriteZeroWithTwoDecimals() {
    assertEquals("{\"price\":\"0.00\"}", json(Money.ZERO));
  }

  @Test
  public void shouldPadAShortFraction() {
    assertEquals("{\"price\":\"12.50\"}", json(new Money("12.5")));
  }

  @Test
  public void shouldRoundALongFraction() {
    assertEquals("{\"price\":\"12.35\"}", json(new Money("12.345")));
  }

  @Test
  public void shouldReadAWholeAmountBack() {
    assertEquals(new MoneyContainer(new Money("10.00")),
            JSonMapper.fromJson("{\"price\":\"10.00\"}", MoneyContainer.class));
  }

  @Test
  public void shouldReadWhatItWrites() {
    MoneyContainer original = new MoneyContainer(new Money("7.25"));
    assertEquals(original, JSonMapper.fromJson(JSonMapper.toJson(original), MoneyContainer.class));
  }

  @Test
  public void shouldStillReadAnAmountWrittenWithoutDecimals() {
    assertEquals(new MoneyContainer(new Money("10")),
            JSonMapper.fromJson("{\"price\":\"10\"}", MoneyContainer.class));
  }
}
