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
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.scoring.AudioContext;
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

  void writeAudioFile(String base64EncodedString,
                      AudioContext audioContext,

                      boolean recordedWithFlash, String deviceType, String device,
                      boolean doFlashcard, boolean recordInResults,
                      boolean addToAudioTable,
                      boolean allowAlternates, AsyncCallback<AudioAnswer> async);


  void getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence, int width, int height,
                           boolean useScoreToColorBkg, String exerciseID, AsyncCallback<PretestScore> async);

  void getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height, String exerciseID,
                            AsyncCallback<ImageResponse> async);

  void getScoreForAnswer(AudioContext audioContext, String answer,
                         long timeSpent, Map<String, Collection<String>> typeToSection, AsyncCallback<Answer> async);

  void addStudentAnswer(long resultID, boolean correct, AsyncCallback<Void> async);

  void getScoresForUser(Map<String, Collection<String>> typeToSection, int userID, Collection<String> exids,
                        AsyncCallback<QuizCorrectAndScore> async);

  <T extends Shell> void getExercise(String id, int userID, boolean isFlashcardReq, AsyncCallback<T> async);

  void getResults(int start, int end, String sortInfo, Map<String, String> unitToValue, int userid, String flText,
                  int req, AsyncCallback<ResultAndTotal> async);


  void logMessage(String message, AsyncCallback<Void> async);

  void getStartupInfo( AsyncCallback<StartupInfo> async);

  void addAnnotation(String exerciseID, String field, String status, String comment, int userID, AsyncCallback<Void> async);

  void markReviewed(String id, boolean isCorrect, int creatorID, AsyncCallback<Void> asyncCallback);

  void setExerciseState(String id, STATE state, int userID, AsyncCallback<Void> async);

  void isValidForeignPhrase(String foreign, AsyncCallback<Boolean> async);

  void deleteItem(String exid, AsyncCallback<Boolean> async);

  void getUserHistoryForList(int userid, Collection<String> ids, long latestResultID,
                             Map<String, Collection<String>> typeToSection, long userListID, AsyncCallback<AVPScoreReport> async);

  void logEvent(String id, String widgetType, String exid, String context, int userid, String hitID, String device,
                AsyncCallback<Void> async);

  void getEvents(AsyncCallback<List<Event>> async);

  void markState(String id, STATE state, int creatorID, AsyncCallback<Void> async);

  void markAudioDefect(AudioAttribute audioAttribute, String exid, AsyncCallback<Void> async);

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
                    String identifier,
                    int reqid, String device, AsyncCallback<AudioAnswer> async);

  void getNumResults(AsyncCallback<Integer> async);

  void getResultAlternatives(Map<String, String> unitToValue,
                             int userid, String flText, String which,
                             AsyncCallback<Collection<String>> async);

  void addRoundTrip(long resultid, int roundTrip, AsyncCallback<Void> async);

  void getResultASRInfo(int resultID, int width, int height, AsyncCallback<PretestScore> async);


  void getASRScoreForAudioPhonemes(int reqid, long resultID, String testAudioFile, String sentence,
                                   int width, int height, boolean useScoreToColorBkg, String exerciseID,
                                   AsyncCallback<PretestScore> async);

  void getContextPractice(AsyncCallback<ContextPractice> async);

  void reloadExercises(AsyncCallback<Void> async);
}
