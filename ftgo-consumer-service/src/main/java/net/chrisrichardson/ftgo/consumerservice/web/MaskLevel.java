package net.chrisrichardson.ftgo.consumerservice.web;

import java.util.Optional;
import java.util.function.UnaryOperator;

/** How much of a name part is kept when a consumer name is masked for privacy. */
public enum MaskLevel {

  /** Keep the name part as written. */
  NONE(part -> part),

  /** Keep only the first letter of the name part, upper-cased and followed by a period. */
  INITIAL(part -> Optional.of(part.trim())
          .filter(trimmed -> !trimmed.isEmpty())
          .map(trimmed -> trimmed.substring(0, 1).toUpperCase() + ".")
          .orElse("")),

  /** Keep nothing of the name part. */
  REDACTED(part -> "");

  private final UnaryOperator<String> rule;

  MaskLevel(UnaryOperator<String> rule) {
    this.rule = rule;
  }

  /** Applies this level to one name part. A missing part stays missing. */
  public String apply(String part) {
    return Optional.ofNullable(part).map(rule).orElse(part);
  }
}
