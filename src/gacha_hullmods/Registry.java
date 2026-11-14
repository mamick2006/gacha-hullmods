package gacha_hullmods;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Pair;

public class Registry {
  private static final String KEY = "gacha_hullmods_registry";

  public static Registry getInstance() {
    if (!Global.getSector().getPersistentData().containsKey(KEY)) {
      Global.getLogger(Registry.class).info("Needed to make new persistent data for Gacha Hullmods.");
      Global.getSector().getPersistentData().put(KEY, new Registry());
    }

    return (Registry) Global.getSector().getPersistentData().get(KEY);
  }

  public static Registry existing() {
    Object stored = Global.getSector().getPersistentData().get(KEY);
    return stored instanceof Registry ? (Registry) stored : null;
  }

  public static final String KEEP_DAYS_KEY = "gacha_registry_keep_days";
  private static final float DEFAULT_KEEP_DAYS = 365f;

  public static float keepDays() {
    return Math.max(0f, Global.getSettings().optFloat(KEEP_DAYS_KEY, DEFAULT_KEEP_DAYS));
  }

  private Map<String, Pair<String, Long>> registry = new HashMap<>();
  private Map<String, String> secondaryRegistry = new HashMap<>();
  private Map<String, String> rarityRegistry = new HashMap<>();
  private Map<String, Long> missingSince = new HashMap<>();

  public void setRarity(String id, String rarityHullmodId) {
    rarityRegistry.put(id, rarityHullmodId);
  }

  public String getRarity(String id) {
    return rarityRegistry.get(id);
  }

  public void addToRegistry(String id, String hullmodId, long ivs) {
    addToRegistry(id, hullmodId, null, ivs);
  }

  public void addToRegistry(String id, String hullmodId, String secondaryInstruction, long ivs) {
    registry.put(id, new Pair<>(hullmodId, ivs));
    if (secondaryInstruction == null || secondaryInstruction.isEmpty()) {
      secondaryRegistry.remove(id);
    } else {
      secondaryRegistry.put(id, secondaryInstruction);
    }
  }

  public Pair<String, Long> getFromRegistry(String id) {
    return registry.get(id);
  }

  public String getSecondaryFromRegistry(String id) {
    return secondaryRegistry.get(id);
  }

  public boolean setIVs(String id, long ivs) {
    Pair<String, Long> entry = registry.get(id);
    if (entry == null) {
      return false;
    }
    registry.put(id, new Pair<>(entry.one, ivs));
    return true;
  }

  private Map<String, Integer> rollsSinceFiveStar = new HashMap<>();

  public int getRollsSinceFiveStar(String id) {
    Integer rolls = rollsSinceFiveStar.get(id);
    return rolls == null ? 0 : rolls.intValue();
  }

  public void countRoll(String id, boolean fiveStar) {
    if (fiveStar) {
      rollsSinceFiveStar.remove(id);
    } else {
      rollsSinceFiveStar.put(id, getRollsSinceFiveStar(id) + 1);
    }
  }

  public void removeFromRegistry(String id) {
    registry.remove(id);
    secondaryRegistry.remove(id);
    rarityRegistry.remove(id);
    missingSince.remove(id);
  }

  public int pruneShipsGoneTooLong() {
    Set<String> unseen = new HashSet<>(registry.keySet());
    unseen.addAll(secondaryRegistry.keySet());
    unseen.addAll(rarityRegistry.keySet());
    unseen.addAll(rollsSinceFiveStar.keySet());
    if (!unseen.isEmpty()) {
      findShips(unseen);
    }
    missingSince.keySet().retainAll(unseen);
    if (unseen.isEmpty()) {
      return 0;
    }
    CampaignClockAPI clock = Global.getSector().getClock();
    float keepDays = keepDays();
    int dropped = 0;
    for (String id : unseen) {
      Long since = missingSince.get(id);
      if (since == null) {
        missingSince.put(id, clock.getTimestamp());
      } else if (clock.getElapsedDaysSince(since) > keepDays) {
        removeFromRegistry(id);
        rollsSinceFiveStar.remove(id);
        dropped++;
      }
    }
    return dropped;
  }

  public static void seeFleet(CampaignFleetAPI fleet) {
    if (fleet == null || Global.getSector() == null) {
      return;
    }
    Registry registry = existing();
    if (registry == null || registry.missingSince.isEmpty()) {
      return;
    }
    FleetDataAPI data = fleet.getFleetData();
    if (data == null) {
      return;
    }
    for (FleetMemberAPI member : data.getMembersListCopy()) {
      registry.missingSince.remove(member.getId());
    }
  }

  private static void findShips(Set<String> unseen) {
    SectorAPI sector = Global.getSector();
    if (sector.getPlayerFleet() != null && see(unseen, sector.getPlayerFleet().getFleetData())) {
      return;
    }
    for (LocationAPI location : sector.getAllLocations()) {
      for (CampaignFleetAPI fleet : location.getFleets()) {
        if (see(unseen, fleet.getFleetData())) {
          return;
        }
      }
      for (SectorEntityToken entity : location.getAllEntities()) {
        if (see(unseen, entity.getMarket())) {
          return;
        }
      }
    }
    for (MarketAPI market : sector.getEconomy().getMarketsCopy()) {
      if (see(unseen, market)) {
        return;
      }
    }
  }

  private static boolean see(Set<String> unseen, FleetDataAPI data) {
    if (data == null) {
      return unseen.isEmpty();
    }
    List<FleetMemberAPI> members = data.getMembersListCopy();
    for (FleetMemberAPI member : members) {
      unseen.remove(member.getId());
    }
    return unseen.isEmpty();
  }

  private static boolean see(Set<String> unseen, MarketAPI market) {
    if (market == null) {
      return unseen.isEmpty();
    }
    for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
      CargoAPI cargo = submarket.getCargoNullOk();
      if (cargo != null && see(unseen, cargo.getMothballedShips())) {
        return true;
      }
    }
    return unseen.isEmpty();
  }
}
