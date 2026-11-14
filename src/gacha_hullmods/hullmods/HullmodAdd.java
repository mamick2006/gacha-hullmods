package gacha_hullmods.hullmods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import gacha_hullmods.GachaHullmodInstructions;
import gacha_hullmods.GachaHullmodInstructions.Need;
import gacha_hullmods.GachaStrings;
import gacha_hullmods.Registry;
import liminal.instructions.Instruction;
import liminal.instructions.Instruction.Group;
import lunalib.lunaSettings.LunaSettings;

public class HullmodAdd extends BaseHullMod implements HullModFleetEffect {

  private static String[] hullmodIds = new String[] { "gacha_hullmods_1", "gacha_hullmods_2",
      "gacha_hullmods_3", "gacha_hullmods_4", "gacha_hullmods_5" };

  private static final String FIVE_STAR = "gacha_hullmods_5";

  public static final String GUARANTEE_KEY = "gacha_pity_pulls";
  private static final int DEFAULT_GUARANTEE = 30;

  public static final float SECOND_EFFECT_CHANCE = 0.2f;
  private static final Group[] SECOND_EFFECT_GROUPS = { Group.NEGATIVE, Group.STANDARD, Group.LOGISTIC,
      Group.SPECIAL };
  private static final float[] SECOND_EFFECT_WEIGHTS = { 0.3f, 0.3f, 0.2f, 0.2f };

  public static class RollResult {
    public final String hullmodId;
    public final String instructionName;
    public final String secondaryInstructionName;
    public final long encodedIVs;

    public RollResult(String hullmodId, String instructionName, String secondaryInstructionName, long encodedIVs) {
      this.hullmodId = hullmodId;
      this.instructionName = instructionName;
      this.secondaryInstructionName = secondaryInstructionName;
      this.encodedIVs = encodedIVs;
    }
  }

  public static RollResult roll(Random random) {
    return roll(random, false, Collections.<Need>emptySet());
  }

  public static RollResult roll(Random random, boolean fiveStarGuaranteed, Set<Need> lacking) {
    float[] probabilities = getProbabilityForHullmod();
    float roll = random.nextFloat();
    float cumulativeProbability = 0;
    String hullmodId = hullmodIds[hullmodIds.length - 1];
    int tier = hullmodIds.length;
    for (int i = 0; i < probabilities.length; i++) {
      cumulativeProbability += probabilities[i];
      if (roll <= cumulativeProbability || i == probabilities.length - 1) {
        hullmodId = hullmodIds[i];
        tier = i + 1;
        break;
      }
    }
    if (fiveStarGuaranteed) {
      hullmodId = FIVE_STAR;
      tier = hullmodIds.length;
    }

    List<Instruction> tierInstructions = usableOn(GachaHullmodInstructions.getInstructionsInTier(tier),
        lacking);
    Instruction instruction = tierInstructions.get(random.nextInt(tierInstructions.size()));

    String secondary = null;
    if (random.nextFloat() < SECOND_EFFECT_CHANCE) {
      Instruction second = rollSecondEffect(random, instruction.getName(), lacking);
      if (second != null) {
        secondary = second.getName();
      }
    }

    return new RollResult(hullmodId, instruction.getName(), secondary, rollIVs(random, hullmodId));
  }

  public static long rollIVs(Random random, String rarityHullmodId) {
    int[] ivs = new int[6];
    for (int j = 0; j < 6; j++) {
      ivs[j] = getTruncatedNormalSample(getMeanForHullmod(rarityHullmodId), 15,
          (int) GachaHullmodInstructions.MIN_IV, (int) GachaHullmodInstructions.MAX_IV, random);
    }
    return GachaHullmodInstructions.encodeIVs(ivs);
  }

  private static List<Instruction> usableOn(List<Instruction> instructions, Set<Need> lacking) {
    if (lacking.isEmpty()) {
      return instructions;
    }
    List<Instruction> usable = new ArrayList<>();
    for (Instruction instruction : instructions) {
      if (isUsable(instruction, lacking)) {
        usable.add(instruction);
      }
    }
    return usable.isEmpty() ? instructions : usable;
  }

  private static boolean isUsable(Instruction instruction, Set<Need> lacking) {
    Need need = GachaHullmodInstructions.getNeed(instruction.getName());
    return need == null || !lacking.contains(need);
  }

  public static Instruction rollSecondEffect(Random random, String primaryName, Set<Need> lacking) {
    for (int attempt = 0; attempt < 10; attempt++) {
      float roll = random.nextFloat();
      float cumulative = 0;
      Group group = SECOND_EFFECT_GROUPS[SECOND_EFFECT_GROUPS.length - 1];
      for (int i = 0; i < SECOND_EFFECT_GROUPS.length; i++) {
        cumulative += SECOND_EFFECT_WEIGHTS[i];
        if (roll <= cumulative) {
          group = SECOND_EFFECT_GROUPS[i];
          break;
        }
      }
      List<Instruction> instructions = GachaHullmodInstructions.getInstructionsInGroup(group);
      if (instructions == null || instructions.isEmpty()) {
        continue;
      }
      Instruction candidate = instructions.get(random.nextInt(instructions.size()));
      if (!candidate.getName().equals(primaryName) && isUsable(candidate, lacking)) {
        return candidate;
      }
    }
    return null;
  }

  public static int guaranteeAt() {
    return Global.getSettings().getSettingsJSON().optInt(GUARANTEE_KEY, DEFAULT_GUARANTEE);
  }

  public static int rollsUntilGuarantee(String memberId) {
    int guarantee = guaranteeAt();
    if (guarantee <= 0) {
      return 0;
    }
    Registry registry = Registry.existing();
    int rolled = registry == null ? 0 : registry.getRollsSinceFiveStar(memberId);
    return Math.max(1, guarantee - rolled);
  }

  public static RollResult rollFor(String memberId, ShipHullSpecAPI hull, Random random) {
    boolean guaranteed = rollsUntilGuarantee(memberId) == 1;
    RollResult result = roll(random, guaranteed, GachaHullmodInstructions.lackedBy(hull));
    Registry registry = Registry.getInstance();
    registry.addToRegistry(memberId, result.instructionName, result.secondaryInstructionName,
        result.encodedIVs);
    registry.setRarity(memberId, result.hullmodId);
    registry.countRoll(memberId, FIVE_STAR.equals(result.hullmodId));
    return result;
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    if (ship.getFleetMemberId() == null || ship.isStationModule()) {
      ship.getVariant().removeMod(id);
      return;
    }
    RollResult result = rollFor(ship.getFleetMemberId(), ship.getHullSpec(), new Random());
    ship.getVariant().addPermaMod(result.hullmodId);

    ship.getVariant().removeMod(id);
    Global.getSoundPlayer().playUISound(result.hullmodId, 1f, 1f);
  }

  @Override
  public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width,
      boolean isForModSpec) {
    if (isForModSpec || ship == null || ship.getFleetMemberId() == null) {
      return;
    }
    FleetMemberAPI member = ship.getMutableStats() == null ? null : ship.getMutableStats().getFleetMember();
    if (HullmodRemove.guaranteeRolls(member) <= 0) {
      return;
    }
    tooltip.addPara("", -10f);
    tooltip.addImage(Global.getSettings().getSpriteName("icons", "gacha_hullmods_line"), 100f, 0f);
    tooltip.addPara("", -15f);
    HullmodRemove.addGuaranteeLine(tooltip, member, 5f);
  }

  @Override
  public String getDescriptionParam(int index, HullSize hullSize) {
    float[] probabilities = getProbabilityForHullmod();
    if (index == 0)
      return "" + (int) (probabilities[0] * 100f) + "%";
    if (index == 1)
      return "" + (int) (probabilities[1] * 100f) + "%";
    if (index == 2)
      return "" + (int) (probabilities[2] * 100f) + "%";
    if (index == 3)
      return "" + (int) (probabilities[3] * 100f) + "%";
    if (index == 4)
      return "" + (int) (probabilities[4] * 100f) + "%";
    if (index == 5)
      return "<1%";
    if (index == 6)
      return GachaHullmodInstructions.MIN_IV + "%";
    if (index == 7)
      return GachaHullmodInstructions.MAX_IV + "%";
    if (index == 8)
      return (int) (SECOND_EFFECT_CHANCE * 100f) + "%";
    return null;
  }

  private static float[] getProbabilityForHullmod() {
    float[] probabilities = new float[] { rarityWeight("gacha_1_star_prob", 0.3f),
        rarityWeight("gacha_2_star_prob", 0.3f), rarityWeight("gacha_3_star_prob", 0.2f),
        rarityWeight("gacha_4_star_prob", 0.15f), rarityWeight("gacha_5_star_prob", 0.05f) };
    float sum = 0;
    for (float probability : probabilities) {
      sum += probability;
    }
    for (int i = 0; i < probabilities.length; i++) {
      probabilities[i] /= sum;
    }
    return probabilities;
  }

  private static float rarityWeight(String key, float fallback) {
    if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
      Float luna = LunaSettings.getFloat("gacha_hullmods", key);
      if (luna != null) {
        return luna.floatValue();
      }
    }
    return Global.getSettings().optFloat(key, fallback);
  }

  private static int getMeanForHullmod(String hullmodId) {
    if (hullmodId.startsWith("gacha_hullmods_1")) {
      return -10;
    } else if (hullmodId.startsWith("gacha_hullmods_2")) {
      return -5;
    } else if (hullmodId.startsWith("gacha_hullmods_3")) {
      return 0;
    } else if (hullmodId.startsWith("gacha_hullmods_4")) {
      return 5;
    } else if (hullmodId.startsWith("gacha_hullmods_5")) {
      return 10;
    }
    return 0;
  }

  private static int getTruncatedNormalSample(int mean, int stdDev, int min, int max, Random random) {
    int result;
    do {
      result = (int) Math.round(mean + stdDev * random.nextGaussian());
    } while (result < min || result > max);

    return result;
  }

  static boolean isRealMember(ShipAPI ship) {
    return ship.getMutableStats() != null && isRealMember(ship.getMutableStats().getFleetMember());
  }

  public static boolean isRealMember(FleetMemberAPI member) {
    FleetDataAPI fleet = member == null ? null : member.getFleetData();
    if (fleet == null) {
      return false;
    }
    if (fleet != scannedFleet || fleet.getNumMembers() != scannedCount
        || (!scannedMembers.contains(member) && member != lastMiss)) {
      scannedFleet = fleet;
      scannedMembers = Collections.newSetFromMap(new IdentityHashMap<FleetMemberAPI, Boolean>());
      scannedMembers.addAll(fleet.getMembersListCopy());
      scannedCount = fleet.getNumMembers();
      lastMiss = null;
    }
    if (scannedMembers.contains(member)) {
      return true;
    }
    lastMiss = member;
    return false;
  }

  private static FleetDataAPI scannedFleet;
  private static int scannedCount;
  private static Set<FleetMemberAPI> scannedMembers = Collections.emptySet();
  private static FleetMemberAPI lastMiss;

  @Override
  public boolean withOnFleetSync() {
    return true;
  }

  @Override
  public void onFleetSync(CampaignFleetAPI fleet) {
    Registry.seeFleet(fleet);
  }

  @Override
  public boolean withAdvanceInCampaign() {
    return false;
  }

  @Override
  public void advanceInCampaign(CampaignFleetAPI fleet) {
  }

  public static void forgetScannedFleet() {
    scannedFleet = null;
    scannedCount = 0;
    scannedMembers = Collections.emptySet();
    lastMiss = null;
  }

  private static boolean hasLiveRoll(ShipAPI ship) {
    return ship.getFleetMemberId() != null && Global.getSector() != null
        && Registry.getInstance().getFromRegistry(ship.getFleetMemberId()) != null;
  }

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    if (ship.isStationModule() || !isRealMember(ship)) {
      return false;
    }
    if (hasLiveRoll(ship)) {
      return false;
    }
    for (String hullmod : ship.getVariant().getHullMods()) {
      if (hullmod.startsWith("gacha_hullmods")) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    if (ship.isStationModule() || !isRealMember(ship)) {
      return GachaStrings.get("reason_module");
    }
    if (hasLiveRoll(ship)) {
      return GachaStrings.get("reason_already_rolled");
    }
    return GachaStrings.get("reason_has_hullmod");
  }

}
