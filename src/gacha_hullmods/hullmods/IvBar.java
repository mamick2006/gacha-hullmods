package gacha_hullmods.hullmods;

public final class IvBar {

  private IvBar() {
  }

  public static final int TRACK = 100;
  public static final int TICK = TRACK / 2;

  public static String signed(int iv) {
    return (iv > 0 ? "+" : "") + iv + "%";
  }

  public static int length(int iv, int maxIv) {
    return Math.min(TICK, Math.round(Math.abs(iv) * (float) TICK / maxIv));
  }

  public static int left(int iv, int maxIv) {
    return iv < 0 ? TICK - length(iv, maxIv) : TICK;
  }
}
