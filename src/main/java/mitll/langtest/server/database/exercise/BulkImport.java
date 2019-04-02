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

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.server.scoring.IPronunciationLookup;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.Pair;
import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static mitll.langtest.server.database.excel.ExcelExport.WORD_EXPRESSION;

public class BulkImport {
  private static final Logger logger = LogManager.getLogger(BulkImport.class);
  private static final boolean DEBUG = false;
  private static final String REGEX = " ";  // no break space!
  private static final String TIC_REGEX = "&#39;";
  //private static final boolean DEBUG_IMPORT = false;

  private ExerciseServices exerciseServices;
  private ProjectServices projectServices;

  public BulkImport(ExerciseServices exerciseServices,
                    ProjectServices projectServices) {

    this.exerciseServices = exerciseServices;
    this.projectServices = projectServices;
  }

  /**
   * Assumes word pairs! No context sentences.
   *
   * @param lines
   * @param knownAlready
   * @param currentKnownFL
   * @param project
   * @param englishProject
   * @param userIDFromSession
   * @return
   * @see #reallyCreateNewItems
   */
  public List<CommonExercise> convertTextToExercises(String[] lines,
                                                     Set<CommonExercise> knownAlready,
                                                     Set<Integer> onListAlready,
                                                     Set<String> currentKnownFL,
                                                     Project project,
                                                     Project englishProject,
                                                     int userIDFromSession) {
    //  boolean onFirst = true;
    boolean firstColIsEnglish = false;
    List<CommonExercise> newItems = new ArrayList<>();

    logger.info("convertTextToExercises currently know about " + currentKnownFL.size() + " items on list" +
        "\n\tproject     " + project +
        "\n\teng project " + englishProject);

    List<Pair> pairs = new ArrayList<>(lines.length);

    HeaderInfo headerInfo = readHeader(lines);

    if (headerInfo.isFoundHeader()) {
      readGivenHeader(lines, pairs, headerInfo);
    } else if (englishProject != null) {  // in local dev could be false
      // gotta guess somehow
      logger.warn("convertTextToExercises : examine " + lines.length + " lines...");
      firstColIsEnglish = guessLanguageInCol(lines, englishProject.getAudioFileHelper().getASR().getPronunciationLookup(), pairs);
    } else {
      logger.warn("convertTextToExercises : no english project??");
      pairs = getFLEnglishPairs(lines);
    }

    if (pairs.isEmpty()) {
      logger.warn("hmm - no items from " + lines.length + " lines.");
    }
    for (Pair pair : pairs) {
      String fl = pair.getProperty();
      String english = pair.getValue();

      if (fl.trim().isEmpty()) {
        logger.warn("convertTextToExercises skipping line " + pair);
      } else {
        if (!currentKnownFL.contains(fl)) {
          CommonExercise known = makeOrFindExercise(newItems, firstColIsEnglish, userIDFromSession, fl, english, project,
              onListAlready);

          if (known == null) {
            logger.warn("convertTextToExercises couldn't make exercise from " + pair);
          } else {
            if (known.getID() > 0) {
              logger.info("convertTextToExercises made or found " + known.getID() +
                  " fl '" + fl + "' = eng '" + english + "'");
              knownAlready.add(known);
            } else {
              logger.info("convertTextToExercises new ex " + known.getID() +
                  "  '" + fl + "' = '" + english + "'");
            }
          }
        } else {
          logger.info("convertTextToExercises skipping " + fl + " that's already on the list.");
        }
      }
    }

    return newItems;
  }

  private List<Pair> getFLEnglishPairs(String[] lines) {
    List<Pair> pairs = new ArrayList<>(lines.length);

    for (String line : lines) {
      String[] parts = line.split("\\t");
      if (parts.length > 1) {
        pairs.add(getPair(parts));
      }
    }
    return pairs;
  }

  /**
   * OK, so we can handle just a single column of fl words
   *
   * @param lines
   * @param pairs
   * @param headerInfo
   */
  private void readGivenHeader(String[] lines, List<Pair> pairs, HeaderInfo headerInfo) {
    boolean skipFirst = true;

    boolean firstIsEnglish = headerInfo.isFirstIsEnglish();
    // assume first col is foreign
    for (String line : lines) {
      if (!skipFirst) {
        String[] parts = line.split("\\t");
        if (parts.length > 1) {
          Pair e = getPair(parts);
          if (firstIsEnglish) e.swap();
          pairs.add(e);
        } else if (parts.length == 1) {
          pairs.add(new Pair(parts[0], ""));
        }
      }
      skipFirst = false;
    }
  }

  /**
   * deal with no break space
   *
   * deal with tic marks...?
   *
   * @param part
   * @return
   */
  private String getTrim(String part) {
    return part.replaceAll(REGEX, " ").replaceAll(TIC_REGEX, "'").trim();
  }

  /**
   * @param lines
   * @param engLookup
   * @param pairs
   * @return true if first col is english
   */
  private boolean guessLanguageInCol(String[] lines, mitll.langtest.server.scoring.IPronunciationLookup engLookup, List<Pair> pairs) {
    // make a decision as to which side is english in word pairs
    int engCountFirst = 0, engCountSecond = 0;
    int totalFirst = 0, totalSecond = 0;

    boolean allTwoCol = true;

    // assume first col is foreign
    for (String line : lines) {
      String[] parts = line.split("\\t");
      int length = parts.length;
      if (length > 0) {
        if (length < 2) allTwoCol = false;
        Pair e = getPair(parts, length);
        pairs.add(e);
        logger.info("line " + line + " : " + e);

        String property = e.getProperty();
        if (!property.isEmpty()) {
          mitll.langtest.server.scoring.IPronunciationLookup.InDictStat tokenStats = engLookup.getTokenStats(property);
          engCountFirst += tokenStats.getNumInDict();
          totalFirst += tokenStats.getNumTokens();
        }

        String value = e.getValue();
        if (!value.isEmpty()) {
          IPronunciationLookup.InDictStat tokenStats = engLookup.getTokenStats(value);
          engCountSecond += tokenStats.getNumInDict();
          totalSecond += tokenStats.getNumTokens();
        }
      }
    }

    return allTwoCol && isFirstColumnEnglish(engCountFirst, engCountSecond, totalFirst, totalSecond);
  }

  @NotNull
  private Pair getPair(String[] parts, int length) {
    Pair e;
    if (length == 1) {
      e = new Pair(parts[0], "");
    } else {
      e = getPair(parts);
    }
    return e;
  }

  private boolean isFirstColumnEnglish(int engCountFirst, int engCountSecond, int totalFirst, int totalSecond) {
    logger.info("guessLanguageInCol eng first  " + engCountFirst + "/" + totalFirst);
    logger.info("guessLanguageInCol eng second " + engCountSecond + "/" + totalSecond);
    boolean firstColIsEnglish;
    if (totalFirst == 0 && totalSecond > 0) {
      firstColIsEnglish = false;
    } else if (totalSecond == 0 && totalFirst > 0) {
      firstColIsEnglish = true;
    } else {
      float firstRatio = totalFirst == 0 ? 0 : (float) engCountFirst / (float) totalFirst;
      float secondRatio = totalSecond == 0 ? 0 : (float) engCountSecond / (float) totalSecond;

      firstColIsEnglish = firstRatio > secondRatio;

      logger.info("guessLanguageInCol eng ratios " + firstRatio + " vs " + secondRatio +
          "\n\tfirst col is english " + firstColIsEnglish);
    }
    return firstColIsEnglish;
  }

  @NotNull
  private Pair getPair(String[] parts) {
    String fl = getTrim(parts[0]);
    String english = getTrim(parts[1]);

    return new Pair(fl, english);
  }

  private HeaderInfo readHeader(String[] lines) {
    boolean foundHeader = false;
    String englishLang = Language.ENGLISH.toString();

    String headerFirst, headerSecond;
    boolean firstIsEnglish = false;
    if (lines.length > 0) {
      String[] parts = lines[0].split("\\t");
      if (parts.length > 1) {
        headerFirst = parts[0].trim();
        headerSecond = parts[1].trim();

        firstIsEnglish = headerFirst.equalsIgnoreCase(englishLang) || headerFirst.equalsIgnoreCase(WORD_EXPRESSION);
        if (firstIsEnglish || headerSecond.equalsIgnoreCase(englishLang) || headerSecond.equalsIgnoreCase(WORD_EXPRESSION)) {
          foundHeader = true;
        }
      }
    }
    return new HeaderInfo(foundHeader, firstIsEnglish);
  }

  private static class HeaderInfo {
    private final boolean foundHeader;
    private final boolean firstIsEnglish;

    HeaderInfo(boolean foundHeader, boolean firstIsEnglish) {
      this.foundHeader = foundHeader;
      this.firstIsEnglish = firstIsEnglish;
    }

    boolean isFoundHeader() {
      return foundHeader;
    }

    boolean isFirstIsEnglish() {
      return firstIsEnglish;
    }
  }

  @NotNull
  private Set<String> getCurrentOnList(UserList<CommonShell> userListByID) {
    Set<String> currentKnownFL = new HashSet<>();
    for (CommonShell shell : userListByID.getExercises()) {
      currentKnownFL.add(shell.getForeignLanguage());
    }
    return currentKnownFL;
  }

  /**
   * @param newItems
   * @param firstColIsEnglish
   * @param userIDFromSession
   * @param fl
   * @param english
   * @param project
   * @param onListAlready
   * @return
   * @see #convertTextToExercises
   */
  private CommonExercise makeOrFindExercise(List<CommonExercise> newItems,
                                            boolean firstColIsEnglish,
                                            int userIDFromSession,
                                            String fl,
                                            String english,
                                            Project project,
                                            Set<Integer> onListAlready) {
    english = english.replaceAll(TIC_REGEX, "'").trim();

    int projectID = project.getID();
    if (firstColIsEnglish) {// || (isValid(project, english) && !isValid(project, fl))) {
      String temp = english;
      english = fl;
      fl = temp;
      logger.info("makeOrFindExercise flip english '" + english + "' to fl '" + fl + "'");
    }
    logger.info("makeOrFindExercise eng '" + english + "' fl '" + fl + "'");

    if (DEBUG) logger.info("makeOrFindExercise : onListAlready " + onListAlready.size() + " " + onListAlready);

    CommonExercise exercise = getExerciseByVocab(projectID, fl);
    boolean found = false;
    if (exercise != null) {
      if (DEBUG) logger.info("makeOrFindExercise : exercise " + exercise.getID() + " " + exercise.getForeignLanguage());
      if (exercise.getEnglish().equalsIgnoreCase(english)) {
        if (DEBUG)
          logger.info("makeOrFindExercise : exercise english match " + exercise.getID() + " " + exercise.getEnglish());
        boolean contains = onListAlready.contains(exercise.getID());
        if (!contains) {
          newItems.add(exercise);
        } else {
          found = true;
          exercise = null;
        }
      } else {
        if (DEBUG) logger.info("english mismatch " + english + " vs " + exercise.getEnglish());
        exercise = null;
      }
    } else {
      if (DEBUG) logger.info("makeOrFindExercise : exercise " + exercise);
      // gotta be mine
      List<CommonExercise> exactMatch = exerciseServices.getExerciseDAO(projectID).getUserDefinedByProjectExactMatch(fl, userIDFromSession);

      if (!exactMatch.isEmpty()) {
        if (exactMatch.size() > 1)
          logger.info("makeOrFindExercise : found " + exactMatch.size() + " exact matches for " + fl);

        CommonExercise next = exactMatch.iterator().next();
        if (DEBUG) logger.info("makeOrFindExercise : next " + next.getID() + " " + next.getForeignLanguage());
        boolean contains = onListAlready.contains(next.getID());
        if (!contains) {
          newItems.add(next);
          exercise = next;
        } else {
          found = true;
          exercise = null;
        }
      }
    }

    if (exercise == null && !found) { // OK gotta make a new one
      Exercise newItem = getNewExercise(userIDFromSession, fl, english, projectID);
      newItems.add(newItem);
      exercise = newItem;
      if (DEBUG) logger.info("reallyCreateNewItems new " + newItem);
    }

    return exercise;
  }

  @NotNull
  private Exercise getNewExercise(int userIDFromSession, String fl, String english, int projectID) {
    Exercise newItem =
        new Exercise(-1,
            userIDFromSession,
            english,
            projectID,
            false);
    newItem.setForeignLanguage(fl);
    return newItem;
  }

  private CommonExercise getExerciseByVocab(int projectID, String foreignLanguage) {
    return projectServices.getProject(projectID).getExerciseByExactMatch(foreignLanguage.trim());
  }

//  private boolean isValid(Project project, String foreign) {
//    return isValidForeignPhrase(project, foreign).isEmpty();
//  }

//  private Collection<String> isValidForeignPhrase(Project project, String foreign) {
//    return project.getAudioFileHelper().checkLTSOnForeignPhrase(foreign, "");
//  }
}
