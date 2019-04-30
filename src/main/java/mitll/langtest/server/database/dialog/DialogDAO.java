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

public class DialogDAO extends DAO implements IDialogDAO {
  private static final Logger logger = LogManager.getLogger(DialogDAO.class);

  /**
   * @see #configureDialog
   */
  private static final String FLTITLE = DialogMetadata.FLTITLE.toString().toLowerCase();//"fltitle";
  public static final String ENGLISH_FIRST_TURN = "E-0";
  public static final String INTERPRETER_FIRST_TURN = "I-0";
  public static final String SPEAKER_A_TURN = "A-0";
  public static final String SPEAKER_A = BaseDialogReader.SPEAKER_A;
  public static final String SPEAKER_B = BaseDialogReader.SPEAKER_B;
  public static final String SPEAKER_PREFIX = "S-";
  public static final boolean DEBUG = false;

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

  @Override
  public List<IDialog> getOneDialog(int id) {
    Collection<SlickDialog> byID = getByID(id);
    if (byID.isEmpty()) return null;
    else {
      return getiDialogs(byID.iterator().next().projid(), byID);
    }
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

    logger.info("getDialogs got " + idToPair.size() + " attributes for project #" + projid);

    Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated =
        userExerciseDAO.getRelatedExercise().getDialogIDToRelated(projid);

    Map<Integer, List<SlickRelatedExercise>> dialogIDToCoreRelated =
        userExerciseDAO.getRelatedCoreExercise().getDialogIDToRelated(projid);

    {
      IProject project = databaseImpl.getIProject(projid);

      logger.info("getDialogs found " + idToDialog.size() + " dialogs for " + project);

      dialogAttributeJoinHelper.getAllJoinByProject(projid).forEach((dialogID, slickDialogAttributeJoins) -> {
        Dialog dialog = idToDialog.get(dialogID);

        if (dialog == null) {
//          logger.info("getDialogs skip deleted dialog #" + dialogID);
        } else {
          configureDialog(projid, dialog, idToPair, dialogIDToRelated, dialogIDToCoreRelated, project, dialogID, slickDialogAttributeJoins);
        }
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

      if (!fltitle.isEmpty()) {
        dialog.getMutableShell().setForeignLanguage(fltitle.iterator().next().getValue());
      }
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

  /**
   * @param slickDialog
   * @return
   * @see #getDialogs(int)
   */
  private Dialog makeDialog(SlickDialog slickDialog) {
    boolean isprivate = slickDialog.isprivate();
    //  logger.info("makeDialog : " + isprivate + " " + slickDialog.id());

    Dialog dialog = new Dialog(
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
        "",  // set by attribute
        slickDialog.entitle(),
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        getDialogType(slickDialog),
        database.getProject(slickDialog.projid()).getProject().countrycode(),
        isprivate);

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
      // logger.warn("addImage no image for dialog " + dialog.getID() + " : " + dialog.getForeignLanguage());
    } else {
      String s = idToImageRef.get(imageid);
      if (s == null) {
        logger.warn("addImage no image by " + imageid + "for dialog " + dialog);
      } else {
        dialog.setImageRef(s);
      }
    }
  }

  /**
   * Add exercises to dialog.
   *
   * Way too complicated - surely an easier way to do it.
   *
   * @param projid
   * @param dialogIDToRelated
   * @param dialogID
   * @param dialog
   * @see #configureDialog(int, Dialog, Map, Map, Map, IProject, Integer, Collection)
   */
  private void addExercises(int projid,
                            Map<Integer, List<SlickRelatedExercise>> dialogIDToRelated,
                            Integer dialogID,
                            Dialog dialog) {
    List<SlickRelatedExercise> slickRelatedExercises = dialogIDToRelated.get(dialogID);

    if (slickRelatedExercises != null) {
      List<CommonExercise> exercises = new ArrayList<>();
      Set<Integer> candidate = new HashSet<>();
      if (DEBUG) logger.info("addExercises got " + slickRelatedExercises.size() + " relations for " + dialogID);
      Map<Integer, CommonExercise> idToEx = new HashMap<>();

      slickRelatedExercises.forEach(slickRelatedExercise -> {
//        logger.info("addExercises relation " + slickRelatedExercise);
        int exid = slickRelatedExercise.exid();
        CommonExercise exercise = idToEx.get(exid);

        if (exercise == null) {
          exercise = databaseImpl.getExercise(projid, exid);

          int before = 0;
          if (exercise != null) {

            if (DEBUG) {
              logger.info("addExercises (" + dialogID + ") " +
                  "\n\tex #   " + exercise.getID() +
                  "\n\tfl     " + exercise.getForeignLanguage() +
                  "\n\ttokens " + exercise.getTokens() +
                  "\n\tattr   " + exercise.getAttributes()
              );
            }
            before = exercise.getAttributes().size();

            idToEx.put(exid, exercise = new Exercise(exercise));

            int after = exercise.getAttributes().size();
            if (after != before) logger.error("\n\n\n\naddExercises huh before " + before + " after " + after);
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
            logger.info("addExercises : (" + dialogID + ") no childid relation " + childid + " on " + exercise);
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

        exercises.forEach(current -> dialog.getExercises().add(current));
      }
    } else {
//      logger.warn("no exercises for " + dialogID);
    }

    String message = "dialog " + dialog.getID() + " " + dialog.getUnit() + " " + dialog.getChapter() +
        " has " + dialog.getExercises().size() + " exercises.";

    if (dialog.getExercises().isEmpty()) {
//      logger.warn(message);
    } else if (DEBUG) {
      logger.info(message);
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
   * TODO : store creation time?
   *
   * @param toAdd
   * @return
   * @see CreateDialogDialog#doCreate
   * @see mitll.langtest.server.services.DialogServiceImpl#addDialog
   */
  @Override
  public IDialog add(IDialog toAdd) {
    int projid = toAdd.getProjid();
    int userid = toAdd.getUserid();

    int add = add(
        userid,
        projid,
        -1,
        toAdd.getImageID(),
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
        logger.info("add : found speaker on " + ex.getID());
      } else {
        logger.warn("\n\n\nadd : no speaker on " + ex.getID() + " : " + ex);
      }
    });

    return toAdd;
  }

  @Override
  public List<ClientExercise> addEmptyExercises(IDialog toAdd, int userid, int afterExid, boolean isLeft) {
    return addEmptyExercises(toAdd, userid, afterExid, isLeft, System.currentTimeMillis());
  }

  /**
   * @param toAdd
   * @param projid
   * @param userid
   * @param afterExid
   * @param isLeft
   * @param now
   * @return
   * @see #add(IDialog)
   */
  public List<ClientExercise> addEmptyExercises(IDialog toAdd, int userid, int afterExid, boolean isLeft, long now) {
    int projid = toAdd.getProjid();
    Project project = databaseImpl.getProject(projid);
    List<String> typeOrder = project.getTypeOrder();

    List<ClientExercise> clientExercises = addExercisesToDialog(toAdd, projid, typeOrder, afterExid, isLeft);

    DialogPopulate dialogPopulate = new DialogPopulate(databaseImpl, project.getPathHelper());
    if (afterExid == -1) {
      dialogPopulate.addExercises2(projid,
          userid,
          typeOrder,
          new Timestamp(now),
          toAdd.getID(),
          clientExercises);
    } else {
      dialogPopulate.addExercisesAndSetID(projid, userid, typeOrder, clientExercises);

      IRelatedExercise relatedExercise = databaseImpl.getUserExerciseDAO().getRelatedExercise();
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
    databaseImpl.getProjectManagement().addDialogInfo(projid, toAdd.getID());

    return clientExercises;
  }

  private void refreshExercises(List<ClientExercise> exercises, Project project) {
    ExerciseDAO<CommonExercise> exerciseDAO = project.getExerciseDAO();

    //List<ClientExercise> exercises = toAdd.getExercises();
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
  public boolean deleteExercise(int projid, int dialogID, int exid) {
    IDialog dialog = databaseImpl.getProject(projid).getDialog(dialogID);
    DialogType dialogType = dialog.getKind();
    IUserExerciseDAO userExerciseDAO = databaseImpl.getUserExerciseDAO();
    IRelatedExercise relatedExercise = userExerciseDAO.getRelatedExercise();
    if (dialogType == DialogType.INTERPRETER) {
      List<ClientExercise> exercises = dialog.getExercises();
      int found = -1;
      for (int i = 0; i < exercises.size(); i++) {
        if (exercises.get(i).getID() == exid) {
          found = i;
          break;
        }
      }
      if (found == -1) {
        logger.error("deleteExercise can't find ex " + exid + " in " + dialogID);
        return false;
      } else {
        ClientExercise clientExercise = exercises.get(found - 1);
        logger.info("deleteExercise prev of " + exid + " is " + clientExercise);
        int prev = clientExercise.getID();
        // boolean didIt = relatedExercise.deleteAndFixForEx(prev);
        if (relatedExercise.deleteAndFixForEx(prev)) {
          if (relatedExercise.deleteAndFixForEx(exid)) {
            // refresh dialogs on project
          } else {
            // refresh dialogs on project
//            userExerciseDAO.deleteByExID(Arrays.asList(prev, exid));
//            databaseImpl.getProjectManagement().addDialogInfo(projid);
            logger.info("deleteExercise did not delete requested ex " + exid + " in " + dialogID);
            //return true;
          }
          userExerciseDAO.deleteByExID(Arrays.asList(prev, exid));
          databaseImpl.getProjectManagement().addDialogInfo(projid, dialogID);
          return true;
        } else {
          logger.error("deleteExercise did not delete prev ex " + prev + " in " + dialogID);
          return false;
        }
      }
    } else {
      if (relatedExercise.deleteAndFixForEx(exid)) {
        userExerciseDAO.deleteByExID(Collections.singleton(exid));
        // refresh dialogs on project
        databaseImpl.getProjectManagement().addDialogInfo(projid, dialogID);
        return true;
      } else {
        logger.error("did not delete ex " + exid + " in " + dialogID);
        return false;
      }
    }
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
  private List<ClientExercise> addExercisesToDialog(IDialog toAdd, int projid, List<String> typeOrder, int exidAfter,
                                                    boolean isLeft) {
    List<ClientExercise> collect = toAdd.getExercises().stream().filter(exercise -> exercise.getID() == exidAfter).collect(Collectors.toList());

    if (collect.isEmpty() && exidAfter != -1) {
      logger.error("addExercisesToDialog : can't find " + exidAfter);
      return Collections.emptyList();
    } else {
//      ClientExercise clientExercise = collect.get(0);
    }

    BaseDialogReader baseDialogReader = new BaseDialogReader(null, null);

    Map<String, String> defaultUnitAndChapter = new HashMap<>();
    if (!typeOrder.isEmpty()) {
      defaultUnitAndChapter.put(typeOrder.get(0), toAdd.getUnit());
    }
    if (typeOrder.size() > 1) {
      defaultUnitAndChapter.put(typeOrder.get(1), toAdd.getChapter());
    }

    List<ClientExercise> newEx = new ArrayList<>();

    List<ClientExercise> exercises = toAdd.getExercises();
    Language languageEnum = databaseImpl.getLanguageEnum(projid);
    boolean interpreter = toAdd.getKind() == DialogType.INTERPRETER;

    String prefix = interpreter ? isLeft ? "E-" : SPEAKER_PREFIX : SPEAKER_PREFIX;

    String speaker = interpreter ?
        isLeft ? BaseDialogReader.ENGLISH_SPEAKER : SPEAKER_A :
        (isLeft ? SPEAKER_A : SPEAKER_B);

    String interpreterPrefix = "I-";
    int index = getIndexOfEx(exidAfter, exercises);
    String speakerTurn = prefix + index;

    if (interpreter) {
      {
        Exercise exercise = baseDialogReader.getExercise("", "", "", speaker, Language.ENGLISH, speakerTurn, defaultUnitAndChapter);
        exercises.add(exercise);
        newEx.add(exercise);
      }

      {
        Exercise exercise1 = baseDialogReader.getExercise("", "", "", BaseDialogReader.INTERPRETERSPEAKER, languageEnum,
            interpreterPrefix + index, defaultUnitAndChapter);
        exercises.add(exercise1);
        newEx.add(exercise1);
      }

    } else {
      Exercise exercise1 = baseDialogReader.getExercise("", "", "", speaker, languageEnum, speakerTurn, defaultUnitAndChapter);
      exercises.add(exercise1);
      newEx.add(exercise1);
    }

    return newEx;
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
    IUserExerciseDAO userExerciseDAO = databaseImpl.getUserExerciseDAO();

    long now = System.currentTimeMillis();

    ExerciseAttribute attribute = new ExerciseAttribute(FLTITLE, toAdd.getForeignLanguage(), false);

    int orAddAttribute = userExerciseDAO.getExerciseAttributeDAO()
        .findOrAddAttribute(projid, now, userid, attribute, false);

    int add = toAdd.getID();
    SlickDialogAttributeJoin e = new SlickDialogAttributeJoin(-1, userid, new Timestamp(now), add, orAddAttribute);

    logger.info("add dialog " +
        "\n\t#         " + add +
        "\n\tattr id   " + orAddAttribute +
        "\n\tattribute " + attribute +
        "\n\tjoins     " + e
    );

    int insert = dialogAttributeJoinHelper.insert(e);

    logger.info("add new id " + insert);

    toAdd.getAttributes().add(attribute.setId(orAddAttribute));
    return now;
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

    boolean b = dao.update(new SlickDialog(
        dialog.getID(),
        dialog.getUserid(),
        projid,
        dialog.getDominoid(),
        dialog.getImageID(),
        new Timestamp(modified),
        new Timestamp(modified),
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
   * @param id
   * @param projid
   * @return
   */
  @Override
  public boolean delete(int id, int projid) {
    boolean b = dao.delete(id) > 0;

    if (b) {
      databaseImpl.getProjectManagement().addDialogInfo(projid);
    }

    return b;
  }

  @Override
  public DialogAttributeJoinHelper getDialogAttributeJoinHelper() {
    return dialogAttributeJoinHelper;
  }
}
