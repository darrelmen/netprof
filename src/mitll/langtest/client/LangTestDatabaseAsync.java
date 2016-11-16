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

package mitll.langtest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.*;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The async counterpart of <code>LangTestDatabase</code>.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public interface LangTestDatabaseAsync {
  <T extends CommonShell> void getExerciseIds(
                                              ExerciseListRequest request,
                                              AsyncCallback<ExerciseListWrapper<T>> async);

  void getUsers(AsyncCallback<List<User>> async);

  void getUserBy(long id, AsyncCallback<User> async);

  void writeAudioFile(String base64EncodedString,
                      AudioContext audioContext,

                      boolean recordedWithFlash, String deviceType, String device,
                      boolean doFlashcard, boolean recordInResults,
                      boolean addToAudioTable,
                      boolean allowAlternates, AsyncCallback<AudioAnswer> async);


  void getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence, String transliteration,
                           String exerciseID, ImageOptions imageOptions, AsyncCallback<PretestScore> async);

  void getImageForAudioFile(int reqid, String audioFile, String imageType, ImageOptions imageOptions, String exerciseID,
                            AsyncCallback<ImageResponse> async);

  void getScoreForAnswer(AudioContext audioContext, String answer,
                         long timeSpent, Map<String, Collection<String>> typeToSection, AsyncCallback<Answer> async);

  void addStudentAnswer(long resultID, boolean correct, AsyncCallback<Void> async);

  void getScoresForUser(Map<String, Collection<String>> typeToSection, int userID, Collection<String> exids,
                        AsyncCallback<QuizCorrectAndScore> async);

  <T extends Shell> void getExercise(String id, long userID, boolean isFlashcardReq, AsyncCallback<T> async);

  void getUserToResultCount(AsyncCallback<Map<User, Integer>> async);

  void getResultCountToCount(AsyncCallback<Map<Integer, Integer>> async);

  void getResultByDay(AsyncCallback<Map<String, Integer>> async);

  void getResultByHourOfDay(AsyncCallback<Map<String, Integer>> async);

  void getResultPerExercise(AsyncCallback<Map<String, Map<String, Integer>>> async);

  void getSessions(AsyncCallback<List<Session>> async);

  void getResults(int start, int end, String sortInfo, Map<String, String> unitToValue, long userid, String flText,
                  int req, AsyncCallback<ResultAndTotal> async);

  void getResultStats(AsyncCallback<Map<String, Number>> async);

  void getResultCountsByGender(AsyncCallback<Map<String, Map<Integer, Integer>>> async);

  void getDesiredCounts(AsyncCallback<Map<String, Map<Integer, Map<Integer, Integer>>>> async);

  void logMessage(String message, AsyncCallback<Void> async);


  void getStartupInfo( AsyncCallback<StartupInfo> async);

  void getUserListsForText(String search, long userid, AsyncCallback<Collection<UserList<CommonShell>>> async);

  void getListsForUser(long userid, boolean onlyCreated, boolean visited, AsyncCallback<Collection<UserList<CommonShell>>> async);

  void addItemToUserList(long userListID, String exID, AsyncCallback<Void> async);

  void reallyCreateNewItem(long userListID, CommonExercise userExercise, AsyncCallback<CommonExercise> async);

  void addUserList(long userid, String name, String description, String dliClass, boolean isPublic, AsyncCallback<Long> async);

  void addVisitor(long userListID, long user, AsyncCallback<Void> asyncCallback);

  void editItem(CommonExercise userExercise, boolean keepAudio, AsyncCallback<Void> async);

  void addAnnotation(String exerciseID, String field, String status, String comment, long userID, AsyncCallback<Void> async);

  void markReviewed(String id, boolean isCorrect, long creatorID, AsyncCallback<Void> asyncCallback);

  void setExerciseState(String id, STATE state, long userID, AsyncCallback<Void> async);

  void isValidForeignPhrase(String foreign, String transliteration, AsyncCallback<Boolean> async);

  void deleteList(long id, AsyncCallback<Boolean> async);

  void deleteItemFromList(long listid, String exid, AsyncCallback<Boolean> async);

  void duplicateExercise(CommonExercise id, AsyncCallback<CommonExercise> async);

  void deleteItem(String exid, AsyncCallback<Boolean> async);

  void getUserHistoryForList(long userid, Collection<String> ids, long latestResultID,
                             Map<String, Collection<String>> typeToSection, long userListID, AsyncCallback<AVPScoreReport> async);

  void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID, String device,
                AsyncCallback<Void> async);

  void getEvents(AsyncCallback<Collection<Event>> async);

  void markState(String id, STATE state, long creatorID, AsyncCallback<Void> async);

  void getReviewLists(AsyncCallback<List<UserList<CommonShell>>> async);

  void markAudioDefect(AudioAttribute audioAttribute, String exid, AsyncCallback<Void> async);

  void setPublicOnList(long userListID, boolean isPublic, AsyncCallback<Void> async);

  void markGender(AudioAttribute attr, boolean isMale, AsyncCallback<Void> async);

  void getMaleFemaleProgress(AsyncCallback<Map<String, Float>> async);

  /**
   * @param base64EncodedString
   * @param textToAlign
   * @param identifier
   * @param reqid
   * @param device
   * @param async
   * @see mitll.langtest.client.scoring.SimplePostAudioRecordButton#postAudioFile(String)
   */
  void getAlignment(String base64EncodedString,
                    String textToAlign,
                    String transliteration,
                    String identifier,
                    int reqid, String device, AsyncCallback<AudioAnswer> async);

  void userExists(String login, String passwordH, AsyncCallback<User> async);

  void addUser(String userID, String passwordH, String emailH, User.Kind kind, String url, String email, boolean isMale,
               int age, String dialect, boolean isCD, String device, AsyncCallback<User> async);

  void resetPassword(String userid, String text, String url, AsyncCallback<Boolean> asyncCallback);

  void forgotUsername(String emailH, String email, String url, AsyncCallback<Boolean> async);

  void getUserIDForToken(String token, AsyncCallback<Long> async);

  void changePFor(String token, String first, AsyncCallback<Boolean> asyncCallback);

  void enableCDUser(String cdToken, String emailR, String url, AsyncCallback<String> asyncCallback);

  void getNumResults(AsyncCallback<Integer> async);

  void getResultAlternatives(Map<String, String> unitToValue, long userid, String flText, String which,
                             AsyncCallback<Collection<String>> async);

  void addRoundTrip(long resultid, int roundTrip, AsyncCallback<Void> async);

  void getResultASRInfo(long resultID, ImageOptions imageOptions, AsyncCallback<PretestScore> async);

  void changeEnabledFor(int userid, boolean enabled, AsyncCallback<Void> async);

  void getASRScoreForAudioPhonemes(int reqid, long resultID, String testAudioFile, String sentence, String transliteration,
                                   String exerciseID,
                                   ImageOptions imageOptions, AsyncCallback<PretestScore> async);

  void getContextPractice(AsyncCallback<ContextPractice> async);

  void getPerformanceForUser(long id, int minRecordings, AsyncCallback<UserPerformance> async);

  void getWordScores(long id, int minRecordings, AsyncCallback<List<WordScore>> async);

  void getPhoneScores(long id, int minRecordings, AsyncCallback<PhoneReport> async);

  void getUsersWithRecordings(AsyncCallback<Collection<UserInfo>> async);

  void getShells(List<String> ids, AsyncCallback<List<CommonShell>> async);

  void reallyCreateNewItems(long creator, long userListID, String userExerciseText, AsyncCallback<Collection<CommonExercise>> async);

  void reloadExercises(AsyncCallback<Void> async);
}
