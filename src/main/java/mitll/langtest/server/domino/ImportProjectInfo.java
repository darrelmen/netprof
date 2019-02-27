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

import mitll.hlt.domino.shared.common.FindOptions;
import mitll.hlt.domino.shared.model.user.DBUser;
import mitll.langtest.shared.project.Language;

import java.util.Date;

public class ImportProjectInfo {
  private String unitName = "";
  private String chapterName = "";
  private int creatorID;
  private String language;
  private long createTime;
  private int dominoProjectID;
  private final String name;

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
    return dominoProjectID + " : " + name + " : " + getLanguage() + " creator " + getCreatorID() +
        " at " + getCreateDate() + " unit " + getUnitName() + " chapt " + getChapterName();
  }
}
