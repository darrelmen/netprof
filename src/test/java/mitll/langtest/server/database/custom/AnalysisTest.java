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
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

public class AnalysisTest {
  private static Logger logger = LogManager.getLogger(AnalysisTest.class);
  private static String dbName;

  @BeforeClass
  public static void setup() {
    String config = "spanish";
    dbName = "npfSpanish";//"mandarin";// "mandarin";
    String dbName = AnalysisTest.dbName;
    String props = "quizlet.properties";

    DatabaseImpl war  = getDatabase(config, dbName, props);
    Collection<CommonExercise> exercises = war.getExercises();
    logger.warn("found " + exercises.size() + " exercises");

    Map<String,CommonExercise> idToEx = new HashMap<String,CommonExercise>();
    for (CommonExercise ex : exercises) idToEx.put(ex.getOldID(),ex);

    Map<Integer, Map<String, Result>> userToResults = null;//((ResultDAO)war.getResultDAO()).getUserToResults(true, war.getUserDAO());
    Map<String, Result> stringResultMap = userToResults.get(71l);

  //  Collection<CorrectAndScore> copy = getFirstCorrectAndScore(resultsForUser);

    logger.warn("users " + userToResults.keySet());
    logger.warn("got " + stringResultMap.size());

    DatabaseImpl war2 = getDatabase(config, "npfSpanishTest", "quizletOld.properties");
    Collection<CommonExercise> oldExercises = war2.getExercises();
    logger.warn("OLD found " + oldExercises.size() + " exercises");


  //  Map<String,CommonExercise> idToEx2 = new HashMap<String,CommonExercise>();
    List<String> exs = new ArrayList<String>();
    for (CommonExercise ex : oldExercises) {
      String id = ex.getOldID();
      CommonExercise newEx = idToEx.get(id);

      if (newEx == null) {
        //logger.warn("no new ex for old " + id);
      }
      else {
        String foreignLanguage  = newEx.getForeignLanguage().toLowerCase().trim();
        String foreignLanguage1 = ex.getForeignLanguage().toLowerCase().trim();
        List<String> tokens = getTokens(foreignLanguage);

        List<String> tokens1 = getTokens(foreignLanguage1);
        if (tokens.size() != tokens1.size()) {
          logger.warn("Diff " + id + "\t'" + foreignLanguage + "' vs '" + foreignLanguage1 +"'");
          exs.add(id);
        }
        else {
          int i = 0;
          for (String token : tokens) {
            String token2 = tokens1.get(i++);
            if (!token.equals(token2)) {
              if (token.endsWith(".")) {
                token = token.substring(0,token.length()-1);
              }
              if (!token.equals(token2)) {
             //   logger.warn("2 Diff " + id + " '" + token + "' vs '" + token2 + "'");
                logger.warn("3 Diff " + id + " '" + foreignLanguage + "' vs '" + foreignLanguage1 + "'");
                exs.add(id);
                break;
              }
            }
            else {
              //logger.debug("comp '" + token + "'  '" + token2 +"'");
            }
          }
        }

//        if (!removePunct(foreignLanguage.toLowerCase()).equals(removePunct(foreignLanguage1.toLowerCase()))) {
//          logger.warn("Diff " + id + " new " + foreignLanguage + " vs " + foreignLanguage1);
//          exs.addExerciseToList(id);
//        }
      }
      //idToEx2.put(ex.getOldID(),ex);
    }
    logger.warn("ids " + exs);
    int c = 0;
    int i = 0;
    for (String ex : exs) {
      CommonExercise exercise = idToEx.get(ex);
      if (exercise == null) {
        logger.error("huh? no exercise for " +ex);
      }
      else {
     //   i += war.attachAudio(exercise);
        for (AudioAttribute att : exercise.getAudioAttributes()) {
             war.getAudioDAO().markDefect(att);
          logger.debug("marked " + att);
          c++;
        }
      }
    }
    logger.warn("marked " + c + "/" + i+ " for " + exs.size());
  }

 /* private static Collection<CorrectAndScore> getFirstCorrectAndScore(List<CorrectAndScore> resultsForUser) {
    Map<String,CorrectAndScore> idToCorrect = new HashMap<String, CorrectAndScore>();

    for (CorrectAndScore correctAndScore : resultsForUser) {
      String key = correctAndScore.getExID() + "/" + correctAndScore.getQid();
      //  CorrectAndScore prev = idToCorrect.get(key);
      // if previous entry had no self grade but this one does take it
      // if (prev == null && correctAndScore.hasUserScore()) {// *//*|| !prev.hasUserScore()*//*) {
      idToCorrect.put(key, correctAndScore);
      //}
    }
    return new ArrayList<CorrectAndScore>(idToCorrect.values()); // required since can't send a values set
  }
*/

  public static List<String> getTokens(String sentence) {
    List<String> all = new ArrayList<String>();
    // logger.debug("initial " + sentence);
    String trimmedSent = getTrimmed(sentence);
    // logger.debug("after  trim " + trimmedSent);

    for (String untrimedToken : trimmedSent.split("\\p{Z}+")) { // split on spaces
      //String tt = untrimedToken.replaceAll("\\p{P}", ""); // remove all punct
      String token = untrimedToken.trim();  // necessary?
      if (token.length() > 0) {
        all.add(token);
      }
    }

    return all;
  }

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
    logger.debug("config dir " + parent +" config     " + file.getName());
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    DatabaseImpl war = new DatabaseImpl(serverProps, new PathHelper("war", serverProps), null, null);
    String media = parent + File.separator + "media";
    logger.debug("made " + war + " media " + media);

    war.setInstallPath(parent + File.separator + war.getServerProps().getLessonPlan());
    Collection<CommonExercise> exercises = war.getExercises();
    return war;
  }

  @Test
  public void testReport() {
    Map<String, Collection<String>> typeToValues = new HashMap<String, Collection<String>>();
    typeToValues.put("Lesson", Arrays.asList("1-1"));
  }
}
