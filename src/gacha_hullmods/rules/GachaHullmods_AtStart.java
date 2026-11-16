package gacha_hullmods.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.start.StartPlace;
import lunalib.lunaSettings.LunaSettings;

public class GachaHullmods_AtStart extends BaseCommandPlugin {

  public static final String SKIP_KEY = "gacha_skip_galatia";

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    if (dialog == null) {
      return false;
    }
    SectorEntityToken entity = dialog.getInteractionTarget();
    MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
    boolean started = Global.getSector().getCharacterData().getMemoryWithoutUpdate()
        .getBoolean("$gacha_hullmods_enabled");
    boolean skip = !started && skipTrip();
    return StartRule.offered(started, StartPlace.isHere(entity, local), skip,
        skip && dockedAtMarketMenu(entity, local));
  }

  public static boolean skipTrip() {
    if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
      Boolean luna = LunaSettings.getBoolean("gacha_hullmods", SKIP_KEY);
      if (luna != null) {
        return luna.booleanValue();
      }
    }
    return Global.getSettings().getSettingsJSON().optBoolean(SKIP_KEY, false);
  }

  private static boolean dockedAtMarketMenu(SectorEntityToken entity, MemoryAPI local) {
    MarketAPI market = entity == null ? null : entity.getMarket();
    return market != null && local != null && !market.isPlanetConditionMarketOnly()
        && !LGlobals.liminalFactionId.equals(market.getFactionId())
        && "main".equals(local.getString("$menuState"))
        && !"NONE".equals(local.getString("$tradeMode"));
  }
}
