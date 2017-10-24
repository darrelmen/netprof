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

package mitll.langtest.server.scoring;

import corpus.EmptyLTS;
import corpus.LTS;
import corpus.ModernStandardArabicLTS;
import mitll.langtest.server.audio.AudioExport;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.CollationKey;
import java.text.Collator;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/1/13
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class LTSFactory {
  private static final Logger logger = LogManager.getLogger(LTSFactory.class);

  private final Language thisLanguage;

  private final LTS unknown = new EmptyLTS();
  private LTS ltsForLanguage = unknown; /// attempt to deal with undefined LTS...

  public static boolean isEmpty(LTS lts) {
    return lts.getClass() == EmptyLTS.class;
  }

  /**
   * TODO : what about Japanese, Korean, ... for LTS?
   * Does reflection to make an appropriate LTS - expecting something like corpus.EnglishLTS
   * <p>
   * ARABIC and MSA  map to MSA LTS
   *
   * @param thisLanguage
   * @seex ASRScoring#ASRScoring
   */
  private LTSFactory(Language thisLanguage) {
    this.thisLanguage = thisLanguage;
    // logger.info("got " +thisLanguage + " " + thisLanguage.name());
    String name = thisLanguage.name();
    String classPrefix = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    String className = "corpus." + classPrefix + "LTS";

    try {
      switch (thisLanguage) {
        case ARABIC:
        case MSA:
          ltsForLanguage = new ModernStandardArabicLTS();
          break;
        default:
          Class<?> aClass = Class.forName(className);
          ltsForLanguage = (LTS) aClass.newInstance();
          break;
      }
    } catch (ClassNotFoundException e) {
      logger.warn("LTSFactory : Couldn't find LTS class '" + className + "'");
    } catch (InstantiationException e) {
      logger.error("Couldn't make instance of LTS class " + className, e);
    } catch (IllegalAccessException e) {
      logger.error("Not allowed to make instance of LTS class " + className, e);
    }

    if (isEmpty(ltsForLanguage)) {
      logger.debug("lts for '" + name + "' found at " + className + " is " + ltsForLanguage);
    }
  }

  /**
   * @return
   * @see mitll.langtest.server.scoring.ASRScoring#ASRScoring
   */
  public LTS getLTSClass() {
    return ltsForLanguage;
  }

  /**
   * @param thisLanguage
   * @see mitll.langtest.server.scoring.ASRScoring#ASRScoring
   */
  LTSFactory(String thisLanguage) {
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
   * @param toSort
   * @param <T>
   * @see Scoring#sort
   */
  public <T extends CommonExercise> void sort(List<T> toSort) {
    Collator collator = getCollator();
    final Map<T, CollationKey> exToKey = new HashMap<T, CollationKey>();
    for (T t : toSort) {
      CollationKey collationKey = collator.getCollationKey(t.getForeignLanguage());
      exToKey.put(t, collationKey);
    }

    toSort.sort((o1, o2) -> {
      CollationKey collationKey1 = exToKey.get(o1);
      CollationKey collationKey2 = exToKey.get(o2);
      return collationKey1.compareTo(collationKey2);
    });
  }

  public static String getLocale(String language1) {
    Language lang;
    try {
      lang = Language.valueOf(language1.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.error("getLocale for unknown language " + language1);
      lang = Language.ENGLISH;
    }
    return LTSFactory.getID(lang);
  }

  /**
   * Consistent with BCP 47.
   * <p>
   * These are based on lookups in <a href='http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry'>subtag-registry</a>
   * <p>
   * Also see <a href='https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation'>Locale identification and negotiation</a>
   *
   * @param lang
   * @return
   * @see AudioExport#getCountryCode
   */
  private static String getID(Language lang) {
    String locale = "en";
    switch (lang) {
      case ARABIC:
        locale = "ar";
        break;
      case CROATIAN:
        locale = "hr";
        break;
      case DARI:
        locale = "prs";
        break;
      case EGYPTIAN:
        locale = "arz";
        break;
      case FARSI:  // or Persian
        locale = "fa";
        break;
      case FRENCH:
        locale = "fr";
        break;
      case GERMAN:
        locale = "de";
        break;
      case HINDI:
        locale = "hi";
        break;
      case IRAQI:
        locale = "ar-IQ";
        break;
      case JAPANESE:
        locale = "ja";
        break;
      case KOREAN:
        locale = "ko";
        break;
      case LEVANTINE:
        locale = "ar-SY";
        break;
      case MANDARIN:
        locale = "cmn";
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
      case SERBIAN:
        locale = "sr";
        break;
      case SORANI: // Kurdish sorani
        locale = "ku";
        break;
      case SPANISH:
        locale = "es";
        break;
      case SUDANESE:
        locale = "apd";
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
      case IRAQI:
        locale = new Locale.Builder().setLanguage("ar").setRegion("iq").build();
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
