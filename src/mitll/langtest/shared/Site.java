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
  public String notes;
  public String exerciseFile;
  public String savedExerciseFile;

  public transient Collection<Exercise> exercises; // i.e. don't serialize
  public Exercise example; //  don't write to db
  private String feedback; //  don't write to db

  public Site() {}
  public Site(long id, long creatorID, String name, String language, String notes, String exerciseFile) {
    this.id = id;
    this.creatorID = creatorID;
    this.name = name;
    this.language = language;
    this.notes = notes;
    this.exerciseFile = exerciseFile;
  }

  public Collection<Exercise> getExercises() { return exercises; }
  public void setExercises(Collection<Exercise> exercises) {this.exercises=exercises; if (!exercises.isEmpty()) example = exercises.iterator().next(); }

  public String getFeedback() {
    return feedback;
  }

  public void setFeedback(String feedback) {
    this.feedback = feedback;
  }
  public String toString() { return "id " + id + " name " +name + " lang " +language + " creatorID " +creatorID + " notes " +notes + " file " +exerciseFile;}
}
