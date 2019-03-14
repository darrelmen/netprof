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
