package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.CommonExercise;

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
   *
   * @param exportTime
   * @paramx updateTime consider using this somehow
   * @param exercises
   * @param language
   * @param lang
   * @param dominoID
   * @see #readExercises
   */
  public ImportInfo(Date exportTime,
                    //   Date updateTime,
                    List<CommonExercise> exercises,
                    String language,
                    mitll.langtest.shared.project.Language lang,
                    int dominoID) {
    this.createTime = exportTime;
   // this.modifiedTime = updateTime;
    this.exercises = exercises;
    this.language = language;
    this.lang = lang;
    this.dominoID = dominoID;
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
