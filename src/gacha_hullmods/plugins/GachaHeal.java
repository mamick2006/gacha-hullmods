package gacha_hullmods.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.campaign.listeners.RefitScreenListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Pair;

import gacha_hullmods.Registry;

public class GachaHeal implements CoreUITabListener, RefitScreenListener {

  @Override
  public void reportAboutToOpenCoreTab(CoreUITabId tab, Object param) {
    healPlayerFleet();
  }

  @Override
  public void reportFleetMemberVariantSaved(FleetMemberAPI member, MarketAPI dockedAt) {
    healPlayerFleet();
  }

  public static void healPlayerFleet() {
    if (Global.getSector() == null) {
      return;
    }
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (fleet == null) {
      return;
    }
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      healMember(member);
    }
  }

  public static void healMember(FleetMemberAPI member) {
    restoreRarityMod(member);
  }

  private static void restoreRarityMod(FleetMemberAPI member) {
    Pair<String, Long> entry = Registry.getInstance().getFromRegistry(member.getId());
    if (entry == null) {
      return;
    }
    for (String mod : member.getVariant().getHullMods()) {
      if (mod.startsWith("gacha_hullmods")) {
        return;
      }
    }
    String rarityId = Registry.getInstance().getRarity(member.getId());
    if (rarityId == null) {
      return;
    }
    ownedVariant(member).addPermaMod(rarityId);
    member.updateStats();
  }

  private static ShipVariantAPI ownedVariant(FleetMemberAPI member) {
    ShipVariantAPI variant = member.getVariant();
    if (variant.getSource() != VariantSource.REFIT) {
      variant = variant.clone();
      variant.setSource(VariantSource.REFIT);
      member.setVariant(variant, false, false);
    }
    return variant;
  }
}
