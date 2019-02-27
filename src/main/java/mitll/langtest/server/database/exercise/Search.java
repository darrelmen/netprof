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

import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Search<T extends CommonExercise> {
  private static final Logger logger = LogManager.getLogger(Search.class);

  private final ProjectServices projectServices;

  public Search(ProjectServices projectServices) {
    this.projectServices = projectServices;
  }

  /**
   * TODO : revisit the parameterized types here.
   *
   * @param <T>
   * @param prefix
   * @param exercises
   * @param predefExercises
   * @param matchOnContext
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseIds
   */
  public <T extends CommonExercise> TripleExercises<T> getExercisesForSearch(String prefix,
                                                                             Collection<T> exercises,
                                                                             boolean predefExercises,
                                                                             int projectID,
                                                                             boolean matchOnContext) {
    Project project = projectServices.getProject(projectID);
    return getExercisesForSearchWithTrie(prefix, exercises, predefExercises, (ExerciseTrie<T>) project.getFullTrie(), projectID, matchOnContext);
  }

  /**
   * If not the full exercise list, build a trie here and use it.
   *
   * @param <T>
   * @param prefix
   * @param exercises
   * @param predefExercises if true, use the fullTrie
   * @param fullTrie
   * @param projectID
   * @param matchOnContext
   * @return
   * @see #getExercisesForSearch
   */
  private <T extends CommonExercise> TripleExercises<T> getExercisesForSearchWithTrie(String prefix,
                                                                                      Collection<T> exercises,
                                                                                      boolean predefExercises,
                                                                                      ExerciseTrie<T> fullTrie,
                                                                                      int projectID,
                                                                                      boolean matchOnContext) {
    ExerciseTrie<T> trie = predefExercises ? fullTrie :
        new ExerciseTrie<>(exercises, getLanguage(projectID), getSmallVocabDecoder(projectID), true, false);
    List<T> basicExercises = trie.getExercises(prefix);
    Project project = getProject(projectID);
    ExerciseTrie<T> fullContextTrie = !matchOnContext || project == null ? null : (ExerciseTrie<T>) project.getFullContextTrie();

    List<T> ts = Collections.emptyList();

    if (predefExercises && fullContextTrie != null) {
      ts = fullContextTrie.getExercises(prefix);
 /*     logger.info("getExercisesForSearchWithTrie for full context for" +
          "\n\tprefix '" + prefix + "'" +
          "\n\tgot " + ts.size());
*/

      logger.info("getExercisesForSearchWithTrie : " +
              "\n\tprojectID " + projectID +
//          "\n\thas full  " + (fullContextTrie != null) +
              //         "\n\tfound     " + (project != null) +
              //  "\n\tpredef    " + predefExercises +
              "\n\tprefix    " + prefix +
              "\n\tmatches   " + basicExercises.size() +
              "\n\tcontext   " + ts.size()
      );
    } else {
      logger.info("getExercisesForSearchWithTrie : " +
          "\n\tprojectID " + projectID +
          "\n\thas full  " + (fullContextTrie != null) +
          "\n\tfound     " + (project != null) +
          "\n\tpredef    " + predefExercises +
          "\n\tprefix    " + prefix +
          "\n\tmatches   " + basicExercises.size());
    }

    return new TripleExercises<T>(
        Collections.emptyList(),
        basicExercises,
        ts);
  }


  public int getID(String prefix) {
    int exid = -1;
    if (!prefix.isEmpty()) {
      try {
        exid = Integer.parseInt(prefix);
      } catch (NumberFormatException e) {
        // logger.info("getExercisesForSearchWithTrie can't parse search number '" + prefix + "'");
      }
    }
    return exid;
  }

  private String getLanguage(int projID) {
    return projectServices.getLanguage(projID);
  }

  protected Project getProject(int projID) {
    return projectServices.getProject(projID);
  }

  private SmallVocabDecoder getSmallVocabDecoder(int projectID) {
    return getAudioFileHelper(projectID).getSmallVocabDecoder();
  }

  protected AudioFileHelper getAudioFileHelper(int projectID) {
    return getAudioFileHelper(projectServices.getProject(projectID));
  }

  @Nullable
  protected AudioFileHelper getAudioFileHelper(Project project) {
    if (project == null) {
      logger.error("getAudioFileHelper no current project???");
      return null;
    }
    return project.getAudioFileHelper();
  }
}
