package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DominoUpdateItem implements HasID {

  private int dominoID;
  private String netprofID;
  private int id;

  private String english;
  private String foreignLanguage;
  private List<String> changedFields = new ArrayList<>();

  private ITEM_STATUS status;

  public enum ITEM_STATUS implements IsSerializable {ADD, CHANGE, DELETE}

  public DominoUpdateItem() {
  }

  public DominoUpdateItem(CommonExercise commonExercise, List<String> changedFields, ITEM_STATUS status) {
    this(commonExercise.getID(), commonExercise.getDominoID(), commonExercise.getOldID(), commonExercise.getEnglish(), commonExercise.getForeignLanguage(), changedFields, status);
  }

  public DominoUpdateItem(int id, int dominoID, String netprofID, String english, String foreignLanguage, List<String> changedFields, ITEM_STATUS status) {
    this.id = id;
    this.dominoID = dominoID;
    this.netprofID = netprofID;
    this.english = english;
    this.foreignLanguage = foreignLanguage;
    this.changedFields = changedFields;
    this.status = status;
  }

  @Override
  public int getID() {
    return dominoID;
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    return Integer.compare(getID(), o.getID());
  }


  public int getDominoID() {
    return dominoID;
  }

  public String getNetprofID() {
    return netprofID;
  }

  public int getId() {
    return id;
  }

  public String getEnglish() {
    return english;
  }

  public String getForeignLanguage() {
    return foreignLanguage;
  }

  public List<String> getChangedFields() {
    return changedFields;
  }

  public DominoUpdateItem addChangedField(String message) {
    changedFields.add(message);
    return this;
  }

  public ITEM_STATUS getStatus() {
    return status;
  }

  public String toString() {
    return "exid " + id + " " + english + "/" + foreignLanguage + " : " + status;
  }
}
