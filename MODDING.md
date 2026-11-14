# Adding Instructions from another mod

Gacha Hullmods reads the set of possible effects from `data/config/gacha/gacha_instructions.csv`. The game
merges that path across every enabled mod before any script runs, so a mod shipping the same path adds
its rows to Gacha's. The main thing this does is allow you to add new instructions that can be rolled by the mod.

For a more complete example of how to do this, see `https://github.com/mamick2006/crossmod-demo`.

## Adding an Instruction

Add to `gacha_instructions.csv` something like:

```
id,tier,plugin,group,opCost,rpMult,name,desc,effect,template,needs
mymod_ablative_wash,4,mymod.instructions.AblativeWash,STANDARD,8,6,Ablative Wash,Sacrificial plating that boils off under fire.,Increases armor by %s.,,
```

And make the corresponding AblativeWash class like:

```java
import liminal.instructions.Instruction;
import liminal.instructions.InstructionSpec;

public class AblativeWash extends Instruction {

  private static final float ARMOR_BONUS = 0.15f;

  public AblativeWash(InstructionSpec spec) {
    super(spec);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getArmorBonus().modifyMult(getName(), 1f + ARMOR_BONUS);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(), tP(ARMOR_BONUS));
  }
}
```

Those two overrides are `Instruction`'s only abstract methods. There are six optional hooks:
`applyEffectsAfterShipCreation`, `applyEffectsToFighterSpawnedByShip`, `advanceInCombat`,
`affectsOPCosts`, `isIncompatible`, and the per-hull-size overload of `getEffectDescription`. Three run
only when the matching `has...` method returns true, so override `hasAdvanceInCombat` when you override
`advanceInCombat`, and the same for the other two.

## The columns

| Column | What it does |
|---|---|
| `id` | The Instruction's name. |
| `tier` | Power tier, 1 to 5. If you have rolled an X star hullmod, you will get a corresponding X star effect. |
| `plugin` | The class where the instruction is implemented. |
| `group` | `STANDARD`, `NEGATIVE`, `LOGISTIC` or `SPECIAL`. You can probably actually make effects not scale with level at all, since they're only using for effects in gacha hullmod, so you can ignore this. |
| `opCost` | Ordnance points it takes in a Liminal template. Ignored on a gacha roll. |
| `rpMult` | Research points to unlock at level 0, and the scale for each level after. Ignored on a gacha roll. |
| `name` | The display name. |
| `desc` | Flavour text, shown in Liminal's research list. |
| `effect` | The effect description. `effectTemplate()` returns it, and each `%s` is filled by the arguments you pass `addPara`. |
| `template` | `true` puts the Instruction in Liminal's template roster instead of the gacha pool. Anything else leaves it in the gacha pool. |
| `needs` | `BAYS`, `MISSILES` or `SHIELD` stops the Instruction from being rolled on a hull with no fighter bays, no missile mounts or no shield. Leave it empty if any ship can use the effect. |
