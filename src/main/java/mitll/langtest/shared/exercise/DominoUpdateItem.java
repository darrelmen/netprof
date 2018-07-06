package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DominoUpdateItem implements HasID {
  private int dominoID;
  private String netprofID;
  private int id;
  private int parent = -1;

  private String english;
  private String foreignLanguage;
  private List<String> changedFields = new ArrayList<>();

  private ITEM_STATUS status;


  public enum ITEM_STATUS implements IsSerializable {ADD, CHANGE, DELETE}

  public DominoUpdateItem() {
  }

  public DominoUpdateItem(ClientExercise commonExercise,   ITEM_STATUS status) {
    this(commonExercise.getID(), commonExercise.getDominoID(), commonExercise.getOldID(), commonExercise.getEnglish(), commonExercise.getForeignLanguage(), new ArrayList<>(), status);
  }
  /**
   * @param commonExercise
   * @param changedFields
   * @param status
   * @see mitll.langtest.server.domino.ProjectSync#getDominoUpdateResponse
   * @see mitll.langtest.server.domino.ProjectSync#getNewAndChangedContextExercises
   */
  public DominoUpdateItem(ClientExercise commonExercise, List<String> changedFields, ITEM_STATUS status) {
    this(commonExercise.getID(), commonExercise.getDominoID(), commonExercise.getOldID(), commonExercise.getEnglish(), commonExercise.getForeignLanguage(), changedFields, status);
  }

  public DominoUpdateItem(ClientExercise commonExercise, String changedField, ITEM_STATUS status) {
    this(commonExercise.getID(), commonExercise.getDominoID(), commonExercise.getOldID(), commonExercise.getEnglish(), commonExercise.getForeignLanguage(), new ArrayList<>(), status);
    changedFields.add(changedField);
  }

  public DominoUpdateItem(int id, int dominoID, String netprofID, String english, String foreignLanguage,
                          List<String> changedFields, ITEM_STATUS status) {
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

  public int getExerciseID() {
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


  public int getParent() {
    return parent;
  }

  public DominoUpdateItem setParent(int parent) {
    this.parent = parent;
    return this;
  }

  public DominoUpdateItem setParent(CommonExercise context) {
    this.parent = context.getParentExerciseID();
    return this;
  }

  public boolean isContext() {
    return parent > -1;
  }

  public String toString() {
    return
        "update" +
            "\n\texid      " + id +
            "\n\tdominoID  " + dominoID +
            "\n\tnetprofID " + netprofID +
            "\n\tparent    " + parent +
            "\n\teng       " + english +
            "\n\tfl        " + foreignLanguage +
            "\n\tstatus    " + status +
            "\n\tchanges   " + changedFields;
  }
}
