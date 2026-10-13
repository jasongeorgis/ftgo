package net.chrisrichardson.ftgo.consumerservice.web;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MaskLevelTest {

  @Test
  public void noneKeepsThePartAsWritten() {
    assertEquals("  John ", MaskLevel.NONE.apply("  John "));
  }

  @Test
  public void initialKeepsTheUpperCasedFirstLetterAndAPeriod() {
    assertEquals("J.", MaskLevel.INITIAL.apply("john"));
  }

  @Test
  public void initialTrimsBeforeTakingTheLetter() {
    assertEquals("D.", MaskLevel.INITIAL.apply("   doe"));
  }

  @Test
  public void initialOfABlankPartIsEmpty() {
    assertEquals("", MaskLevel.INITIAL.apply("   "));
    assertEquals("", MaskLevel.INITIAL.apply(""));
  }

  @Test
  public void initialIsStableWhenAppliedAgain() {
    assertEquals("J.", MaskLevel.INITIAL.apply(MaskLevel.INITIAL.apply("John")));
  }

  @Test
  public void redactedKeepsNothing() {
    assertEquals("", MaskLevel.REDACTED.apply("John"));
  }

  @Test
  public void everyLevelLeavesAMissingPartMissing() {
    for (MaskLevel level : MaskLevel.values()) {
      assertEquals(level.name(), null, level.apply(null));
    }
  }
}
