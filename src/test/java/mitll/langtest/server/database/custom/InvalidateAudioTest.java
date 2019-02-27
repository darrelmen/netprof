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

package mitll.langtest.server.database.custom;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

public class InvalidateAudioTest {
  private static Logger logger = LogManager.getLogger(InvalidateAudioTest.class);
  //private static String dbName;

  @BeforeClass
  public static void setup() {
//    markDefectsForFL(config, props);
  }

  @Test
  public void testMarkDefectsForContext() {
    String config = "mandarin";
    String dbName = config;
    String props = "quizlet.properties";

    markDefectsContext(config, dbName, props);
  }

  private void markDefectsContext(String config, String dbName, String props) {
    DatabaseImpl newDB = getDatabase(config, dbName, props);
    SmallVocabDecoder smallVocabDecoder = getSmallVocabDecoder(newDB);

    Map<String, CommonExercise> idToNewEx = getIDToExercise(newDB);

    Collection<CommonExercise> oldExercises = getDatabase(config, config + "Old", "quizletOld.properties").getExercises();
    logger.warn("OLD found " + oldExercises.size() + " exercises");

    Set<String> idsOfDefects = new TreeSet<String>();
    for (CommonExercise oldEx : oldExercises) {
      String id = oldEx.getOldID();
      CommonExercise newEx = idToNewEx.get(id);

      if (newEx == null) {
        logger.warn("no new oldEx for old " + id);
      } else {
        //      boolean different = isDifferentContext(id, oldEx, newEx,smallVocabDecoder);
        //    if (different) {
        //    idsOfDefects.addExerciseToList(id);
        //  }
      }
    }
    logger.warn("ids to change - context " + idsOfDefects);
    markContextDefects(newDB, idToNewEx, idsOfDefects);
  }

  private SmallVocabDecoder getSmallVocabDecoder(DatabaseImpl newDB) {
    ServerProperties serverProps = newDB.getServerProps();
    AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war", serverProps), serverProps, newDB, null, null);
    return audioFileHelper.getSmallVocabDecoder();
  }


/*  @Test
  public void testMarkDefectsForFL() {
    String config = "mandarin";
    String dbName = config;
    String props = "quizlet.properties";

    markDefectsFL(config, dbName, props);
  }*/
/*
  private void markDefectsFL(String config, String dbName, String props) {
    DatabaseImpl newDB = getDatabase(config, dbName, props);
    Map<String, CommonExercise> idToNewEx = getIDToExercise(newDB);
    SmallVocabDecoder smallVocabDecoder = getSmallVocabDecoder(newDB);

    Collection<CommonExercise> oldExercises = getDatabase(config, config + "Old", "quizletOld.properties").getExercises();
    logger.warn("OLD found " + oldExercises.size() + " exercises");

    Set<String> idsOfDefects = new TreeSet<String>();
    for (CommonExercise oldEx : oldExercises) {
      String id = oldEx.getOldID();
      CommonExercise newEx = idToNewEx.get(id);

      if (newEx == null) {
        logger.warn("no new oldEx for old " + id);
      } else {
        boolean different = isDifferent(id, oldEx, newEx, smallVocabDecoder);
        if (different) {
          idsOfDefects.add(id);
        }
      }
    }
    logger.warn("ids to change " + idsOfDefects);
    markDefects(newDB, idToNewEx, idsOfDefects);
  }*/

/*  private boolean isDifferent(String id, CommonExercise oldEx, CommonExercise newEx, SmallVocabDecoder smallVocabDecoder) {
    String foreignLanguageNew = newEx.getForeignLanguage().toLowerCase().trim();
    String foreignLanguageOld = oldEx.getForeignLanguage().toLowerCase().trim();

    return isDifferent(id, foreignLanguageNew, foreignLanguageOld, smallVocabDecoder);
  }*/

/*  private boolean isDifferentContext(String id, CommonExercise oldEx, CommonExercise newEx, SmallVocabDecoder smallVocabDecoder) {
    String foreignLanguageNew = newEx.getContext().toLowerCase().trim();
    String foreignLanguageOld = oldEx.getContext().toLowerCase().trim();

    return isDifferent(id, foreignLanguageNew, foreignLanguageOld,smallVocabDecoder);
  }*/
/*
  private boolean isDifferent(String id, String foreignLanguageNew, String foreignLanguageOld, SmallVocabDecoder smallVocabDecoder) {
    List<String> newTokens = getTokens(foreignLanguageNew, smallVocabDecoder);
    List<String> oldTokens = getTokens(foreignLanguageOld, smallVocabDecoder);

    return isDifferent(id, foreignLanguageNew, foreignLanguageOld, newTokens, oldTokens);
  }*/

  private boolean isDifferent(String id, String foreignLanguageNew, String foreignLanguageOld,
                              Collection<String> newTokens, List<String> oldTokens) {
    if (newTokens.size() != oldTokens.size()) {
      logger.warn("Diff " + id + "\t'" + foreignLanguageNew + "' vs '" + foreignLanguageOld + "'");
      return true;
    } else {
      int i = 0;
      for (String newToken : newTokens) {
        String oldToken = oldTokens.get(i++);
        if (!newToken.equals(oldToken)) {
          if (newToken.endsWith(".")) {
            newToken = newToken.substring(0, newToken.length() - 1);
          }
          if (!newToken.equals(oldToken)) {
            logger.warn("3 Diff " + id + " new '" + newToken +
                "' vs old '" + oldToken +
                "' : '" + foreignLanguageNew + "' vs '" + foreignLanguageOld + "'");
            return true;
          }
        }
      }
    }
    return false;
  }

  private Map<String, CommonExercise> getIDToExercise(DatabaseImpl newDB) {
    Map<String, CommonExercise> idToNewEx = new HashMap<String, CommonExercise>();
    Collection<CommonExercise> currentExercises = newDB.getExercises();
    logger.warn("found " + currentExercises.size() + " exercises");
    for (CommonExercise ex : currentExercises) idToNewEx.put(ex.getOldID(), ex);
    return idToNewEx;
  }

  public static void markDefects(DatabaseImpl war, Map<String, CommonExercise> idToEx, Collection<String> exs) {
    IAudioDAO audioDAO = war.getAudioDAO();

    logger.warn("ids " + exs);
    int c = 0;
    int i = 0;
    for (String ex : exs) {
      CommonExercise exercise = idToEx.get(ex);
      if (exercise == null) {
        logger.error("huh? no exercise for " + ex);
      } else {
       // i += war.attachAudio(exercise);
        for (AudioAttribute att : exercise.getAudioAttributes()) {
          audioDAO.markDefect(att);
          logger.debug("marked " + att);
          c++;
        }
      }
    }
    logger.warn("marked " + c + "/" + i + " for " + exs.size());
  }

  public static void markContextDefects(DatabaseImpl war, Map<String, CommonExercise> idToEx, Collection<String> exs) {
    IAudioDAO audioDAO = war.getAudioDAO();

    logger.warn("ids " + exs);
    int c = 0;
    int i = 0;
    for (String ex : exs) {
      CommonExercise exercise = idToEx.get(ex);
      if (exercise == null) {
        logger.error("huh? no exercise for " + ex);
      } else {
       // i += war.attachAudio(exercise);
        for (AudioAttribute att : exercise.getAudioAttributes()) {
          if (att.isContextAudio()) {
            audioDAO.markDefect(att);
            logger.debug("\t for " + ex + " marked " + att);
            c++;
          }
        }
      }
    }
    logger.warn("marked " + c + "/" + i + " for " + exs.size());
  }

/*  public List<String> getTokens(String sentence, SmallVocabDecoder smallVocabDecoder) {
    List<String> all = new ArrayList<String>();
    // logger.debug("initial " + sentence);
    String trimmedSent = getTrimmed(sentence);
    // logger.debug("after  trim " + trimmedSent);

    Collection<String> tokens = smallVocabDecoder.getMandarinTokens(sentence);
    String[] onSpaces = trimmedSent.split("\\p{Z}+");
    for (String untrimedToken : tokens) { // split on spaces
      //String tt = untrimedToken.replaceAll("\\p{P}", ""); // remove all punct
      String token = untrimedToken.trim();  // necessary?
      if (token.length() > 0) {
        all.add(token);
      }
    }

    return all;
  }*/

  public static String getTrimmed(String sentence) {
    // logger.debug("after  convert " + sentence);
    return sentence.replaceAll("\\s+", " ").trim();
  }

  private static List<String> removePunct(Collection<String> possibleSentences) {
    List<String> foreground = new ArrayList<String>();
    for (String ref : possibleSentences) {
      foreground.add(removePunct(ref));
    }
    return foreground;
  }

  /**
   * Replace elipsis with space. Then remove all punct.
   *
   * @param t
   * @return
   */
  private static String removePunct(String t) {
    return t.replaceAll("\\.\\.\\.", " ").replaceAll("\\p{P}", "");
  }

  public static DatabaseImpl getDatabase(String config, String dbName, String props) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + props);
    String parent = file.getParent();
    logger.debug("config dir " + parent + " config     " + file.getName());
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    DatabaseImpl war = new DatabaseImpl(serverProps,
        new PathHelper("war", serverProps), null, null);
    String media = parent + File.separator + "media";
    logger.debug("made " + war + " media " + media);

    war.setInstallPath(parent + File.separator + war.getServerProps().getLessonPlan(), null, true);
    war.getExercises();
    return war;
  }

  @Test
  public void testInvalidate() {
  }

  @Test
  public void testReport() {
    Map<String, Collection<String>> typeToValues = new HashMap<String, Collection<String>>();
    typeToValues.put("Lesson", Arrays.asList("1-1"));
  }
}
