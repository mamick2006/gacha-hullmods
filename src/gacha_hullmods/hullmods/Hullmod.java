package gacha_hullmods.hullmods;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import gacha_hullmods.GachaHullmodInstructions;
import gacha_hullmods.GachaStrings;
import gacha_hullmods.Registry;
import liminal.instructions.Instruction;
import liminal.instructions.Instruction.Group;
import liminal.instructions.Instructions;

public class Hullmod extends BaseHullMod {

  public static Map<String, String> staticInstructions = new HashMap<>() {
    {
      put("fighter_bay_gacha", "gacha_hullmods_fb");
      put("weapon_op_reduction_gacha", "gacha_hullmods_op");
    }
  };
  public static float levelMultiplier = 1.5f;

  private List<Instruction> activeInstructions(String shipId, Pair<String, Long> pair) {
    List<Instruction> result = new ArrayList<>();
    addNonStatic(result, pair.one);
    addNonStatic(result, Registry.getInstance().getSecondaryFromRegistry(shipId));
    return result;
  }

  private void addNonStatic(List<Instruction> result, String name) {
    if (name == null || name.isEmpty() || staticInstructions.containsKey(name)) {
      return;
    }
    Instruction instruction = GachaHullmodInstructions.getInstruction(name);
    if (instruction != null) {
      result.add(instruction);
    }
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    if (stats.getFleetMember() == null || stats.getFleetMember().getId() == null) {
      return;
    }
    String shipId = stats.getFleetMember().getId();
    Pair<String, Long> pair = Registry.getInstance().getFromRegistry(shipId);
    if (pair == null) {
      return;
    }
    int[] ivs = GachaHullmodInstructions.decodeIVs(pair.two);
    stats.getHullBonus().modifyMult(id, 1 + ivs[0] * 0.01f);
    stats.getBallisticWeaponDamageMult().modifyMult(id, 1 + ivs[1] * 0.01f);
    stats.getArmorBonus().modifyMult(id, 1 + ivs[2] * 0.01f);
    stats.getEnergyWeaponDamageMult().modifyMult(id, 1 + ivs[3] * 0.01f);
    stats.getFluxCapacity().modifyMult(id, 1 + ivs[4] * 0.01f);
    stats.getFluxDissipation().modifyMult(id, 1 + ivs[4] * 0.01f);
    stats.getMaxSpeed().modifyMult(id, 1 + ivs[5] * 0.01f);
    stats.getAcceleration().modifyMult(id, 1 + ivs[5] * 0.01f * 1.5f);
    stats.getDeceleration().modifyMult(id, 1 + ivs[5] * 0.01f * 1.5f);
    stats.getTurnAcceleration().modifyMult(id, 1 + ivs[5] * 0.01f * 2f);
    stats.getMaxTurnRate().modifyMult(id, 1 + ivs[5] * 0.01f * 1.5f);

    for (Instruction instruction : activeInstructions(shipId, pair)) {
      instruction.applyEffectsBeforeShipCreation(hullSize, stats, id, levelMultiplier);
    }
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    if (ship == null || ship.getFleetMemberId() == null) {
      return;
    }
    String shipId = ship.getFleetMemberId();
    Pair<String, Long> pair = Registry.getInstance().getFromRegistry(shipId);
    if (pair == null) {
      return;
    }
    String secondaryName = Registry.getInstance().getSecondaryFromRegistry(shipId);
    for (String name : new String[] { pair.one, secondaryName }) {
      if (name == null) {
        continue;
      }
      String staticInstruction = staticInstructions.get(name);
      if (staticInstruction == null) {
        continue;
      }
      boolean found = false;
      for (String hullmod : ship.getVariant().getPermaMods()) {
        if (hullmod.equals(staticInstruction)) {
          found = true;
          break;
        }
      }
      if (!found) {
        ship.getVariant().addPermaMod(staticInstruction);
      }
    }
    for (Instruction instruction : activeInstructions(shipId, pair)) {
      if (instruction.hasApplyEffectsAfterShipCreation()) {
        instruction.applyEffectsAfterShipCreation(ship, id, levelMultiplier);
      }
    }
  }

  @Override
  public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
    if (ship == null || ship.getFleetMemberId() == null) {
      return;
    }
    String shipId = ship.getFleetMemberId();
    Pair<String, Long> pair = Registry.getInstance().getFromRegistry(shipId);
    if (pair == null) {
      return;
    }
    for (Instruction instruction : activeInstructions(shipId, pair)) {
      if (instruction.hasApplyEffectsToFighterSpawnedByShip()) {
        instruction.applyEffectsToFighterSpawnedByShip(fighter, ship, id, levelMultiplier);
      }
    }
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    if (ship == null || ship.getFleetMemberId() == null) {
      return;
    }
    String shipId = ship.getFleetMemberId();
    Pair<String, Long> pair = Registry.getInstance().getFromRegistry(shipId);
    if (pair == null) {
      return;
    }
    for (Instruction instruction : activeInstructions(shipId, pair)) {
      if (instruction.hasAdvanceInCombat()) {
        instruction.advanceInCombat(ship, amount, levelMultiplier);
      }
    }
  }

  @Override
  public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width,
      boolean isForModSpec) {
    if (isForModSpec) {
      return;
    }
    if (ship == null || ship.getFleetMemberId() == null) {
      return;
    }
    FleetMemberAPI member = ship.getMutableStats() == null ? null : ship.getMutableStats().getFleetMember();
    addRollSection(tooltip, hullSize, ship.getFleetMemberId(), ship.getVariant().getHullSpec(), member, width);
  }

  private static final float STRIP_W = 91f;
  private static final float STRIP_H = 14f;
  private static final float STRIP_PAD = 6f;
  private static final float EFFECT_PAD = 8f;
  private static final float HEADING_PAD = 10f;
  private static final float ROWS_PAD = 7f;
  private static final float COST_PAD = 10f;
  private static final float REASON_PAD = 3f;

  public void addRollSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, String memberId,
      ShipHullSpecAPI hull, FleetMemberAPI member, float width) {
    Pair<String, Long> pair = Registry.getInstance().getFromRegistry(memberId);
    if (pair == null) {
      return;
    }
    int stars = stars();
    if (stars > 0) {
      tooltip.addImage(Global.getSettings().getSpriteName("icons", "gacha_hullmods_stars_" + stars), STRIP_W,
          STRIP_H, STRIP_PAD);
    }
    addEffect(tooltip, hullSize, pair.one, false);
    addEffect(tooltip, hullSize, Registry.getInstance().getSecondaryFromRegistry(memberId), true);

    tooltip.addSectionHeading(GachaStrings.get("ivs_heading"), Alignment.MID, HEADING_PAD);
    IvRows.add(tooltip, width, GachaHullmodInstructions.decodeIVs(pair.two), ROWS_PAD);

    boolean canPay = HullmodRemove.canPay(hull);
    HullmodRemove.addCostLine(tooltip, hull, COST_PAD, HullmodRemove.feeColour(canPay));
    if (!canPay) {
      tooltip.addPara(GachaStrings.get("reason_no_credits"), Misc.getNegativeHighlightColor(), REASON_PAD);
    }
    HullmodRemove.addGuaranteeLine(tooltip, member, HullmodRemove.GUARANTEE_PAD);
  }

  private int stars() {
    String id = spec == null ? "" : spec.getId();
    int digit = id.isEmpty() ? 0 : id.charAt(id.length() - 1) - '0';
    return digit >= 1 && digit <= 5 ? digit : 0;
  }

  private void addEffect(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, String name, boolean second) {
    Instruction instruction = name == null || name.isEmpty() ? null
        : GachaHullmodInstructions.getInstruction(name);
    if (instruction == null) {
      return;
    }
    if (instruction.getGroup() == Group.NEGATIVE) {
      Color text = Misc.getNegativeHighlightColor();
      tooltip.addSectionHeading(GachaStrings.get(second ? "effect_second_negative" : "effect_negative"), text,
          Misc.scaleColorOnly(text, 0.25f), Alignment.MID, HEADING_PAD);
    } else if (second) {
      tooltip.addSectionHeading(GachaStrings.get("effect_second"), Alignment.MID, HEADING_PAD);
    }
    addInstructionToTooltip(tooltip, hullSize, instruction, EFFECT_PAD);
  }

  private void addInstructionToTooltip(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, Instruction instruction,
      float pad) {
    tooltip.setParaFontColor(Instructions.getGroupColor(instruction.getGroup()));
    tooltip.addPara(instruction.getDisplayName(), pad);
    tooltip.setParaFontColor(Misc.getTextColor());
    if (hullSize == null) {
      instruction.getEffectDescription(tooltip, 3f, levelMultiplier);
    } else {
      instruction.getEffectDescription(tooltip, 3f, hullSize, levelMultiplier);
    }
  }

}
