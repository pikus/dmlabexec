package dmlabexec;

import java.util.HashSet;
import java.util.Set;

/**
 * Making sure, that seeds are unique.
 */
public class DuplicateCheck implements SeedAccepter {

  private Set<Long> collected = new HashSet<>();

  /**
   * @throws if <code>seed</code> was already supplied.
   */
  @Override
  public void acceptSeed(long seed) {
    boolean added = collected.add(Long.valueOf(seed));
    if (!added) {
      throw new IllegalArgumentException("Duplicate seed " + seed);
    }
  }
}
