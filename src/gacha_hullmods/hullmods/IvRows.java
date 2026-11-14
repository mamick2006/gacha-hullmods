package gacha_hullmods.hullmods;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import gacha_hullmods.GachaHullmodInstructions;
import gacha_hullmods.GachaStrings;

final class IvRows extends BaseCustomUIPanelPlugin {

  static final boolean BARS = true;

  private static String[] labels() {
    return new String[] { GachaStrings.get("iv_hp"), GachaStrings.get("iv_atk"), GachaStrings.get("iv_def"),
        GachaStrings.get("iv_sp_atk"), GachaStrings.get("iv_sp_def"), GachaStrings.get("iv_spd") };
  }

  private static final float ROW_H = 17f;
  private static final float VALUE_W = 44f;
  private static final float VALUE_GAP = 10f;
  private static final float LABEL_GAP = 4f;
  private static final float INSET = 5f;
  private static final float MARGIN = 1f;
  private static final float TEXT_PAD = 1f;
  private static final float TRACK_H = 8f;
  private static final float TRACK_TOP = 5f;
  private static final float TICK_H = 12f;
  private static final Color TRACK = new Color(16, 38, 46);

  private static final Color[] FIFTHS = { new Color(68, 206, 27, 255), new Color(187, 219, 68, 255),
      new Color(247, 227, 121, 255), new Color(242, 161, 52, 255), new Color(229, 31, 31, 255) };

  static Color colour(int iv) {
    int min = (int) GachaHullmodInstructions.MIN_IV;
    int fifth = (int) (GachaHullmodInstructions.MAX_IV - GachaHullmodInstructions.MIN_IV + 1) / 5;
    for (int i = 0; i < FIFTHS.length - 1; i++) {
      if (iv >= min + fifth * (FIFTHS.length - 1 - i)) {
        return FIFTHS[i];
      }
    }
    return FIFTHS[FIFTHS.length - 1];
  }

  static void add(TooltipMakerAPI tooltip, float width, int[] ivs, float pad) {
    String[] labels = labels();
    int n = Math.min(ivs.length, labels.length);
    int max = (int) GachaHullmodInstructions.MAX_IV;
    String[] values = new String[n];
    Color[] colours = new Color[n];
    float longestLabel = 0f;
    for (int i = 0; i < n; i++) {
      values[i] = IvBar.signed(ivs[i]);
      colours[i] = colour(ivs[i]);
      longestLabel = Math.max(longestLabel, tooltip.computeStringWidth(labels[i]));
    }
    float widestValue = Math.max(tooltip.computeStringWidth(IvBar.signed(max)),
        tooltip.computeStringWidth(IvBar.signed((int) GachaHullmodInstructions.MIN_IV)));
    float valueRight = width - IvBar.TRACK - VALUE_GAP;
    float valueW = Math.max(VALUE_W, widestValue + MARGIN);
    float labelW = longestLabel + MARGIN;
    boolean fits = longestLabel + LABEL_GAP <= valueRight - widestValue;
    if (!BARS || !fits) {
      tooltip.beginGridFlipped(width, 1, VALUE_W, VALUE_GAP);
      for (int i = 0; i < n; i++) {
        tooltip.addToGrid(0, i, labels[i], values[i], colours[i]);
      }
      tooltip.addGrid(pad);
      return;
    }
    IvRows bars = new IvRows(n, colours);
    CustomPanelAPI panel = Global.getSettings().createCustom(width, n * ROW_H, bars);
    for (int i = 0; i < n; i++) {
      bars.left[i] = IvBar.left(ivs[i], max);
      bars.length[i] = IvBar.length(ivs[i], max);
      text(panel, 0f, i * ROW_H, labelW, labels[i], Misc.getTextColor(), Alignment.LMID);
      text(panel, valueRight - valueW, i * ROW_H, valueW, values[i], colours[i], Alignment.RMID);
    }
    tooltip.addCustom(panel, pad);
  }

  private static void text(CustomPanelAPI panel, float boxX, float y, float boxW, String text, Color c,
      Alignment align) {
    TooltipMakerAPI el = panel.createUIElement(boxW + 2f * INSET, ROW_H, false);
    el.addPara(text, c, TEXT_PAD).setAlignment(align);
    panel.addUIElement(el).inTL(boxX - INSET, y);
  }

  private final int[] left;
  private final int[] length;
  private final Color[] colours;
  private final Color tick = Misc.getBasePlayerColor();
  private PositionAPI pos;

  private IvRows(int n, Color[] colours) {
    this.left = new int[n];
    this.length = new int[n];
    this.colours = colours;
  }

  @Override
  public void positionChanged(PositionAPI position) {
    this.pos = position;
  }

  @Override
  public void renderBelow(float alphaMult) {
    if (pos == null) {
      return;
    }
    float trackX = Math.round(pos.getX() + pos.getWidth()) - IvBar.TRACK;
    float top = Math.round(pos.getY() + pos.getHeight());
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glBegin(GL11.GL_QUADS);
    for (int i = 0; i < left.length; i++) {
      float y = top - i * ROW_H - TRACK_TOP - TRACK_H;
      quad(trackX, y, IvBar.TRACK, TRACK_H, TRACK, alphaMult);
      if (length[i] > 0) {
        quad(trackX + left[i], y, length[i], TRACK_H, colours[i], alphaMult);
      }
      quad(trackX + IvBar.TICK, y - (TICK_H - TRACK_H) / 2f, 1f, TICK_H, tick, alphaMult);
    }
    GL11.glEnd();
    GL11.glPopAttrib();
  }

  private static void quad(float x, float y, float w, float h, Color c, float alphaMult) {
    GL11.glColor4ub((byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue(),
        (byte) (c.getAlpha() * alphaMult));
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x + w, y);
  }
}
