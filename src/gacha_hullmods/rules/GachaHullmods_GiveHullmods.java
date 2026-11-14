package gacha_hullmods.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import gacha_hullmods.GachaStrings;
import gacha_hullmods.hullmods.HullmodRerollIVs;

public class GachaHullmods_GiveHullmods extends BaseCommandPlugin {

  public static void grantHullmods() {
    Global.getSector().getCharacterData().addHullMod("gacha_hullmods_add");
    Global.getSector().getCharacterData().addHullMod("gacha_hullmods_remove");
    Global.getSector().getCharacterData().addHullMod(HullmodRerollIVs.ID);
  }

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    grantHullmods();
    TextPanelAPI text = dialog.getTextPanel();
    text.setFontSmallInsignia();
    text.addParagraph(GachaStrings.get("dialog_gained"), Misc.getPositiveHighlightColor());
    text.setFontInsignia();
    Global.getSoundPlayer().playUISound(BaseIntelPlugin.getSoundStandardPosting(), 1f, 1f);
    return true;
  }
}
