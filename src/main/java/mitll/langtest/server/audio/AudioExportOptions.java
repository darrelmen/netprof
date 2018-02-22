package mitll.langtest.server.audio;

/**
 * Created by go22670 on 6/5/17.
 */
public class AudioExportOptions {
  private boolean justMale = false;
  private boolean justRegularSpeed = true;
  private boolean justContext = false;
//  private boolean allContext = false;
  private boolean isUserList = false;
  private boolean skip = false;
  private boolean hasProjectSpecificAudio = false;

  /**
   * @see mitll.langtest.server.DownloadServlet#getAudioExportOptions
   * @param hasProjectSpecificAudio
   */
  public AudioExportOptions(boolean hasProjectSpecificAudio) {
    this.hasProjectSpecificAudio=hasProjectSpecificAudio;
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

  public void setSkip(boolean skip) {
    this.skip = skip;
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

  boolean isUserList() {
    return isUserList;
  }

  public void setUserList(boolean userList) {
    this.isUserList = userList;
  }


/*
  public boolean isAllContext() {
    return allContext;
  }
*/

  /**
   * @paramx justContext
   * @see mitll.langtest.server.DownloadServlet#getAudioExportOptions
   */
/*  public void setAllContext(boolean justContext) {
    this.allContext = justContext;
  }*/

  public boolean isHasProjectSpecificAudio() {
    return hasProjectSpecificAudio;
  }

  public void setHasProjectSpecificAudio(boolean hasProjectSpecificAudio) {
    this.hasProjectSpecificAudio = hasProjectSpecificAudio;
  }

  public String toString() {
    return "options " +
        getInfo() + " " +
        (isUserList ? "user list" : "predef");
  }

  public String getInfo() {
//    if (isAllContext()) {
//      return "";
//    } else {
      return skip || isUserList ?
          "" :
          "_" + (justMale ? "male" : "female") + "_" +
              (justRegularSpeed ? "regular" : "slow") + "_" +
              (justContext ? "context" : "vocab");
//    }
  }
}
