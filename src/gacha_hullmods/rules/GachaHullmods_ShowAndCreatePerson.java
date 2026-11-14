package gacha_hullmods.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

public class GachaHullmods_ShowAndCreatePerson extends BaseCommandPlugin {
  public static final String CHARACTER = "gacha_hullmods_character";

  public static PersonAPI getOrCreatePerson(String personId) {
    ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
    PersonAPI person = ip.getPerson(personId);
    if (person == null) {
      if (personId.equals(CHARACTER)) {
        person = Global.getFactory().createPerson();
        person.setId(CHARACTER);
        person.setImportance(PersonImportance.VERY_LOW);
        person.setVoice(Voices.PATHER);
        person.setFaction(Factions.INDEPENDENT);
        person.setGender(FullName.Gender.MALE);
        person.setRankId(Ranks.CITIZEN);
        person.setPostId(Ranks.DEPUTY_STAR_MARSHAL);
        person.getName().setFirst("Strange");
        person.getName().setLast("Man");
        person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "gacha_hullmods_character"));
        ip.addPerson(person);
      } else {
        throw new RuntimeException("Unknown person: " + personId);
      }
    }
    return person;
  }

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    String personId = params.get(0).getString(memoryMap);
    PersonAPI person = getOrCreatePerson(personId);
    dialog.getVisualPanel().showPersonInfo(person, true);
    return true;
  }
}
