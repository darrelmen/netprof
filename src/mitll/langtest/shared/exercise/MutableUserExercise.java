package mitll.langtest.shared.exercise;

/**
 * Created by go22670 on 2/1/16.
 */
public interface MutableUserExercise {
  void setID(String id);

  long getCreator();

  void setCreator(long id);

  void setUniqueID(long uniqueID);

  long getUniqueID();

  boolean isPredefined();

  boolean checkPredef();
}
