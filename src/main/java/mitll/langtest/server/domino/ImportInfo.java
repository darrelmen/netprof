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

package mitll.langtest.server.domino;

import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static mitll.langtest.server.database.project.Project.MANDARIN;

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
