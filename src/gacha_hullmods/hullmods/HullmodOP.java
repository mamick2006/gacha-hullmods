package gacha_hullmods.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import gacha_hullmods.GachaHullmodInstructions;
import liminal.instructions.Instruction;

public class HullmodOP extends BaseHullMod {

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    Instruction instruction = GachaHullmodInstructions.getInstruction("weapon_op_reduction_gacha");
    instruction.applyEffectsBeforeShipCreation(hullSize, stats, id, Hullmod.levelMultiplier);
  }

  @Override
  public boolean affectsOPCosts() {
    return true;
  }
}
