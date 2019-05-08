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

package mitll.langtest.server.database.project;

import mitll.hlt.domino.server.extern.importers.ImportResult;
import mitll.hlt.domino.server.extern.importers.vocab.ExcelVocabularyImporter;
import mitll.hlt.domino.shared.common.ImportMode;
import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.DocumentRevision;
import mitll.hlt.domino.shared.model.HeadDocumentRevision;
import mitll.hlt.domino.shared.model.document.*;
import mitll.hlt.domino.shared.model.metadata.MetadataTypes;
import mitll.hlt.domino.shared.model.project.ClientPMProject;
import mitll.hlt.domino.shared.model.task.Task;
import mitll.hlt.domino.shared.model.task.TaskStatus;
import mitll.hlt.domino.shared.model.taskspec.TaskSpecification;
import mitll.hlt.domino.shared.util.DateFormatter;
import mitll.langtest.server.database.exercise.Facet;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.domino.IDominoImport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static java.util.Collections.emptySet;
import static mitll.hlt.domino.shared.model.metadata.MetadataTypes.ImportMetadata.*;

/**
 *
 */
class MyExcelVocabularyImporter extends ExcelVocabularyImporter {
  private static final Logger logger = LogManager.getLogger(MyExcelVocabularyImporter.class);
  private static final Logger log = logger;

  //  private ProjectManagement projectManagement;
  private Collection<String> typeOrder;
  private static final Map<String, String> autoPopulateFields;

  MyExcelVocabularyImporter(ProjectManagement projectManagement, int defaultUser, ClientPMProject clientPMProject, Collection<String> typeOrder,
                            IDominoImport dominoImport,
                            IUserDAO userDAO) {
    super(dominoImport.getDominoContext(), userDAO.lookupDominoUser(defaultUser), clientPMProject);
    //  this.projectManagement = projectManagement;
    this.typeOrder = typeOrder;
  }

  @Override
  public ImportResult importDocument(Command cmd) {
    if (!(cmd instanceof MyVocabularyImportCommand)) {
      return new ImportResult("Invalid command type!");
    }

    MyVocabularyImportCommand tdtCmd = (MyVocabularyImportCommand) cmd;
    try {

      logger.info("type order " + typeOrder);
      ExcelReader excelReader = new ExcelReader(tdtCmd.getFileName(), getDominoContext(), tdtCmd);
      Iterator<String> iterator = typeOrder.iterator();

      if (iterator.hasNext()) {
        String next = iterator.next();
        if (next.equalsIgnoreCase(Facet.SEMESTER.toString())) {
          next = iterator.next(); //skip it
        }
        excelReader.setUnitColumnHeader(next);
      }
      if (iterator.hasNext()) {
        excelReader.setChapter(iterator.next());
      }
      Collection<VocabularyItem> content1 = excelReader.getContent();

      //				OptionSpecification unitOrder = proj.getWorkflow().getOptionSpec(UNIT_ORDER_OPTION);
      //			log.info("After " + unitOrder + " : " + unitOrder.getChildOptionStrings());
      Collection<HeadDocumentRevision> headDocumentRevisions =
          importAllDirect(getCurrentUser(), tdtCmd.getImportMode(), content1);

      ImportResult importResult = new ImportResult();
      for (HeadDocumentRevision documentRevision : headDocumentRevisions)
        importResult.addImportedDoc(documentRevision);
      return importResult;
    } catch (Exception ex) {
      logger.error("Encountered exception reading file " + tdtCmd.getFileName(), ex);
      return new ImportResult("Unknown exception encountered!");
    }
  }

  private Collection<HeadDocumentRevision> importAllDirect(mitll.hlt.domino.shared.model.user.User user, ImportMode iMode, Collection<VocabularyItem> content1) {
    Date now = new Date();
    int c = 0;
    int failures = 0;
    int imported = 0;

    int n = content1.size();

    List<HeadDocumentRevision> importedDocs = new ArrayList<>();

    logger.info(" importing ---- " + n + " items ");

    for (VocabularyItem item : content1) {
      boolean success = importDoc(user, iMode, now, importedDocs, item);
      if (success) imported++;
      else failures++;

      //if (c > MAX) break;
      if (c++ % 100 == 0) logger.info("did " + c);
    }

    if (failures > 0) logger.error("failed to import " + failures + "/" + n);
    else logger.info("imported " + imported + "/" + n + " items");

    return importedDocs;
  }

  private boolean importDoc(mitll.hlt.domino.shared.model.user.User user, ImportMode iMode, Date now, List<HeadDocumentRevision> importedDocs, IDocument doc) {
    try {
      //log.info(i + " START ---- " + item.getString(EN) + " ----------------- ");
      ImportCommand cmd = new ImportCommand(user, now, null, iMode);
      ImportResult importResult = addDocument(cmd, doc, null, MetadataTypes.VocabularyMetadata.V_NP_ID, null);
      if (importResult.isSuccess()) {
        //imported++;
        importedDocs.add(importResult.getOnlyImportedDoc());
        return true;
      } else {
//					if (failures++ < 10) log.error("couldn't import doc " + testItem + " : " + importResult.getErrorMessage());
        return false;
      }
      //	log.info(i + " END   ---- " + item.getString(EN) + " ----------------- ");
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }
    return false;
  }

  /**
   * Add a document to the project according to the import command.
   *
   * @param cmd           The details of the add.
   * @param iDoc          The document to import.
   * @param title         The document title.
   * @param itemJoinField The field to join with existing documents on. (HubId, or ImportUID).
   * @param convertFLO    When true, convert FLO from paradox archive naming conventions.
   * @param workingDoc    The existing document to update (may be null).
   * @return The result.
   * @throws Exception
   */
  protected ImportResult addDocument(ImportCommand cmd, IDocument iDoc, String title, String itemJoinField,
                                     boolean cleanHtml, boolean convertFLO, HeadDocumentRevision workingDoc) throws Exception {
    Date start = cmd.getStartDate();
    Date end = cmd.getEndDate();
    if (start != null && end != null && end.getTime() == start.getTime()) {
      Calendar c = Calendar.getInstance();
      c.setTime(start);
      c.add(Calendar.MILLISECOND, +100);
      end = c.getTime();
    }
    populateImportStep(iDoc, convertFLO);

    String err = checkForImportErrors(workingDoc, iDoc, cmd);
    if (err != null) {
      log.warn("Import aborted! File parsed, but error detected: " + err);
      return new ImportResult(err);
    }

    String langCode = getProject().getContent().getLanguageCode();
    IDocument existingIDoc = (workingDoc != null) ? workingDoc.getDocument() : null;
    log.info("Have existing doc id {}: doc {}", (workingDoc != null ? workingDoc.getId() : -1), workingDoc);

    String attMsg = cmd.getAttachmentDescription();
    if (attMsg == null || attMsg.isEmpty()) {
      String taskStr = "";
      TaskSpecification spec = getProject().getWorkflow().getTaskSpec(cmd.getFirstTaskName());
      if (spec != null) {
        taskStr = " - During " + spec.getLongName();
      }

      attMsg = "Imported file " + taskStr;
    }
    Date changeDate = (start != null) ? start : end;

/*    if (iDoc instanceof TestItem && (existingIDoc == null || existingIDoc instanceof TestItem)) {
      log.info("Add attachment for TestItem. type=" + iDoc.getClass().getSimpleName() +
          " existingDoc type=" + ((existingIDoc != null) ? existingIDoc.getClass() : "Null"));
      TestItem importTIDoc = (TestItem)iDoc;
      TestItem existingTIDoc = (TestItem)existingIDoc;
      String sourceAudioFileName = cmd.sourceAudioFileName;
      boolean hasSourceAudio = sourceAudioFileName != null && sourceAudioFileName.trim().length() > 0;
      if (hasSourceAudio) {
        // Mime types could be probed here as in the code
        // below, but issues exist on mac in jdk 1.7. Instead
        // explicitly look up the value.
        //
        //Path audioFileP = FileSystems.getDefault().getPath(audioFilename);
        //String mimeType = Files.probeContentType(audioFileP);
        CAttachment cAttachment = addAttachment(importTIDoc, attMsg, changeDate, IDocumentComposite.SOURCE_ATT_C_NM,
            sourceAudioFileName, cmd.newSourceAudioFileName, AttachmentType.Audio);
        File attFile = getDSDelegate().getAttachmentManager().toLocalFile(cAttachment.getFilename());
        new AudioInfo(attFile.toPath()).addAudioMetadata(cAttachment, false);
      }

      String onlineAudioFileName = cmd.onlineAudioFileName;
      if (onlineAudioFileName != null && onlineAudioFileName.trim().length() > 0) {
        // Mime types could be probed here as in the code
        // below, but issues exist on mac in jdk 1.7. Instead
        // explicitly look up the value.
        //
        //Path audioFileP = FileSystems.getDefault().getPath(audioFilename);
        //String mimeType = Files.probeContentType(audioFileP);
        CAttachment cAttachment = addAttachment(importTIDoc, attMsg, changeDate, TestItem.ONLINE_AUDIO_SECT_NM,
            onlineAudioFileName, cmd.newOnlineAudioFileName, AttachmentType.Audio);
        File attFile = getDSDelegate().getAttachmentManager().toLocalFile(cAttachment.getFilename());
        new AudioInfo(attFile.toPath()).addAudioMetadata(cAttachment, true);
      }

      if (cmd.getFileName() != null) {
        String docAttCName = IDocumentComposite.SOURCE_ATT_C_NM;
        if (existingIDoc != null) {
          docAttCName = getAttachmentCName(cmd.getUserDBID(), existingTIDoc);
        } else if (hasSourceAudio) {
          docAttCName = getAttachmentCName(cmd.getUserDBID(), importTIDoc);
        }
        addAttachment(importTIDoc, attMsg, changeDate, docAttCName, cmd.getFileName(),
            cmd.newFileName, AttachmentType.Document);
      }
    } else {
      log.info("Skip attachment add for non TestItem. type=" + iDoc.getClass().getSimpleName() +
          " existingDoc type=" + ((existingIDoc != null) ? existingIDoc.getClass() : "Null"));
    }*/

    HeadDocumentRevision savedDoc = null;
    String taskCmt = cmd.getRevDescription();
    if (taskCmt == null || taskCmt.isEmpty()) {
      if (cmd.getFileName() != null) {
        String localFilename = getLocalFilename(cmd.getFileName());
        taskCmt = "Imported file " + localFilename;
      } else {
        taskCmt = "Imported from file";
      }
    }
    if (existingIDoc == null) {
      log.info("Calling add for new document");
      // Add when document already exists.
      savedDoc = getDSDelegate().addDocument(getProject(), cmd.getUser(),
          changeDate, title, iDoc, cleanHtml, cmd.getRevDescription());
      if (start != null) {
        log.info("Starting task without existing item");
        for (String startTName : cmd.getTaskNames()) {
          SResult<HeadDocumentRevision> result = getWfDelegate().startTask(getProject(), cmd.getUser(), savedDoc,
              taskCmt, startTName, start, true);
          if (result.isSuccess()) {
            savedDoc = result.get();
          } else {
            log.warn("Attempt to Start task {} failed for document {}! Error: {}",
                startTName, savedDoc.getId(), result.getResponseMessage());
            return new ImportResult(result);
          }
        }
      } else {
        log.warn("Start date not defined");
      }
    } else {
      log.info("Run merge with existing document {}", workingDoc.getId());
      // Merge and save when document already exists.
      if (start != null) {
        log.info("Starting task with existing item for {}", existingIDoc.getName());
        for (String startTName : cmd.getTaskNames()) {
          SResult<HeadDocumentRevision> result = getWfDelegate().startTask(getProject(), cmd.getUser(), workingDoc,
              taskCmt, startTName, start, true);
          if (result.isSuccess()) {
            workingDoc = result.get();
          } else {
            log.warn("Task '{}' could not be started!", startTName);
            return new ImportResult(result);
          }
        }
      } else {
        log.warn("Start date not defined");
      }

/*
      if (iDoc instanceof TestItem && existingIDoc instanceof TestItem) {
        // when doing a normal - non-item import, merge the exiting document
        // into the imported document. This effectively pulls any
        // metadata, attachments, and questions in the original document that aren't
        // in the imported document, but overwrites the existing document
        // content (passage, etc) with that imported
        TestItem existingTI = (TestItem)existingIDoc;
        TestItem iTI = (TestItem)iDoc;
        log.info("Cass update on {} with join field {}", workingDoc.getId(), itemJoinField);
        updateDocument(cmd.getImportMode(), itemJoinField, existingTI, iTI);
      } else {
        log.error("Can not merge documents for invalid types existing=" +
            existingIDoc.getClass() + ", new=" + iDoc.getClass());
      }*/

      if (cmd.getImportMode() != ImportMode.ItemsOnly) {
        workingDoc.setDocument(iDoc);
        // Update the title.
        workingDoc.setName(title);
      } else {
        workingDoc.setDocument(existingIDoc);
      }
      Date originalTS = workingDoc.getUpdateTime();
      workingDoc.setUpdateTime(changeDate);
      workingDoc.setUpdater(cmd.getUser());
      log.info("Saving document revision with existing document {}", workingDoc.getId());

      // TODO support exam related set during import.
      savedDoc = getDSDelegate().saveDocument(getProject(), getCurrentUser(), workingDoc, changeDate,
          originalTS, true, true, taskCmt, langCode, cleanHtml, emptySet(), true);

      if (savedDoc != null) {
        ImportResult errResult = new ImportResult("Document imported, but unknown revision errors found.<br/>" +
            "Item #" + savedDoc.getProjItemNum() + " Version " + savedDoc.getVersionNum());
        // Validate by looking up the saved revision
        try {
          DocumentRevision rev = getDSDelegate().getDocumentRevisionFromVersionNum(
              getProject(), getCurrentUser(), savedDoc.getId(), savedDoc.getVersionNum());
          if (rev == null) {
            log.warn(errResult.getErrorMessage());
            return errResult;
          }
        } catch (Exception ex) {
          log.error(errResult.getErrorMessage(), ex);
          return errResult;
        }
      } else {
        return new ImportResult("Unknown save failure. Item #" +
            workingDoc.getProjItemNum() + " Version " +
            workingDoc.getVersionNum());
      }
    }
    if (savedDoc != null) {
      if (end != null) {
        int projItemNum = savedDoc.getProjItemNum();
        int versNum = savedDoc.getVersionNum();
        for (String completeTName : cmd.getTaskNames()) {
          log.info("Completing task {}", completeTName);
          SResult<HeadDocumentRevision> result = getWfDelegate().updateTask(getProject(), cmd.getUser(),
              savedDoc, taskCmt, completeTName, end, TaskStatus.Completed, true);
          if (result.isSuccess()) {
            savedDoc = result.get();
          } else {
            return new ImportResult("Document #" + projItemNum + " version " + versNum +
                " imported with errors<br>" + result.getResponseMessage());
          }
        }
      }
      return new ImportResult(savedDoc);
    }
    return new ImportResult("Document save error!");
  }

  private void populateImportStep(IDocument doc, boolean convertFLO) {
//		log.info("Populate Import Before");
//		doc.getMetadataFields().forEach(mf -> log.info("     ( " + mf.getName() + ", " + mf.getDisplayValue() + " )"));
    autoPopulateFields.entrySet().forEach(fEnt -> {
      IMetadataField importStepField = doc.getMetadataField(fEnt.getKey());
      IMetadataField metaStepField = doc.getMetadataField(fEnt.getValue());

//      log.info("Populate metadata {} -> {} : {}", fEnt.getKey(), fEnt.getValue(), importStepField);
      if (importStepField instanceof CFloatMetadata) {
        float val = ((CFloatMetadata) importStepField).getValue();
        // handle issue #1091. Importing from paradox archive typically has 0 as ILR level which
        // invalid. Import without change in import step. Change to undefined in metadata step.
        if (I_ILR.equals(fEnt.getKey()) && val == 0.0) {
          val = MetadataTypes.NO_ILR_VAL;
        }
        if (metaStepField == null) {
          metaStepField = new CFloatMetadata(fEnt.getValue(), val);
          doc.addMetadataField(metaStepField);
        } else {
          ((CFloatMetadata) metaStepField).setValue(val);
        }
      } else if (importStepField instanceof CStringMetadata) {
        String val = ((CStringMetadata) importStepField).getValue();
        MetadataTypes.SkillType docSkill = getDocHelper().getSkill(doc);
        String mappedVal = getOptionMapper().getMappedValue(docSkill, Document.DOC_C_NM, fEnt.getValue(), val);

        if (metaStepField == null) {
          metaStepField = new CStringMetadata(fEnt.getValue(), mappedVal);
          doc.addMetadataField(metaStepField);
        } else {
          ((CStringMetadata) metaStepField).setValue(mappedVal);
        }
      } else if (importStepField instanceof CDateMetadata) {
        CDateMetadata dm = (CDateMetadata) importStepField;
        if (metaStepField == null) {
          metaStepField = new CDateMetadata(fEnt.getValue(), dm.getValue(), dm.isTimeIncluded());
          doc.addMetadataField(metaStepField);
        } else {
          CDateMetadata mf = (CDateMetadata) metaStepField;
          mf.setValue(dm.getValue());
          mf.setTimeIncluded(dm.isTimeIncluded());
        }
      } else if (importStepField instanceof CBooleanMetadata) {
        boolean val = ((CBooleanMetadata) importStepField).getValue();
        if (metaStepField == null) {
          metaStepField = new CBooleanMetadata(fEnt.getValue(), val);
          doc.addMetadataField(metaStepField);
        } else {
          ((CBooleanMetadata) metaStepField).setValue(val);
        }
      } else if (importStepField instanceof CIntMetadata) {
        int val = ((CIntMetadata) importStepField).getValue();
        if (metaStepField == null) {
          metaStepField = new CIntMetadata(fEnt.getValue(), val);
          doc.addMetadataField(metaStepField);
        } else {
          ((CIntMetadata) metaStepField).setValue(val);
        }
      }
    });
//		log.info("Populate Import After");
//		doc.getMetadataFields().forEach(mf -> log.info("     ( " + mf.getName() + ", " + mf.getDisplayValue() + " )"));
  }
  /**
   * Check to ensure the document is consistent with the existing
   * document, as well as the arguments specified in the command. For example,
   * looks for the addition of a CR document onto an existing MC document, and
   * validates revision dates to ensure the date requestd is not before the
   * last revision.
   *
   * @return Return null if document is consistent otherwise, returns
   * a description of the inconsistency.
   */
  private String checkForImportErrors(HeadDocumentRevision existingDoc, IDocument iDoc, ImportCommand cmd) {
    MetadataTypes.SkillType newSkill = getDocHelper().getSkill(iDoc);
    MetadataTypes.TestItemType newIType = getDocHelper().getItemType(iDoc);
    String err = checkForProjectErrors(newSkill, newIType);
//    if (err == null) {
//      err = checkForAudioSkillMismatch(existingDoc, newSkill, cmd);
//    }
    if (err == null) {
      err = checkForDateErrors(existingDoc, cmd);
    }
    return err;
  }

  private String checkForProjectErrors(MetadataTypes.SkillType itemSkill, MetadataTypes.TestItemType itemIType) {
    MetadataTypes.SkillType projSkill = getProject().getContent().getSkill();
    MetadataTypes.TestItemType projIType = getProject().getContent().getTestItemType();
    if (projSkill != MetadataTypes.SkillType.Mixed && projSkill != itemSkill) {
      return "The document has skill " + itemSkill +
          ", but this project requires " + projSkill + " documents!";
    }
    if (projIType != MetadataTypes.TestItemType.Mixed && projIType != itemIType) {
      return "The document is " + itemIType.getDisplayName() +
          ", but this project requires " + projIType.getDisplayName() + " items!";
    }
    return null;
  }

  private String checkForDateErrors(HeadDocumentRevision existingDoc, Command cmd) {
    String err = checkCommandInternalDateErrors(cmd);

    if (err == null && existingDoc != null) {
      if (existingDoc.getUpdateTime().getTime() > cmd.getStartDate().getTime()) {
        err = "The desired task start date was before the last update update (" +
            DateFormatter.get().formatDateTime(existingDoc.getUpdateTime()) +
            ") to document #" + existingDoc.getProjItemNum();
      } else {
        for (String tName : cmd.getTaskNames()) {
          Task existingTask = existingDoc.getDStatus().getTask(tName);
          err = getWfDelegate().isValidActionDate(existingTask, cmd.getStartDate());
          if (err != null && !err.isEmpty()) {
            return err;
          }
        }
      }
    }

    return err;
  }

  private String checkCommandInternalDateErrors(Command cmd) {
    String err = null;
    Date now = new Date();

    if (cmd.getEndDate() != null && cmd.getStartDate().after(cmd.getEndDate())) {
      err = "The task start date was after the end date";
    }
    if (err == null && now.before(cmd.getStartDate())) {
      err = "The task start date was after the current time";
    }
    if (err == null && cmd.getEndDate() != null && now.before(cmd.getEndDate())) {
      err = "The task end date was after the current time";
    }
    return err;
  }

  static {
    autoPopulateFields = new HashMap<>();
    autoPopulateFields.put(I_ILR, MetadataTypes.CommonMetadata.ILR);
    autoPopulateFields.put(I_TOPIC, MetadataTypes.CommonMetadata.TOPIC);
    autoPopulateFields.put(I_FLO, MetadataTypes.CommonMetadata.FLO);
    autoPopulateFields.put(I_BATCH_ID, MetadataTypes.CommonMetadata.BATCH_ID);
    //autoPopulateFields.put(I_TEXT_TYPE, CommonMetadata.TEXT_TYPE);
    autoPopulateFields.put(I_SRC_CITATION, MetadataTypes.CommonMetadata.SRC_CITATION);
    autoPopulateFields.put(I_COUNTRY, MetadataTypes.CommonMetadata.SRC_COUNTRY);
  }
}
