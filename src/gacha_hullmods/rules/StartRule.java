package gacha_hullmods.rules;

public final class StartRule {

  private StartRule() {
  }

  public static boolean offered(boolean started, boolean atStartPlace, boolean skipTrip,
      boolean dockedAtMarketMenu) {
    if (started) {
      return false;
    }
    return atStartPlace || (skipTrip && dockedAtMarketMenu);
  }

  public static boolean tellsStartPlace(boolean started, boolean skipTrip) {
    return !started && !skipTrip;
  }
}
