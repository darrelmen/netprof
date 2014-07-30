package mitll.langtest.server.scoring;

import corpus.DariLTS;
import corpus.EnglishLTS;
import corpus.FarsiLTS;
import corpus.LTS;
import corpus.LevantineLTS;
import corpus.ModernStandardArabicLTS;
import corpus.PashtoLTS;
import corpus.UrduLTS;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/1/13
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class LTSFactory {
  private static final Logger logger = Logger.getLogger(LTSFactory.class);

  // known languages
  private static final String ARABIC = "Arabic";
  private static final String DARI = "Dari";
  private static final String EGYPTIAN = "Egyptian";
  private static final String ENGLISH = "English";
  private static final String FARSI = "Farsi";
  private static final String LEVANTINE = "Levantine";
  private static final String MANDARIN = "Mandarin";
  private static final String MSA = "MSA";
  private static final String PASHTO = "Pashto";
  private static final String URDU = "Urdu";
  // TODO : what about Japanese, Korean, ...?

  private static final Map<String,LTS> languageToLTS = new HashMap<String,LTS>();
  private static final LTS unknown = new LTS() {
    @Override
    public String[][] process(String word) {
      return new String[0][];
    }
  };

  public LTSFactory() {
    languageToLTS.put(ARABIC.toLowerCase(), new ModernStandardArabicLTS());
    languageToLTS.put(DARI.toLowerCase(), new DariLTS());
    languageToLTS.put(EGYPTIAN.toLowerCase(), new ModernStandardArabicLTS());
    languageToLTS.put(ENGLISH.toLowerCase(),new EnglishLTS());
    languageToLTS.put(FARSI.toLowerCase(), new FarsiLTS());
    languageToLTS.put(LEVANTINE.toLowerCase(), new LevantineLTS());
    languageToLTS.put(MANDARIN.toLowerCase(), unknown);
    languageToLTS.put(MSA.toLowerCase(), new ModernStandardArabicLTS());
    languageToLTS.put(PASHTO.toLowerCase(), new PashtoLTS());
    languageToLTS.put(URDU.toLowerCase(), new UrduLTS());
  }

  /**
   * @see mitll.langtest.server.scoring.ASRScoring#ASRScoring(String, java.util.Map, corpus.HTKDictionary)
   * @param language
   * @return
   */
  public LTS getLTSClass(String language) {
    LTS letterToSoundClass = languageToLTS.get(language.toLowerCase());

    if (letterToSoundClass == null) {
      logger.warn("NOTE: we have no LTS for '" + language + "', using the empty LTS class : " + unknown.getClass());
      letterToSoundClass = unknown;
    }
    return letterToSoundClass;
  }
}
