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

package mitll.langtest.server.services;

import mitll.langtest.client.custom.dialog.CreateDialogDialog;
import mitll.langtest.client.dialog.EditorTurn;
import mitll.langtest.client.dialog.RehearseViewHelper;
import mitll.langtest.client.services.DialogService;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.*;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.user.Permission;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;
import static mitll.langtest.client.custom.INavigation.QC_PERMISSIONS;

/**
 * Probably going to need to parameterize by exercises?
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public class DialogServiceImpl<T extends IDialog> extends MyRemoteServiceServlet implements DialogService {
  private static final Logger logger = LogManager.getLogger(DialogServiceImpl.class);

  /**
   * @param request
   * @return
   * @see mitll.langtest.client.dialog.DialogExerciseList#getTypeToValues
   */
  public FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException {

    getUserIDFromSessionOrDB();

    ISection<IDialog> sectionHelper = getDialogSectionHelper(request);

    if (sectionHelper == null) {
      logger.info("getTypeToValues no reponse...");// + "\n\ttype->selection" + typeToSelection);
      return new FilterResponse();
    } else {
      FilterResponse typeToValues = sectionHelper.getTypeToValues(request, false);
      logger.info("getTypeToValues for " + request + " got " + typeToValues);
      return typeToValues;
    }
  }

  /**
   * Worry about visibility.
   * @see #getTypeToValues(FilterRequest)
   * @param request
   * @return
   */
  private ISection<IDialog> getDialogSectionHelper(FilterRequest request) {
    Project project = getProject(request.getProjID());

    ISection<IDialog> sectionHelper;

    User byID = db.getUserDAO().getByID(request.getUserID());
    if (isCanSeeAll(byID.isAdmin(), byID.getPermissions())) {
      sectionHelper = project.getDialogSectionHelper();
    } else {
      List<IDialog> collect = getDialogVisibleToMe(request.getUserID(), project.getDialogs());
      sectionHelper = new SectionHelper<>();
      project.populateDialogSectionHelper(collect, sectionHelper);
    }
    return sectionHelper;
  }

  @NotNull
  private List<IDialog> getDialogVisibleToMe(int userID, Collection<IDialog> dialogs) {
    return dialogs
        .stream()
        .filter(d ->
            !d.isPrivate() ||
                d.getUserid() == userID).collect(Collectors.toList());
  }

  private boolean isCanSeeAll(boolean isAdmin, Collection<Permission> permissions) {
    boolean canSeeAll = isAdmin;

    if (!canSeeAll) {
      for (Permission perm : permissions) {
        if (QC_PERMISSIONS.contains(perm)) {
          canSeeAll = true;
          break;
        }
      }
    }
    return canSeeAll;
  }

  /**
   * Allow search over title of dialog via prefix field of request
   *
   * @param request
   * @return
   * @throws DominoSessionException
   * @see mitll.langtest.client.dialog.DialogExerciseList#getExerciseIDs(Map, String, int, ExerciseListRequest)
   * @see ExerciseListRequest#getPrefix()
   */
  @Override
  public ExerciseListWrapper<IDialog> getDialogs(ExerciseListRequest request) throws DominoSessionException {
    ISection<IDialog> sectionHelper = getDialogSectionHelper();

    logger.info("getDialogs for " + request);

    if (sectionHelper == null) {
      logger.info("getDialogs no response...");
      return new ExerciseListWrapper<>();
    } else {
      int userIDFromSessionOrDB = getUserIDFromSessionOrDB();

      if (userIDFromSessionOrDB != -1) {
        List<IDialog> dialogList = getDialogsForRequest(request, sectionHelper);

        Map<Integer, CorrectAndScore> scoreHistoryPerExercise = getScoreHistoryForDialogs(userIDFromSessionOrDB, dialogList);

        setDialogScores(dialogList, scoreHistoryPerExercise);

        logger.info("getDialogs returning " + dialogList.size() + " dialogs");

        return new ExerciseListWrapper<>(request.getReqID(), dialogList, scoreHistoryPerExercise, new HashMap<>());
      } else {
        logger.info("getDialogs : no user?");
        return new ExerciseListWrapper<>();
      }
    }
  }

  /**
   * Worry about dialog visibility.
   *
   * @param request
   * @param sectionHelper
   * @return
   * @see #getDialogs(ExerciseListRequest)
   */
  @NotNull
  private List<IDialog> getDialogsForRequest(ExerciseListRequest request, ISection<IDialog> sectionHelper) {
    List<IDialog> dialogVisibleToMe;

    User byID = db.getUserDAO().getByID(request.getUserID());
    if (isCanSeeAll(byID.isAdmin(), byID.getPermissions())) {
      dialogVisibleToMe = new ArrayList<>(getDialogs(request.getProjID()));
    } else {
      dialogVisibleToMe = getDialogVisibleToMe(request.getUserID(), getDialogs(request.getProjID()));
      sectionHelper = new SectionHelper<>();
      getProject(request.getProjID()).populateDialogSectionHelper(dialogVisibleToMe, sectionHelper);
    }

    List<IDialog> dialogList =  (request.getTypeToSelection().isEmpty()) ?
        dialogVisibleToMe :
        new ArrayList<>(sectionHelper.getExercisesForSelectionState(request.getTypeToSelection()));

    dialogList = getFilteredBySearchTerm(request, dialogList);

    // sort... by date or unit, chapter, title
    if (request.isSortByDate()) {
      dialogList.sort(comparingLong(IDialog::getModified));
    } else {
      dialogList.sort(this::getDialogComparator);
    }
    return dialogList;
  }

  private void setDialogScores(List<IDialog> dialogList, Map<Integer, CorrectAndScore> scoreHistoryPerExercise) {
    dialogList.forEach(iDialog -> {
      CorrectAndScore correctAndScore = scoreHistoryPerExercise.get(iDialog.getID());
      if (correctAndScore != null) {
        int percentScore = correctAndScore.getPercentScore();
        //  logger.info("scores " + iDialog.getID() + " - " + percentScore);
        iDialog.getMutableShell().setScore(percentScore);
      }
    });
  }

  /**
   * TODO : get latest session phase - STUDY, REHEARSE, PERFORM
   *
   * @param userIDFromSessionOrDB
   * @param dialogList
   * @return
   * @see #getDialogs(ExerciseListRequest)
   */
  @NotNull
  private Map<Integer, CorrectAndScore> getScoreHistoryForDialogs(int userIDFromSessionOrDB, List<IDialog> dialogList) {
    Map<Integer, CorrectAndScore> scoreHistoryPerDialog = new HashMap<>();

    if (!dialogList.isEmpty()) {
      IDialog iDialog = dialogList.get(0);
      Map<Integer, Map<String, Float>> latestDialogSessionScoresPerMode =
          db.getDialogSessionDAO().getLatestDialogSessionScoresPerMode(iDialog.getProjid(), userIDFromSessionOrDB);
      latestDialogSessionScoresPerMode.forEach((k, v) -> {
        if (v.isEmpty()) {
          logger.info("getScoreHistoryForDialogs : no scores for dialog #" + k);
        } else {
          scoreHistoryPerDialog.put(k, new CorrectAndScore(v.values().iterator().next(), v.keySet().iterator().next()));
        }
      });
    }
    return scoreHistoryPerDialog;
  }

  private List<IDialog> getFilteredBySearchTerm(ExerciseListRequest request, List<IDialog> dialogList) {
    String prefix = request.getPrefix().trim();
    if (!prefix.isEmpty()) {
      String lowerCase = prefix.toLowerCase();
      dialogList = dialogList
          .stream()
          .filter(iDialog ->
              iDialog.getEnglish().toLowerCase().contains(lowerCase) ||
                  iDialog.getForeignLanguage().toLowerCase().contains(lowerCase))
          .collect(Collectors.toList());
    }
    return dialogList;
  }

  private int getDialogComparator(IDialog o1, IDialog o2) {
    int i = o1.getUnit().compareTo(o2.getUnit());

    if (i == 0) {
      i = o1.getChapter().compareTo(o2.getChapter());
    }
    if (i == 0) {
      String page1 = o1.getAttributeValue(DialogMetadata.PAGE);
      String page2 = o2.getAttributeValue(DialogMetadata.PAGE);
      i = page1.compareTo(page2);
    }
    if (i == 0) i = o1.getForeignLanguage().compareTo(o2.getForeignLanguage());
    return i;
  }

  /**
   * @param userid
   * @param dialogid
   * @return
   * @throws DominoSessionException
   * @see mitll.langtest.client.analysis.SessionAnalysis#SessionAnalysis
   */
  @Override
  public List<IDialogSession> getDialogSessions(int userid, int dialogid) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    logger.info("getDialogSessions session user " + userIDFromSessionOrDB + " req user " + userid + " dialog " + dialogid);

    if (dialogid == -1) {
      return new ArrayList<>();
    } else {
      List<IDialogSession> dialogSessions = db.getDialogSessionDAO().getDialogSessions(userid, dialogid);
      logger.info("getDialogSessions session user " + userIDFromSessionOrDB + " req user " + userid + " got " + dialogSessions.size());
      return dialogSessions;
    }
  }

  /**
   * @param dialogSession
   * @throws DominoSessionException
   * @see RehearseViewHelper#setSession
   */
  @Override
  public int addSession(DialogSession dialogSession) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();

    if (userIDFromSessionOrDB != dialogSession.getUserid()) {
      logger.warn("addSession huh? session user " + userIDFromSessionOrDB + " vs dialog session " + dialogSession.getUserid());
    }
    return db.getDialogSessionDAO().add(dialogSession);
  }

//  private Collection<IDialog> getDialogs(ExerciseListRequest request, ISection<IDialog> sectionHelper) {
//    return (request.getTypeToSelection().isEmpty()) ?
//        getDialogVisibleToMe(request.getUserID(),getDialogs(request.getProjID())) :
//        new ArrayList<>(sectionHelper.getExercisesForSelectionState(request.getTypeToSelection()));
//  }

  /**
   * Delete a dialog!
   *
   * @param projid
   * @param id
   * @return
   * @throws DominoSessionException
   */
  @Override
  public boolean delete(int projid, int id) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    boolean delete = db.getDialogDAO().delete(projid, id);
    if (delete) {
      getProject(projid).forgetDialog(id);
    }
    return delete;
  }

  /**
   * Delete one turn if normal dialog or a pair if interpreter
   *
   * @param projid
   * @param dialogID
   * @param exid
   * @return
   * @throws DominoSessionException
   * @see mitll.langtest.client.dialog.DialogEditor#deleteCurrentTurnOrPair(EditorTurn)
   */
  public List<Integer> deleteATurnOrPair(int projid, int dialogID, int exid) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    return db.getDialogDAO().deleteExercise(projid, dialogID, exid);
  }

  public boolean deleteCoreExercise(int dialogID, int exid) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    return db.getDialogDAO().deleteCoreExercise(dialogID, exid);
  }

  /**
   * @param dialog
   * @return
   * @throws DominoSessionException
   * @see CreateDialogDialog#doCreate
   */
  @Override
  public IDialog addDialog(IDialog dialog) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    return db.getDialogDAO().add(dialog, getProject(dialog.getProjid()).getLanguage());
  }

  public void update(IDialog dialog) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    db.getDialogDAO().update(dialog);
  }

//  @Override
//  public boolean updateImage(int dialogID, int imageID) throws DominoSessionException {
//    getUserIDFromSessionOrDB();
//    return db.getDialogDAO().updateImage(dialogID,imageID);
//  }

  public DialogExChangeResponse addEmptyExercises(int dialogID, int afterExid, boolean isLeftSpeaker) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();

    int projectForDialog = db.getDialogDAO().getProjectForDialog(dialogID);
    IDialog dialog = getProject(projectForDialog).getDialog(dialogID);
    int before = dialog.getExercises().size();

    List<ClientExercise> added = db.getDialogDAO().addEmptyExercises(dialog, userIDFromSessionOrDB, afterExid, isLeftSpeaker, System.currentTimeMillis());

    int after = dialog.getExercises().size();
    if (dialog.getKind() == DialogType.INTERPRETER) {
      if (after - before != 2) logger.error("before there were " + before + " but after add only " + after);
    } else {
      if (after - before != 1) logger.error("before there were " + before + " but after add only " + after);
    }

    return new DialogExChangeResponse(dialog, added);
  }

  public DialogExChangeResponse addEmptyCoreExercise(int dialogID, int afterExid) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();

    int projectForDialog = db.getDialogDAO().getProjectForDialog(dialogID);
    IDialog dialog = getProject(projectForDialog).getDialog(dialogID);
    int before = dialog.getCoreVocabulary().size();

    dialog.getCoreVocabulary().forEach(exercise ->
        logger.info("addEmptyCoreExercise before  (" + dialogID +
            ") vocab for " + exercise.getID() + " eng '" + exercise.getEnglish() + "' '" + exercise.getForeignLanguage() + "'"));

    ClientExercise added = db.getDialogDAO().addCoreVocab(dialog, userIDFromSessionOrDB, afterExid, System.currentTimeMillis());

    logger.info("addEmptyCoreExercise Added exercise #" + added.getID());

    dialog = getProject(projectForDialog).getDialog(dialogID);
    int after = dialog.getCoreVocabulary().size();

    if (after - before != 1) {
      logger.error("addEmptyCoreExercise before there were " + before + " but after add only " + after);
    }

    dialog.getCoreVocabulary().forEach(exercise ->
        logger.info("addEmptyCoreExercise (" + dialogID +
            ") vocab for " + exercise.getID() + " eng '" + exercise.getEnglish() + "' '" + exercise.getForeignLanguage() + "'"));


    List<ClientExercise> objects = new ArrayList<>();
    objects.add(added);

    return new DialogExChangeResponse(dialog, objects);
  }
}
