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
//  private boolean hasProjectSpecificAudio = false;
  private String search = "";
  private boolean includeAudio;

  /**
   * @param hasProjectSpecificAudio
   * @see DownloadServlet#getAudioExportOptions
   */
  public AudioExportOptions(boolean hasProjectSpecificAudio) {
    /*this.hasProjectSpecificAudio = hasProjectSpecificAudio;*/
  }

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

  public String getInfo() {
    return
        "_" + (justMale ? "male" : "female") + "_" +
            (justRegularSpeed ? "regular" : "slow") + "_" +
            (justContext ? "context" : "vocab");
  }

  public String toString() {
    return "options " +
        getInfo() + " " +
        (isUserList ? "user list" : "predef");
  }

  public void setIncludeAudio(boolean includeAudio) {
    this.includeAudio = includeAudio;
  }

  public boolean getIncludeAudio() {
    return includeAudio;
  }
}
