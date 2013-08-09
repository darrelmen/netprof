package mitll.langtest.server.database;

import com.github.gwtbootstrap.client.ui.Modal;
import mitll.langtest.shared.TabooState;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
  private Map<User,User> giverToReceiver = new HashMap<User,User>();

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

  public TabooState anyAvailable(long userid) {
    int diff = online.size() - active.size();
    logger.info("online " + online.size() + " active " + active.size() + " available = " + diff);
    boolean avail = diff > 1;

    boolean giver = false;
    boolean receiver = false;
    for (User u : giverToReceiver.keySet()) if (u.id == userid) giver = true;
    for (User u : giverToReceiver.values()) if (u.id == userid) receiver = true;
    boolean joined = giver || receiver;

    if (joined) {
     logger.info("yea! just joined " + userid);
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

    return new TabooState(avail, joined);
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

  public void removePair(User giver, User receiver) {
    giverToReceiver.remove(giver);
    removeActive(giver);
    removeActive(receiver);
  }

  private static class Pair {
    User first = null, second = null;
    public Pair(User first, User second) { this.first = first; this.second = second; }
    public String toString() { return "Pair : " + first + " and " + second; }
  }
}
