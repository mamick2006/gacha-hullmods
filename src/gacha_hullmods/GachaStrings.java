package gacha_hullmods;

import com.fs.starfarer.api.Global;

public final class GachaStrings {

  private GachaStrings() {
  }

  public static String get(String key) {
    return Global.getSettings().getString("gacha_hullmods", key);
  }
}
