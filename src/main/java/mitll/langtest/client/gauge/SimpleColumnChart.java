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

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/21/2014.
 */
public class SimpleColumnChart {
  public static String MAX = "#EEF3E2";//getColor(1.0f);

  /**
   * This gives a smooth range red->yellow->green:
   *  on green 0->255 over score 0->0.5, 255 for > 0.5 and
   *  on red from 255->0 over 0.5->1 in score, 255 for < 0.5
   *
   *  NOTE : this is the same as in audio.image.TranscriptImage
   * @param score
   * @return color in # hex rgb format
   */
  public static String getColor(float score) {
    if (score > 1.0) score = 1.0f;
    if (score < 0f)  score = 0f;
    int red   = (int)Math.max(0,(255f - (Math.max(0, score-0.5)*2f*255f)));
    int green = (int)Math.min(255f, score*2f*255f);
    int blue  = 0;
    // System.out.println("s " +score + " red " + red + " green " + green + " b " +blue);
    return "#" + getHexNumber(red) + getHexNumber(green) + getHexNumber(blue);
    //return new Color(red, green, blue, BKG_ALPHA);
  }

  private static String getHexNumber(int number){
    String hexString = Integer.toHexString(number).toUpperCase();

    if(hexString.length() == 0){
      return "00";
    }
    else if(hexString.length() == 1){
      return "0" + hexString;
    }
    else{
      return hexString;
    }
  }
}
