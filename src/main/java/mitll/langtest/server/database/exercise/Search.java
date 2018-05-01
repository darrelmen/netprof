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

  //private DatabaseServices database;
  private ProjectServices projectServices;

  public Search(ProjectServices projectServices) {
    this.projectServices = projectServices;
    //this.database = database;
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
        new ExerciseTrie<>(exercises, getLanguage(projectID), getSmallVocabDecoder(projectID), true);
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
 /*   int exid = getExid(prefix);


    List<T> byID = new ArrayList<>();

    if (exid != -1) {
      T customOrPredefExercise = (T) database.getCustomOrPredefExercise(projectID, exid);
      if (customOrPredefExercise != null) {
        byID.add(customOrPredefExercise);
      }
      else logger.warn("getExercisesForSearchWithTrie no ex for " +projectID + " = " + exid);
    }*/
    return new TripleExercises<T>(
        Collections.emptyList(),
        basicExercises,
        ts);
  }


  public int getExid(String prefix) {
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
