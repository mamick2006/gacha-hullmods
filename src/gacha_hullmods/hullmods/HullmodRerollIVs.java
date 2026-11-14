package gacha_hullmods.hullmods;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import gacha_hullmods.GachaStrings;
import gacha_hullmods.Registry;

public class HullmodRerollIVs extends BaseHullMod {

  public static final String ID = "gacha_hullmods_reroll_ivs";
  public static final int COST_IN_REMOVAL_FEES = 5;

  public static int costForHull(ShipHullSpecAPI hull) {
    return (int) Math.min(Integer.MAX_VALUE,
        (long) COST_IN_REMOVAL_FEES * HullmodRemove.costForHull(hull));
  }

  public static int costForMember(FleetMemberAPI member) {
    return costForHull(member.getVariant().getHullSpec());
  }

  public enum Blocked {
    NOT_IN_A_FLEET,
    NO_ROLL,
    NOT_ENOUGH_CREDITS
  }

  public static Blocked blockedReason(FleetMemberAPI member) {
    if (!HullmodAdd.isRealMember(member)) {
      return Blocked.NOT_IN_A_FLEET;
    }
    if (rarityOf(member) == null) {
      return Blocked.NO_ROLL;
    }
    CargoAPI cargo = playerCargo();
    if (cargo != null && cargo.getCredits().get() < costForMember(member)) {
      return Blocked.NOT_ENOUGH_CREDITS;
    }
    return null;
  }

  public static String reasonText(Blocked reason) {
    if (reason == Blocked.NOT_IN_A_FLEET) {
      return GachaStrings.get("reason_module");
    }
    if (reason == Blocked.NOT_ENOUGH_CREDITS) {
      return GachaStrings.get("reason_reroll_no_credits");
    }
    return GachaStrings.get("reason_nothing_to_reroll");
  }

  public static boolean reroll(FleetMemberAPI member, Random random) {
    if (blockedReason(member) != null) {
      return false;
    }
    CargoAPI cargo = playerCargo();
    if (cargo != null) {
      cargo.getCredits().subtract(costForMember(member));
    }
    Registry.getInstance().setIVs(member.getId(), HullmodAdd.rollIVs(random, rarityOf(member)));
    return true;
  }

  private static String rarityOf(FleetMemberAPI member) {
    if (Global.getSector() == null || Registry.getInstance().getFromRegistry(member.getId()) == null) {
      return null;
    }
    return Registry.getInstance().getRarity(member.getId());
  }

  private static CargoAPI playerCargo() {
    if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) {
      return null;
    }
    return Global.getSector().getPlayerFleet().getCargo();
  }

  private static FleetMemberAPI memberOf(ShipAPI ship) {
    return ship.getMutableStats() == null ? null : ship.getMutableStats().getFleetMember();
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    if (!ship.isStationModule()) {
      FleetMemberAPI member = memberOf(ship);
      if (member != null && reroll(member, new Random())) {
        Global.getSoundPlayer().playUISound(rarityOf(member), 1f, 1f);
      }
    }
    ship.getVariant().removeMod(id);
  }

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    return !ship.isStationModule() && blockedReason(memberOf(ship)) == null;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    return reasonText(ship.isStationModule() ? Blocked.NOT_IN_A_FLEET : blockedReason(memberOf(ship)));
  }

  @Override
  public String getDescriptionParam(int index, HullSize hullSize) {
    if (index == 0)
      return String.valueOf(COST_IN_REMOVAL_FEES);
    return null;
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
    tooltip.addPara("", -10f);
    tooltip.addImage(Global.getSettings().getSpriteName("icons", "gacha_hullmods_line"), 100f, 0f);
    tooltip.addPara("", -15f);
    tooltip.addPara(GachaStrings.get("reroll_cost_tooltip"), 5f, Misc.getNegativeHighlightColor(),
        Misc.getDGSCredits(costForHull(ship.getVariant().getHullSpec())));
  }
}
