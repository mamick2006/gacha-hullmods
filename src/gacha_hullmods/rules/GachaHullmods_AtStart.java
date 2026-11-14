package gacha_hullmods.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import liminal.start.StartPlace;

public class GachaHullmods_AtStart extends BaseCommandPlugin {

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    return dialog != null
        && StartPlace.isHere(dialog.getInteractionTarget(), memoryMap.get(MemKeys.LOCAL));
  }
}
