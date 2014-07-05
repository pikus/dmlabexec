package dmlabexec;

public enum MVariantProfile {

  N_30_DELTA_05_RO_00(15, 0.5, 0),

  N_30_DELTA_10_RO_00(15, 1, 0),

  N_30_DELTA_15_RO_00(15, 1.5, 0),

  N_30_DELTA_05_RO_02(15, 0.5, 0.2),

  N_30_DELTA_05_RO_04(15, 0.5, 0.4),

  N_30_DELTA_10_RO_02(15, 1, 0.2),

  N_30_DELTA_10_RO_04(15, 1, 0.4),

  N_30_DELTA_15_RO_02(15, 1.5, 0.2),

  N_30_DELTA_15_RO_04(15, 1.5, 0.4),

  N_50_DELTA_05_RO_00(25, 0.5, 0),

  N_50_DELTA_10_RO_00(25, 1, 0),

  N_50_DELTA_15_RO_00(25, 1.5, 0),

  N_50_DELTA_05_RO_02(25, 0.5, 0.2),

  N_50_DELTA_05_RO_04(25, 0.5, 0.4),

  N_50_DELTA_10_RO_02(25, 1, 0.2),

  N_50_DELTA_10_RO_04(25, 1, 0.4),

  N_50_DELTA_15_RO_02(25, 1.5, 0.2),

  N_50_DELTA_15_RO_04(25, 1.5, 0.4);

  private int n;
  private double delta;
  private double ro;

  public static void main(String args[]) {
    System.out.println("Constants:");
    long seed = System.currentTimeMillis();
    for (MVariantProfile profile : MVariantProfile.values()) {
      for (int i = 1; i <= 100; i++) {
        StringBuilder command = new StringBuilder("java -classpath dmlabexec-1.0.0-SNAPSHOT-jar-with-dependencies.jar dmlabexec.MCFSRunner ");
        command.append(" --seed " + seed);
        command.append(" --start " + i);
        command.append(" --end " + i);
        command.append(" --projections 3000 ");
        command.append(" --profile " + profile.name());
        System.out.println(command);
        seed = seed + 1;
      }
    }
  }

  private MVariantProfile(int n, double delta, double ro) {
    this.n = n;
    this.delta = delta;
    this.ro = ro;
  }

  public int n() {
    return n;
  }

  public double ro() {
    return ro;
  }

  public double delta() {
    return delta;
  }

}
