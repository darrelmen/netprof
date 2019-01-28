/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.project.ProjectType;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/30/16.
 */
public class FilterRequest implements IsSerializable {
  private int reqID = 1;
  private List<Pair> typeToSelection = new ArrayList<>();
  private String prefix = "";
  private int limit = -1;
  private int userListID = -1;
  private boolean recordRequest = false;
  private boolean exampleRequest = false;
  private boolean onlyUninspected = false;
  private boolean onlyWithAnno = false;
  //private ProjectType projectType = ProjectType.DEFAULT;
  private ProjectMode mode = ProjectMode.VOCABULARY;

  public FilterRequest() {
  }

  /**
   * @param reqID
   * @param pairs
   * @param userListID
   * @see mitll.langtest.client.list.FacetExerciseList#getExerciseListRequest
   */
  public FilterRequest(int reqID, List<Pair> pairs, int userListID) {
    this.reqID = reqID;
    this.typeToSelection = pairs;
    this.userListID = userListID;
  }

  public void addPair(Pair pair) {
    Optional<Pair> first = typeToSelection.stream().filter(pair1 -> pair1.property.equalsIgnoreCase(pair.getProperty())).findFirst();

    first.ifPresent(pair1 -> typeToSelection.remove(pair1));

    this.typeToSelection.add(pair);
  }

  public void addPair(String p, String v) {
    addPair(new Pair(p, v));
  }

  public boolean isNoFilter() {
    return limit == -1 &&
        typeToSelection.isEmpty() &&
        prefix.isEmpty();
  }

  /**
   * TODO something less error prone
   *
   * @param other
   * @return
   */
  public boolean sameAs(FilterRequest other) {
    return prefix.equals(other.getPrefix()) &&
        typeToSelection.equals(other.getTypeToSelection());
  }

  public int getReqID() {
    return reqID;
  }

  public List<Pair> getTypeToSelection() {
    return typeToSelection;
  }

  /**
   * @param typeToSelection
   * @return
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercisesUsingPrefix
   */
  public FilterRequest setTypeToSelection(List<Pair> typeToSelection) {
    this.typeToSelection = typeToSelection;
    return this;
  }

  public String getPrefix() {
    return prefix;
  }

  public FilterRequest setPrefix(String prefix) {
    this.prefix = prefix;
    return this;
  }


  public int getLimit() {
    return limit;
  }

  public FilterRequest setLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public int getUserListID() {
    return userListID;
  }

  public boolean isRecordRequest() {
    return recordRequest;
  }

  /**
   * @param recordRequest
   * @return
   * @see mitll.langtest.client.custom.recording.RecordingFacetExerciseList#getFilterRequest
   */
  public FilterRequest setRecordRequest(boolean recordRequest) {
    this.recordRequest = recordRequest;
    return this;
  }

  public boolean isExampleRequest() {
    boolean exampleRequest = this.exampleRequest;
  /*  if (!exampleRequest) {
      for (Pair pair : typeToSelection) {
        if (pair.getProperty().equalsIgnoreCase("CONTENT")) {
          exampleRequest = pair.getValue().startsWith("Sentences");

          break;
        }
      }
      ;
    }*/
    return exampleRequest;
  }

  /**
   * @param exampleRequest
   * @return
   * @see mitll.langtest.client.custom.recording.RecordingFacetExerciseList#getFilterRequest
   */
  public FilterRequest setExampleRequest(boolean exampleRequest) {
    this.exampleRequest = exampleRequest;
    return this;
  }

  public boolean isOnlyUninspected() {
    return onlyUninspected;
  }

  public FilterRequest setOnlyUninspected(boolean onlyDefaultAudio) {
    this.onlyUninspected = onlyDefaultAudio;
    return this;
  }

  public boolean isOnlyWithAnno() {
    return onlyWithAnno;
  }

  public FilterRequest setOnlyWithAnno(boolean onlyWithAnno) {
    this.onlyWithAnno = onlyWithAnno;
    return this;
  }

  public void prune() {
    Map<String, String> pv = new LinkedHashMap<>();
    List<Pair> typeToSelection = getTypeToSelection();
    typeToSelection.forEach(pair -> pv.put(pair.getProperty(), pair.getValue()));
    typeToSelection.clear();
    pv.forEach((k, v) -> typeToSelection.add(new Pair(k, v)));
  }

//  public ProjectType getProjectType() {
//    return projectType;
//  }
//
//  public FilterRequest setProjectType(ProjectType projectType) {
//    this.projectType = projectType;
//    return this;
//  }

  public ProjectMode getMode() {
    return mode;
  }

  public FilterRequest setMode(ProjectMode mode) {
    this.mode = mode;
    return this;
  }

  /**
   * @return
   */
  public String toString() {
    return
        (userListID == -1 ? "" : "userListID =" + userListID) +
            (limit == -1 ? "" : "limit '" + limit + "'") +
            (prefix.isEmpty() ? "" : "prefix '" + prefix + "' ") +
            (recordRequest ? "recordRequest " : "") +
            (onlyUninspected ? "onlyUninspected " : "") +
            (onlyWithAnno ? "onlyWithAnno " : "") +
            //(projectType != ProjectType.DEFAULT ? projectType : "") +
            (mode != ProjectMode.VOCABULARY ? mode : "") +
            (getTypeToSelection().isEmpty() ? "" : "\n\tselection " + getTypeToSelection());
  }
}
