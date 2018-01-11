package mitll.langtest.server.domino;

import mitll.langtest.shared.exercise.CommonExercise;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ImportInfo {
  private final Date createTime;
  private final List<CommonExercise> exercises;
  private final Collection<Integer> deletedDominoIDs;
  private Set<String> deletedNPIDs;

  private final String language;
  private final mitll.langtest.shared.project.Language lang;

  private final int dominoID;

  /**
   *
   * @param importProjectInfo
   * @param exercises
   * @param deletedNPIDs
   * @see mitll.langtest.server.database.exercise.DominoExerciseDAO#readExercises(int, ImportProjectInfo, DominoImport.ChangedAndDeleted)
   */
  public ImportInfo(ImportProjectInfo importProjectInfo, List<CommonExercise> exercises, Collection<Integer> deletedDominoIDs, Set<String> deletedNPIDs) {
    this(
        importProjectInfo.getDominoProjectID(),
        importProjectInfo.getLanguage(),
        importProjectInfo.getCreateDate(),

        exercises,
        deletedDominoIDs);

    this.deletedNPIDs = deletedNPIDs;
  }

  /**
   * @param dominoID
   * @param language
   * @param exportTime
   * @param exercises
   */
  private ImportInfo(int dominoID,
                     String language,
                     Date exportTime,
                     List<CommonExercise> exercises,
                     Collection<Integer> deletedDominoIDs) {
    this.createTime = exportTime;
    this.exercises = exercises;
    this.language = language;
    this.lang = getLanguage(language);
    this.dominoID = dominoID;
    this.deletedDominoIDs = deletedDominoIDs;
  }

  @NotNull
  private mitll.langtest.shared.project.Language getLanguage(String languageName) {
    mitll.langtest.shared.project.Language lang = mitll.langtest.shared.project.Language.UNKNOWN;
    try {
      lang = mitll.langtest.shared.project.Language.valueOf(languageName);
//        logger.info("Got " + languageName + " " + lang);
    } catch (IllegalArgumentException e) {
    }
    return lang;
  }

  private Date getExportTime() {
    return createTime;
  }
  public List<CommonExercise> getExercises() {    return exercises;  }

  /**
   * @return
   */
  public int getDominoID() {
    return dominoID;
  }

  /**
   * @see ProjectSync#doDelete
   * @return
   */
  public Collection<Integer> getDeletedDominoIDs() {
    return deletedDominoIDs;
  }

  public Set<String> getDeletedNPIDs() {
    return deletedNPIDs;
  }

  public String toString() {
    return "lang " + language + "/" + lang + " " + getDominoID() + " " + getExportTime() + " num " + getExercises().size() + " deleted "+ getDeletedDominoIDs().size();
  }
}
