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

package mitll.langtest.client.gauge;

import java.util.logging.Logger;

/**
 * Worris about black legibility on colored background.
 * Consistent with google's choice of black lettering vs white in
 * <a href='https://www.google.com/search?q=color+picker&oq=color+&aqs=chrome.1.69i57j0l5.2190j0j7&sourceid=chrome&ie=UTF-8'>google color picker</a>
 *  Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/21/2014.
 */
public class SimpleColumnChart {
  //  private static final Logger logger = Logger.getLogger("SimpleColumnChart");

  private static final float MAXV = 255f;
  public static final double BWC = 0.0722d;
  public static final double GWC = 0.7152d;
  private static final int MIN_GREEN = 50; // higher more plum
  private static final int MINGREENRGB = 113;
  private static final int MAXBLUE = 161;
  public static String MAX = "#EEF3E2";

  /*private static int[] getGreenBlue(int r, int g) {
    double rlum = lum((double) r);
    double glum = lum((double) g);

    logger.info("rlm " + rlum);
    logger.info("glm " + glum);
    double[] lum = getLum(rlum, glum);
    int[] gb = new int[2];
    gb[0] = toInt(lum[0]);
    gb[1] = toInt(lum[1]);
    return gb;
  }

  private static int toInt(double v) {
    return Long.valueOf(Math.round(v * 255d)).intValue();
  }

  // 113 green black
  private static double[] getLum(double r, double g) {
    double rw = r * 0.2126d;
    double gw = g * GWC;

    //  double bw = b* BWC;

    double target = 0.179d;

    target = target - rw - gw;
    logger.info("target " + target);

    if (target < 0) {
      target *= -1;
      double candidateBlue = target / BWC;

      logger.info("candidateBlue " + candidateBlue);

      double candidateGreen = g;


      if (candidateBlue > 1) {
        target = 0.179d - rw - BWC;
        candidateGreen = target / GWC;
      }

      double[] doubles = new double[2];
      doubles[0] = candidateGreen;
      doubles[1] = candidateBlue;
      return doubles;
    } else {
      double[] doubles = new double[2];
      doubles[0] = g;
      doubles[1] = 0;
      return doubles;
    }
    //residual > 0
  }

  private static double lum(double c) {
    c /= 255d;
    if (c <= 0.03928d) {
      c /= 12.92d;
    } else {
      c = Math.pow((c + 0.055d) / 1.055d, 2.4d);
    }
    return c;
  }
*/
  /**
   * if L > 0.179 use #000000 else use #ffffff
   * for each c in r,g,b:
   *    c = c / 255.0
   * if c <= 0.03928 then c = c/12.92 else c = ((c+0.055)/1.055) ^ 2.4
   * L = 0.2126 * r + 0.7152 * g + 0.0722 * b
   */

  /**
   * This gives a smooth range red->yellow->green:
   * <p>
   * OLD on red   255->0 over 0.5->1, 255 for < 0.5
   * OLD on green 0->255 over 0->0.5, 255 for > 0.5
   * <p>
   * on red   255->0 over 0.5->1, 255 for < 0.5
   * on green 50->255 over 0->0.5, 255 for > 0.5
   * add in blue at 1.4x green when green below 113
   * <p>
   * <p>
   * NOTE : this is the same as in mitll.langtest.server.audio.image.TranscriptImage
   *
   * @param score 0-1
   * @return color in # hex rgb format
   */
  public static String getColor(float score) {
    if (score > 1.0) score = 1.0f;
    if (score < 0f) score = 0f;
    int red = (int) Math.max(0, (MAXV - (Math.max(0, score - 0.5) * 2f * MAXV)));
    int green = (int) Math.max(MIN_GREEN, Math.min(MAXV, score * 2f * MAXV));
   // logger.warning("getColor score " + score + " red " + red + " green " + green + " b ?");
    int blue = (green < MINGREENRGB) ? MAXBLUE - Math.round((float) green * 1.4f) : 0; //greenBlue[1];
    String s = "#" + getHexNumber(red) + getHexNumber(green) + getHexNumber(blue);
  //  logger.warning("getColor score " + score + " red " + red + " green " + green + " b " + blue + " " + s);
    return s;
    //return new Color(red, green, blue, BKG_ALPHA);
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

//  public static void main(String[] arg) {
//    float s = 0.408f;
//    for (float f = 0f; f < 1.0f; f += 0.05)
//      getColor(f);
//  }
}
