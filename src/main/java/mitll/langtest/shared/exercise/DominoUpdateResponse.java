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
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @see mitll.langtest.client.project.ProjectChoices#showImportDialog
 * @see mitll.langtest.server.services.ProjectServiceImpl#addPending
 * @since 3/30/16.
 */
public class DominoUpdateResponse implements HasID {
  private Map<String, String> props = new HashMap<>();
  private int dominoID;
  private int currentDominoID;
  private UPLOAD_STATUS status;
  private String message;

  private List<DominoUpdateItem> updates;

  @Override
  public int getID() {
    return dominoID;
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    return Integer.compare(getID(), o.getID());
  }

  public enum UPLOAD_STATUS implements IsSerializable {SUCCESS, FAIL, WRONG_PROJECT, ANOTHER_PROJECT;}

  public DominoUpdateResponse() {
  }

  /**
   * @param success
   * @param dominoID
   * @param currentDominoID
   * @param props
   * @param updates
   */
  public DominoUpdateResponse(UPLOAD_STATUS success,
                              int dominoID,
                              int currentDominoID,
                              Map<String, String> props,
                              List<DominoUpdateItem> updates
  ) {
    this.status = success;
    this.dominoID = dominoID;
    this.currentDominoID = currentDominoID;
    this.props = props;
    this.updates = updates;
  }

  public Map<String, String> getProps() {
    return props;
  }

  public void setProps(Map<String, String> props) {
    this.props = props;
  }

  public int getDominoID() {
    return dominoID;
  }

  public void setDominoID(int dominoID) {
    this.dominoID = dominoID;
  }

  public int getCurrentDominoID() {
    return currentDominoID;
  }

  public UPLOAD_STATUS getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<DominoUpdateItem> getUpdates() {
    return updates;
  }

  public String toString() {
    return "response " + dominoID + " " + currentDominoID + " " + props;
  }
}
