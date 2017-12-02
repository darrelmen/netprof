package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.CommonExercise;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class ImportInfo {
  private final Date createTime;
  //  private final Date modifiedTime;
  private final List<CommonExercise> exercises;

  private final String language;
  private final mitll.langtest.shared.project.Language lang;

  private final int dominoID;

  /**
   * @param dominoID
   * @param language
   * @param exportTime
   * @param exercises
   * @paramx lang
   * @paramx updateTime consider using this somehow
   * @see #readExercises
   */
  private ImportInfo(int dominoID,
                     String language,

                     Date exportTime,
                     //   Date updateTime,
                     List<CommonExercise> exercises) {
    this.createTime = exportTime;
    // this.modifiedTime = updateTime;
    this.exercises = exercises;
    this.language = language;
    this.lang = getLanguage(language);
    this.dominoID = dominoID;
  }

  public ImportInfo(ImportProjectInfo importProjectInfo, List<CommonExercise> exercises) {
    this(
        importProjectInfo.getDominoProjectID(),
        importProjectInfo.getLanguage(),
        importProjectInfo.getCreateDate(),
        exercises);
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

  public List<CommonExercise> getExercises() {
    return exercises;
  }

/*
  public String getLanguage() {
    return language;
  }
*/

/*
  public mitll.langtest.shared.project.Language getLang() {
    return lang;
  }
*/

  /**
   * @return
   */
  public int getDominoID() {
    return dominoID;
  }

/*
  public Date getModifiedTime() {
    return modifiedTime;
  }
*/

  public String toString() {
    return "lang " + language + "/" + lang + " " + getDominoID() + " " + getExportTime() + " num " + getExercises().size();
  }
}
