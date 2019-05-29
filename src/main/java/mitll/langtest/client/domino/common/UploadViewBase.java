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
package mitll.langtest.client.domino.common;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Form.SubmitCompleteEvent;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.FormType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Hidden;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.user.BasicDialog;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static com.github.gwtbootstrap.client.ui.constants.LabelType.IMPORTANT;

public class UploadViewBase extends DivWidget {
  private final Logger logger = Logger.getLogger("UploadViewBase");

  private static final String PROJECT_ID = "project-id";
  private static final int WIDTH = 475;
  private static final String HORRIBLE_PREV = "<pre style=\"word-wrap: break-word; white-space: pre-wrap;\">";

  private Form mainForm;
  protected Fieldset fields;

  private FileUpload uploadBox;

  private int user;
  private static final boolean DEBUG = false;

  public interface ResultListener {
    void got(UploadViewBase.UploadResult result);
  }

  public UploadViewBase(int user) {
    this.user = user;
  }

  @NotNull
  protected String getDialogTitle() {
    return "Upload excel file to Domino";
  }

  @NotNull
  protected String getHint() {
    return "Choose an excel file.";
  }

  @NotNull
  protected String getAcceptValue() {
    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  }

  @NotNull
  protected String getService() {
    return "exercise-manager";
  }

  protected void handleFormSubmitSuccess(UploadResult result) {
    result.inform();
  }

  /**
   * @param projectID
   */
  public UploadViewBase init(int projectID) {
    // logger.info("init " + projectID);
    mainForm = new Form();
    //String sUrl = DownloadHelper.toDominoUrl("langtest/exercise-manager");
    mainForm.setAction(DownloadHelper.toDominoUrl("langtest/" +
        getService()));
    mainForm.setEncoding(FormPanel.ENCODING_MULTIPART);
    mainForm.setMethod(FormPanel.METHOD_POST);
    mainForm.setType(FormType.HORIZONTAL);

    fields = new Fieldset();

    addHidden(projectID);

    mainForm.add(fields);
    addFormFields(fields);

    addMainFormSubmitHandler();
    addMainFormSubmitCompleteHandler();

    addInfoLabels();

    mainForm.addStyleName("topFiveMargin");
    add(mainForm);
    return this;
    //	initWidget(mainForm);
  }


  protected void addInfoLabels() {
    Label w = getLabel("Only do this if you're sure the NP_ID column is correct.");
    add(w);
    add(getLabel("Otherwise you may end up with lots of duplicate items that will need to be removed one-by-one."));
    add(getLabel("It's strongly encouraged to edit content in the domino project directly."));
    Label label = getLabel("Try to limit import to short spreadsheets - maybe 500 rows or so.");
    label.setType(IMPORTANT);
    label.addStyleName("topFiveMargin");

    add(label);

    label = getLabel("So please be patient.");
    label.setType(IMPORTANT);
    add(label);
  }

  @NotNull
  private Label getLabel(String text) {
    Label w = new Label(LabelType.WARNING, text);
    w.getElement().getStyle().setFontSize(16, Style.Unit.PX);
    w.getElement().getStyle().setWhiteSpace(Style.WhiteSpace.NORMAL);
    return w;
  }

  protected void addHidden(int projectID) {
    String pidVal = Integer.toString(projectID);
    fields.add(new Hidden(PROJECT_ID, pidVal));
    fields.add(new Hidden("user-id", "" + user));
  }

  private void addFormFields(Fieldset fields) {
    initUploadBox();
    new BasicDialog().addControlGroupEntry(fields, "Upload File", uploadBox, getHint());
  }

  private void initUploadBox() {
    uploadBox = new FileUpload();
    setAcceptOnInput(uploadBox.getElement());
    uploadBox.setName("import-file");
  }

  /**
   * Override to filter for only a certain file type, e.g. audio or video.
   *
   * @param element
   */
  private void setAcceptOnInput(Element element) {
    element.setAttribute("accept", getAcceptValue());
  }


  private DialogHelper dialogHelper;

  public void showModal(DialogHelper.CloseListener closeListener) {
    logger.info("showModal got " + closeListener);
    dialogHelper = new DialogHelper(false) {
      @Override
      protected void afterGotYes(Button closeButton) {
      }
    };
    dialogHelper.show(getDialogTitle(), this, new DialogHelper.CloseListener() {
      @Override
      public boolean gotYes() {

        submitForm();
        if (closeListener != null) {
          closeListener.gotYes();
        }
        return false;
      }

      @Override
      public void gotNo() {
        if (closeListener != null) {
          closeListener.gotNo();
        }
      }

      @Override
      public void gotHidden() {
        if (closeListener != null) {
          closeListener.gotHidden();
        }
      }
    }, 400, WIDTH);
  }


  private boolean validateAndWarn() {
    boolean uploadSuccess = true;//validateFileUpload();
//		if (uploadSuccess) {
//			uploadFields.clearError();
//		}
    return uploadSuccess;
  }

  private void addMainFormSubmitHandler() {
    mainForm.addSubmitHandler(event -> {
      if (!validateAndWarn()) {
        event.cancel();
      }
    });
  }

  private void addMainFormSubmitCompleteHandler() {
    mainForm.addSubmitCompleteHandler(this::handleSubmitComplete);
  }

  private void handleSubmitComplete(SubmitCompleteEvent event) {
    // When the form submission is successfully completed, this event is
    // fired. Assuming the service returned a response of type text/html,
    // we can get the result text here (see the FormPanel documentation for
    // further explanation).
    String results = event.getResults();
    logger.info("handleSubmitComplete got " + results);

    if (results != null) {
      if (results.startsWith("<pre>")) results = results.substring("<pre>".length());
      if (results.startsWith(HORRIBLE_PREV)) results = results.substring(HORRIBLE_PREV.length());
      if (results.endsWith("</pre>")) results = results.substring(0, results.length() - "</pre>".length());

      String[] split = results.split("\\{");
      if (split.length > 1) {
        results = "{" + split[1];
        logger.info("handleSubmitComplete result now " + results);
      }
      JSONObject jsonObj = digestJsonResponse(results);
      if (jsonObj != null) {
        UploadResult result = new UploadResult(jsonObj);
        logger.info("handleSubmitComplete Submission complete " + result);
        dialogHelper.hide();
//      if (!result.success || (result.docId < 0 && result.attId < 0)) {
//        getMsgHelper().makeLoggedInlineMessage("Error on upload!",
//            false, AlertType.ERROR);
//      } else {
        handleFormSubmitSuccess(result);
        // }
      } else {
        dialogHelper.hide();
        handleFormSubmitSuccess(new UploadResult(results.contains("{\"Success\":true")));

//      getMsgHelper().makeLoggedInlineMessage(
//          "Upload failed due to server error!<br/>",
//          false, AlertType.ERROR);
      }
    }
  }

  /**
   * Digest a json response from a servlet checking for a session expiration code
   */
  private JSONObject digestJsonResponse(String json) {
    logger.info("handleSubmitComplete Digesting response '" + json + "'");
    try {
      JSONValue val = JSONParser.parseStrict(json);
      JSONObject obj = (val != null) ? val.isObject() : null;
//			JSONValue code = obj == null ? null : obj.get(Constants.SESSION_EXPIRED_CODE);
//			if (code != null && code.isBoolean() != null && code.isBoolean().booleanValue()) {
//				//getSessionHelper().logoutUserInClient(null, true);
//				return null;
//			} else {
//				return obj;
//			}
      return obj;
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * @see #showModal
   */
  private void submitForm() {
    //   logger.info("submitForm -- ");
    // String pidVal = Integer.toString(getState().getCurrentProject().getId());
    // fields.add(new Hidden(PROJECT_ID, "" + projectID));
    //fields.add(new Hidden(AttachmentUpload.ATT_TYPE_PNM, getRequiredAttachmentType().name()));
    mainForm.submit();
  }

  /**
   * TODO:for audio, get metadata info, save the document
   */
  public class UploadResult {
    private boolean success;

    private int num; // Returned for document attachments.
    private String errMsg;
    private String filePath;
    private int imageID;

    UploadResult(boolean success) {
      this.success = success;
    }

    UploadResult(JSONObject jsonObj) {
      if (jsonObj != null) {
        logger.info("got back " + jsonObj);

        JSONValue jVal = jsonObj.get("Success");
        //   logger.info("Success " + jVal);
        success = (jVal != null && jVal.isBoolean() != null) && jVal.isBoolean().booleanValue();
        //  logger.info("Success " + jVal + " " + success);

        jVal = jsonObj.get("Error");
        errMsg = (jVal != null && jVal.isString() != null) ? jVal.isString().stringValue() : "";

        jVal = jsonObj.get("Num");
        num = (jVal != null && jVal.isNumber() != null) ? (int) jVal.isNumber().doubleValue() : -1;

        jVal = jsonObj.get("FilePath");
        filePath = (jVal != null && jVal.isString() != null) ? jVal.isString().stringValue() : "";


        jVal = jsonObj.get("ImageID");
        imageID = (jVal != null && jVal.isNumber() != null) ? (int) jVal.isNumber().doubleValue() : -1;

        //  logger.info("Parsed result: (1) success=" + success + ", num=" + num + ", errMsg=" + errMsg);

      } else {
        success = false;
      }
      logger.info("Parsed result: success=" + success + ", num=" + num + ", errMsg=" + errMsg);
    }

    void inform() {
      new DialogHelper(false).showErrorMessage("Import Complete!", success ? "Imported " + num + " items" : "Failed to import : " + errMsg);
    }

    public String getFilePath() {
      return filePath;
    }

    public boolean isSuccess() {
      return success;
    }

    public int getNum() {
      return num;
    }

    public String getErrMsg() {
      return errMsg;
    }

    public int getImageID() {
      return imageID;
    }
  }

}
