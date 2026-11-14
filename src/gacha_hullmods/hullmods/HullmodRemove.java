package gacha_hullmods.hullmods;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;

import gacha_hullmods.GachaStrings;
import gacha_hullmods.Registry;
import lunalib.lunaSettings.LunaSettings;

public class HullmodRemove extends BaseHullMod {

  public static final String COST_FRACTION_KEY = "gacha_remove_cost_frac";
  private static final double DEFAULT_COST_FRACTION = 0.3;
  public static final String SURVIVAL_KEY = "gacha_survival_adjustments";
  public static final String SURVIVAL_FEE_MULT_KEY = "gacha_survival_fee_mult";
  private static final double DEFAULT_SURVIVAL_FEE_MULT = 2.0;

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    Registry.getInstance().removeFromRegistry(ship.getFleetMemberId());
    List<String> toRemove = new ArrayList<>();
    for (String hullmod : ship.getVariant().getPermaMods()) {
      if (hullmod.startsWith("gacha_hullmods")) {
        toRemove.add(hullmod);
      }
    }
    for (String hullmod : toRemove) {
      ship.getVariant().removePermaMod(hullmod);
    }
    if (hasCargo()) {
      MutableValue credits = Global.getSector().getPlayerFleet().getCargo().getCredits();
      credits.subtract(getCostForShip(ship));
    }
    Global.getSoundPlayer().playUISound("liminal_monthly_income_negative", 1f, 1f);
    ship.getVariant().removeMod(id);
  }

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    if (ship.isStationModule() || !HullmodAdd.isRealMember(ship)) {
      return false;
    }
    if (!canPay(ship.getVariant().getHullSpec())) {
      return false;
    }
    for (String hullmod : ship.getVariant().getHullMods()) {
      if (hullmod.startsWith("gacha_hullmods")) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getDescriptionParam(int index, HullSize hullSize) {
    if (index == 0)
      return Misc.getRoundedValueMaxOneAfterDecimal((float) (costFraction() * 100)) + "%";
    return null;
  }

  public int getCostForShip(ShipAPI ship) {
    return costForHull(ship.getVariant().getHullSpec());
  }

  public static int costForHull(ShipHullSpecAPI hull) {
    SectorAPI sector = Global.getSector();
    return costForHull(hull, sector != null && sector.isExtMode());
  }

  public static int costForHull(ShipHullSpecAPI hull, boolean survivalCampaign) {
    double fraction = costFraction();
    if (survivalCampaign && survivalAdjustmentsOn()) {
      fraction *= survivalFeeMult();
    }
    return (int) Math.floor(hull.getBaseValue() * fraction + 1e-6);
  }

  public static double survivalFeeMult() {
    return Math.max(0d, Global.getSettings().getSettingsJSON().optDouble(SURVIVAL_FEE_MULT_KEY,
        DEFAULT_SURVIVAL_FEE_MULT));
  }

  public static boolean survivalAdjustmentsOn() {
    if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
      Boolean luna = LunaSettings.getBoolean("gacha_hullmods", SURVIVAL_KEY);
      if (luna != null) {
        return luna.booleanValue();
      }
    }
    return Global.getSettings().getSettingsJSON().optBoolean(SURVIVAL_KEY, true);
  }

  public static double costFraction() {
    if (Global.getSettings() == null) {
      return DEFAULT_COST_FRACTION;
    }
    if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
      Double luna = LunaSettings.getDouble("gacha_hullmods", COST_FRACTION_KEY);
      if (luna != null) {
        return Math.max(0d, luna.doubleValue());
      }
    }
    return Math.max(0d, Global.getSettings().getSettingsJSON().optDouble(COST_FRACTION_KEY,
        DEFAULT_COST_FRACTION));
  }

  public boolean hasCargo() {
    if (Global.getSector() == null) {
      return false;
    }
    if (Global.getSector().getPlayerFleet() == null) {
      return false;
    }
    if (Global.getSector().getPlayerFleet().getCargo() == null) {
      return false;
    }
    return true;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    if (ship.isStationModule() || !HullmodAdd.isRealMember(ship)) {
      return GachaStrings.get("reason_module");
    }
    if (!canPay(ship.getVariant().getHullSpec())) {
      return GachaStrings.get("reason_no_credits");
    }
    return GachaStrings.get("reason_nothing_to_remove");
  }

  public static boolean canPay(ShipHullSpecAPI hull) {
    SectorAPI sector = Global.getSector();
    if (sector == null || sector.getPlayerFleet() == null || sector.getPlayerFleet().getCargo() == null) {
      return true;
    }
    return sector.getPlayerFleet().getCargo().getCredits().get() >= costForHull(hull);
  }

  static final float GUARANTEE_PAD = 7f;

  static void addCostLine(TooltipMakerAPI tooltip, ShipHullSpecAPI hull, float pad, Color number) {
    tooltip.addPara(GachaStrings.get("remove_cost_tooltip"), pad, number, Misc.getDGSCredits(costForHull(hull)));
  }

  static Color feeColour(boolean canPay) {
    return canPay ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor();
  }

  static int guaranteeRolls(FleetMemberAPI member) {
    if (Global.getSector() == null || !HullmodAdd.isRealMember(member)) {
      return 0;
    }
    return HullmodAdd.rollsUntilGuarantee(member.getId());
  }

  static void addGuaranteeLine(TooltipMakerAPI tooltip, FleetMemberAPI member, float pad) {
    int rolls = guaranteeRolls(member);
    if (rolls <= 0) {
      return;
    }
    if (rolls == 1) {
      tooltip.addPara(GachaStrings.get("guarantee_next_tooltip"), pad);
    } else {
      tooltip.addPara(GachaStrings.get("guarantee_tooltip"), pad, Misc.getHighlightColor(), "" + rolls);
    }
  }

  public static void addRemoveSection(TooltipMakerAPI tooltip, ShipHullSpecAPI hull, FleetMemberAPI member) {
    tooltip.addPara("", -10f);
    tooltip.addImage(Global.getSettings().getSpriteName("icons", "gacha_hullmods_line"), 100f, 0f);
    tooltip.addPara("", -15f);
    addCostLine(tooltip, hull, 5f, feeColour(canPay(hull)));
    addGuaranteeLine(tooltip, member, GUARANTEE_PAD);
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
    addRemoveSection(tooltip, ship.getVariant().getHullSpec(), member);
  }
}
