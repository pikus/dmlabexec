package dmlabexec;


public interface SeedAccepter {

  /**
   * Throws exception if <code>seed</code> is not accepted.
   * @param seed
   */
  void acceptSeed(long seed);
}
