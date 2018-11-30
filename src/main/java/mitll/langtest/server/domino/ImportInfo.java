package mitll.langtest.server.domino;

import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static mitll.langtest.server.database.exercise.Project.MANDARIN;

/**
 * @see ProjectSync#addPending
 */
public class ImportInfo {
  private final Date createTime;
  private final List<CommonExercise> changedExercises;
  private final List<CommonExercise> addedExercises;
  private final Collection<Integer> deletedDominoIDs;
 // private final Map<String, Integer> npToDomino;
  private Set<String> deletedNPIDs;

  private final String language;
  private final mitll.langtest.shared.project.Language lang;

  private final int dominoID;

  /**
   * @param importProjectInfo
   * @param changedExercises
   * @param deletedNPIDs
   * @see mitll.langtest.server.database.exercise.DominoExerciseDAO#readExercises(int, ImportProjectInfo, DominoImport.ChangedAndDeleted, boolean)
   */
  public ImportInfo(ImportProjectInfo importProjectInfo,
                    List<CommonExercise> addedExercises,
                    List<CommonExercise> changedExercises,
                    Collection<Integer> deletedDominoIDs,
                    Set<String> deletedNPIDs ) {
    this(
        importProjectInfo.getDominoProjectID(),
        importProjectInfo.getLanguage(),
        importProjectInfo.getCreateDate(),

        addedExercises, changedExercises,
        deletedDominoIDs//,
    //    npToDomino
    );

    this.deletedNPIDs = deletedNPIDs;
  }

  /**
   * @param dominoID
   * @param language
   * @param exportTime
   * @param changed
   */
  private ImportInfo(int dominoID,
                     String language,
                     Date exportTime,
                     List<CommonExercise> addedExercises,
                     List<CommonExercise> changed,
                     Collection<Integer> deletedDominoIDs
  //    , Map<String, Integer> npToDomino
  ) {
    this.createTime = exportTime;
    this.addedExercises = addedExercises;
    this.changedExercises = changed;
    this.language = language;
    this.lang = getLanguage(language);
    this.dominoID = dominoID;
    this.deletedDominoIDs = deletedDominoIDs;
   // this.npToDomino = npToDomino;
  }

  @NotNull
  private mitll.langtest.shared.project.Language getLanguage(String languageName) {
    mitll.langtest.shared.project.Language lang = mitll.langtest.shared.project.Language.UNKNOWN;
    try {
      if (languageName.equalsIgnoreCase(MANDARIN)) languageName = Language.MANDARIN.name();
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

  /**
   * @see ProjectSync#doDelete
   * @return
   */
  Set<String> getDeletedNPIDs() {
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
        //+
        //"\n\tnp->domino " + getNpToDomino().size()
        ;
  }
/*
  public Map<String, Integer> getNpToDomino() {
    return npToDomino;
  }
*/
}
