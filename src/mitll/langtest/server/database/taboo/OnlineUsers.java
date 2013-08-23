package mitll.langtest.server.database.taboo;

import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.taboo.PartnerState;
import mitll.langtest.shared.taboo.StimulusAnswerPair;
import mitll.langtest.shared.taboo.TabooState;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
  //private Collection<User> active = new HashSet<User>();
  private Collection<Pair> candidates = new ArrayList<Pair>();

  // TODO : write to database
  private Map<User,User> giverToReceiver = new HashMap<User,User>();
  // TODO : write to database
  private Map<User,StimulusAnswerPair> receiverToStimulus = new HashMap<User,StimulusAnswerPair>();
  // TODO : write to database
  private Map<User,Map<String,List<AnswerBundle>>> receiverToAnswer = new HashMap<User, Map<String, List<AnswerBundle>>>();
  private Map<User,Map<String, Collection<String>>> receiverToState = new HashMap<User, Map<String, Collection<String>>>();

  // TODO keep track of join time, order pairings on that basis

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
      logger.info("---> addOnline now " + getOnline().size() + " online...");
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
        //logger.debug("isPartnerOnline : for giver " + giverOrReceiver + ", receiver  " + receiver + " is online, state " + typeToSelectionByPartner);
        return new PartnerState(true,typeToSelectionByPartner);
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

    boolean giver = false;
    boolean receiver = false;
    for (User u : giverToReceiver.keySet()) if (u.id == userid) giver = true;
    for (User u : giverToReceiver.values()) if (u.id == userid) receiver = true;

    if (giver && receiver) {  // sanity check
      logger.error("\n\n---> huh? how can " + userid + " be both giver and receiver?\n\n");
    }
    boolean joined = giver || receiver;

    if (joined) {
      logger.info("yea! just joined " + userid + " giver " + giver + " receiver " + receiver);
    } else if (avail) { // take us out of the pool
      addCandidatePair(userid);
    }

    TabooState tabooState = new TabooState(avail, joined, giver);
   // logger.debug("returning " + tabooState);
    return tabooState;
  }

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
 /*       active.add(first);
        active.add(second);*/
        break;
      }
    }
  }

  /**
   * User has chosen to be either a giver or receiver.
   *
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
   // addActive(giver);
   // addActive(receiver);
  }

  /**
   * 0 == OK
   * 1 == inactive receiver
   * 2 == paused...
   *
   *
   *
   *
   * @param userid
   * @param exerciseID
   * @param stimulus
   * @param answer
   * @param onLastStimulus
   * @param skippedItem
   * @return
   */
  public synchronized int sendStimulus(long userid, String exerciseID, String stimulus, String answer, boolean onLastStimulus, boolean skippedItem) {
    User receiver = getReceiverForGiver(userid);
    if (receiver == null) {
      return 1;
    }
    logger.debug("sending " + stimulus + " to " + receiver + " from giver " + userid);
    receiverToStimulus.put(receiver, new StimulusAnswerPair(exerciseID, stimulus, answer, onLastStimulus, skippedItem));
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
    receiverToStimulus.remove(getUser(receiverUserID));

    User receiver = getUser(receiverUserID);
    Map<String, List<AnswerBundle>> stimToAnswer = receiverToAnswer.get(receiver);
    if (stimToAnswer == null) {
      receiverToAnswer.put(receiver, stimToAnswer = new HashMap<String, List<AnswerBundle>>());
    }
    List<AnswerBundle> answerBundles = stimToAnswer.get(stimulus);
    if (answerBundles == null) {
      stimToAnswer.put(stimulus, answerBundles = new ArrayList<AnswerBundle>());
    }
    answerBundles.add(new AnswerBundle(stimulus,answer,correct));

    logger.debug("registerAnswer : user->answer now " + receiverToAnswer);
  }

 // int count = 0;

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#checkCorrect
   * @param giverUserID
   * @param stimulus
   * @return
   */
  public synchronized int checkCorrect(long giverUserID, String stimulus) {
    User receiver = getReceiverForGiver(giverUserID);
   // logger.debug("Giver " + giverUserID + " checking for answer from " + receiver);

    Map<String, List<AnswerBundle>> stimToAnswer = receiverToAnswer.get(receiver);
    if (stimToAnswer == null) {
    //  logger.debug("no answer yet...");
      return -1;
    }
    else {
      List<AnswerBundle> answerBundles = stimToAnswer.get(stimulus);
      if (answerBundles == null) {
        //if (count++ < 4) logger.error("huh? '" +stimulus + "' is not recorded in " + stimToAnswer.keySet() + " for " + receiver);
        return -1;
      }
      else {
        AnswerBundle answerBundle = answerBundles.get(answerBundles.size() - 1);
        int isCorrectResponse = answerBundle.correct ? 1 : 0;
        stimToAnswer.remove(stimulus); // sent response to giver -- no need to remember them anymore
        return isCorrectResponse;
      }
    }
  }

  /**
   * @see #checkCorrect(long, String)
   * @see #sendStimulus(long, String, String, String, boolean, boolean)
   * @param giver
   * @return
   */
  private User getReceiverForGiver(long giver) {
    User giverUser = getUser(giver);
    if (giverUser == null) {
      logger.warn("huh? getReceiverForGiver " + giver + " is unknown.");
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

  public void registerSelectionState(long receiver, Map<String, Collection<String>> selectionState) {
    receiverToState.put(getUser(receiver),selectionState);
  }

  private static class Pair {
    User first = null, second = null;
    public Pair(User first, User second) { this.first = first; this.second = second; }
    public String toString() { return "Pair : " + first + " and " + second; }
  }

  private static class AnswerBundle {
    String stimulus;
    String answer;
    boolean correct;
    long timestamp;

    public AnswerBundle(String stimulus, String answer, boolean correct) {
      this.stimulus = stimulus;
      this.answer = answer;
      this.correct = correct;
      this.timestamp = System.currentTimeMillis();
    }

    public String toString() { return /*"stim : " + stimulus +*/ " answer '" +  answer+
      "' is " +(correct ? "correct" : "incorrect") + " at " + new Date(timestamp);
    }
  }
}
