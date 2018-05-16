package mitll.langtest.server.domino;

import mitll.langtest.shared.exercise.CommonExercise;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ImportInfo {
  private final Date createTime;
  private final List<CommonExercise> changedExercises;
  private final List<CommonExercise> addedExercises;
  private final Collection<Integer> deletedDominoIDs;
  private Set<String> deletedNPIDs;

  private final String language;
  private final mitll.langtest.shared.project.Language lang;

  private final int dominoID;

  /**
   * @param importProjectInfo
   * @param changedExercises
   * @param deletedNPIDs
   * @see mitll.langtest.server.database.exercise.DominoExerciseDAO#readExercises(int, ImportProjectInfo, DominoImport.ChangedAndDeleted)
   */
  public ImportInfo(ImportProjectInfo importProjectInfo,
                    List<CommonExercise> changedExercises,
                    List<CommonExercise> addedExercises,
                    Collection<Integer> deletedDominoIDs,
                    Set<String> deletedNPIDs) {
    this(
        importProjectInfo.getDominoProjectID(),
        importProjectInfo.getLanguage(),
        importProjectInfo.getCreateDate(),

        changedExercises,
        addedExercises,
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
                     List<CommonExercise> addedExercises,
                     Collection<Integer> deletedDominoIDs) {
    this.createTime = exportTime;
    this.changedExercises = exercises;
    this.addedExercises = addedExercises;
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

  /**
   * @return
   */
  List<CommonExercise> getChangedExercises() {
    return changedExercises;
  }

  /**
   * @return
   */
  public int getDominoID() {
    return dominoID;
  }

  /**
   * @return
   * @see ProjectSync#doDelete
   */
  Collection<Integer> getDeletedDominoIDs() {
    return deletedDominoIDs;
  }

  public Set<String> getDeletedNPIDs() {
    return deletedNPIDs;
  }

  List<CommonExercise> getAddedExercises() {
    return addedExercises;
  }

  public String toString() {
    return "lang " + language + "/" + lang +
        "\n\tdomino #" + getDominoID() +
        "n\tat       " + getExportTime() +
        "\n\tadded   " + getAddedExercises().size() +
        "\n\tchanged " + getChangedExercises().size() +
        "\n\tdeleted " + getDeletedDominoIDs().size() +
        "\n\tdeleted np " + getDeletedNPIDs().size()
        ;
  }
}
