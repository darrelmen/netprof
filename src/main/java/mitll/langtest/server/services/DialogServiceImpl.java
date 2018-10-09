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

package mitll.langtest.server.services;

import mitll.langtest.client.banner.RehearseViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.DialogService;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.scoring.AlignmentHelper;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.DialogSession;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.dialog.IDialogSession;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.FilterResponse;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickRelatedResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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
   * @see mitll.langtest.client.banner.DialogExerciseList#getTypeToValues
   */
  public FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException {
    ISection<IDialog> sectionHelper = getDialogSectionHelper();
    if (sectionHelper == null) {
      logger.info("getTypeToValues no reponse...");// + "\n\ttype->selection" + typeToSelection);
      return new FilterResponse();
    } else {
      return sectionHelper.getTypeToValues(request, false);
    }
  }

  /**
   * Allow search over title of dialog via prefix field of request
   *
   * @param request
   * @return
   * @throws DominoSessionException
   * @see mitll.langtest.client.banner.DialogExerciseList#getExerciseIDs(Map, String, int, ExerciseListRequest)
   * @see ExerciseListRequest#getPrefix()
   */
  @Override
  public ExerciseListWrapper<IDialog> getDialogs(ExerciseListRequest request) throws DominoSessionException {
    ISection<IDialog> sectionHelper = getDialogSectionHelper();
    if (sectionHelper == null) {
      logger.info("getDialogs no response...");
      return new ExerciseListWrapper<>();
    } else {
      int userIDFromSessionOrDB = getUserIDFromSessionOrDB();

      if (userIDFromSessionOrDB != -1) {
        List<IDialog> dialogList = getDialogs(request, sectionHelper, userIDFromSessionOrDB);

        dialogList = getFilteredBySearchTerm(request, dialogList);
        dialogList.sort(this::getDialogComparator);

        Map<Integer, CorrectAndScore> scoreHistoryPerExercise = getScoreHistoryForDialogs(userIDFromSessionOrDB, dialogList);


        dialogList.forEach(iDialog -> {
          CorrectAndScore correctAndScore = scoreHistoryPerExercise.get(iDialog.getID());
          if (correctAndScore != null) {
            int percentScore = correctAndScore.getPercentScore();
            logger.info("scores " + iDialog.getID() + " - " + percentScore);

            iDialog.getMutableShell().setScore(percentScore);
          }
        });

        return new ExerciseListWrapper<>(request.getReqID(), dialogList, null, scoreHistoryPerExercise);
      } else {
        logger.info("getDialogs no user?");
        return new ExerciseListWrapper<>();
      }
    }
  }

  /**
   * TODO : get latest session phase - STUDY, REHEARSE, PERFORM
   *
   * @param userIDFromSessionOrDB
   * @param dialogList
   * @return
   */
  @NotNull
  private Map<Integer, CorrectAndScore> getScoreHistoryForDialogs(int userIDFromSessionOrDB, List<IDialog> dialogList) {
    Map<Integer, CorrectAndScore> scoreHistoryPerExercise = new HashMap<>();

    if (!dialogList.isEmpty()) {
      IDialog iDialog = dialogList.get(0);
      Map<Integer, Float> latestDialogSessionScores =
          db.getDialogSessionDAO().getLatestDialogSessionScores(iDialog.getProjid(), userIDFromSessionOrDB);
      latestDialogSessionScores.forEach((k, v) -> scoreHistoryPerExercise.put(k, new CorrectAndScore(v, null)));
    }
    return scoreHistoryPerExercise;
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
      String page1 = o1.getAttributeValue(IDialog.METADATA.PAGE);
      String page2 = o2.getAttributeValue(IDialog.METADATA.PAGE);
      i = page1.compareTo(page2);
    }
    if (i == 0) i = o1.getForeignLanguage().compareTo(o2.getForeignLanguage());
    return i;
  }

  /**
   * for now, get the latest session with any results in it...
   *
   * @param dialogid
   * @return
   * @throws DominoSessionException
   */
  @Override
  public int getLatestDialogSessionID(int dialogid) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();

//    Map<Integer, List<SlickRelatedResult>> sessionToResults = db.getRelatedResultDAO().getByProjectForDialogForUser(getProjectIDFromUser(userIDFromSessionOrDB), dialogid, userIDFromSessionOrDB);
    SlickRelatedResult slickRelatedResult = db.getRelatedResultDAO()
        .latestByProjectForDialogForUser(getProjectIDFromUser(userIDFromSessionOrDB), dialogid, userIDFromSessionOrDB);
    return slickRelatedResult == null ? -1 : slickRelatedResult.dialogsessionid();
  }

  // user implicit -

  /**
   * WHY?
   *
   * @param dialogid
   * @return
   * @throws DominoSessionException
   */
/*  public List<IDialogSession> getDialogSessions(int dialogid) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    return db.getDialogSessionDAO().getDialogSessions(userIDFromSessionOrDB, dialogid);
  }*/

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
      dialogid = getFirstDialog(userIDFromSessionOrDB);
    }
    List<IDialogSession> dialogSessions = db.getDialogSessionDAO().getDialogSessions(userid, dialogid);
    logger.info("getDialogSessions session user " + userIDFromSessionOrDB + " req user " + userid + " got " + dialogSessions.size());
    return dialogSessions;
  }

  private int getFirstDialog(int userIDFromSessionOrDB) {
    int projectIDFromUser = getProjectIDFromUser(userIDFromSessionOrDB);
    if (projectIDFromUser != -1) {
      List<IDialog> dialogs = getProject(projectIDFromUser).getDialogs();
      if (!dialogs.isEmpty()) return dialogs.get(0).getID();
    }
    return -1;
  }

  /**
   * @param dialogSession
   * @throws DominoSessionException
   * @see RehearseViewHelper#clearScores
   */
  @Override
  public int addSession(DialogSession dialogSession) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();

    if (userIDFromSessionOrDB != dialogSession.getUserid()) {
      logger.warn("addSession huh? session user " + userIDFromSessionOrDB + " vs dialog session " + dialogSession.getUserid());
    }
    return db.getDialogSessionDAO().add(dialogSession);
  }

  private List<IDialog> getDialogs(ExerciseListRequest request,
                                   ISection<IDialog> sectionHelper,
                                   int userIDFromSessionOrDB) {
    return (request.getTypeToSelection().isEmpty()) ?
        getDialogs(userIDFromSessionOrDB) :
        new ArrayList<>(sectionHelper.getExercisesForSelectionState(request.getTypeToSelection()));
  }
}