package net.chrisrichardson.ftgo.consumerservice.web;

import net.chrisrichardson.ftgo.common.PersonName;

import java.util.Optional;

/**
 * Masks consumer names so the service stores less personal data than it was sent.
 *
 * <p>Masking is idempotent: masking an already masked name changes nothing, so a name that
 * passes through more than one layer is not degraded further.
 */
public final class NameMasking {

  private NameMasking() {
  }

  /** The level applied to every incoming consumer name. */
  public static MaskLevel defaultLevel() {
    return MaskLevel.INITIAL;
  }

  public static PersonName mask(PersonName name) {
    return mask(name, defaultLevel());
  }

  public static PersonName mask(PersonName name, MaskLevel level) {
    return Optional.ofNullable(name)
            .map(present -> new PersonName(level.apply(present.getFirstName()), level.apply(present.getLastName())))
            .orElse(name);
  }

  /** Whether the two names are the same once both are masked at the given level. */
  public static boolean sameWhenMasked(PersonName first, PersonName second, MaskLevel level) {
    PersonName left = mask(first, level);
    PersonName right = mask(second, level);
    return Optional.ofNullable(left).map(PersonName::getFirstName).equals(Optional.ofNullable(right).map(PersonName::getFirstName))
            && Optional.ofNullable(left).map(PersonName::getLastName).equals(Optional.ofNullable(right).map(PersonName::getLastName));
  }
}
