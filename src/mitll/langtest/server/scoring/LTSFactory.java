/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.scoring;

import corpus.EmptyLTS;
import corpus.LTS;
import corpus.ModernStandardArabicLTS;
import mitll.langtest.server.database.AudioExport;
import mitll.langtest.shared.exercise.CommonExercise;
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
    ARABIC, DARI, EGYPTIAN, ENGLISH, FARSI, FRENCH, GERMAN, JAPANESE, IRAQI, LEVANTINE, KOREAN, MANDARIN, MSA,
    PASHTO, PORTUGUESE, RUSSIAN, SPANISH, SUDANESE, TAGALOG, URDU
  }

  private final LTS unknown = new EmptyLTS();
  private LTS ltsForLanguage = unknown; /// attempt to deal with undefined LTS...

  /**
   * TODO : what about Japanese, Korean, ... for LTS?
   * Does reflection to make an appropriate LTS - expecting something like corpus.EnglishLTS
   * <p>
   * ARABIC, MSA, and IRAQI all map to MSA LTS
   *
   * @param thisLanguage
   * @see ASRScoring#ASRScoring
   */
  private LTSFactory(Language thisLanguage) {
    this.thisLanguage = thisLanguage;
    String name = thisLanguage.name();
    String classPrefix = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    String className = "corpus." + classPrefix + "LTS";

    try {
      switch (thisLanguage) {
        case ARABIC:
        case MSA:
        case IRAQI:
          ltsForLanguage = new ModernStandardArabicLTS();
          break;
        case MANDARIN:
          ltsForLanguage = unknown;
          break;
        default:
          Class<?> aClass = Class.forName(className);
          ltsForLanguage = (LTS) aClass.newInstance();
          break;
      }
    } catch (ClassNotFoundException e) {
      logger.error("Couldn't find LTS class '" + className + "'");
    } catch (InstantiationException e) {
      logger.error("Couldn't make instance of LTS class " + className, e);
    } catch (IllegalAccessException e) {
      logger.error("Not allowed to make instance of LTS class " + className, e);
    }

    logger.debug("lts for " + name + " found at " + className + " is " + ltsForLanguage);
  }

  /**
   * @return
   * @see mitll.langtest.server.scoring.ASRScoring#ASRScoring
   */
  public LTS getLTSClass() { return ltsForLanguage; }

  /**
   * @param thisLanguage
   * @see mitll.langtest.server.scoring.ASRScoring#ASRScoring
   */
  public LTSFactory(String thisLanguage) {
    this(Language.valueOf(thisLanguage.toUpperCase()));
  }

  /**
   * @return
   * @see mitll.langtest.server.scoring.ASRScoring#getCollator
   * @see #sort(List)
   */
  public Collator getCollator() {
    return Collator.getInstance(getLocale(thisLanguage));
  }

  /**
   * @see Scoring#sort(List)
   * @param toSort
   * @param <T>
   */
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

  /**
   * @param lang
   * @return
   * @see AudioExport#writeContextToStream
   */
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
   * @param lang
   * @return
   * @see #getCollator
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
}
