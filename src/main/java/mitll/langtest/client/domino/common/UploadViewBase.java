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

import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.FileUpload;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.Form.SubmitCompleteEvent;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.FormType;
import com.google.gwt.dom.client.Element;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Hidden;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.user.BasicDialog;

import java.util.logging.Logger;

public class UploadViewBase extends DivWidget {
  public static final String PROJECT_ID = "project-id";
  private final Logger logger = Logger.getLogger("UploadViewBase");

//	protected static final Logger log = Logger.getLogger(UploadViewBase.class.getName());

  //	private DominoSaveableModal modal;
  private Form mainForm;
  private Fieldset fields;

  //protected DecoratedFields uploadFields;
  private FileUpload uploadBox;
//  protected TextBox descBox;

//  protected String modalBtnNm = "Upload";
//  protected String modalTitle = "Upload File";

  //	protected ModalSize mType = ModalSize.Small;
  //private  Modal modal;
  private int projectID;
  private int user;

  public UploadViewBase(int projectID, int user) {
    this.user = user;

    init(projectID);
  }

  protected void handleFormSubmitSuccess(UploadResult result) {
    result.inform();
  }

  private void init(int projectID) {
    this.projectID = projectID;
    mainForm = new Form();
    String sUrl = DownloadHelper.toDominoUrl("langtest/exercise-manager");
    mainForm.setAction(sUrl);
    mainForm.setEncoding(FormPanel.ENCODING_MULTIPART);
    mainForm.setMethod(FormPanel.METHOD_POST);
    mainForm.setType(FormType.HORIZONTAL);

    fields = new Fieldset();

    addHidden(projectID);

    mainForm.add(fields);
    addFormFields(fields);

    addMainFormSubmitHandler();
    addMainFormSubmitCompleteHandler();

    add(mainForm);
    //	initWidget(mainForm);
  }

  private void addHidden(int projectID) {
    String pidVal = Integer.toString(projectID);
    fields.add(new Hidden(PROJECT_ID, pidVal));
    fields.add(new Hidden("user-id", "" + user));
  }

  private void addFormFields(Fieldset fields) {
    initUploadBox();

    new BasicDialog().addControlGroupEntry(fields, "Upload File", uploadBox, "Choose an excel file.");
//    fields.add(addControlGroupEntryNoLabel(this, new Label("Upload File")));

//		uploadFields = new DecoratedFields("Upload File", uploadBox);
//		fields.add(uploadFields.getCtrlGroup());
//		createAddTooltip(uploadFields.getCtrlGroup(),
//				getUploadFileTip(getRequiredAttachmentType()), Placement.BOTTOM);
//		addDescription(fields);
  }

//  private ControlGroup addControlGroupEntryNoLabel(HasWidgets dialogBox, Widget widget) {
//    final ControlGroup userGroup = new ControlGroup();
//    userGroup.addStyleName("leftFiveMargin");
//    widget.addStyleName("leftFiveMargin");
//
//    userGroup.add(widget);
//    dialogBox.add(userGroup);
//    return userGroup;
//  }

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
    element.setAttribute("accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  }
//
//	protected void addDescription(Fieldset fields) {
//		descBox = new TextBox();
//		descBox.setName(AttachmentUpload.ATT_DESC_PNM);
//		fields.add(new DecoratedFields("File Description",
//						descBox).getCtrlGroup());
//	}

//	protected String getUploadFileTip(AttachmentType attType) {
//		return "Upload a file (" + attType.getExtensionsString() + ")";
//	}

  DialogHelper dialogHelper;

  public void showModal() {
//		modal = new DominoSaveableModal(true, modalTitle, modalBtnNm,
//				getModalType(), this) {
//			@Override protected void handleSave() {
//				submitForm();
//			}
//		};
//		modal.init();

    dialogHelper = new DialogHelper(false);
    dialogHelper.show("Upload excel file to Domino", this, new DialogHelper.CloseListener() {
      @Override
      public boolean gotYes() {
        submitForm();
        return false;
      }

      @Override
      public void gotNo() {

      }

      @Override
      public void gotHidden() {

      }
    }, 400, 400);
  }

//	protected ModalSize getModalType() {
//		return mType;
//	}

  private boolean validateAndWarn() {
    boolean uploadSuccess = true;//validateFileUpload();
//		if (uploadSuccess) {
//			uploadFields.clearError();
//		}
    return uploadSuccess;
  }

//	private boolean validateFileUpload() {
//		CommonValidation cValidation = new CommonValidation();
//		CommonValidation.Result result = cValidation.validateAttachment(
//				getRequiredAttachmentType(), uploadBox.getFilename());
//		if (result.errorFound) {
//			uploadFields.setError(result.message);
//		}
//
//		return !result.errorFound;
//	}

  private static final String[] EXCEL_TYPES =
      {"xlsx", "xls"};

//	public boolean validateAttachment( String filename) {
//		if (filename == null) {
//			return new CommonValidation.Result("No filename provided: " + filename);
//		}
//		String[] validExts = EXCEL_TYPES;
//		for (int i = 0; i < validExts.length; i++) {
//			if (filename.trim().toLowerCase().endsWith(validExts[i])) {
//				return new CommonValidation.Result(); // success
//			}
//		}
//		log.warning("Invalid attachment filename: " + filename);
//		return new CommonValidation.Result("File type must be " + aType.getExtensionsString());
//	}

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
    if (results.startsWith("<pre>")) results = results.substring("<pre>".length());
    if (results.endsWith("</pre>")) results = results.substring(0, results.length() - "</pre>".length());

    JSONObject jsonObj = digestJsonResponse(results);
    if (jsonObj != null) {
      UploadResult result = new UploadResult(jsonObj);
      logger.info("Submission complete " + result);
      dialogHelper.hide();
//      if (!result.success || (result.docId < 0 && result.attId < 0)) {
//        getMsgHelper().makeLoggedInlineMessage("Error on upload!",
//            false, AlertType.ERROR);
//      } else {
      handleFormSubmitSuccess(result);
      // }
    } else {
      dialogHelper.hide();
//      getMsgHelper().makeLoggedInlineMessage(
//          "Upload failed due to server error!<br/>",
//          false, AlertType.ERROR);
    }
  }

  /**
   * Digest a json response from a servlet checking for a session expiration code
   */
  private JSONObject digestJsonResponse(String json) {
    logger.info("Digesting response " + json);
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
    logger.info("submitForm -- ");
    // String pidVal = Integer.toString(getState().getCurrentProject().getId());
    // fields.add(new Hidden(PROJECT_ID, "" + projectID));
    //fields.add(new Hidden(AttachmentUpload.ATT_TYPE_PNM, getRequiredAttachmentType().name()));
    mainForm.submit();
  }

  /**
   * TODO:for audio, get metadata info, save the document
   */
  class UploadResult {
    public boolean success;

    public int num; // Returned for document attachments.
    //
//		public final int attId; // Returned for project/exam attachments
    public String errMsg;

    //		public final String attFilename;
//		public final String attContentType;
//		private final JSONObject audioMetadata;
//		public final double attSize;
//
    UploadResult(JSONObject jsonObj) {
      if (jsonObj != null) {
        JSONValue jVal = jsonObj.get("Success");
        success = (jVal != null && jVal.isBoolean() != null) && jVal.isBoolean().booleanValue();
        jVal = jsonObj.get("Error");
        errMsg = (jVal != null && jVal.isString() != null) ? jVal.isString().stringValue() : "";

        jVal = jsonObj.get("Num");
        num = (jVal != null && jVal.isNumber() != null) ? (int) jVal.isNumber().doubleValue() : -1;
//				jVal = jsonObj.get(AttachmentUpload.ATT_ID_RVAL);
//				attId = (jVal != null && jVal.isNumber() != null) ? (int)jVal.isNumber().doubleValue() : -1;
//				jVal = jsonObj.get(AttachmentUpload.ATT_FILENAME_RVAL);
//				attFilename = (jVal != null && jVal.isString() != null) ? jVal.isString().stringValue() : "";
//				jVal = jsonObj.get(AttachmentUpload.ATT_CONTENT_TYPE_RVAL);
//				attContentType = (jVal != null && jVal.isString() != null) ? jVal.isString().stringValue() : "";
//				jVal = jsonObj.get(AttachmentUpload.ATT_CONTENT_SIZE_RVAL);
//				attSize = (jVal != null && jVal.isNumber() != null) ? jVal.isNumber().doubleValue() : -1.0;
//
//				audioMetadata = jsonObj.get(AttachmentUpload.AUDIO_METADATA).isObject();
      } else {
        success = false;
//				docId = -1;
//				attId = -1;
//				errMsg = "Unknown Error";
//				attContentType = "";
//				attFilename = "";
//				attSize = -1.0;
//				audioMetadata = new JSONObject();

      }
      logger.info("Parsed result: success=" + success + ", num=" + num + ", errMsg=" + errMsg);

    }

    public void inform() {
      new DialogHelper(false).showErrorMessage("Import Complete!", success ? "Imported " + num + " items" : "Failed to import : " + errMsg);

    }

    /**
     * @see OpenDocumentUploadViewBase#addAudioMetadata
     * @return
     */
//		public JSONObject getAudioMetadata() {
//			return audioMetadata;
//		}
  }

}
