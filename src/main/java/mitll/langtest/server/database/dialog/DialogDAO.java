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

package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.SlickAudioDAO;
import mitll.langtest.server.database.project.IProject;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogStatus;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.*;
import mitll.npdata.dao.dialog.DialogAttributeJoinDAOWrapper;
import mitll.npdata.dao.dialog.DialogDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class DialogDAO extends DAO implements IDialogDAO {
  private static final Logger logger = LogManager.getLogger(DialogDAO.class);

  //  private static final long MIN = 60 * 1000L;
//  private static final long HOUR = 60 * MIN;
//  private static final long DAY = 24 * HOUR;
//  public static final long YEAR = 365 * DAY;
  private static final String FLTITLE = "fltitle";

  private final DialogDAOWrapper dao;

  private final DatabaseImpl databaseImpl;
  private final DialogAttributeJoinHelper dialogAttributeJoinHelper;

  /**
   * @param database
   * @param dbConnection
   * @see DatabaseImpl#initializeDAOs
   */
  public DialogDAO(Database database,
                   DBConnection dbConnection,
                   DatabaseImpl databaseImpl) {
    super(database);
    dao = new DialogDAOWrapper(dbConnection);
    this.databaseImpl = databaseImpl;

    dialogAttributeJoinHelper = new DialogAttributeJoinHelper(new DialogAttributeJoinDAOWrapper(dbConnection));
  }

  /**
   * @param defaultUser
   * @return
   * @see DatabaseImpl#finalSetup(SlickAudioDAO)
   */
  public int ensureDefault(int defaultUser) {
    SlickDialog defaultProject = getDefaultDialog();
    if (defaultProject == null) {
      addDefault(defaultUser);
      defaultProject = getDefaultDialog();
      return defaultProject == null ? -1 : defaultProject.id();
    } else {
      return defaultProject.id();
    }
  }

  private void addDefault(int defaultUser) {
    long now = System.currentTimeMillis();
    add(defaultUser,
        databaseImpl.getProjectDAO().getDefault(),
        -1,
        1,
        now,
        now,
        "", "",
        DialogType.DEFAULT,
        DialogStatus.DEFAULT,
        "",
        ""
    );
  }

  private SlickDialog getDefaultDialog() {
    Collection<SlickDialog> aDefault = dao.getDefault();
    return (aDefault.isEmpty()) ? null : aDefault.iterator().next();
  }

  /**
   * join with attributes = meta data from domino
   *
   * join with exercises, in order
   *
   * TODO : join with aggregate score
   *
   * join with images
   *
   * @param projid
   * @return
   */
  @Override
  public List<IDialog> getDialogs(int projid) {
    List<IDialog> dialogs = new ArrayList<>();
    Map<Integer, Dialog> idToDialog = new HashMap<>();

    getByProjID(projid).forEach(slickDialog -> {
      Dialog e = makeDialog(slickDialog);
      dialogs.add(e);
      idToDialog.put(slickDialog.id(), e);
    });

    // add dialog attributes
    Map<Integer, ExerciseAttribute> idToPair = databaseImpl.getUserExerciseDAO().getExerciseAttribute().getIDToPair(projid);

    logger.info("getDialogs got " + idToPair.size() + " attributes for project #" + projid);

    Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated =
        databaseImpl.getUserExerciseDAO().getRelatedExercise().getDialogIDToRelated(projid);

    Map<Integer, List<SlickRelatedExercise>> dialogIDToCoreRelated =
        databaseImpl.getUserExerciseDAO().getRelatedCoreExercise().getDialogIDToRelated(projid);

    {
      IProject project = databaseImpl.getIProject(projid);

      dialogAttributeJoinHelper.getAllJoinByProject(projid).forEach((dialogID, slickDialogAttributeJoins) -> {
        configureDialog(projid,  idToDialog.get(dialogID), idToPair, dialogIDToRelated, dialogIDToCoreRelated, project, dialogID, slickDialogAttributeJoins);
      });
    }

    return dialogs;
  }

  private void configureDialog(int projid,
                               Dialog dialog,
                               Map<Integer, ExerciseAttribute> idToPair,
                               Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated,
                               Map<Integer, List<SlickRelatedExercise>> dialogIDToCoreRelated,
                               IProject project,
                               Integer dialogID,
                               Collection<SlickDialogAttributeJoin> slickDialogAttributeJoins) {
    // add attributes
    addAttributes(idToPair, slickDialogAttributeJoins, dialog);

    {
      List<ExerciseAttribute> fltitle =
          dialog.getAttributes()
              .stream()
              .filter(exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase(FLTITLE)).collect(Collectors.toList());
      if (!fltitle.isEmpty()) dialog.getMutableShell().setForeignLanguage(fltitle.iterator().next().getValue());
    }

    //add exercises
    addExercises(projid, dialogIDToRelated, dialogID, dialog);

    // add core vocab
    addCoreVocab(dialogIDToCoreRelated.get(dialogID), project, dialog);

    // add images
    addImage(projid, dialog);
  }

  private Collection<SlickDialog> getByProjID(int projid) {
    return dao.byProjID(projid);
  }

  /**
   * @see #configureDialog(int, Dialog, Map, Map, Map, IProject, Integer, Collection)
   * @param relatedExercises
   * @param project
   * @param dialog
   */
  private void addCoreVocab(List<SlickRelatedExercise> relatedExercises, IProject project, Dialog dialog) {
    if (relatedExercises != null) {
      Set<CommonExercise> uniq = new HashSet<>();

      relatedExercises.forEach(slickRelatedExercise ->
          uniq
              .add(project
                  .getExerciseByID(slickRelatedExercise.exid()))
      );

      List<CommonExercise> inOrder = new ArrayList<>(uniq);
      inOrder.sort(Comparator.comparing(CommonShell::getForeignLanguage));
      dialog.getCoreVocabulary().addAll(inOrder);
    }
  }

  /**
   * @see #getDialogs(int)
   * @param slickDialog
   * @return
   */
  private Dialog makeDialog(SlickDialog slickDialog) {
    return new Dialog(
        slickDialog.id(),
        slickDialog.userid(),
        slickDialog.projid(),
        slickDialog.imageid(),
        slickDialog.dominoid(),
        slickDialog.modified().getTime(),
        slickDialog.unit(),
        slickDialog.lesson(),
        slickDialog.orientation(),
        "",
        "",
        slickDialog.entitle(),
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        getDialogType(slickDialog),
        database.getProject(slickDialog.projid()).getProject().countrycode());
  }

  /**
   * @see #makeDialog(SlickDialog)
   * @param slickDialog
   * @return
   */
  @Nullable
  private DialogType getDialogType(SlickDialog slickDialog) {
    String kind = slickDialog.kind();
    DialogType dialogType = null;
    try {
      dialogType = DialogType.valueOf(kind);
    } catch (IllegalArgumentException e) {
      logger.warn("getDialogType got unknown type " + kind);
    }
    return dialogType;
  }

  /**
   * @param idToPair
   * @param slickDialogAttributeJoins
   * @param dialog
   * @see #getDialogs
   */
  private void addAttributes(Map<Integer, ExerciseAttribute> idToPair,
                             Collection<SlickDialogAttributeJoin> slickDialogAttributeJoins,
                             Dialog dialog) {
    slickDialogAttributeJoins.forEach(slickDialogAttributeJoin ->
    {
      ExerciseAttribute e = idToPair.get(slickDialogAttributeJoin.attrid());
//          logger.info("adding attribute #" + attrid + " = " + e);

      if (e == null) {
        logger.error("addAttributes no attr for id #" + slickDialogAttributeJoin.attrid());
      } else {
        dialog.getAttributes().add(e);
      }
    });
  }

  /**
   * TODO: For now, don't do a check for existences for images
   *
   * @param projid
   * @param dialog
   */
  private void addImage(int projid, Dialog dialog) {
    List<SlickImage> all = databaseImpl.getImageDAO().getAllNoExistsCheck(projid);
//    logger.warn("addImage got " + all.size());

    Map<Integer, String> idToImageRef = new HashMap<>();
    all.forEach(slickImage -> idToImageRef.put(slickImage.id(), slickImage.filepath()));
    //  logger.warn("idToImageRef got " + idToImageRef.size());
    int imageid = dialog.getImageid();//dialog.getSlickDialog().imageid();
    if (imageid < 1) {
      logger.warn("no image for dialog " + dialog);
    } else {
      String s = idToImageRef.get(imageid);
      if (s == null) {
        logger.warn("no image by " + imageid +
            "for dialog " + dialog);
      } else {
        dialog.setImageRef(s);
      }
    }
  }

  /**
   * Add exercises to dialog.
   *
   * @param projid
   * @param dialogIDToRelated
   * @param dialogID
   * @param dialog
   */
  private void addExercises(int projid,
                            Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated,
                            Integer dialogID,
                            Dialog dialog) {
    List<SlickRelatedExercise> slickRelatedExercises = dialogIDToRelated.get(dialogID);

    if (slickRelatedExercises != null) {
      List<CommonExercise> exercises = new ArrayList<>();
      Set<Integer> candidate = new HashSet<>();
//      logger.info("addExercises got " + slickRelatedExercises.size() + " relations for " + dialogID);
      Map<Integer, CommonExercise> idToEx = new HashMap<>();

      slickRelatedExercises.forEach(slickRelatedExercise -> {
//        logger.info("addExercises relation " + slickRelatedExercise);
        int exid = slickRelatedExercise.exid();
        CommonExercise exercise = idToEx.get(exid);

        if (exercise == null) {
          exercise = databaseImpl.getExercise(projid, exid);

          if (exercise != null) {
            logger.info("addExercises ex #" + exercise.getID() + " " + exercise.getForeignLanguage() + " -> tokens : " + exercise.getTokens());
            idToEx.put(exid, exercise = new Exercise(exercise));
          }
        }

        if (exercise != null) {
          int childid = slickRelatedExercise.contextexid();
          CommonExercise childEx = idToEx.get(childid);

          if (childEx == null && childid != exid) {
            CommonExercise childExOrig = databaseImpl.getExercise(projid, childid);
            idToEx.put(childid, childEx = new Exercise(childExOrig));
          }

          if (childEx == null || childid == exid) {
            logger.info("Skip relation " + childid + " on " + exercise);
          } else {
            exercise.getDirectlyRelated().add(childEx);
            childEx.getMutable().setParentExerciseID(exercise.getParentExerciseID());

            if (!candidate.contains(exercise.getID())) {
              exercises.add(exercise);
              candidate.add(exercise.getID());
            }
            if (!candidate.contains(childEx.getID())) {
              exercises.add(childEx);
              candidate.add(childEx.getID());
            }
          }
        } else {
          logger.warn("can't find related ex " + exid);
        }
      });

      //  logger.info("got exercises  " + exercises.size());
      //  logger.info("got candidates " + candidate.size() + " relations for " + dialogID + " : " + candidate);

      {
//        List<CommonExercise> firstEx = exercises
//            .stream()
//            .filter(commonExercise -> candidate.contains(commonExercise.getID()))
//            .collect(Collectors.toList());

        int size = exercises.size();
        if (size == 0) {
          logger.error("huh no exercises?");
        }
//      else if (size == 1) {
        //   } else if (size == 2) logger.warn("not expecting multiple parents " + firstEx);

        exercises.forEach(current -> dialog.getExercises().add(current));
      }
    }

    String message = "dialog " + dialog.getID() + " " + dialog.getUnit() + " " + dialog.getChapter() +
        " has " + dialog.getExercises().size() + " exercises.";

    if (dialog.getExercises().isEmpty()) {
      logger.warn(message);
    } else {
//      logger.info(message);
    }
  }

  /**
   * @param id
   * @see mitll.langtest.server.database.project.DialogPopulate#cleanDialog
   */
  @Override
  public void removeForProject(int id) {
    dao.removeByProjid(id);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * @param userid
   * @param dominoID
   * @param modified
   * @param status
   * @return
   * @see #ensureDefault
   * @see #addDefault(int)
   * @see mitll.langtest.server.database.project.DialogPopulate#addDialogs(Project, Project, IDialogDAO, Map, int, DialogType, Map, boolean)
   */
  @Override
  public int add(int userid,
                 int projid,
                 int dominoID,
                 int imageID,

                 long modified,
                 long lastimport,
                 String unit,
                 String lesson,
                 DialogType kind,
                 DialogStatus status,
                 String entitle,
                 String orientation
  ) {
    return dao.insert(new SlickDialog(
        -1,
        userid,
        projid,
        dominoID,
        imageID,
        new Timestamp(modified),
        new Timestamp(lastimport),
        unit, lesson,
        kind.toString(),
        status.toString(),
        entitle,
        orientation
    ));
  }

  @Override
  public boolean delete(int id) {
    return dao.delete(id) > 0;
  }

  @Override
  public DialogAttributeJoinHelper getDialogAttributeJoinHelper() {
    return dialogAttributeJoinHelper;
  }
}
