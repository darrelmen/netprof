/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package audio.image;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Draws a transcript view into a buffer.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 6/24/11
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class TranscriptImage extends AudioImage {
  private static final List<Integer> FONT_SIZES = Arrays.asList(14, 12, 10, 9);
  private static final String FONT_NAME = "NotoSans-Regular, Arial Unicode MS, Arial, sans-serif"; //"Dialog";
  private static final int BKG_ALPHA = 150;

  // don't color these events when coloring
  public static final HashSet<String> IGNORE_TOKENS = new HashSet<>(Arrays.asList(
      "sil",
      "SIL",
      "<s>",
      "</s>",
      "<S>",
      "</S>",
      "[SILENCE]",
      "sp",
      "[+UNK+]",
      "[+unk+]"));

  private Color bkg;                          /// Background color for widget painting
  private static final Color DEFAULT_BKG = new Color(220, 230, 242);


  public TranscriptImage(float framesPerSecond, int x, int y,
                         int displayStart, int displayEnd,
                         Map<Float, TranscriptEvent> events,
                         float scoreScalar,
                         boolean useScoreToColorBkg) {
    this(framesPerSecond, x, y, displayStart, displayEnd, DEFAULT_BKG, RYB_COLOR_MAP, useScoreToColorBkg, events, scoreScalar);
  }

  /**
   * For subclasses
   *
   * @param framesPerSecond
   * @param x
   * @param y
   * @param displayStart
   * @param displayEnd
   * @param bkg
   * @param colormap
   * @param fill
   * @param events
   * @param scoreScalar
   */
  TranscriptImage(float framesPerSecond, int x, int y,
                  int displayStart, int displayEnd,
                  Color bkg,
                  float[][] colormap,
                  boolean fill,
                  Map<Float, TranscriptEvent> events,
                  float scoreScalar) {
    super(framesPerSecond, x, y);
    this.bkg = bkg;

    drawImage(x, y, displayStart, displayEnd, colormap, fill, events, IGNORE_TOKENS, scoreScalar);
  }

  /**
   * Use this color to fill in tiles that need to be padded.
   *
   * @param graphics ignored here
   * @return Color
   */
  @Override
  protected Color getBackgroundColor(Graphics2D graphics) {
    return bkg;
  }

  /**
   * Adjusts font size for available horizontal space.
   * <p>
   * TODO : colormap hard set to red->yellow->green colormap
   *
   * @param xm           image width
   * @param ym           image height
   * @param displayStart window start
   * @param displayEnd   window end
   * @param colormap     to map score to color
   * @param fill         true if want to fill the word box with color
   * @param events       time (seconds) to event map
   * @param ignore       set of event tokens to ignore
   */
  private void drawImage(int xm, int ym, int displayStart, int displayEnd,
                         float[][] colormap,
                         boolean fill, Map<Float, TranscriptEvent> events,
                         Set<String> ignore, float scoreScalar) {
    int x, xe;
    Graphics2D gri = this.createGraphics();
    Color bkg = getBackgroundColor(gri);
    // Background
    gri.setColor(bkg);
    gri.fillRect(0, 0, xm, ym);
    gri.setColor(Color.black);

    List<Font> fonts = getFonts();

    float wStart = toSeconds(displayStart);
    float wEnd = toSeconds(displayEnd);
    // Draw the events
    float denom = getLen(displayStart, displayEnd);
    for (float st : events.keySet()) {
      TranscriptEvent event = events.get(st);
      String str = event.event;
      if (ignore.contains(str)) continue;

      if (event.end < wStart) continue;
      if (st > wEnd) break;

      float a = (st - wStart) / denom, b = (event.end - wStart) / denom;
      x = (int) (a * (float) xm);
      xe = (int) (b * (float) xm);
      int left = Math.max(x, 0);
      int length = Math.min(xm, xe) - left;

      //  Color c = mapScoreToColor(colormap, scoreScalar, event.score);
      Color c = getColor2(Math.min(1.0f, event.score * scoreScalar));

      if (!fill || ignore.contains(str)) {
        gri.setColor(bkg);
      } else {
        gri.setColor(c);
      }

      gri.fillRect(left, 0, length, ym);
//        if (Math.abs(event.end - wEnd) > 0.02) gri.fillRect(x, 0, xe - x, ym);
//        else gri.fillRect(x, 0, xm - x, ym);
      gri.setColor(Color.black);
      if (x > 0) gri.drawLine(x, 0, x, ym);
      if (Math.abs(event.end - wEnd) > 0.02) gri.drawLine(xe, 0, xe, ym);

      drawLabel(ym, gri, fonts, str, left, length);
    }
  }


  private List<Font> getFonts() {
    List<Font> fonts = new ArrayList<Font>();
    for (int i : FONT_SIZES) {
      fonts.add(new Font(FONT_NAME, 0, i));
    }
    return fonts;
  }

/*  public static Color getColorForProbability(float score) {
    return mapScoreToColor(RYB_COLOR_MAP,1.0f,score);
  }*/

  /**
   * Choose a color based on score - map a score in the range [0..1] to an index of a color in
   * the color map [1..n].
   * TODO : this is buggy -- compare to getColor2
   * @see #drawImage(int, int, int, int, float[][], boolean, java.util.Map, java.util.Set, float)
   * @param colormap
   * @param scoreScalar scale the score before we choose a color
   * @param score
   * @return Color for the score
   */
/*  private static Color mapScoreToColor(float[][] colormap, float scoreScalar, float score) {
    float eventScore = Math.min(1.0f, score *scoreScalar);
    float nf = Math.max(eventScore, 0.0f) * (float) (colormap.length - 2);
    int idx = (int) Math.floor(nf);
    int[] color = {0, 0, 0};
    for (int cc = 0; cc < 3; cc++)
      color[cc] = Math.round((colormap[idx + 1][cc] - colormap[idx][cc]) * (nf - (float) idx) + colormap[idx][cc]);
    //System.err.println(String.format("INFO: input = %f, color = %d %d %d, nf == %f", event.score, color[0], color[1], color[2], nf));
    return new Color(color[0], color[1], color[2], BKG_ALPHA);
  }*/

  /**
   * This gives a smooth range red->yellow->green:
   * on green 0->255 over score 0->0.5, 255 for > 0.5 and
   * on red from 255->0 over 0.5->1 in score, 255 for < 0.5
   *
   * @param score
   * @return
   */
  private static Color getColor2(float score) {
    if (score > 1.0) score = 1.0f;
    if (score < 0f) score = 0f;
    int red = (int) Math.max(0, (255f - (Math.max(0, score - 0.5) * 2f * 255f)));
    int green = (int) Math.min(255f, score * 2f * 255f);
    int blue = 0;
    //return "#" + getHexNumber(red) + getHexNumber(green) + getHexNumber(blue);
    return new Color(red, green, blue, BKG_ALPHA);
  }

  private static String getHexNumber(int number) {
    String hexString = Integer.toHexString(number).toUpperCase();

    if (hexString.length() == 0) {
      return "00";
    } else if (hexString.length() == 1) {
      return "0" + hexString;
    } else {
      return hexString;
    }
  }

  /**
   * iterate through the fonts trying to find one that fits in the horizontal space provided
   *
   * @param ym
   * @param gri
   * @param fonts
   * @param str
   * @param left
   * @param length
   */
  private void drawLabel(int ym, Graphics2D gri, List<Font> fonts, String str, int left, int length) {
    for (Font f : fonts) {
      gri.setFont(f);
      FontMetrics metrics = gri.getFontMetrics(gri.getFont());
      int stringWidthInFont = metrics.stringWidth(str);
      //  System.out.println("len " + length + " vs " + stringWidthInFont + " font " + f+ " for " + str);
      if (stringWidthInFont < length) {//((events.get(st).end - st) / denom) * xm)
        //   System.out.println("\tusing len " + length + " vs " + stringWidthInFont + " font " + f+ " for " + str);

        // centers the text
        int center = (left + (left + length)) / 2 - stringWidthInFont / 2;
        gri.drawString(str, center, ym - 5);
        break;
      }
    }
  }

  @Override
  public IMAGE_FORMAT getFormat() {
    return IMAGE_FORMAT.PNG;
  }

  @Override
  public ImageType getName() {
    return ImageType.WORD_TRANSCRIPT;
  }

  private static void generateColors(int numColors) {
    float numColorsF = (float) numColors;
    float step = 1f / numColorsF;
    for (float f = 0; f <= 1.01; f += step) {
      Color color2 = getColor2(f);
      //   Color colorForProbability = transcriptImage.getColorForProbability(f);
      System.out.println("f " + f + " " + color2 + " " +
          "#" + getHexNumber(color2.getRed()) + getHexNumber(color2.getGreen()) + getHexNumber(color2.getBlue()));
    }
  }

  public static void main(String[] arg) {
    generateColors(19);
  }

}
