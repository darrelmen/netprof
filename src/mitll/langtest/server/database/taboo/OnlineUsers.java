package mitll.langtest.server.database.taboo;

import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.User;
import mitll.langtest.shared.flashcard.Leaderboard;
import mitll.langtest.shared.flashcard.ScoreInfo;
import mitll.langtest.shared.taboo.AnswerBundle;
import mitll.langtest.shared.taboo.Game;
import mitll.langtest.shared.taboo.GameInfo;
import mitll.langtest.shared.taboo.PartnerState;
import mitll.langtest.shared.taboo.StimulusAnswerPair;
import mitll.langtest.shared.taboo.TabooState;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/9/13
 * Time: 4:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class OnlineUsers {
  private static final Logger logger = Logger.getLogger(OnlineUsers.class);

  private final UserDAO userDAO;
  private Collection<User> online = new HashSet<User>();
  private Collection<Pair> candidates = new ArrayList<Pair>();

  // TODO : write to database
  private Map<User,User> giverToReceiver = new HashMap<User,User>();
  // TODO : write to database
  private Map<User,StimulusAnswerPair> receiverToStimulus = new HashMap<User,StimulusAnswerPair>();
  // TODO : write to database
  private Map<User,AnswerBundle> receiverToAnswer = new HashMap<User, AnswerBundle>();
  private Map<User,Map<String, Collection<String>>> receiverToState = new HashMap<User, Map<String, Collection<String>>>();
  private Map<User,Game> receiverToGame = new HashMap<User, Game>();
  private Map<String,Leaderboard> stateToScores = new HashMap<String,Leaderboard>();

  public OnlineUsers(UserDAO userDAO) { this.userDAO = userDAO; }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#userOnline(long, boolean)
   * @param userid
   */
  public synchronized void addOnline(long userid) {
    User userWhere = getUser(userid);
    int before = online.size();
    if (userWhere != null) online.add(userWhere);

    if (online.size() != before) {
      logger.info("---> addOnline online now " + getOnline());
    }
    else {
      //logger.info("---> addOnline online user " + userid + " now "+getOnline().size() + " online...");
    }
  }

  private User getUser(long userid) {
    return userDAO.getUserWhere(userid);
  }

  public synchronized void removeOnline(long userid) {
    User userWhere = getUser(userid);
    if (userWhere != null) online.remove(userWhere);

    logger.info("---> removeOnline : removed " + userid + " online now " + getOnline());
  }

  private Collection<User> getOnline() { return online; }

  /**
   * For any partner query, there are three states:
   *
   * 1) The partner is online, so you return true (plus some info)
   *  -- if I'm a giver, I also expect the receiver to tell me which chapters we're doing...
   * 2) The partner is offline, in which case we clean up the giver->receiver map
   * 3) The partner claims they have been paired, but we've erased the pairing in the giver->receiver map
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#isPartnerOnline(long, boolean)
   * @param giverOrReceiver
   * @param isGiver
   * @return
   */
  public PartnerState isPartnerOnline(long giverOrReceiver, boolean isGiver) {
    //  logger.debug("isPartnerOnline : checking for partner of " + giverOrReceiver);
    User testUser = getUser(giverOrReceiver);
    if (testUser == null) {
      logger.error("isPartnerOnline : giver " + giverOrReceiver + " is unknown...?");

      checkReceiverForGiver(giverOrReceiver);
      return new PartnerState(); // huh?
    }

    // case 1 : I'm the giverOrReceiver
    if (isGiver) {
      // User giverUser = getUser(giverOrReceiver);
      User receiver = giverToReceiver.get(testUser);
      if (receiver == null) {
        // giverOrReceiver is not a giver or we've cleaned up the entry already...(?)
        logger.warn("isPartnerOnline : user " + giverOrReceiver + " is not the giver (???)");

        return new PartnerState();
      } else if (online.contains(receiver)) {
        Map<String, Collection<String>> typeToSelectionByPartner = receiverToState.get(receiver);
        GameInfo game = getGame(giverOrReceiver, isGiver);
        if (game == null) logger.error("huh? no game state for giver " + giverOrReceiver);
        //logger.debug("isPartnerOnline : for giver " + giverOrReceiver + ", receiver  " + receiver + " is online, state " + typeToSelectionByPartner);
        return new PartnerState(true, typeToSelectionByPartner, game);
      } else {
        logger.debug("isPartnerOnline : for giver " + giverOrReceiver + ", receiver  " + receiver + " is not online...");
        checkReceiverForGiver(giverOrReceiver);

        return new PartnerState();
      }
    } else {
      // case 2 : I'm the receiver
      User giverForReceiver = getGiverForReceiver(giverOrReceiver);
      if (giverForReceiver == null) {
        logger.warn("isPartnerOnline : user " + giverOrReceiver + " is not the receiver (???)");
        return new PartnerState();
      } else if (online.contains(giverForReceiver)) {
  //      logger.debug("isPartnerOnline : for receiver " + giverOrReceiver + ", giver  " + giverForReceiver + " is online.");
        return new PartnerState(true,null);
      } else {
        logger.debug("isPartnerOnline : for receiver " + giverOrReceiver + " giver " + giverForReceiver + " is not online...");
        checkReceiverForGiver(giverOrReceiver);

        return new PartnerState();
      }
    }
  }

  private User getGiverForReceiver(long receiver) {
    // case 2 : I'm the receiver
    for (Map.Entry<User, User> pair : giverToReceiver.entrySet()) {
      if (pair.getValue().id == receiver) {
        return pair.getKey();
      }
    }
    return null;
  }

  /**
   * The given user asks whether they're in a pairing.
   * We check the current map of giver->receiver to see if the user is included.
   * If they are, we return which side of the relationship they're on.
   *
   * So the state machine : online->active (when two online have been paired but haven't accepted their roles)->paired
   * @see mitll.langtest.server.LangTestDatabaseImpl#anyUsersAvailable(long)
   * @param userid
   * @return
   */
  public synchronized TabooState anyAvailable(long userid) {
    int diff = online.size() - candidates.size() * 2 - giverToReceiver.size() * 2;
    //logger.info("online " + online.size() + " active " + active.size() + " available = " + diff);
    boolean avail = diff > 1;
    if (avail) {
      logger.info("anyAvailable: online " + online.size() + " candidate pairs " + candidates.size() * 2 + " registered pairs " +
        giverToReceiver.size() * 2 + " available = " + diff);
    }
    else {
/*      logger.info("anyAvailable: online " + online.size() + " candidate pairs " + candidates.size() * 2 + " registered pairs " +
        giverToReceiver.size() * 2 + " available = " + diff);*/
    }

    boolean giver = false;
    boolean receiver = false;
    for (User u : giverToReceiver.keySet()) if (u.id == userid) giver = true;
    for (User u : giverToReceiver.values()) if (u.id == userid) receiver = true;

    if (giver && receiver) {  // sanity check
      logger.error("\n\n---> anyAvailable : huh? how can " + userid + " be both giver and receiver?\n\n");
    }
    boolean joined = giver || receiver;

    if (joined) {
      logger.info("anyAvailable : yea! just joined " + userid + " giver " + giver + " receiver " + receiver);

      User receiverUser;
      long giverID, receiverID;
      if (giver) {
        receiverUser = giverToReceiver.get(getUser(userid));
        giverID = userid;
        receiverID = receiverUser.id;
      } else {
        giverID = getGiverForReceiver(userid).id;
        receiverID = userid;
      }
      logger.info("anyAvailable : giver : " + giverID + " and receiver " + receiverID);
    } else if (avail) { // take us out of the pool
      addCandidatePair(userid);
    }

    TabooState tabooState = new TabooState(avail, joined, giver);
   // logger.debug("returning " + tabooState);
    return tabooState;
  }

  /**
   * Just for debugging
   */
  private long lastTimestamp;
  public GameInfo getGame(long userID, boolean isGiver) {
    Game game = getGameFor(userID, isGiver);
    // if (gameItems == null) logger.error("getGame : game for " + userID + " has not started?");
    GameInfo gameInfo = game.getGameInfo();
    if (gameInfo.getTimestamp() != lastTimestamp) {
      logger.info("OnlineUsers.getGame for " + userID + " game info " + gameInfo);
      lastTimestamp = gameInfo.getTimestamp();
    }
    return gameInfo;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#startGame
   * @see mitll.langtest.client.taboo.ReceiverExerciseFactory#startGame()
   * @param userID
   * @param startOver
   * @return
   */
  public GameInfo startGame(long userID, boolean startOver) {
    Game game = getGameFor(userID, false);
    if (startOver) {
      logger.info("startGame for " + userID+" starting over...");

      game.resetToFirstGame();
      receiverToStimulus.remove(getUser(userID));
    }
    List<ExerciseShell> itemsInGame = game.startGame();
    if (itemsInGame == null) logger.error("startGame huh? game for " + userID + " has not started???\n\n\n");
    GameInfo gameInfo = game.getGameInfo();
    logger.info("startGame for " + userID+" game info " + gameInfo);

    return gameInfo;
  }

  private Game getGameFor(long userID, boolean isGiver) {
    User receiverUser = isGiver ? giverToReceiver.get(getUser(userID)) : getUser(userID);
    return receiverToGame.get(receiverUser);
  }

  /**
   * @see #anyAvailable(long)
   * @param userid
   */
  private void addCandidatePair(long userid) {
    User first = null, second = null;
    for (User u : online) {
      if (u.id == userid) {
        first = u;
      } else {
        second = u;
      }
      if (first != null && second != null) {
        candidates.add(new Pair(first, second));
        break;
      }
    }
  }

  /**
   * User has chosen to be either a giver or receiver.
   * @see mitll.langtest.server.LangTestDatabaseImpl#registerPair(long, boolean)
   * @see mitll.langtest.client.taboo.Taboo#askUserToChooseRole(long)
   * @param userid
   * @param isGiver
   */
  public synchronized void registerPair(long userid, boolean isGiver) {
    Pair found = null;

    for (Pair p : candidates) {
      if ((p.first.id == userid|| p.second.id == userid)) {
        found = p;
        break;
      }
    }

    User giver = null, receiver = null;

    if (found != null) {
      if (isGiver) {
        if (found.first.id == userid) {
          giver = found.first;
          receiver = found.second;
        }
        else if (found.second.id == userid) {
          giver = found.second;
          receiver = found.first;
        }
      }
      else {
        if (found.second.id == userid) {
          giver = found.first;
          receiver = found.second;
        }
        else if (found.first.id == userid) {
          giver = found.second;
          receiver = found.first;
        }
      }
      candidates.remove(found);
    }
    else {
      logger.error("huh? " + giver  + " and " + receiver + " were never a candidate pair.");
    }

    giverToReceiver.put(giver, receiver);

    logger.info("Yea! pair established " + giver + " -> " + receiver);
  }

  /**
   * 0 == OK
   * 1 == inactive receiver
   * 2 == paused...
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#sendStimulus
   * @see mitll.langtest.client.taboo.GiverExerciseFactory.GiverPanel#sendStimulus
   * @param userid
   * @param exerciseID
   * @param stimulus
   * @param answer
   * @param onLastStimulus
   * @param numClues
   * @param isGameOver
   * @param giverChosePoorly
   * @return
   */
  public synchronized int sendStimulus(long userid, String exerciseID, String stimulus, String answer,
                                       boolean onLastStimulus, int numClues, boolean isGameOver, boolean giverChosePoorly) {
    User receiver = getReceiverForGiver(userid);
    if (receiver == null) {
      return 1;
    }
    logger.debug("OnlineUsers.sendStimulus : sending " + stimulus + " to " + receiver + " from giver " +
      userid + " on last stim " + onLastStimulus + " game over " + isGameOver);
    receiverToStimulus.put(receiver, new StimulusAnswerPair(exerciseID, stimulus, answer, onLastStimulus, numClues, isGameOver, giverChosePoorly));
    return 0;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#checkForStimulus(long)
   * @param receiverUserID
   * @return
   */
  public synchronized StimulusAnswerPair checkForStimulus(long receiverUserID) {
    StimulusAnswerPair stimulusAnswerPair = receiverToStimulus.get(getUser(receiverUserID));
    if (stimulusAnswerPair == null) {
      stimulusAnswerPair = new StimulusAnswerPair();
      stimulusAnswerPair.setNoStimYet(true);
    }
    return stimulusAnswerPair;
  }

  public synchronized void registerAnswer(long receiverUserID, String stimulus, String answer, boolean correct) {
    User receiver = getUser(receiverUserID);
    if (getGiverForReceiver(receiverUserID) == null) {
 //     logger.debug("not remembering answer, since " + receiverUserID + " is playing by him/herself.");
    } else {
      receiverToStimulus.remove(receiver);
      answer = new ProfanityCleaner().replaceProfanity(answer);
      receiverToAnswer.put(receiver,new AnswerBundle(stimulus, answer, correct));

      logger.debug("OnlineUsers.registerAnswer : user->answer now " + receiverToAnswer);
    }
  }

  /**
   * Make sure this answer is for the expected question/stimulus.
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#checkCorrect
   * @see mitll.langtest.client.taboo.GiverExerciseFactory.GiverPanel#checkForCorrect
   * @param giverUserID
   * @return
   */
  public synchronized AnswerBundle checkCorrect(long giverUserID, String stimulus) {
    User receiver = getReceiverForGiver(giverUserID);
    //logger.debug("OnlineUsers.checkCorrect : Giver " + giverUserID + " checking for answer from " + receiver.id);

    AnswerBundle answerBundle = receiverToAnswer.remove(receiver);// sent response to giver -- no need to remember them anymore
    if (answerBundle == null) {
      answerBundle = new AnswerBundle();
    }
    else if (!answerBundle.getStimulus().contains(stimulus)) {  // TODO : this is kinda cheesy
      logger.info("\tOnlineUsers.checkCorrect : answer stim " + answerBundle.getStimulus() + " not same as " + stimulus);
      answerBundle = new AnswerBundle();
    } else {
      logger.debug("\tOnlineUsers.checkCorrect : Giver " + giverUserID + " checking for answer from " + receiver.id + " got " + answerBundle);
    }
    return answerBundle;
  }

  /**
   * @see #checkCorrect
   * @see #sendStimulus(long, String, String, String, boolean, int, boolean, boolean)
   * @param giver
   * @return
   */
  private User getReceiverForGiver(long giver) {
    User giverUser = getUser(giver);
    if (giverUser == null) {
      logger.error("huh? getReceiverForGiver " + giver + " is unknown.");
      return null;
    }
    return giverToReceiver.get(giverUser);
  }

  /**
   * Clean up the giver->receiver mapping if either the giver or receiver signs out.
   * @param giverOrReceiver
   */
  private void checkReceiverForGiver(long giverOrReceiver) {
    // case 1: user is a giver
    User giverUser = getUser(giverOrReceiver);
    if (!online.contains(giverUser)) {
      logger.debug("checkReceiverForGiver : Giver " + giverOrReceiver + " is not online...");
      if (giverToReceiver.containsKey(giverUser)) giverToReceiver.remove(giverUser);
    } else {
      User receiver = giverToReceiver.get(giverUser);
      if (!online.contains(receiver)) {
        logger.debug("checkReceiverForGiver : receiver " + receiver + " is not online...");
        giverToReceiver.remove(giverUser);
      }
    }
    // case 2: user is a receiver

    User giverForReceiver = getGiverForReceiver(giverOrReceiver);
    if (giverForReceiver != null &&!online.contains(giverForReceiver)) {
      logger.debug("checkReceiverForGiver : for receiver " + giverOrReceiver + " giver " + giverForReceiver + " is not online...");
      giverToReceiver.remove(giverForReceiver);
    }
  }

  /**
   * @see mitll.langtest.client.taboo.TabooExerciseList#tellPartnerMyChapterSelection(mitll.langtest.client.exercise.SelectionState)
   * @param receiver
   * @param selectionState
   * @param exercisesForSection
   */
  public void registerSelectionState(long receiver, Map<String, Collection<String>> selectionState, List<ExerciseShell> exercisesForSection) {
    logger.debug("-----> registerSelectionState : for receiver " + receiver+ " selectionState " + selectionState);

    User user = getUser(receiver);
    Map<String, Collection<String>> current = receiverToState.get(user);
    boolean sameSelection = current != null && current.equals(selectionState);
    if (!sameSelection) {
      receiverToState.put(user, selectionState);

      Game game = receiverToGame.get(user);
      logger.debug("registerSelectionState.previous game was " + game);
      Game newGame = new Game(exercisesForSection);
      if (game != null && game.hasStarted()) {
        newGame.startGame();
      }
      receiverToGame.put(user, newGame);
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#postGameScore(long, int, int)
   * @see mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel#dealWithGameOver(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel, boolean, boolean)
   * @param receiverID
   * @param score
   * @param maxPossibleScore
   */
  public synchronized void postGameScore(long receiverID, int score, int maxPossibleScore) {
    User receiver = getUser(receiverID);
    Map<String, Collection<String>> selectionState = receiverToState.get(receiver);
    Leaderboard leaderboard = stateToScores.get(selectionState.toString());
    if (leaderboard == null) stateToScores.put(selectionState.toString(), leaderboard = new Leaderboard());

    User giverForReceiver = getGiverForReceiver(receiverID);

    long giverID = giverForReceiver == null ? -1 : giverForReceiver.id;
    leaderboard.addScore(new ScoreInfo(receiver.id, giverID, score, maxPossibleScore-score, 0l, selectionState));  // TODO fill in time taken?
     logger.debug("state->scores now " + stateToScores);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getLeaderboard
   * @see mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel#dealWithGameOver
   * @param selectionState
   * @return
   */
  public synchronized Leaderboard getLeaderboard(Map<String, Collection<String>> selectionState) {
    Leaderboard leaderboard = stateToScores.get(selectionState.toString());
    if (leaderboard == null) logger.error("huh? no scores for " + selectionState.toString());
    return leaderboard;
  }

  private static class Pair {
    User first = null, second = null;
    public Pair(User first, User second) { this.first = first; this.second = second; }
    public String toString() { return "Pair : " + first + " and " + second; }
  }
}
