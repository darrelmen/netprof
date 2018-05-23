package mitll.langtest.server.domino;

import mitll.hlt.domino.shared.common.FindOptions;
import mitll.hlt.domino.shared.model.user.DBUser;

import java.util.Date;

public class ImportProjectInfo {
  private String unitName = "";
  private String chapterName = "";
  private int creatorID;
  private String language;
  private long createTime;
  private int dominoProjectID;
  private String name;

  public ImportProjectInfo(String unitName, String chapterName) {
    this.unitName = unitName;
    this.chapterName = chapterName;
  }

  /**
   * @param dominoProjectID
   * @param creatorID
   * @param name
   * @param language
   * @param createTime
   * @see DominoImport#getImportProjectInfos(FindOptions, DBUser)
   */
  public ImportProjectInfo(int dominoProjectID, int creatorID, String name, String language, long createTime) {
    this.dominoProjectID = dominoProjectID;
    this.creatorID = creatorID;
    this.name = name;
    this.language = language;
    this.createTime = createTime;
  }

  public String getUnitName() {
    return unitName;
  }

  public String getChapterName() {
    return chapterName;
  }

  public int getCreatorID() {
    return creatorID;
  }

  public void setCreatorID(int creatorID) {
    this.creatorID = creatorID;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(long createTime) {
    this.createTime = createTime;
  }

  public void setDominoProjectID(int dominoProjectID) {
    this.dominoProjectID = dominoProjectID;
  }

  int getDominoProjectID() {
    return dominoProjectID;
  }

  Date getCreateDate() {
    return new Date(createTime);
  }

  public String getName() {
    return name;
  }

  public void setUnitName(String unitName) {
    this.unitName = unitName;
  }

  void setChapterName(String chapterName) {
    this.chapterName = chapterName;
  }

  public String toString() {
    return dominoProjectID + " : " + name + " : " + getLanguage() + " creator " + getCreatorID() + " at " + getCreateDate() + " unit " + getUnitName() + " chapt " + getChapterName();
  }
}
