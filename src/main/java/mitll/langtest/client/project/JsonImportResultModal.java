package mitll.langtest.client.project;

/**
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2016 - 2018. Other request for this document shall
 * be referred to DLIFLC.
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
 * © 2016 - 2018 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import mitll.hlt.domino.client.common.DominoSimpleModal;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static mitll.hlt.domino.server.extern.importers.audio.AudioImportResult.STATUS_FIELD;

public class JsonImportResultModal  {
  private static final Logger log = Logger.getLogger(JsonImportResultModal.class.getName());

  protected static final String NAME = "Name";
  protected static final String DOC_ID = "Doc ID";
  protected static final String IMPORT_SUCCESS_MSG = "Import successful.";
  protected final String importFilename;
  protected final String messageStr;
  protected final JSONObject results;
  protected final String cmt;
  protected final String fileFormat;
  protected final boolean isSuccess;

  public JsonImportResultModal(String title,
                               String message,
                               //ModalSize mType,
                               String cmt,
                               String importFilename, String fileFormat, JSONObject results) {
    //super(title, mType);
    this.cmt = cmt;
    this.importFilename = importFilename;
    this.fileFormat = fileFormat;
    this.isSuccess = message.isEmpty();
    this.messageStr = isSuccess ? IMPORT_SUCCESS_MSG : "Import not processed. " + message;

    this.results = results;
  }

  protected String getString(JSONObject jsonObj, String field) {
    JSONValue jsonValue = jsonObj.get(field);
    if (jsonValue == null) {
      log.warning("no field " + field + " on " + jsonObj);
      return "";
    } else {
      return jsonValue.isString().stringValue();
    }
  }

  protected int getInt(JSONObject jsonObj, String field) {
    return (int) getDouble(jsonObj, field);
  }

  private double getDouble(JSONObject jsonObj, String field) {
    JSONValue jsonValue = jsonObj.get(field);
    if (jsonValue == null) {
      log.warning("no field " + field);
      return 0d;
    } else {
      return jsonValue.isNumber().doubleValue();
    }
  }

/*  @Override
  public void onLoad() {
    Scheduler.get().scheduleDeferred(() -> getCloseButton().setFocus(true));
  }*/

  protected void markRed(Label label) {
    label.getElement().getStyle().setColor("red");
  }

  protected int addCheckOrBlank(Grid g, int row, int col, boolean isFormatted) {
    if (isFormatted) {
      g.setWidget(row, col, new Icon(IconType.OK));
      g.getCellFormatter().addStyleName(row, col, "centered-col");
      col++;
    } else {
      addLabel(g, row, col++, "");
    }

    return col;
  }

  protected IconType getIconType(JSONObject oneResult) {
    String status = getString(oneResult, STATUS_FIELD);
    IconType iType;
    switch (status) {
      case "Success":
        iType = IconType.OK;
        break;
      case "Fail":
        iType = IconType.REMOVE;
        break;
      case "NotImported":
        iType = IconType.CHECK_EMPTY;
        break;
      case "MatchError":
        iType = IconType.EXCLAMATION;
        break;
      default:
        iType = IconType.QUESTION;
        break;
    }
    return iType;
  }

  private int addLabel(Grid g, int row, int col, String string) {
    return addLabel(g, row, col, string, false);
  }

  protected int addLabel(Grid g, int row, int col, String string, boolean isCentered) {
    SafeHtmlBuilder shb = new SafeHtmlBuilder();
    shb.appendEscapedLines(string);
    HTML idlblVal = new HTML(shb.toSafeHtml());
    g.setWidget(row, col, idlblVal);
    if (isCentered) {
      g.getCellFormatter().addStyleName(row, col, "centered-col");
    }
    return ++col;
  }

  protected int addStringColMaybeRed(Grid g, int row, int col, JSONObject oneResult, String field) {
    String string = getString(oneResult, field);
    Label idLbl = new Label(string);
    if (!field.isEmpty()) markRed(idLbl);
    g.setWidget(row, col++, idLbl);
    return col;
  }

  protected int addStringCol(Grid g, int row, int col, JSONObject oneResult, String field) {
    return addStringCol(g, row, col, oneResult, field, false);
  }

  protected int addStringCol(Grid g, int row, int col, JSONObject oneResult, String field, boolean isCentered) {
    String string = getString(oneResult, field);
    return addLabel(g, row, col, string, isCentered);
  }

  protected int addIntCol(Grid g, int row, int col, JSONObject oneResult, String field, boolean isCentered) {
    String string = ""+getInt(oneResult, field);
    return addLabel(g, row, col, string, isCentered);
  }

  protected int addHeader(Grid g, int row, int col, String string) {
    return addLabel(g, row, col, string, true);
  }

  @NotNull
  protected Label getLabel(String messageStr) {
    Label msgLabel = new Label(messageStr);
    msgLabel.addStyleName("bulk-update-modal-msg-label");
    //msgLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
    return msgLabel;
  }
}
