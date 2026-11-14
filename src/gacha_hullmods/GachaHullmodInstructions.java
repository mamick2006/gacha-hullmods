package gacha_hullmods;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

import liminal.instructions.Instruction;
import liminal.instructions.Instruction.Group;
import liminal.instructions.InstructionSpec;
import liminal.instructions.Instructions;

public class GachaHullmodInstructions {

  public static final String TABLE_PATH = "data/config/gacha/gacha_instructions.csv";
  private static final String MASTER_MOD = "gacha_hullmods";

  private static Map<String, Instruction> INSTRUCTION_MAP = null;
  private static SortedMap<Group, List<Instruction>> GROUP_INSTRUCTION_MAP = null;
  private static SortedMap<Integer, List<Instruction>> TIER_INSTRUCTION_MAP = null;
  private static Map<String, Integer> POWER_TIERS = null;
  private static Map<String, Need> NEEDS = null;
  private static List<Instruction> templateArrivals = null;

  public static final int MIN_TIER = 1;
  public static final int MAX_TIER = 5;
  private static final int DEFAULT_TIER = 3;

  public enum Need {
    BAYS,
    MISSILES,
    SHIELD
  }

  public static void ensureLoaded() {
    if (INSTRUCTION_MAP != null) {
      return;
    }
    try {
      init();
    } catch (RuntimeException e) {
      INSTRUCTION_MAP = null;
      POWER_TIERS = null;
      NEEDS = null;
      GROUP_INSTRUCTION_MAP = null;
      TIER_INSTRUCTION_MAP = null;
      templateArrivals = null;
      throw e;
    }
  }

  private static void init() {
    INSTRUCTION_MAP = new LinkedHashMap<String, Instruction>(Instructions.GACHA_INSTRUCTION_MAP);
    POWER_TIERS = new HashMap<String, Integer>();
    NEEDS = new HashMap<String, Need>();
    templateArrivals = new ArrayList<Instruction>();
    loadTable();
    Instructions.setTemplateInstructions(templateArrivals);
    GROUP_INSTRUCTION_MAP = new TreeMap<Group, List<Instruction>>();
    TIER_INSTRUCTION_MAP = new TreeMap<Integer, List<Instruction>>();
    for (Instruction instruction : INSTRUCTION_MAP.values()) {
      Group group = instruction.getGroup();
      if (!GROUP_INSTRUCTION_MAP.containsKey(group)) {
        GROUP_INSTRUCTION_MAP.put(group, new ArrayList<Instruction>());
      }
      GROUP_INSTRUCTION_MAP.get(group).add(instruction);

      int tier = getPowerTier(instruction.getName());
      if (!TIER_INSTRUCTION_MAP.containsKey(tier)) {
        TIER_INSTRUCTION_MAP.put(tier, new ArrayList<Instruction>());
      }
      TIER_INSTRUCTION_MAP.get(tier).add(instruction);
    }
  }

  private static void loadTable() {
    JSONArray table;
    try {
      table = Global.getSettings().getMergedSpreadsheetDataForMod("id", TABLE_PATH, MASTER_MOD);
    } catch (Exception e) {
      Global.getLogger(GachaHullmodInstructions.class).error("Gacha Hullmods: could not read "
          + TABLE_PATH + ", so every instruction rolls at tier " + DEFAULT_TIER + " and no other"
          + " mod's instructions are available.", e);
      return;
    }
    for (int i = 0; i < table.length(); i++) {
      try {
        readRow(table.getJSONObject(i));
      } catch (Exception e) {
        Global.getLogger(GachaHullmodInstructions.class).warn("Gacha Hullmods: skipping row " + i
            + " of " + TABLE_PATH + "; the instruction it names cannot roll.", e);
      }
    }
  }

  private static void readRow(JSONObject row) throws Exception {
    String id = row.optString("id", "").trim();
    if (id.isEmpty() || id.startsWith("#")) {
      return;
    }
    int tier = Math.max(MIN_TIER, Math.min(MAX_TIER, row.optInt("tier", DEFAULT_TIER)));
    POWER_TIERS.put(id, tier);
    Need need = needOf(row, id);
    if (need != null) {
      NEEDS.put(id, need);
    }
    String pluginClass = row.optString("plugin", "").trim();
    if (pluginClass.isEmpty()) {
      return;
    }
    InstructionSpec spec = specOf(row, id, tier);
    Instruction instruction = instantiate(pluginClass, spec);
    if (instruction == null) {
      return;
    }
    if (spec.template) {
      templateArrivals.add(instruction);
      return;
    }
    INSTRUCTION_MAP.put(instruction.getName(), instruction);
  }

  private static InstructionSpec specOf(JSONObject row, String id, int tier) {
    return new InstructionSpec(id, groupOf(row, id), row.optInt("opCost", 0),
        Math.max(1, row.optInt("rpMult", 1)), tier,
        nonEmpty(row.optString("name", ""), id), row.optString("desc", ""),
        row.optString("effect", ""), "true".equalsIgnoreCase(row.optString("template", "").trim()),
        row.optString("fs_rowSource", "an unidentified mod"));
  }

  private static Group groupOf(JSONObject row, String id) {
    String group = row.optString("group", "").trim();
    if (group.isEmpty()) {
      return Group.STANDARD;
    }
    for (Group candidate : Group.values()) {
      if (candidate.name().equalsIgnoreCase(group)) {
        return candidate;
      }
    }
    Global.getLogger(GachaHullmodInstructions.class).warn("Gacha Hullmods: instruction '" + id
        + "' asks for group '" + group + "', which is not one of STANDARD, NEGATIVE, LOGISTIC or"
        + " SPECIAL; it rolls as STANDARD.");
    return Group.STANDARD;
  }

  private static Need needOf(JSONObject row, String id) {
    String need = row.optString("needs", "").trim();
    if (need.isEmpty()) {
      return null;
    }
    for (Need candidate : Need.values()) {
      if (candidate.name().equalsIgnoreCase(need)) {
        return candidate;
      }
    }
    Global.getLogger(GachaHullmodInstructions.class).warn("Gacha Hullmods: instruction '" + id
        + "' needs '" + need + "', which is not one of BAYS, MISSILES or SHIELD; it rolls on any"
        + " hull.");
    return null;
  }

  private static String nonEmpty(String value, String fallback) {
    return value.trim().isEmpty() ? fallback : value;
  }

  private static Instruction instantiate(String pluginClass, InstructionSpec spec) {
    Instruction instruction;
    try {
      Class<?> cls = Global.getSettings().getScriptClassLoader().loadClass(pluginClass);
      instruction = (Instruction) MethodHandles.publicLookup()
          .findConstructor(cls, MethodType.methodType(void.class, InstructionSpec.class))
          .invoke(spec);
    } catch (Throwable t) {
      Global.getLogger(GachaHullmodInstructions.class).warn("Gacha Hullmods: " + spec.rowSource
          + " lists instruction '" + spec.id + "' with plugin " + pluginClass + ", which did not"
          + " build, so that instruction never rolls. It needs to extend"
          + " liminal.instructions.Instruction with one public constructor taking a single"
          + " liminal.instructions.InstructionSpec.", t);
      return null;
    }
    if (!spec.id.equals(instruction.getName())) {
      Global.getLogger(GachaHullmodInstructions.class).warn("Gacha Hullmods: " + spec.rowSource
          + " lists instruction '" + spec.id + "' whose plugin " + pluginClass + " calls itself '"
          + instruction.getName() + "' instead, so that instruction never rolls. A roll persists the"
          + " name, and the two have to match for it to resolve again on the next load.");
      return null;
    }
    return instruction;
  }

  public static int getPowerTier(String name) {
    if (POWER_TIERS == null) {
      ensureLoaded();
    }
    String baseName = name.endsWith("_gacha") ? name.substring(0, name.length() - "_gacha".length()) : name;
    Integer tier = POWER_TIERS.get(baseName);
    return tier != null ? tier : DEFAULT_TIER;
  }

  public static Need getNeed(String name) {
    if (NEEDS == null) {
      ensureLoaded();
    }
    String baseName = name.endsWith("_gacha") ? name.substring(0, name.length() - "_gacha".length()) : name;
    return NEEDS.get(baseName);
  }

  public static Set<Need> lackedBy(ShipHullSpecAPI hull) {
    Set<Need> lacked = EnumSet.noneOf(Need.class);
    if (hull.getFighterBays() <= 0) {
      lacked.add(Need.BAYS);
    }
    if (!mountsMissiles(hull)) {
      lacked.add(Need.MISSILES);
    }
    ShieldType shield = hull.getShieldType();
    if (shield == null || shield == ShieldType.NONE || shield == ShieldType.PHASE) {
      lacked.add(Need.SHIELD);
    }
    return lacked;
  }

  private static boolean mountsMissiles(ShipHullSpecAPI hull) {
    for (WeaponSlotAPI slot : hull.getAllWeaponSlotsCopy()) {
      WeaponType type = slot.getWeaponType();
      if (type == WeaponType.MISSILE || type == WeaponType.COMPOSITE || type == WeaponType.SYNERGY
          || type == WeaponType.UNIVERSAL) {
        return true;
      }
    }
    for (String weaponId : hull.getBuiltInWeapons().values()) {
      if (Global.getSettings().getWeaponSpec(weaponId).getType() == WeaponType.MISSILE) {
        return true;
      }
    }
    return false;
  }

  public static List<Instruction> getInstructionsInGroup(Group group) {
    if (GROUP_INSTRUCTION_MAP == null) {
      ensureLoaded();
    }
    return GROUP_INSTRUCTION_MAP.get(group);
  }

  public static List<Instruction> getInstructionsInTier(int tier) {
    if (TIER_INSTRUCTION_MAP == null) {
      ensureLoaded();
    }
    return TIER_INSTRUCTION_MAP.get(tier);
  }

  public static Instruction getInstruction(String name) {
    if (name.equals("")) {
      return null;
    }
    if (INSTRUCTION_MAP == null) {
      ensureLoaded();
    }
    return INSTRUCTION_MAP.get(name);
  }

  public static List<Instruction> getAllInstructions() {
    if (INSTRUCTION_MAP == null) {
      ensureLoaded();
    }
    return new ArrayList<Instruction>(INSTRUCTION_MAP.values());
  }

  public static boolean isFromAnotherMod(Instruction instruction) {
    return instruction.getSpec() != null;
  }

  public static List<Instruction> getOwnInstructions() {
    List<Instruction> own = new ArrayList<Instruction>();
    for (Instruction instruction : getAllInstructions()) {
      if (!isFromAnotherMod(instruction)) {
        own.add(instruction);
      }
    }
    return own;
  }

  public static Map<String, Integer> getTableTiers() {
    if (POWER_TIERS == null) {
      ensureLoaded();
    }
    return Collections.unmodifiableMap(POWER_TIERS);
  }

  public static final long MAX_IV = 30;
  public static final long MIN_IV = -30;

  public static int[] decodeIVs(long longIVs) {
    int[] ivs = new int[6];
    long[] multipliers = { 1, 100, 10000, 1000000, 100000000, 10000000000L };

    for (int i = 0; i < 6; i++) {
      long value = (longIVs / multipliers[i]) % 100L;
      ivs[i] = (int) value - (int) (MAX_IV + 1);
    }

    return ivs;
  }

  public static long encodeIVs(int[] ivs) {
    if (ivs.length != 6) {
      throw new IllegalArgumentException("IVs array must contain exactly 6 values");
    }

    long[] multipliers = { 1, 100, 10000, 1000000, 100000000, 10000000000L };
    long result = 0;

    for (int i = 0; i < 6; i++) {
      long adjustedValue = ivs[i] + (MAX_IV + 1);
      result += adjustedValue * multipliers[i];
    }

    return result;
  }
}
