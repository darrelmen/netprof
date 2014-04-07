package mitll.langtest.server.scoring;

import corpus.ArabicLTS;
import corpus.DariLTS;
import corpus.EnglishLTS;
import corpus.FarsiLTS;
import corpus.LTS;
import corpus.LevantineLTS;
import corpus.ModernStandardArabicLTS;
import corpus.PashtoLTS;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/1/13
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class LTSFactory {
  private static final Logger logger = Logger.getLogger(LTSFactory.class);

  public LTS getLTSClass(String language) {
    LTS letterToSoundClass = language != null && language.equals("English") ? new EnglishLTS() : new ArabicLTS();

    if (language == null) {
      letterToSoundClass = new ArabicLTS();
      logger.warn("using default arabic LTS -- this may not be what you want -- set the language property.");
    } else if (language.equalsIgnoreCase("English")) {
      letterToSoundClass = new EnglishLTS();
    } else if (language.equalsIgnoreCase("Arabic")) {
      letterToSoundClass = new ArabicLTS();
    } else if (language.equalsIgnoreCase("Dari")) {
      letterToSoundClass = new DariLTS();
    } else if (language.equalsIgnoreCase("Pashto")) {
      letterToSoundClass = new PashtoLTS();
    } else if (language.equalsIgnoreCase("MSA")) {
      letterToSoundClass = new ModernStandardArabicLTS();
    } else if (language.equalsIgnoreCase("Levantine")) {
      letterToSoundClass = new LevantineLTS();
    } else if (language.equalsIgnoreCase("Farsi")) {
      letterToSoundClass = new FarsiLTS();  // TODO is there a FarsiLTS class???
    } else if (language.equalsIgnoreCase("Mandarin")) {
      //ara logger.info("NOTE: there is no LTS for " + language + " : that's OK though.");
    } else {
      logger.warn("NOTE: we have no LTS for '" + language +"', using " + letterToSoundClass.getClass());
    }
    return letterToSoundClass;
  }

}
