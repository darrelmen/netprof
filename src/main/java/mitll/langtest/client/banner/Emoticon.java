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
