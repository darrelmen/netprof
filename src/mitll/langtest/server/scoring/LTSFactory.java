package mitll.langtest.server.scoring;

import corpus.*;
import mitll.langtest.shared.CommonExercise;
import org.apache.log4j.Logger;

import java.text.CollationKey;
import java.text.Collator;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/1/13
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class LTSFactory implements CollationSort {
  private static final Logger logger = Logger.getLogger(LTSFactory.class);

  private final Language thisLanguage;

  // known languages
  public enum Language {
    ARABIC, DARI, EGYPTIAN, ENGLISH, FARSI, JAPANESE, LEVANTINE, KOREAN, MANDARIN, MSA, PASHTO, RUSSIAN, SPANISH, SUDANESE, TAGALOG, URDU
  }
  // TODO : what about Japanese, Korean, ... for LTS?

  private final Map<String, LTS> languageToLTS = new HashMap<String, LTS>();
  private final LTS unknown = new EmptyLTS();

  /**
   * @param thisLanguage
   * @see ASRScoring#ASRScoring
   */
  private LTSFactory(Language thisLanguage) {
    this.thisLanguage = thisLanguage;
    languageToLTS.put(Language.ARABIC.name().toLowerCase(), new ModernStandardArabicLTS());
    languageToLTS.put(Language.DARI.name().toLowerCase(), new DariLTS());
    languageToLTS.put(Language.EGYPTIAN.name().toLowerCase(), new EgyptianLTS());
    languageToLTS.put(Language.ENGLISH.name().toLowerCase(), new EnglishLTS());
    languageToLTS.put(Language.FARSI.name().toLowerCase(), new FarsiLTS());
    // languageToLTS.put(Language.JAPANESE.name().toLowerCase(), new LevantineLTS());
    languageToLTS.put(Language.LEVANTINE.name().toLowerCase(), new LevantineLTS());
    languageToLTS.put(Language.MANDARIN.name().toLowerCase(), unknown);
    languageToLTS.put(Language.MSA.name().toLowerCase(), new ModernStandardArabicLTS());
    languageToLTS.put(Language.PASHTO.name().toLowerCase(), new PashtoLTS());
    languageToLTS.put(Language.URDU.name().toLowerCase(), new UrduLTS());
    languageToLTS.put(Language.SUDANESE.name().toLowerCase(), new SudaneseLTS());
    languageToLTS.put(Language.TAGALOG.name().toLowerCase(), unknown);

/*    for (Language lang : Language.values()) {
      getLocale(lang);
    }*/
  }

  /**
   * @see mitll.langtest.server.scoring.ASRScoring#ASRScoring
   * @param thisLanguage
   */
  public LTSFactory(String thisLanguage) {
    this(Language.valueOf(thisLanguage.toUpperCase()));
  }

  /**
   * @see mitll.langtest.server.scoring.ASRScoring#getCollator
   * @return
   */
  @Override
  public Collator getCollator() {
    return Collator.getInstance(getLocale(thisLanguage));
  }

  public <T extends CommonExercise> void sort(List<T> toSort) {
    Collator collator = getCollator();
    final Map<T, CollationKey> exToKey = new HashMap<T, CollationKey>();
    for (T t : toSort) {
      CollationKey collationKey = collator.getCollationKey(t.getForeignLanguage());
      exToKey.put(t, collationKey);
    }

    Collections.sort(toSort, new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        CollationKey collationKey1 = exToKey.get(o1);
        CollationKey collationKey2 = exToKey.get(o2);
        return collationKey1.compareTo(collationKey2);
      }
    });
  }

  public static String getID(Language lang) {
    String locale = "en";
    switch (lang) {
      case ARABIC:
        locale = "ar";
        break;
      case DARI:
        locale = "da";
        break;
      case EGYPTIAN:
        locale = "ae";
        break;
      case FARSI:
        locale = "fa";
        break;
      case KOREAN:
        locale = "ko";
        break;
      case JAPANESE:
        locale = "jp";
        break;
      case LEVANTINE:
        locale = "al";
        break;
      case MANDARIN:
        locale = "mn";
        break;
      case MSA:
        locale = "ar";
        break;
      case PASHTO:
        locale = "ps";
        break;
      case RUSSIAN:
        locale = "ru";
        break;
      case SPANISH:
        locale = "es";
        break;
      case SUDANESE:
        locale = "as";
        break;
      case TAGALOG:
        locale = "tl";
        break;
      case URDU:
        locale = "ur";
        break;
    }
//
//    logger.debug("Name of Locale: " + locale.getDisplayName());
//    logger.debug("Language Code: " + locale.getLanguage() + ", Language Display Name: " + locale.getDisplayLanguage());
//    logger.debug("Country Code: " + locale.getCountry() + ", Country Display Name: " + locale.getDisplayCountry());

    return locale;
  }
  /**
   * @see #getCollator
   * @param lang
   * @return
   */
  private Locale getLocale(Language lang) {
    Locale locale = Locale.ENGLISH;
    switch (lang) {
      case ARABIC:
        locale = new Locale.Builder().setLanguage("ar").build();
        break;
      case DARI:
        locale = new Locale.Builder().setLanguage("fa").setRegion("pk").build();
        break;
      case EGYPTIAN:
        locale = new Locale.Builder().setLanguage("ar").setRegion("eg").build();
        break;
      case FARSI:
        locale = new Locale.Builder().setLanguage("fa").setRegion("ir").build();
        break;
      case KOREAN:
        locale = Locale.KOREAN;
        break;
      case JAPANESE:
        locale = Locale.JAPANESE;
        break;
      case LEVANTINE:
        locale = new Locale.Builder().setLanguage("ar").setRegion("sy").build();
        break;
      case MANDARIN:
        locale = Locale.CHINESE;
        break;
      case MSA:
        locale = new Locale.Builder().setLanguage("ar").build();
        break;
      case PASHTO:
        locale = new Locale.Builder().setLanguage("ps").build();
        break;
      case RUSSIAN:
        locale = new Locale.Builder().setLanguage("ru").build();
        break;
      case SPANISH:
        locale = new Locale.Builder().setLanguage("es").build();
        break;
      case SUDANESE:
        locale = new Locale.Builder().setLanguage("ar").setRegion("sd").build();
        break;
      case TAGALOG:
        locale = new Locale.Builder().setLanguage("tl").build();
        break;
      case URDU:
        locale = new Locale.Builder().setLanguage("ur").build();
        break;
    }
//
//    logger.debug("Name of Locale: " + locale.getDisplayName());
//    logger.debug("Language Code: " + locale.getLanguage() + ", Language Display Name: " + locale.getDisplayLanguage());
//    logger.debug("Country Code: " + locale.getCountry() + ", Country Display Name: " + locale.getDisplayCountry());

    return locale;
  }

  /**
   * @param language
   * @return
   * @see mitll.langtest.server.scoring.ASRScoring#ASRScoring
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
