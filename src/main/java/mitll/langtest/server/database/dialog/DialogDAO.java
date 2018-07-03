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

package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.*;
import mitll.npdata.dao.dialog.DialogAttributeJoinDAOWrapper;
import mitll.npdata.dao.dialog.DialogDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class DialogDAO extends DAO implements IDialogDAO {
  private static final Logger logger = LogManager.getLogger(DialogDAO.class);

  private static final long MIN = 60 * 1000L;
  private static final long HOUR = 60 * MIN;
  private static final long DAY = 24 * HOUR;
  public static final long YEAR = 365 * DAY;

  private final DialogDAOWrapper dao;

  private final DatabaseImpl databaseImpl;
  private final DialogAttributeJoinHelper dialogAttributeJoinHelper;

  /**
   * @param database
   * @param dbConnection
   * @param userExerciseDAO
   * @see DatabaseImpl#initializeDAOs
   */
  public DialogDAO(Database database,
                   DBConnection dbConnection,
                   IUserExerciseDAO userExerciseDAO,
                   DatabaseImpl databaseImpl) {
    super(database);
    dao = new DialogDAOWrapper(dbConnection);
    this.databaseImpl = databaseImpl;

    //ensureDefault(databaseImpl.getUserDAO().getDefaultUser());
    dialogAttributeJoinHelper = new DialogAttributeJoinHelper(new DialogAttributeJoinDAOWrapper(dbConnection));
  }

  public int ensureDefault(int defaultUser) {
    SlickDialog defaultProject = getDefaultDialog();
    if (defaultProject == null) {
      add(defaultUser,
          databaseImpl.getProjectDAO().getDefault(),
          -1,
          1,

          System.currentTimeMillis(),
          System.currentTimeMillis(),
          "", "",
          DialogType.DEFAULT,
          DialogStatus.DEFAULT,
          "",
          ""
      );
      defaultProject = getDefaultDialog();
      return defaultProject == null ? -1 : defaultProject.id();
    } else {
      return defaultProject.id();
    }
  }

  public int getDefault() {
    SlickDialog defaultProject = getDefaultDialog();
    return defaultProject == null ? -1 : defaultProject.id();
  }

  private SlickDialog getDefaultDialog() {
    Collection<SlickDialog> aDefault = dao.getDefault();
    if (aDefault.isEmpty()) {
      return null;
    } else {
      return aDefault.iterator().next();
    }
  }

/*
  @Override
  public boolean exists(int projid) {
    Collection<SlickDialog> SlickDialogs = dao.byID(projid);
    return !SlickDialogs.isEmpty();
  }
*/

  public SlickDialog getByID(int projid) {
    Collection<SlickDialog> SlickDialogs = dao.byID(projid);
    return SlickDialogs.isEmpty() ? null : SlickDialogs.iterator().next();
  }

  private Collection<SlickDialog> getByProjID(int projid) {
    Collection<SlickDialog> slickDialogs = dao.byProjID(projid);
    return slickDialogs;
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
    Collection<SlickDialog> byProjID = getByProjID(projid);

    List<IDialog> dialogs = new ArrayList<>();
    Map<Integer, Dialog> idToDialog = new HashMap<>();

    byProjID.forEach(slickDialog -> {
      Dialog e = new Dialog(slickDialog);
      dialogs.add(e);
      idToDialog.put(slickDialog.id(), e);
    });

    Map<Integer, Collection<SlickDialogAttributeJoin>> allJoinByProject = dialogAttributeJoinHelper.getAllJoinByProject(projid);

    // add dialog attributes
    Map<Integer, ExerciseAttribute> idToPair = databaseImpl.getUserExerciseDAO().getExerciseAttribute().getIDToPair(projid);

    Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated =
        databaseImpl.getUserExerciseDAO().getRelatedExercise().getDialogIDToRelated(projid);

    allJoinByProject.forEach((dialogID, slickDialogAttributeJoins) -> {
      Dialog dialog = idToDialog.get(dialogID);
      // add attributes

      addAttributes(idToPair, slickDialogAttributeJoins, dialog);

      //add exercises
      addExercises(projid, dialogIDToRelated, dialogID, dialog);

      // add images
      addImage(projid, dialog);
    });

    return dialogs;
  }

  private void addAttributes(Map<Integer, ExerciseAttribute> idToPair,
                             Collection<SlickDialogAttributeJoin> slickDialogAttributeJoins,
                             Dialog dialog) {
    slickDialogAttributeJoins
        .forEach(slickDialogAttributeJoin ->
        {
          int attrid = slickDialogAttributeJoin.attrid();
          ExerciseAttribute e = idToPair.get(attrid);

//          logger.info("adding attribute #" + attrid + " = " + e);

          if (e == null) {
            logger.error("no attr for id #" + attrid);
          } else {
 //           logger.info("adding attribute dialog " + dialog);
  //          logger.info("adding attribute dialog attr " + dialog.getAttributes());

            dialog.getAttributes().add(e);
          }
        });
  }

  /**
   * TODO: For now, don't do a check for existences for images
   * @param projid
   * @param dialog
   */
  private void addImage(int projid, Dialog dialog) {
    List<SlickImage> all = databaseImpl.getImageDAO().getAllNoExistsCheck(projid);
//    logger.warn("addImage got " + all.size());

    Map<Integer, String> idToImageRef = new HashMap<>();
    all.forEach(slickImage -> idToImageRef.put(slickImage.id(), slickImage.filepath()));
  //  logger.warn("idToImageRef got " + idToImageRef.size());
    int imageid = dialog.getSlickDialog().imageid();
    if (imageid < 1) {
      logger.warn("no image for dialog " + dialog);
    }
    else {
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
  private void addExercises(int projid, Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated, Integer dialogID, Dialog dialog) {
    List<SlickRelatedExercise> slickRelatedExercises = dialogIDToRelated.get(dialogID);

    List<CommonExercise> exercises = new ArrayList<>();
    Set<Integer> candidate = new HashSet<>();

//    logger.info("got " + slickRelatedExercises.size() + " relations for " + dialogID);

    slickRelatedExercises.forEach(slickRelatedExercise -> {
//      logger.info("relation " + slickRelatedExercise);

      CommonExercise exercise = databaseImpl.getExercise(projid, slickRelatedExercise.exid());
      if (exercise != null) {
        CommonExercise parent = new Exercise(exercise);
        CommonExercise child = new Exercise(databaseImpl.getExercise(projid, slickRelatedExercise.contextexid()));

        parent.getDirectlyRelated().add(child);
        child.getMutable().setParentExerciseID(parent.getParentExerciseID());

        exercises.add(parent);
        exercises.add(child);

        candidate.add(parent.getID());
        candidate.add(child.getID());
      }
    });

  //  logger.info("got exercises  " + exercises.size());
  //  logger.info("got candidates " + candidate.size() + " relations for " + dialogID + " : " + candidate);

    {
      List<CommonExercise> firstEx = exercises
          .stream()
          .filter(commonExercise -> candidate.contains(commonExercise.getID()))
          .collect(Collectors.toList());

      int size = firstEx.size();
      if (size == 0) {
        logger.error("huh no first exercise");
        // }
//      else if (size == 1) {
      } else if (size == 2) logger.warn("not expecting multiple parents " + firstEx);

      firstEx.forEach(current->dialog.getExercises().add(current));
    }
  }

  /**
   * Don't update the project properties...
   *
   * @param changed
   * @return
   * @see mitll.langtest.server.domino.ProjectSync#updateProjectIfSomethingChanged
   */
  @Override
  public boolean easyUpdate(SlickDialog changed) {
    return dao.update(changed) > 0;
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }


  /**
   * Really does delete it - could take a long time if a big project.
   * <p>
   * Mainly called from drop.sh or tests
   * In general we want to retire projects when we don't want them visible.
   *
   * @param id
   * @see mitll.langtest.server.database.copy.CopyToPostgres#dropOneConfig
   */
  public boolean delete(int id) {
    // logger.info("delete project #" + id);
    return dao.delete(id) > 0;
  }

  public boolean deleteAllBut(int id) {
    // logger.info("delete project #" + id);
    return dao.deleteAllBut(id) > 0;
  }

  /**
   * TODO : consider adding lts class
   * TODO : consider adding domino project id
   *
   * @param userid
   * @param dominoID
   * @param modified
   * @param status
   * @return
   * @see #ensureDefault
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

//                 String fltitle,
                 String entitle,
                 //               String flpresentation,
                 //             String enpresentation,
                 //           int numSpeakers,

                 //         float ilr,
                 String orientation
                 //  String audio,
                 //       String passage,
                 //     String translation
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

        //       fltitle,
        entitle,
        //     flpresentation,
        //   enpresentation,
        //   numSpeakers,
        //   ilr,
        orientation
        //audio,
        //   passage,
        //   translation
    ));
  }

  @Override
  public DialogAttributeJoinHelper getDialogAttributeJoinHelper() {
    return dialogAttributeJoinHelper;
  }
}
