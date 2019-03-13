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

package mitll.langtest.server.audio;

import mitll.langtest.server.DownloadServlet;

/**
 * Created by go22670 on 6/5/17.
 */
public class AudioExportOptions {
  private boolean justMale = false;
  private boolean justRegularSpeed = true;
  private boolean justContext = false;
  private boolean isUserList = false;
  private String info;

  private String search = "";
  private boolean includeAudio;

  /**
   * @param hasProjectSpecificAudio
   * @see DownloadServlet#getAudioExportOptions
   */
//  public AudioExportOptions(boolean hasProjectSpecificAudio) {
//    /*this.hasProjectSpecificAudio = hasProjectSpecificAudio;*/
//  }
  public void setJustMale(boolean justMale) {
    this.justMale = justMale;
  }

  boolean isJustMale() {
    return justMale;
  }

  public void setJustRegularSpeed(boolean justRegularSpeed) {
    this.justRegularSpeed = justRegularSpeed;
  }

  boolean isJustRegularSpeed() {
    return justRegularSpeed;
  }

  /**
   * @return
   */
  boolean isJustContext() {
    return justContext;
  }

  public void setJustContext(boolean justContext) {
    this.justContext = justContext;
  }

  public boolean isUserList() {
    return isUserList;
  }

  public void setUserList(boolean userList) {
    this.isUserList = userList;
  }

/*
  public boolean isHasProjectSpecificAudio() {
    return hasProjectSpecificAudio;
  }

  public void setHasProjectSpecificAudio(boolean hasProjectSpecificAudio) {
    this.hasProjectSpecificAudio = hasProjectSpecificAudio;
  }
*/

  public void setSearch(String search) {
    this.search = search;
  }

  public String getSearch() {
    return search;
  }

  public AudioExportOptions setIncludeAudio(boolean includeAudio) {
    this.includeAudio = includeAudio;
    return this;
  }

  public boolean getIncludeAudio() {
    return includeAudio;
  }

  public String getInfo() {
    return
        "_" + (info == null ?
            ((justMale ? "male" : "female") + "_" +
                (justRegularSpeed ? "regular" : "slow") + "_" +
                (justContext ? "context" : "vocab")) :
            info);
  }

  public String toString() {
    return "options " +
        getInfo() + " " +
        (isUserList ? "user list" : "predef");
  }

  public void setInfo(String info) {
    this.info = info;
  }
}
