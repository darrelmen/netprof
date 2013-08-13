package mitll.langtest.server.database;

import mitll.langtest.shared.StimulusAnswerPair;
import mitll.langtest.shared.TabooState;
import mitll.langtest.shared.User;
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
  private Collection<User> active = new HashSet<User>();
  private Collection<Pair> candidates = new ArrayList<Pair>();

  // TODO : write to database
  private Map<User,User> giverToReceiver = new HashMap<User,User>();
  // TODO : write to database
  private Map<User,StimulusAnswerPair> receiverToStimulus = new HashMap<User,StimulusAnswerPair>();
  // TODO : write to database
  private Map<User,Map<String,List<AnswerBundle>>> receiverToAnswer = new HashMap<User, Map<String, List<AnswerBundle>>>();

  // TODO keep track of join time, order pairings on that basis

  public OnlineUsers(UserDAO userDAO) { this.userDAO = userDAO; }

  public void addOnline(long userid) {
    User userWhere = getUser(userid);
    if (userWhere != null) online.add(userWhere);

    logger.info("\n---> addOnline online now " + getOnline());
  }

  private User getUser(long userid) {
    return userDAO.getUserWhere(userid);
  }

  public void removeOnline(long userid) {
    User userWhere = getUser(userid);
    if (userWhere != null) online.remove(userWhere);

    logger.info("removeOnline online now " + getOnline());
  }

  public Collection<User> getOnline() {
    return online;
  }

  public void addActive(User user) {
    if (!online.contains(user)) logger.error("huh" + user + " is not online.");
    active.add(user);
  }

  public void removeActive(User user) {
    boolean remove = active.remove(user);
    if (!remove) logger.error("huh" + user + " was not active.");
  }

  /**
   * The given user asks whether they're in a pairing.
   * We check the current map of giver->receiver to see if the user is included.
   * If they are, we return which side of the relationship they're on.
   *
   * @param userid
   * @return
   */
  public TabooState anyAvailable(long userid) {
    int diff = online.size() - active.size();
    logger.info("online " + online.size() + " active " + active.size() + " available = " + diff);
    boolean avail = diff > 1;

    boolean giver = false;
    boolean receiver = false;
    for (User u : giverToReceiver.keySet()) if (u.id == userid) giver = true;
    for (User u : giverToReceiver.values()) if (u.id == userid) receiver = true;

    if (giver && receiver) {  // sanity check
      logger.error("\n\n---> huh? how can " + userid + " be both giver and receiver?");
    }
    boolean joined = giver || receiver;

    if (joined) {
     logger.info("yea! just joined " + userid + " giver " + giver + " receiver " + receiver);
    } else if (avail) { // take us out of the pool
      User first = null, second = null;
      for (User u : online) {
        if (u.id == userid) {
          first = u;
        } else {
          second = u;
        }
        if (first != null && second != null) {
          candidates.add(new Pair(first, second));
          active.add(first);
          active.add(second);
          break;
        }
      }
    }

    TabooState tabooState = new TabooState(avail, joined, giver);
    logger.debug("returning " + tabooState);
    return tabooState;
  }

  public void registerPair(long userid, boolean isGiver) {
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

  public void sendStimulus(long userid, String stimulus, String answer) {
    User receiver = getReceiverForGiver(userid);
    logger.debug("sending " + stimulus + " to " + receiver + " from giver " + userid);
    receiverToStimulus.put(receiver, new StimulusAnswerPair(stimulus, answer));
  }

  public StimulusAnswerPair checkForStimulus(long receiverUserID) {
    return receiverToStimulus.get(getUser(receiverUserID));
  }

  public void registerAnswer(long receiverUserID, String stimulus, String answer, boolean correct) {
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

    logger.debug("user->answer now " + receiverToAnswer);
  }

  int count = 0;

  /**
   * @see DatabaseImpl#checkCorrect(long, String)
   * @param giverUserID
   * @param stimulus
   * @return
   */
  public int checkCorrect(long giverUserID, String stimulus) {
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
        return answerBundle.correct ? 1 : 0;
      }
    }
  }

  private User getReceiverForGiver(long userid) {
    return giverToReceiver.get(getUser(userid));
  }

  /*  public void removePair(User giver, User receiver) {
    giverToReceiver.remove(giver);
    removeActive(giver);
    removeActive(receiver);
  }*/

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
  }
}
