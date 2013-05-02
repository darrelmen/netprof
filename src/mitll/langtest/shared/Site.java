package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 2/13/13
 * Time: 2:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class Site implements IsSerializable {
  public long id;
  public String name = null;
  public String language;
  public long creatorID;
  private String creator;
  public String notes;
  public String exerciseFile;
  public String savedExerciseFile;
  public String savedExerciseFileName;

  public transient Collection<Exercise> exercises; // i.e. don't serialize
  public Exercise example; //  don't write to db
  private String feedback;
  private boolean deployed;
  private long creationDate;
  public String creationDateReadable;

  public Site() {}

  /**
   * @see mitll.langtest.server.database.SiteDAO#addSite
   * @see mitll.langtest.server.database.SiteDAO#getSites()
   * @param id
   * @param creatorID
   * @param name
   * @param language
   * @param notes
   * @param exerciseFile
   * @param filePath
   * @param savedFileName
   * @param feedback
   * @param deployed
   * @param timestamp
   */
  public Site(long id, long creatorID, String name, String language, String notes, String exerciseFile, String filePath, String savedFileName,
              String feedback, boolean deployed, long timestamp,
              String creationDateReadable) {
    this.id = id;
    this.creatorID = creatorID;
    this.name = name;
    this.language = language;
    this.notes = notes;
    this.exerciseFile = exerciseFile;
    this.savedExerciseFile = filePath;
    this.feedback = feedback;
    this.deployed = deployed;
    this.creationDate = timestamp;
    this.creationDateReadable = creationDateReadable;
    this.savedExerciseFileName = savedFileName;
  }

  public Collection<Exercise> getExercises() { return exercises; }
  public void setExercises(Collection<Exercise> exercises) {this.exercises=exercises; if (!exercises.isEmpty()) example = exercises.iterator().next(); }

  public String getFeedback() {
    return feedback;
  }

  public void setFeedback(String feedback) {
    this.feedback = feedback;
  }

  public boolean isDeployed() {
    return deployed;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }


  public String toString() { return "id " + id + " name " +name + " lang " +language + " creatorID " +creatorID +
      " notes " +notes + " file " +exerciseFile + " path "+ savedExerciseFile;}


/*  public void setDeployed(boolean deployed) {
    this.deployed = deployed;
  }*/
}
