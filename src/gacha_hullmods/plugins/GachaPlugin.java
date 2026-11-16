package gacha_hullmods.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

import gacha_hullmods.GachaHullmodInstructions;
import gacha_hullmods.GachaStrings;
import gacha_hullmods.Registry;
import gacha_hullmods.hullmods.HullmodAdd;
import gacha_hullmods.rules.GachaHullmods_AtStart;
import gacha_hullmods.rules.StartRule;
import liminal.start.StartPlace;

public class GachaPlugin extends BaseModPlugin {
  @Override
  public void onApplicationLoad() throws Exception {
    try {
      GachaHullmodInstructions.ensureLoaded();
    } catch (Throwable t) {
      Global.getLogger(GachaPlugin.class).error("Gacha Hullmods: could not read the instruction table"
          + " at startup, so it is read again on first use.", t);
    }
  }

  @Override
  public void onNewGame() {
  }

  @Override
  public void onGameLoad(boolean newGame) {
    HullmodAdd.forgetScannedFleet();
    Global.getSector().getListenerManager().addListener(new GachaHeal(), true);
    GachaHeal.healPlayerFleet();
    tellStartPlace();
    Registry registry = Registry.existing();
    if (registry == null) {
      return;
    }
    try {
      int dropped = registry.pruneShipsGoneTooLong();
      if (dropped > 0) {
        Global.getLogger(GachaPlugin.class).info("Gacha Hullmods: dropped the rolls of " + dropped
            + " ships not found for " + Registry.keepDays() + " days.");
      }
    } catch (Throwable t) {
      Global.getLogger(GachaPlugin.class).error("Gacha Hullmods: could not check which rolled ships"
          + " still exist, so every roll is kept until the next load.", t);
    }
  }

  private static void tellStartPlace() {
    try {
      StartPlace.decide();
      boolean started = Global.getSector().getCharacterData().getMemoryWithoutUpdate()
          .getBoolean("$gacha_hullmods_enabled");
      if (StartRule.tellsStartPlace(started, !started && GachaHullmods_AtStart.skipTrip())) {
        StartPlace.tell(GachaStrings.get("msg_start_place"),
            Global.getSettings().getSpriteName("icons", "gacha_hullmods_icon"), Misc.getHighlightColor());
      }
    } catch (Throwable t) {
      Global.getLogger(GachaPlugin.class).warn("Gacha Hullmods: could not work out where the gacha"
          + " starts in this sector, so it starts at the Galatia Academy if the sector has one.", t);
    }
  }
}
