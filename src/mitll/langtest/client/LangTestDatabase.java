package mitll.langtest.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.STATE;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:50 PM
 * To change this template use File | Settings | File Templates.
 */
@RemoteServiceRelativePath("langtestdatabase")
public interface LangTestDatabase extends RemoteService {
  boolean WRITE_ALTERNATE_COMPRESSED_AUDIO = false;

  ExerciseListWrapper getExerciseIds(int reqID, Map<String, Collection<String>> typeToSelection, String prefix, long userListID, int userID, String role, boolean onlyUnrecordedByMe);

  CommonExercise getExercise(String id, long userID);

  void markAudioDefect(AudioAttribute audioAttribute, String exid);
  void markGender(AudioAttribute attr, boolean isMale);
  // user DAO
  long addUser(int age, String gender, int experience, String nativeLang, String dialect, String userID, Collection<User.Permission> permissions);

  List<User> getUsers();
  int userExists(String login);
  User getUserBy(long id);

  // answer DAO
  void addTextAnswer(int userID, CommonExercise exercise, int questionID, String answer, String answerType);
  AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, int question, int user,
                             int reqid, boolean flq, String audioType, boolean doFlashcard, boolean recordInResults, boolean addToAudioTable, boolean recordedWithFlash);

  ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height, String exerciseID);

  PretestScore getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence, int width, int height, boolean useScoreToColorBkg, String exerciseID);

  // monitoring support

  Map<User, Integer> getUserToResultCount();

  Map<Integer, Integer> getResultCountToCount();

  Map<String,Integer> getResultByDay();
  Map<String,Integer> getResultByHourOfDay();

  Map<String, Float> getMaleFemaleProgress();

  Map<String, Map<String, Integer>> getResultPerExercise();
  List<Session> getSessions();

  int getNumResults();


  List<Result> getResults(int start, int end, String sortInfo);

  Map<String,Number> getResultStats();

  Map<String, Map<Integer, Integer>> getResultCountsByGender();
  Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts();
  Map<Integer, Map<String, Map<String, Integer>>> getGradeCountPerExercise();

  void logMessage(String message);

  List<AVPHistoryForList> getUserHistoryForList(long userid, Collection<String> ids, long latestResultID);

  StartupInfo getStartupInfo();
  long addUserList(long userid, String name, String description, String dliClass, boolean isPublic);
  void setPublicOnList(long userListID, boolean isPublic);
  void addVisitor(long userListID, long user);
  Collection<UserList> getListsForUser(long userid, boolean onlyCreated, boolean visited);
  Collection<UserList> getUserListsForText(String search, long userid);
  void addItemToUserList(long userListID, UserExercise userExercise);

  boolean isValidForeignPhrase(String foreign);

  UserExercise reallyCreateNewItem(long userListID, UserExercise userExercise);

  UserExercise duplicateExercise(UserExercise id);

  void editItem(UserExercise userExercise);

  void addAnnotation(String exerciseID, String field, String status, String comment, long userID);
  void markReviewed(String exid, boolean isCorrect, long creatorID);
  void markState(String id, STATE state, long creatorID);

  void setAVPSkip(Collection<Long> ids);

  void setExerciseState(String id, STATE state, long userID);

  List<UserList> getReviewLists();

  boolean deleteList(long id);
  boolean deleteItemFromList(long listid, String exid);
  boolean deleteItem(String exid);

  void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID);
  List<Event> getEvents();
}
