/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.banner;

import mitll.langtest.client.LangTest;
import mitll.langtest.shared.project.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Emoticon extends com.github.gwtbootstrap.client.ui.Image {
  private static final float NATIVE_HARD_CODE = 0.70F;

  /**
   * 9/13/18 histo from production
   * Did histogram of 85K korean scores, split 6 ways, even 12K piles
   */
  private static final List<Float> koreanThresholds = new ArrayList<>(Arrays.asList(0.31F, 0.43F, 0.53F, 0.61F, NATIVE_HARD_CODE));
  /**
   * Did histogram of 18K english scores, split 6 ways, even 3K piles
   */
  private static final List<Float> englishThresholds = new ArrayList<>(Arrays.asList(0.23F, 0.36F, 0.47F, 0.58F, NATIVE_HARD_CODE));  // technically last is 72

  private static final List<String> emoticons = new ArrayList<>(Arrays.asList(
      "frowningEmoticon.png", // <0.3
      "confusedEmoticon.png", //0.4
      "thinkingEmoticon.png", //0.5
      "neutralEmoticon.png", // 0.6
      "smilingEmoticon.png", // 0.7
      "grinningEmoticon.png"
  ));

  private static final String BEST_EMOTICON = emoticons.get(emoticons.size() - 1);

  public void setEmoticon(double total, Language language) {
    String choice = BEST_EMOTICON;

    List<Float> thresholds = language == Language.KOREAN ? koreanThresholds : englishThresholds;

    for (int i = 0; i < thresholds.size(); i++) {
      if (total < thresholds.get(i)) {
        choice = emoticons.get(i);
        break;
      }
    }

    setUrl(LangTest.LANGTEST_IMAGES + choice);
  }
}
