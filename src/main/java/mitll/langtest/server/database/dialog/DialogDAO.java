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
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.project.DialogPopulate;
import mitll.langtest.server.database.project.IProject;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.database.userexercise.IRelatedExercise;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.dialog.*;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import mitll.npdata.dao.*;
import mitll.npdata.dao.dialog.DialogAttributeJoinDAOWrapper;
import mitll.npdata.dao.dialog.DialogDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.shared.project.Language.ENGLISH;

public class DialogDAO extends DAO implements IDialogDAO {
  private static final Logger logger = LogManager.getLogger(DialogDAO.class);

  /**
   * @see #configureDialog
   */
  private static final String FLTITLE = DialogMetadata.FLTITLE.toString().toLowerCase();
  public static final String ENGLISH_FIRST_TURN = "E-0";
  public static final String INTERPRETER_FIRST_TURN = "I-0";
  public static final String SPEAKER_A_TURN = "A-0";
  private static final String SPEAKER_A = BaseDialogReader.SPEAKER_A;
  private static final String SPEAKER_B = BaseDialogReader.SPEAKER_B;
  private static final String SPEAKER_PREFIX = "S-";

  public static final boolean DEBUG = false;
  private static final boolean DEBUG_ADD_EXERCISE = false;

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
        "",
        true);
  }

  private SlickDialog getDefaultDialog() {
    Collection<SlickDialog> aDefault = dao.getDefault();
    return (aDefault.isEmpty()) ? null : aDefault.iterator().next();
  }

  /**
   * join with attributes = meta data from domino
   * <p>
   * join with exercises, in order
   * <p>
   * TODO : join with aggregate score
   * <p>
   * join with images
   *
   * @param projid
   * @return
   */
  @Override
  public List<IDialog> getDialogs(int projid) {
    return getiDialogs(projid, getByProjID(projid));
  }

  @NotNull
  private List<IDialog> getiDialogs(int projid, Collection<SlickDialog> byProjID) {
    List<IDialog> dialogs = new ArrayList<>();
    Map<Integer, Dialog> idToDialog = new HashMap<>();

    byProjID.forEach(slickDialog -> {
      Dialog e = makeDialog(slickDialog);
      dialogs.add(e);
      idToDialog.put(slickDialog.id(), e);
    });

    // add dialog attributes
    IUserExerciseDAO userExerciseDAO = databaseImpl.getUserExerciseDAO();

    Map<Integer, ExerciseAttribute> idToPair = userExerciseDAO.getExerciseAttributeDAO().getIDToPair(projid);

    if (DEBUG) logger.info("getDialogs got " + idToPair.size() + " attributes for project #" + projid);

    Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated =
        userExerciseDAO.getRelatedExercise().getDialogIDToRelated(projid);

    Map<Integer, List<SlickRelatedExercise>> dialogIDToCoreRelated =
        userExerciseDAO.getRelatedCoreExercise().getDialogIDToRelated(projid);

    configureDialogs(projid, idToDialog, idToPair, dialogIDToRelated, dialogIDToCoreRelated);

    return dialogs;
  }

  @Override
  public List<IDialog> getOneDialog(int id) {
    Collection<SlickDialog> byID = getByID(id);
    return (byID.isEmpty()) ? Collections.emptyList() : getOneDialog(byID.iterator().next().projid(), byID.iterator().next());
  }

  @NotNull
  private List<IDialog> getOneDialog(int projid, SlickDialog theDialog) {
    Dialog e = makeDialog(theDialog);
    int id = theDialog.id();

    // add dialog attributes
    IUserExerciseDAO userExerciseDAO = databaseImpl.getUserExerciseDAO();

    Map<Integer, ExerciseAttribute> idToPair = userExerciseDAO.getExerciseAttributeDAO().getIDToPair(projid);

    if (DEBUG) logger.info("getDialogs got " + idToPair.size() + " attributes for project #" + projid);

    Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated =
        userExerciseDAO.getRelatedExercise().getDialogIDToRelatedForDialog(id);

    Map<Integer, List<SlickRelatedExercise>> dialogIDToCoreRelated =
        userExerciseDAO.getRelatedCoreExercise().getDialogIDToRelatedForDialog(id);

    {
      Map<Integer, Dialog> idToDialog = new HashMap<>();
      idToDialog.put(id, e);
      configureDialogs(projid, idToDialog, idToPair, dialogIDToRelated, dialogIDToCoreRelated);
    }

    List<IDialog> dialogs = new ArrayList<>();
    dialogs.add(e);
    return dialogs;
  }

  private void configureDialogs(int projid,
                                Map<Integer, Dialog> idToDialog,
                                Map<Integer, ExerciseAttribute> idToPair,
                                Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated,
                                Map<Integer, List<SlickRelatedExercise>> dialogIDToCoreRelated) {
    IProject project = databaseImpl.getIProject(projid);

    if (DEBUG) logger.info("getDialogs found " + idToDialog.size() + " dialogs for " + project);

    dialogAttributeJoinHelper.getAllJoinByProject(projid).forEach((dialogID, slickDialogAttributeJoins) -> {
      Dialog dialog = idToDialog.get(dialogID);

      if (dialog == null) {
//          logger.info("getDialogs skip deleted dialog #" + dialogID);
      } else {
        configureDialog(projid, dialog, idToPair, dialogIDToRelated, dialogIDToCoreRelated, project, dialogID, slickDialogAttributeJoins);
      }
    });
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

      if (!fltitle.isEmpty()) {
        dialog.getMutableShell().setForeignLanguage(fltitle.iterator().next().getValue());
      }
    }

    //add exercises
    List<ClientExercise> clientExercises = addExercises(projid, dialogIDToRelated, dialogID, dialog);

    // sanity check for interpreter dialogs
    if (dialog.getKind() == DialogType.INTERPRETER) {
      if (clientExercises.size() % 2 != 0) {
        logger.error("\n\n\nconfigureDialog : huh? dialog " + dialog + " has " + clientExercises.size() + " exercises - should be even!\n\n\n");
      }
    }

    // add core vocab
    addCoreVocabNoSort(dialogIDToCoreRelated.get(dialogID), project, dialog);

    // add images
    addImage(projid, dialog);
  }

  private Collection<SlickDialog> getByProjID(int projid) {
    return dao.byProjID(projid);
  }

  private Collection<SlickDialog> getByID(int id) {
    return dao.byID(id);
  }

  /**
   * @param relatedExercises
   * @param project
   * @param dialog
   * @see #configureDialog(int, Dialog, Map, Map, Map, IProject, Integer, Collection)
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

  private void addCoreVocabNoSort(List<SlickRelatedExercise> relatedExercises, IProject project, Dialog dialog) {
    if (relatedExercises != null) {
      List<CommonExercise> inOrder = new ArrayList<>(relatedExercises.size());

      relatedExercises.forEach(slickRelatedExercise ->
          inOrder
              .add(project
                  .getExerciseByID(slickRelatedExercise.exid()))
      );

      dialog.getCoreVocabulary().addAll(inOrder);
    }
  }

  /**
   * @param slickDialog
   * @return
   * @see #getDialogs(int)
   */
  private Dialog makeDialog(SlickDialog slickDialog) {
    // boolean isprivate = slickDialog.isprivate();
    //  logger.info("makeDialog : " + isprivate + " " + slickDialog.id());

    Dialog dialog = new Dialog(
        slickDialog.id(),
        slickDialog.userid(),
        slickDialog.projid(),
        slickDialog.imageid(),
        slickDialog.modified().getTime(),
        slickDialog.unit(),
        slickDialog.lesson(),
        slickDialog.orientation(),
        "",
        "",  // set by attribute
        slickDialog.entitle(),
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        getDialogType(slickDialog),
        database.getProject(slickDialog.projid()).getProject().countrycode(),
        slickDialog.isprivate());

//    logger.info("makeDialog : " + dialog);

    return dialog;
  }

  /**
   * @param slickDialog
   * @return
   * @see #makeDialog(SlickDialog)
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
        if (e.getProperty() == null || e.getValue() == null) {
          logger.error("addAttributes huh? made an attribute with null fields ? " + e);
        }
        if (dialog != null) {
          dialog.getAttributes().add(e);
        }
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
      // logger.warn("addImage no image for dialog " + dialog.getID() + " : " + dialog.getForeignLanguage());
    } else {
      String s = idToImageRef.get(imageid);
      if (s == null) {
        logger.warn("addImage no image by " + imageid + "for dialog " + dialog);
      } else {
        logger.info("addImage image ref " + s);
        String audioBaseDir = databaseImpl.getServerProps().getAudioBaseDir();
        s = s.substring(audioBaseDir.length());
        logger.info("addImage image ref now " + s);
        dialog.setImageRef(s);
      }
    }
  }

  /**
   * Add exercises to dialog.
   * <p>
   * Way too complicated - surely an easier way to do it.
   * <p>
   * Reloads the exercises from the database - maybe hydra/score1 has altered the safety!
   *
   * @param projid
   * @param dialogIDToRelated
   * @param dialogID
   * @param dialog
   * @see #configureDialog(int, Dialog, Map, Map, Map, IProject, Integer, Collection)
   */
  private List<ClientExercise> addExercises(int projid,
                                            Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated,
                                            Integer dialogID,
                                            Dialog dialog) {
    List<SlickRelatedExercise> slickRelatedExercises = dialogIDToRelated.get(dialogID);

    List<ClientExercise> exercises1 = dialog.getExercises();
    if (slickRelatedExercises != null) {
      List<CommonExercise> exercises = new ArrayList<>();
      Set<Integer> candidate = new HashSet<>();
      if (DEBUG_ADD_EXERCISE)
        logger.info("addExercises got " + slickRelatedExercises.size() + " relations for " + dialogID);
      Map<Integer, CommonExercise> idToEx = new HashMap<>();

      slickRelatedExercises.forEach(slickRelatedExercise -> {
        if (DEBUG) logger.info("addExercises relation " + slickRelatedExercise);

        int exid = slickRelatedExercise.exid();
        CommonExercise exercise = idToEx.get(exid);

        if (exercise == null) {
          if (!databaseImpl.getExerciseDAO(projid).refresh(exid)) {  // so we need to keep both netprof instances in sync
            logger.warn("addExercises : didn't refresh " + exid);
          }

          exercise = databaseImpl.getExercise(projid, exid);

          if (exercise != null) {
            boolean safeToDecode = exercise.isSafeToDecode();
            if (safeToDecode) {
              if (DEBUG) logger.info("addExercises ex " + exercise.getID() + " safe " + safeToDecode);
            } else {
              logger.warn("addExercises ex " + exercise.getID() + " is not safe to decode");
            }
          }

//          if (exercise == null) {
//            if (!databaseImpl.getExerciseDAO(projid).refresh(exid)) {
//              logger.warn("addExercises : didn't refresh " + exid);
//            }
//            exercise = databaseImpl.getExercise(projid, exid);
//            if (exercise == null) logger.warn("even after attempt to refresh, can't find " + exid);
//          }
          int before;
          if (exercise != null) {
            if (DEBUG_ADD_EXERCISE) {
              logger.info("addExercises (" + dialogID + ") " +
                      "\n\tex #   " + exercise.getID() +
                      "\n\tfl     '" + exercise.getForeignLanguage() + "'" +
//                  "\n\ttokens " + exercise.getTokens() +
                      "\n\tattr   " + exercise.getAttributes()
              );
            }
            before = exercise.getAttributes().size();
            idToEx.put(exid, exercise = new Exercise(exercise));

            int after = exercise.getAttributes().size();
            if (after != before) logger.error("\n\n\n\naddExercises huh before " + before + " after " + after);
          } else {
            logger.info("addExercises : no exercise with id " + exid);
          }
        }

        if (exercise != null) {
          int childid = slickRelatedExercise.contextexid();
          CommonExercise childEx = idToEx.get(childid);

          if (childEx == null && childid != exid) {
            CommonExercise childExOrig = databaseImpl.getExercise(projid, childid);

            if (childExOrig == null) {
              logger.warn("\n\n\naddExercises : can't find " + childid);
            } else {
              if (DEBUG_ADD_EXERCISE) {
                logger.info("addExercises (" + dialogID + ") child - " +
                    "\n\tex #   " + childExOrig.getID() +
                    "\n\tfl     '" + childExOrig.getForeignLanguage() + "'" +
                    //"\n\ttokens " + childExOrig.getTokens() +
                    "\n\tattr   " + childExOrig.getAttributes()
                );
              }
              idToEx.put(childid, childEx = new Exercise(childExOrig));
            }
          }

          if (childEx == null || childid == exid) {
//            logger.info("addExercises : (" + dialogID + ") no childid relation " + childid + " on " + exercise);
            if (!candidate.contains(exercise.getID())) {
              exercises.add(exercise);
            }
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
          logger.warn("addExercises : on dialog #" + dialogID + " can't find related ex " + exid);
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
          logger.warn("addExercises huh no exercises on dialog #" + dialog.getID());
        }
//      else if (size == 1) {
        //   } else if (size == 2) logger.warn("not expecting multiple parents " + firstEx);

        exercises1.addAll(exercises);
      }
    } else {
//      logger.warn("no exercises for " + dialogID);
    }

    String message = "dialog " + dialog.getID() + " " + dialog.getUnit() + " " + dialog.getChapter() +
        " has " + exercises1.size() + " exercises.";

    if (exercises1.isEmpty()) {
//      logger.warn(message);
    } else if (DEBUG) {
      logger.info(message);
    }

    return exercises1;
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
   * TODO : store creation time?
   *
   * @param toAdd
   * @param language
   * @return
   * @see CreateDialogDialog#doCreate
   * @see mitll.langtest.server.services.DialogServiceImpl#addDialog
   */
  @Override
  public IDialog add(IDialog toAdd, String language) {
    int projid = toAdd.getProjid();
    int userid = toAdd.getUserid();

    int imageID = toAdd.getImageID();

//    String filePath = toAdd.getFilePath();
//    int imageID = insertAndGetImageID(language, projid, filePath);
    int add = add(
        userid,
        projid,
        -1,
        imageID,
        toAdd.getModified(),
        toAdd.getModified(),
        toAdd.getUnit(),
        toAdd.getChapter(),
        toAdd.getKind(),
        DialogStatus.DEFAULT,
        toAdd.getEnglish(),
        toAdd.getOrientation(),
        toAdd.isPrivate()
    );

    if (add > -1) {
      toAdd.getMutable().setID(add);
    }

    long now = addFLAttribute(toAdd, projid, userid);

    addEmptyExercises(toAdd, userid, -1, true, now);

    toAdd.getExercises().forEach(ex -> {
      if (ex.getAttributes().stream()
          .anyMatch(exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase(DialogMetadata.SPEAKER.toString()))) {
        //  logger.info("add : found speaker on " + ex.getID());
      } else {
        logger.warn("\n\n\nadd : no speaker on " + ex.getID() + " : " + ex);
      }
    });

    return toAdd;
  }

/*
  public boolean setImageID(int dialogID, int imageID) {
    return dao.setImageID(dialogID, imageID) > 0;
  }
*/

  /**
   * @param toAdd
   * @param projid
   * @param userid
   * @param afterExid
   * @param isLeft
   * @param now
   * @return
   * @see IDialogDAO#add(IDialog, String)
   */
  public List<ClientExercise> addEmptyExercises(IDialog toAdd, int userid, int afterExid, boolean isLeft, long now) {
    int projid = toAdd.getProjid();
    Project project = databaseImpl.getProject(projid);
    List<String> typeOrder = project.getTypeOrder();

    if (afterExid == -1) {
      if (!toAdd.getExercises().isEmpty()) {
        logger.warn("\n\n\naddEmptyExercises : after id is -1 but dialog has " + toAdd.getExercises().size() + " exercises");
      }
    }

    List<ClientExercise> clientExercises = addExercisesToDialog(toAdd, projid, typeOrder, afterExid, isLeft);

    DialogPopulate dialogPopulate = new DialogPopulate(databaseImpl, project.getPathHelper());

    int dialogID = toAdd.getID();
    if (afterExid == -1) {
      dialogPopulate.addExercises2(projid,
          userid,
          typeOrder,
          new Timestamp(now),
          dialogID,
          clientExercises);
    } else {
      dialogPopulate.addExercisesAndSetID(projid, userid, typeOrder, clientExercises);

      IRelatedExercise relatedExercise = databaseImpl.getUserExerciseDAO().getRelatedExercise();

      logger.info("addEmptyExercises : insert " + clientExercises.size() + " after " + afterExid);

      //   sanityCheckLanguageAndSpeaker(toAdd, afterExid, clientExercises);

      for (ClientExercise newEx : clientExercises) {
        if (!relatedExercise.insertAfter(afterExid, newEx.getID())) {
          logger.warn("addEmptyExercises : didn't insert " + newEx.getID() + " after " + afterExid);
          break;
        }
        afterExid = newEx.getID();
      }
    }

    refreshExercises(clientExercises, project);

    // refresh dialogs on project
    databaseImpl.getProjectManagement().addDialogInfo(projid, dialogID);

    // report on changes
    List<ClientExercise> exercises = databaseImpl.getProject(projid).getDialog(dialogID).getExercises();


    clientExercises.forEach(exercise -> logger.info("addEmptyExercises : new exercise " + exercise.getID() + " : " + exercise.getSpeaker() + " (" + exercise.hasEnglishAttr() + ") at position " + exercises.indexOf(exercise)));

    exercises.forEach(exercise -> logger.info("addEmptyExercises : dialog exercises are now " + exercise.getID() + " : " + exercise.getSpeaker() + " eng " + exercise.hasEnglishAttr()));

    return clientExercises;
  }

  @Override
  public ClientExercise addCoreVocab(IDialog toAdd, int userid, int afterExid, long now) {
    int projid = toAdd.getProjid();
    Project project = databaseImpl.getProject(projid);
    List<String> typeOrder = project.getTypeOrder();

    if (afterExid == -1) {
      if (!toAdd.getCoreVocabulary().isEmpty()) {
        logger.warn("\n\n\naddCoreVocab : after id is -1 but dialog has " + toAdd.getCoreVocabulary().size() + " exercises");
      }
    }

    ClientExercise coreVocab = getCoreVocab(toAdd, typeOrder);
    List<ClientExercise> clientExercises = Collections.singletonList(coreVocab);


    DialogPopulate dialogPopulate = new DialogPopulate(databaseImpl, project.getPathHelper());

    int dialogID = toAdd.getID();
    if (afterExid == -1) {
      dialogPopulate.addCoreExercises(projid,
          userid,
          typeOrder,
          new Timestamp(now),
          dialogID,
          clientExercises);

    } else {
      Map<CommonExercise, Integer> commonExerciseIntegerMap = dialogPopulate.addExercisesAndSetID(projid, userid, typeOrder, clientExercises);
      CommonExercise newEx = commonExerciseIntegerMap.keySet().iterator().next();

      IRelatedExercise relatedExercise = databaseImpl.getUserExerciseDAO().getRelatedCoreExercise();

      logger.info("addCoreVocab : insert " + clientExercises.size() + " after " + afterExid);

      if (!relatedExercise.insertAfter(afterExid, newEx.getID())) {
        logger.warn("addCoreVocab : didn't insert " + newEx.getID() + " after " + afterExid);
      }
    }

    refreshExercises(clientExercises, project);

    // refresh dialogs on project
    databaseImpl.getProjectManagement().addDialogInfo(projid, dialogID);

    return coreVocab;
  }

  /*  private void sanityCheckLanguageAndSpeaker(IDialog toAdd, int afterExid, List<ClientExercise> clientExercises) {
    if (toAdd.getKind() == DialogType.INTERPRETER) {
      ClientExercise currentEx = toAdd.getExByID(afterExid);
      ClientExercise nextEx = clientExercises.get(0);

      {
        boolean hasEnglishAttr = currentEx.hasEnglishAttr();
        boolean hasEnglishAttr1 = nextEx.hasEnglishAttr();
        if (hasEnglishAttr && hasEnglishAttr1) {
          logger.error("huh? adding another english " + nextEx + " after a current english turn : " +
              currentEx +
              "?");
        } else if (!hasEnglishAttr && !hasEnglishAttr1) {
          logger.error("huh? adding another fl " + nextEx + " after a current fl turn : " +
              currentEx +
              "?");
        }
      }

      {
        String currentExSpeaker = currentEx.getSpeaker();
        String nextExSpeaker = nextEx.getSpeaker();

        if (currentExSpeaker.equalsIgnoreCase(nextExSpeaker)) {
          logger.error("huh? adding two turns with same speaker : " +
              currentExSpeaker +
              "?");
        }
      }
    }
  }*/

  private void refreshExercises(List<ClientExercise> exercises, Project project) {
    ExerciseDAO<CommonExercise> exerciseDAO = project.getExerciseDAO();
    exercises.forEach(exercise -> {
      int id = exercise.getID();

      boolean refresh = exerciseDAO.refresh(id);

      if (refresh) {
        logger.info("refreshExercises ex " + id + " " + exercise.getAttributes());
      } else {
        logger.warn("refreshExercises didn't refresh " + id);
      }

      CommonExercise exerciseByID = project.getExerciseByID(id);

      if (exerciseByID == null) {
        logger.warn("huh? couldn't find it again?");
      } else {
        logger.info("refreshExercises : found " + exerciseByID + " " + exercise.getAttributes());
      }
    });
  }

  /**
   * Always indicate the second exercise (the interpreter) when deleting from an interpreter dialog
   *
   * @param projid
   * @param dialogID
   * @param exid
   * @return
   */
  public List<Integer> deleteExercise(int projid, int dialogID, int exid) {
    Project project = databaseImpl.getProject(projid);
    IDialog dialog = project.getDialog(dialogID);

    IUserExerciseDAO userExerciseDAO = databaseImpl.getUserExerciseDAO();
    IRelatedExercise relatedExercise = userExerciseDAO.getRelatedExercise();
    List<Integer> deletedIDs = new ArrayList<>();

    if (dialog.getKind() == DialogType.INTERPRETER) {
      List<ClientExercise> exercises = dialog.getExercises();

      logger.info("deleteExercise found " + exercises.size() + " exercises on " + dialogID);

      int found = getIndex(exid, exercises);
      if (found == -1) {
        logger.error("deleteExercise can't find ex " + exid + " in " + dialogID);

      } else {
        ClientExercise prevCandidate = exercises.get(found - 1);
        logger.info("deleteExercise prev of " + exid + " is " + prevCandidate);
        int prev = prevCandidate.getID();


  /*      if (relatedExercise.deleteAndFixForEx(prev)) {
          logger.info("\tdeleteExercise deleted prev relation " + exid);

          if (relatedExercise.deleteAndFixForEx(exid)) {
            logger.info("\tdeleteExercise deleted current relation " + exid);
          } else {
            logger.info("deleteExercise did not delete requested ex " + exid + " in " + dialogID);
          }

          userExerciseDAO.deleteByExID(Arrays.asList(prev, exid));

          deletedIDs.add(prev);
          deletedIDs.add(exid);

          project.forgetExercise(prev);
          project.forgetExercise(exid);

          databaseImpl.getProjectManagement().addDialogInfo(projid, dialogID);

        } else {
          logger.error("deleteExercise did not delete prev ex " + prev + " in " + dialogID);
//          return false;
        }*/

        if (relatedExercise.deleteAndFixPair(prev, exid)) {
          userExerciseDAO.deleteByExID(Arrays.asList(prev, exid));

          deletedIDs.add(prev);
          deletedIDs.add(exid);

          project.forgetExercise(prev);
          project.forgetExercise(exid);

          databaseImpl.getProjectManagement().addDialogInfo(projid, dialogID);
        } else {
          logger.error("deleteExercise did not delete prev ex " + prev + " and " + exid +
              " in " + dialogID);
//          return false;
        }
      }
    } else {
      logger.info("deleteExercise : dialog delete " + exid);
      if (relatedExercise.deleteAndFixForEx(exid)) {
        logger.info("deleteExercise : deleteAndFixForEx success for " + exid);
        deleteTheExercise(projid, dialogID, exid, project, userExerciseDAO);
        deletedIDs.add(exid);
      } else { // look for the prev to this one
        List<ClientExercise> exercises = dialog.getExercises();

        int found = getIndex(exid, exercises);
        if (found == -1) {
          logger.error("deleteExercise 2 can't find ex " + exid + " in " + dialogID);
        } else {
          logger.info("deleteExercise 2 index " + found);
          ClientExercise prevCandidate = exercises.get(found - 1);
          logger.info("deleteExercise 2 prev of " + exid + " is " + prevCandidate);
          if (relatedExercise.deleteAndFixForEx(prevCandidate.getID())) {
            deleteTheExercise(projid, dialogID, exid, project, userExerciseDAO);
            deletedIDs.add(exid);
          } else {
            logger.error("deleteExercise : did not delete ex " + exid + " in " + dialogID);
          }
        }

//        if (true) {
//        } else {
//          logger.error("deleteExercise : did not delete ex " + exid + " in " + dialogID);
//        }
      }
    }
    logger.info("deleteExercise : deletedIDs " + deletedIDs);

    return deletedIDs;
  }

  @Override
  public boolean deleteCoreExercise(int dialogID, int exid) {
    int projid = databaseImpl.getDialogDAO().getProjectForDialog(dialogID);
    Project project = databaseImpl.getProject(projid);
    //IDialog dialog = project.getDialog(dialogID);

    IUserExerciseDAO userExerciseDAO = databaseImpl.getUserExerciseDAO();
    IRelatedExercise relatedExercise = userExerciseDAO.getRelatedCoreExercise();

    if (relatedExercise.deleteAndFixForEx(exid)) {
      logger.info("deleteExercise : deleteAndFixForEx success for " + exid);
      deleteTheExercise(projid, dialogID, exid, project, userExerciseDAO);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Blow it away in the exercise table -
   * tell the project to forget about it
   * reload the dialog.
   *
   * @param projid
   * @param dialogID
   * @param exid
   * @param project
   * @param userExerciseDAO
   */
  private void deleteTheExercise(int projid, int dialogID, int exid,
                                 Project project,
                                 IUserExerciseDAO userExerciseDAO) {
    if (userExerciseDAO.deleteByExID(Collections.singleton(exid))) {
      // refresh dialogs on project
      CommonExercise commonExercise = project.forgetExercise(exid);
      logger.info("deleteTheExercise : forgot " + commonExercise);

      databaseImpl.getProjectManagement().addDialogInfo(projid, dialogID);
    } else {
      logger.warn("deleteTheExercise : didn't delete " + exid);
    }
  }

  private int getIndex(int exid, List<ClientExercise> exercises) {
    int found = -1;
    for (int i = 0; i < exercises.size(); i++) {
      if (exercises.get(i).getID() == exid) {
        found = i;
        break;
      }
    }
    return found;
  }

  /**
   * TODO: add insert exercise
   * TODO: add exercise to other side of conversation
   * TODO: remove exercise
   *
   * @param toAdd
   * @param projid
   * @param baseDialogReader
   * @param typeOrder
   * @see #addEmptyExercises
   */
  private List<ClientExercise> addExercisesToDialog(IDialog toAdd,
                                                    int projid,
                                                    List<String> typeOrder,
                                                    int exidAfter,
                                                    boolean isLeft) {
    List<ClientExercise> collect = toAdd.getExercises().stream().filter(exercise -> exercise.getID() == exidAfter).collect(Collectors.toList());

    if (collect.isEmpty() && exidAfter != -1) {
      logger.error("addExercisesToDialog : can't find " + exidAfter);
      return Collections.emptyList();
    }

    BaseDialogReader baseDialogReader = new BaseDialogReader(null, null);

    Map<String, String> defaultUnitAndChapter = getDefaultUnitAndChapter(toAdd, typeOrder);

    List<ClientExercise> newEx = new ArrayList<>();

    List<ClientExercise> exercises = toAdd.getExercises();
    Language projectLang = databaseImpl.getLanguageEnum(projid);
    boolean interpreter = toAdd.getKind() == DialogType.INTERPRETER;

    String prefix = interpreter ? isLeft ? "E-" : SPEAKER_PREFIX : SPEAKER_PREFIX;

    String speaker = interpreter ?
        isLeft ? BaseDialogReader.ENGLISH_SPEAKER : SPEAKER_B :
        (isLeft ? SPEAKER_A : SPEAKER_B);

    String interpreterPrefix = "I-";
    int index = getIndexOfEx(exidAfter, exercises);
    String speakerTurn = prefix + index;

    if (interpreter) {
      {
        Language firstLang = isLeft ? ENGLISH : projectLang;
        Exercise exercise = baseDialogReader.getExercise("", "", "", speaker, firstLang, speakerTurn, defaultUnitAndChapter);
        exercises.add(index, exercise);
        newEx.add(exercise);
      }

      {
        Language secondLang = isLeft ? projectLang : ENGLISH;
        Exercise exercise1 = baseDialogReader.getExercise("", "", "", BaseDialogReader.INTERPRETERSPEAKER, secondLang,
            interpreterPrefix + index, defaultUnitAndChapter);
        exercises.add(index + 1, exercise1);
        newEx.add(exercise1);
      }

    } else {
      Exercise exercise1 = baseDialogReader.getExercise("", "", "", speaker, projectLang, speakerTurn, defaultUnitAndChapter);
      exercises.add(index, exercise1);
      newEx.add(exercise1);
    }

    return newEx;
  }

  private ClientExercise getCoreVocab(IDialog toAdd, List<String> typeOrder) {
    Map<String, String> defaultUnitAndChapter = getDefaultUnitAndChapter(toAdd, typeOrder);

    Exercise exercise = new Exercise();
    exercise.setUnitToValue(defaultUnitAndChapter);

    return exercise;
  }

  @NotNull
  private Map<String, String> getDefaultUnitAndChapter(IDialog toAdd, List<String> typeOrder) {
    Map<String, String> defaultUnitAndChapter = new HashMap<>();
    if (!typeOrder.isEmpty()) {
      defaultUnitAndChapter.put(typeOrder.get(0), toAdd.getUnit());
    }
    if (typeOrder.size() > 1) {
      defaultUnitAndChapter.put(typeOrder.get(1), toAdd.getChapter());
    }
    return defaultUnitAndChapter;
  }

  private int getIndexOfEx(int exidAfter, List<ClientExercise> exercises) {
    int found = 0;
    for (int i = 0; i < exercises.size(); i++) {
      if (exercises.get(i).getID() == exidAfter) {
        found = i + 1;
        break;
      }
    }
    return found;
  }

  private long addFLAttribute(IDialog toAdd, int projid, int userid) {
    long now = System.currentTimeMillis();

    ExerciseAttribute attribute = new ExerciseAttribute(FLTITLE, toAdd.getForeignLanguage(), false);

    int orAddAttribute = databaseImpl.getUserExerciseDAO().getExerciseAttributeDAO()
        .findOrAddAttribute(projid, now, userid, attribute, false);

    int add = toAdd.getID();
    SlickDialogAttributeJoin e = new SlickDialogAttributeJoin(-1, userid, new Timestamp(now), add, orAddAttribute);

    int insert = dialogAttributeJoinHelper.insert(e);

    logger.info("add dialog " +
        "\n\t#         " + add +
        "\n\tattr id   " + orAddAttribute +
        "\n\tattribute " + attribute +
        "\n\tjoins     " + e +
        "\n\tadd new id " + insert);

    toAdd.getAttributes().add(attribute.setId(orAddAttribute));
    return now;
  }

  @Override
  public boolean updateImage(int dialogID, int imageID) {
    return dao.setImageID(dialogID, imageID) > 0;
  }

  @Override
  public boolean update(IDialog dialog) {
    long modified = System.currentTimeMillis();

    int projid = dialog.getProjid();

    ExerciseAttribute attribute = dialog.getAttribute(DialogMetadata.FLTITLE);

    if (attribute == null) {
      logger.warn("can't find the fl title attribute in " + dialog.getAttributes());
    } else {
      logger.info("update : fl title attr " + attribute + " : " + attribute.getId() + " old " + attribute.getValue() + " new " + dialog.getForeignLanguage());
      if (!dialog.getForeignLanguage().equals(attribute.getValue())) {
        IUserExerciseDAO userExerciseDAO = databaseImpl.getUserExerciseDAO();
        if (!userExerciseDAO.getExerciseAttributeDAO().update(attribute.getId(), dialog.getForeignLanguage())) {
          logger.warn("didn't update fltitle?");
        }
      }
    }

    int imageID = dialog.getImageID();
    logger.info("update image id " + imageID);
    Timestamp modified1 = new Timestamp(modified);
    boolean b = dao.update(new SlickDialog(
        dialog.getID(),
        dialog.getUserid(),
        projid,
        -1,
        imageID,
        modified1,
        modified1,
        dialog.getUnit(), dialog.getChapter(),
        dialog.getKind().toString(),
        DialogStatus.DEFAULT.toString(),
        dialog.getEnglish(),
        dialog.getOrientation(),
        dialog.isPrivate()
    )) > 0;

    if (b) {
      databaseImpl.getProjectManagement().addDialogInfo(projid, dialog.getID());
    } else {
      logger.warn("update : didn't alter the dialog " + dialog);
    }

    return b;
  }

  /**
   * @param userid
   * @param dominoID
   * @param modified
   * @param status
   * @param isPrivate
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
                 String orientation,
                 boolean isPrivate) {
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
        orientation,
        isPrivate
    ));
  }

  /**
   * TODOx : pass in project id -
   * TODO : what if two people are updating the set of dialogs at the same time???
   *
   * @param projid
   * @param id
   * @return
   */
  @Override
  public boolean delete(int projid, int id) {
    boolean b = dao.delete(id) > 0;

    if (b) {
      databaseImpl.getProjectManagement().addDialogInfo(projid, id);
    }

    return b;
  }

  @Override
  public DialogAttributeJoinHelper getDialogAttributeJoinHelper() {
    return dialogAttributeJoinHelper;
  }

  @Override
  public int getProjectForDialog(int dialogID) {
    Collection<SlickDialog> byID = getByID(dialogID);
    return (byID.isEmpty()) ? -1 : byID.iterator().next().projid();
  }
}
