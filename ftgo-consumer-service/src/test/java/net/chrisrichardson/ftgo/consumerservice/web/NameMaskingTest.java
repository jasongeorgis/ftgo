package net.chrisrichardson.ftgo.consumerservice.web;

import net.chrisrichardson.ftgo.common.PersonName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class NameMaskingTest {

  private static void assertName(String first, String last, PersonName actual) {
    assertEquals(first, actual.getFirstName());
    assertEquals(last, actual.getLastName());
  }

  @Test
  public void shouldKeepOnlyInitialsByDefault() {
    assertName("J.", "D.", NameMasking.mask(new PersonName("John", "Doe")));
  }

  @Test
  public void shouldUseInitialsAsTheDefaultLevel() {
    assertEquals(MaskLevel.INITIAL, NameMasking.defaultLevel());
  }

  @Test
  public void shouldUpperCaseInitials() {
    assertName("J.", "D.", NameMasking.mask(new PersonName("john", "doe")));
  }

  @Test
  public void shouldIgnoreSurroundingWhitespace() {
    assertName("J.", "D.", NameMasking.mask(new PersonName("  John ", "\tDoe\n")));
  }

  @Test
  public void shouldTurnBlankPartsIntoEmptyParts() {
    assertName("", "D.", NameMasking.mask(new PersonName("   ", "Doe")));
    assertName("J.", "", NameMasking.mask(new PersonName("John", "")));
  }

  @Test
  public void shouldKeepMissingPartsMissing() {
    PersonName masked = NameMasking.mask(new PersonName("John", null));
    assertEquals("J.", masked.getFirstName());
    assertEquals(null, masked.getLastName());
  }

  @Test
  public void shouldHandleSingleLetterParts() {
    assertName("A.", "B.", NameMasking.mask(new PersonName("a", "B")));
  }

  @Test
  public void shouldKeepTheFirstLetterOfAccentedNames() {
    assertName("É.", "N.", NameMasking.mask(new PersonName("émile", "Nadeau")));
  }

  @Test
  public void shouldBeIdempotent() {
    PersonName once = NameMasking.mask(new PersonName("John", "Doe"));
    PersonName twice = NameMasking.mask(once);
    assertName(once.getFirstName(), once.getLastName(), twice);
  }

  @Test
  public void shouldReturnANewNameAndLeaveTheOriginalUntouched() {
    PersonName original = new PersonName("John", "Doe");
    PersonName masked = NameMasking.mask(original);
    assertNotSame(original, masked);
    assertName("John", "Doe", original);
  }

  @Test
  public void shouldKeepTheNameAsWrittenAtLevelNone() {
    assertName("John", "Doe", NameMasking.mask(new PersonName("John", "Doe"), MaskLevel.NONE));
  }

  @Test
  public void shouldRedactEveryPartAtLevelRedacted() {
    assertName("", "", NameMasking.mask(new PersonName("John", "Doe"), MaskLevel.REDACTED));
  }

  @Test
  public void shouldPassAMissingNameThrough() {
    assertEquals(null, NameMasking.mask(null));
    assertEquals(null, NameMasking.mask(null, MaskLevel.REDACTED));
  }

  @Test
  public void shouldTreatNamesWithTheSameInitialsAsTheSameWhenMasked() {
    PersonName john = new PersonName("John", "Doe");
    PersonName jane = new PersonName("Jane", "Dorsey");
    assertTrue(NameMasking.sameWhenMasked(john, jane, MaskLevel.INITIAL));
    assertFalse(NameMasking.sameWhenMasked(john, jane, MaskLevel.NONE));
  }

  @Test
  public void shouldTreatEveryNameAsTheSameWhenRedacted() {
    assertTrue(NameMasking.sameWhenMasked(new PersonName("John", "Doe"), new PersonName("Mary", "Major"), MaskLevel.REDACTED));
  }

  @Test
  public void shouldTellDifferentInitialsApart() {
    assertFalse(NameMasking.sameWhenMasked(new PersonName("John", "Doe"), new PersonName("Mary", "Doe"), MaskLevel.INITIAL));
  }

  @Test
  public void shouldCompareMissingNamesAsEqualOnlyToMissingNames() {
    assertTrue(NameMasking.sameWhenMasked(null, null, MaskLevel.INITIAL));
    assertFalse(NameMasking.sameWhenMasked(null, new PersonName("John", "Doe"), MaskLevel.INITIAL));
  }
}
