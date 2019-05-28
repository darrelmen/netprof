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

package mitll.langtest.server.domino;

import com.google.gson.JsonObject;
import mitll.hlt.domino.server.extern.importers.ImportResult;
import mitll.hlt.domino.shared.Constants;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.services.MyRemoteServiceServlet;
import mitll.langtest.shared.common.DominoSessionException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static mitll.langtest.server.audio.HTTPClient.UTF_8;

public class ExcelUpload {
  private static final Logger logger = LogManager.getLogger(ExcelUpload.class);

  public void doExcelUpload(HttpServletRequest request,
                            HttpServletResponse response,
                            MyRemoteServiceServlet serviceServlet,
                            IProjectManagement projectManagement) {
    logger.info("doExcelUpload : " +
        "\n\tRequest " + request.getQueryString() +
        "\n\tpath    " + request.getPathInfo());

    int projId = -1;
    int userID = -1;
    FileItem docImportAttachment = null;

    DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(fileItemFactory);
//
//    ServletContext servletContext = this.getServletConfig().getServletContext();
//    File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
//    if (repository == null) {
//      logger.warn("no repo for temp files?");
//    } else {
//      logger.info("tmpdir is " + repository.getAbsolutePath());
//      fileItemFactory.setRepository(repository);
//    }

    try {
      List<FileItem> items = upload.parseRequest(request);

      for (FileItem item : items) {
        String fieldName = item.getFieldName();
        String fieldValue = item.getString();
        String name = item.getName();

        logger.info("doExcelUpload field '" + fieldName + "' = " + fieldValue.length() + " bytes : '" + name + "'");

        if (Constants.PROJ_ID_PNM.equals(fieldName)) {
          projId = Integer.parseInt(fieldValue);
        } else if (Constants.Import.SRC_FILE_PNM.equals(fieldName)) {
          if (!name.isEmpty()) {
            docImportAttachment = item;
          }
        } else if ("user-id".equalsIgnoreCase(fieldName)) {
          userID = Integer.parseInt(fieldValue);
        }
      }

      int sessionUserID = serviceServlet.getSessionUserID();

      logger.info("doExcelUpload service : " +
          "\n\tprojId              " + projId +
          "\n\tsessionUserID       " + sessionUserID +
          "\n\tuserID              " + userID +
          "\n\tdocImportAttachment " + docImportAttachment);

      if (projId == -1) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Success", false);
        jsonObject.addProperty("Error", "no project specified?");
        sendResponse(response, jsonObject.toString());
      } else if (docImportAttachment == null) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Success", false);
        jsonObject.addProperty("Error", "no import excel file specified?");
        sendResponse(response, jsonObject.toString());
      } else {
        Project project = serviceServlet.getProject(projId);
        int dominoid = project.getProject().dominoid();
        if (dominoid == -1) {
          JsonObject jsonObject = new JsonObject();
          jsonObject.addProperty("Success", false);
          jsonObject.addProperty("Error", "no domino id set for project.");
          sendResponse(response, jsonObject.toString());
        } else {
          ImportResult b = projectManagement.doDominoImport(dominoid, docImportAttachment, project.getTypeOrder(), userID);
          JsonObject jsonObject = new JsonObject();
          jsonObject.addProperty("Success", b.isSuccess());
          jsonObject.addProperty("Error", "None");
          jsonObject.addProperty("Num", b.getImportedDocs().size());
          sendResponse(response, jsonObject.toString());
        }
      }
    } catch (FileUploadException e) {
      logger.error("got error uploading " + e, e);

      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("Success", false);
      jsonObject.addProperty("Error", "FileUploadException");
      sendResponse(response, jsonObject.toString());
    } catch (DominoSessionException dse) {
      logger.info("session exception " + dse, dse);

      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("Success", false);
      jsonObject.addProperty("Error", "DominoSessionException");
      sendResponse(response, jsonObject.toString());
    }
  }

  private void sendResponse(HttpServletResponse response, String outJSON) {
    logger.info("Sending JSON response: " + outJSON);
    try {
      response.setContentType("application/json; charset=UTF-8");
      response.setCharacterEncoding(UTF_8);

      response.getWriter().write(outJSON);
    } catch (Exception ex) {
      logger.error("Exception writing response", ex);
    }
  }
}
