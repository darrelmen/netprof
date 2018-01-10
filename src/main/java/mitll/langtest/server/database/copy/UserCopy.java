package mitll.langtest.server.database.copy;

import mitll.hlt.domino.shared.model.user.AccountDetail;
import mitll.hlt.domino.shared.model.user.ClientUserDetail;
import mitll.hlt.domino.shared.model.user.DBUser;
import mitll.hlt.domino.shared.model.user.Group;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.result.UserToCount;
import mitll.langtest.server.database.user.*;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickUserProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mitll.langtest.server.database.user.DominoUserDAOImpl.NETPROF;

/**
 * Created by go22670 on 10/26/16.
 */
public class UserCopy {
  private static final Logger logger = LogManager.getLogger(UserCopy.class);

  private static final boolean DEBUG = false;
  private static final boolean MAKE_COLLISION_ACCOUNT = false;
  private static final boolean WARN_ON_COLLISION = true;
  private static final String D_ADMIN = "d.admin";
  private static final String UNALLOWED_REGEX = "[^a-zA-Z0-9_\\-.]";
  private static final String UNKNOWN = "unknown";
  private static final String USER_LANG_SEPARATOR = "_";

  /**
   * What can happen:
   * <p>
   * 1) User id and password match - no action - they're already there
   * 2) User id matches, but password doesn't - go ahead and add the user id and password
   * -- could be multiple users with same userid but different password... trouble later on?
   * 3) User id is new - add a new user
   * <p>
   * Worry about how to merge two or more user tables -
   * find existing ids in postgres...
   * if some from import db aren't there, add them
   * return map of old ids -> new ids
   * <p>
   * For collisions -- must reset password -- someone loses.
   * <p>
   * Also, going forward, we must store emails, since we need to be able to send the sign up message?
   *
   * @param db
   * @param projid       to import into
   * @param oldResultDAO - so we can check if the user ever recorded anything - if not we skip them on import
   * @param optName      if you want to name the project something other than the language
   * @see CopyToPostgres#copyOneConfig
   */
  Map<Integer, Integer> copyUsers(DatabaseImpl db, int projid, IResultDAO oldResultDAO, String optName) throws Exception {
    DominoUserDAOImpl dominoUserDAO = (DominoUserDAOImpl) db.getUserDAO();

    Map<Integer, Integer> oldToNew = new HashMap<>();
    addDefaultUsers(oldToNew, dominoUserDAO);

    IUserProjectDAO slickUserProjectDAO = db.getUserProjectDAO();

    UserDAO userDAO = new UserDAO(db);
    int defectDetector = userDAO.getDefectDetector();
    oldToNew.put(defectDetector, dominoUserDAO.getDefectDetector());

    List<User> importUsers = userDAO.getUsers();
    logger.info("copyUsers h2 importUsers  " + importUsers.size());
    UserToCount userToNumAnswers = oldResultDAO.getUserToNumAnswers();
    Map<Integer, Integer> idToCount = userToNumAnswers.getIdToCount();
    if (DEBUG) logger.info("copyUsers id->childCount " + idToCount.size() + " values " + idToCount.values().size());

    Group group = dominoUserDAO.getGroup();

    int collisions = 0;
    int lurker = 0;
    List<ClientUserDetail> added = new ArrayList<>();
    int c = 0;
    Map<Integer, Long> userToCreation = new HashMap<>();

    for (User toImport : importUsers) {
      c++;

      int importID = toImport.getID();
      String importUserID = getNormalizedUserID(toImport);

      if (DEBUG) logger.info("copyUsers copying " + importID + " : " + importUserID);

      if (importID != defectDetector && !dominoUserDAO.isDefaultUser(importUserID)) {
        if (importUserID.isEmpty() && idToCount.get(importID) != null && idToCount.get(importID) == 0) {
          logger.info("copyUsers skipping old user " + toImport + " since they have an empty user name and no recordings");
          lurker++;
        } else {
          if (DEBUG) logger.info("copyUsers #" + c + "/" + importUsers.size() + " : import " + toImport);
          mitll.hlt.domino.shared.model.user.DBUser dominoDBUser = dominoUserDAO.getDBUser(importUserID);

          if (dominoDBUser == null) { // new user
//            logger.info("copyUsers no existing user id '" + importUserID + "'");
            ClientUserDetail newUser = addUser(dominoUserDAO, oldToNew, toImport, optName, group);
            userToCreation.put(newUser.getDocumentDBID(), getAccountCreationTime(newUser));
            added.add(newUser);
          } else { // user exists
            if (dominoDBUser.getUserId().equals(D_ADMIN)) {
              logger.warn("copyUsers found d.admin " + dominoDBUser);
            } else {
              userToCreation.put(dominoDBUser.getDocumentDBID(), getAccountCreationTime(dominoDBUser));
            }

            if (foundExistingUser(projid,
                optName,
                dominoUserDAO,
                oldToNew,
                added,
                toImport,
                dominoDBUser,
                group)) {
              collisions++;
            }
          }
        }
      }
    }

    addUserProjectBinding(slickUserProjectDAO, projid, userToCreation);

    logger.info("copyUsers after, postgres importUsers " +
        //"num = " + dominoUserDAO.getUsers().size() +
        "\n\tadded          " + added.size() +
        "\n\tcollisions     " + collisions +
        "\n\tlurker         " + lurker +
        "\n\tuserToCreation " + userToCreation.size()
    );

    return oldToNew;
  }

  private long getAccountCreationTime(DBUser user) {
    return user.getAcctDetail().getCrTime().getTime();
  }

  @NotNull
  private String getNormalizedUserID(User toImport) {
    String importUserID = toImport.getUserID();
    // deal with spaces

    if (hasSpaces(importUserID)) {
//      logger.info("replacing spaces in " +importUserID);
      importUserID = importUserID.trim().replaceAll("\\s++", "_");
//      logger.info("copyUsers now no spaces in " + importUserID);
      toImport.setUserID(importUserID);
    }

    // deal with short user id
    if (importUserID.length() == 4) { // domino only really wants user ids that are 5 long, and old netprof would allow 4 (like "demo")
      importUserID += "_";
      toImport.setUserID(importUserID);
    }

    // deal with punct

    String normalized = importUserID.replaceAll(UNALLOWED_REGEX, "_");
    if (!normalized.equals(importUserID)) {
      importUserID = normalized;
      toImport.setUserID(importUserID);
    }
    if (importUserID.isEmpty()) importUserID = UNKNOWN;

    return importUserID;
  }

  private boolean hasSpaces(String s) {
    Pattern pattern = Pattern.compile("\\s");
    Matcher matcher = pattern.matcher(s);
    return matcher.find();
  }

  Map<String,String> userIDToPass=new HashMap<>();
  /**
   * Checks the password for the import user to see if it's the same as the current one in mongo.
   * <p>
   * Two people with the same user id but different passwords are in a race - first guy gets the password,
   * second guy needs to get a new account.
   *
   * @param projid        only for debugging
   * @param optName       of the project, if not the language
   * @param dominoUserDAO to use to add to or query for existing users
   * @param oldToNew      remember the mapping of old h2 userid to new mongo/domino user id
   * @param added         only used if we make collision accounts
   * @param toImport      old user from h2
   * @param dominoUser    user found in mongo with same user id
   * @return true if a collision - same user id but different password
   * @throws Exception
   */
  private boolean foundExistingUser(int projid,
                                    String optName,
                                    DominoUserDAOImpl dominoUserDAO,
                                    Map<Integer, Integer> oldToNew,
                                    List<ClientUserDetail> added,
                                    User toImport,
                                    mitll.hlt.domino.shared.model.user.DBUser dominoUser,
                                    Group group
  ) throws Exception {
    String passwordHash = toImport.getPasswordHash();
    if (passwordHash == null) passwordHash = "";
    int dominoDBID = dominoUser.getDocumentDBID();

    int importID = toImport.getID();
    String importUserID = toImport.getUserID();

    boolean didPasswordMatch = dominoUserDAO.isMatchingPassword(importUserID, passwordHash, userIDToPass);
    boolean differentGender = isDifferentGender(toImport, dominoUser);
    boolean doesGenderMatter = doesGenderMatter(toImport.getPermissions());
    boolean makeCollisionAnyway = differentGender && doesGenderMatter;

    if (didPasswordMatch) { // existing user with same password
      // do nothing, but remember id mapping


      if (makeCollisionAnyway) {
        logger.info("foundExistingUser different gender : current netprof user " + toImport + " is a " + toImport.getRealGender());
        logger.info("foundExistingUser different gender : current domino  user " + dominoUser + " is a " + dominoUser.getGender());
        dominoDBID = makeCollisionAccount(optName, dominoUserDAO, oldToNew, added, toImport, importUserID, group);
      } else {
        if (DEBUG) {
          logger.info("foundExistingUser found existing user '" + importUserID + "' :" + "\n\tdomino " + dominoUser + " password matches.");
        }
        checkMatchingGender(dominoUserDAO, toImport, dominoUser);
      }
      oldToNew.put(importID, dominoDBID);
      return false;
    } else {
      if (DEBUG) {
        logger.info("copyUsers found existing user '" + importUserID + "' :" +
            "\n\tdomino " + dominoUser);
      }

      // User "adam" already exists with a different password - what to do?
      if (MAKE_COLLISION_ACCOUNT || makeCollisionAnyway) {
        // give the person a new id in the name space of the language
        if (differentGender) {
          logger.info("different gender : current netprof user " + toImport + " is a " + toImport.getRealGender());
          logger.info("different gender : current domino  user " + dominoUser + " is a " + dominoUser.getGender());
        }
        dominoDBID = makeCollisionAccount(optName, dominoUserDAO, oldToNew, added, toImport, importUserID, group);
        //oldToNew.put(importID, dominoID);
      } else {
        // second person is out of luck - they need to make a new account
        if (WARN_ON_COLLISION) {
          logger.info("COLLISION : copyUsers found existing user with password difference " + importUserID +
              " : " + dominoUser);
        }

        checkUserApplications(dominoUserDAO, dominoUser);
        checkMatchingGender(dominoUserDAO, toImport, dominoUser);
      }
      oldToNew.put(importID, dominoDBID);

      logger.info("copyUsers user collision to project #" + projid + " map old user id " + importID + "-> new " + dominoDBID +
          " : " + dominoUser);
      return true;
    }
  }

  private boolean doesGenderMatter(Collection<User.Permission> permissions) {
    return permissions.contains(User.Permission.RECORD_AUDIO) ||
        permissions.contains(User.Permission.DEVELOP_CONTENT) ||
        permissions.contains(User.Permission.QUALITY_CONTROL) ||
        permissions.contains(User.Permission.TEACHER_PERM);
  }

  /**
   * Update the gender if it's missing.
   * @param dominoUserDAO
   * @param toImport
   * @param dominoUser
   */
  private void checkMatchingGender(IUserDAO dominoUserDAO, User toImport, DBUser dominoUser) {
    MiniUser.Gender realGender = toImport.getRealGender();
    mitll.hlt.domino.shared.model.user.User.Gender initialGender = dominoUser.getGender();
    if (initialGender == mitll.hlt.domino.shared.model.user.User.Gender.Unspecified &&
        realGender != MiniUser.Gender.Unspecified) {
      dominoUser.setGender(realGender == MiniUser.Gender.Male ? mitll.hlt.domino.shared.model.user.User.Gender.Male :
          realGender == MiniUser.Gender.Female ? mitll.hlt.domino.shared.model.user.User.Gender.Female : mitll.hlt.domino.shared.model.user.User.Gender.Unspecified);
      dominoUserDAO.updateUser(dominoUser);
      logger.info("checkMatchingGender : update initialGender for " + dominoUser + " now " + dominoUser.getGender() + " import " + realGender + " initial " + initialGender);
    } else {
/*      logger.info("checkMatchingGender : no change - initialGender for " + dominoUser.getUserId() +
          " is " + dominoUser.getGender() + " vs " + realGender);*/
    }
  }

  private boolean isDifferentGender(User toImport, DBUser dominoUser) {
    MiniUser.Gender realGender = toImport.getRealGender();
    mitll.hlt.domino.shared.model.user.User.Gender gender = dominoUser.getGender();
    return realGender == MiniUser.Gender.Male && gender == mitll.hlt.domino.shared.model.user.User.Gender.Female ||
        realGender == MiniUser.Gender.Female && gender == mitll.hlt.domino.shared.model.user.User.Gender.Male;
  }

  /**
   * Update the application slot if needed.
   * @param dominoUserDAO
   * @param dominoUser
   */
  private void checkUserApplications(IUserDAO dominoUserDAO, DBUser dominoUser) {//}, int dominoDBID) {
    Set<String> applicationAbbreviations = dominoUser.getApplicationAbbreviations();
    if (applicationAbbreviations.contains(NETPROF)) {
      // logger.info("foundExistingUser : found existing application entry for user #" + dominoDBID);
    } else {
      applicationAbbreviations.add(NETPROF);
//      logger.info("before " + dominoUser.getApplicationAbbreviationsString());
//      logger.info("before " + dominoUser.getApplicationAbbreviations());

      /*SResult<ClientUserDetail> clientUserDetailSResult =*/
      dominoUserDAO.updateUser(dominoUser);
/*
      logger.info("Got back " + clientUserDetailSResult);
      logger.info("Got back " + clientUserDetailSResult.get().getApplicationAbbreviations());
      logger.info("Got back " + clientUserDetailSResult.get().getApplicationAbbreviationsString());
      logger.info("foundExistingUser : updating application entry for user #" + dominoDBID);
      */
    }
  }

  /**
   * For the moment
   *
   * @param optName
   * @param dominoUserDAO
   * @param oldToNew
   * @param added
   * @param toImport
   * @param importUserID
   * @throws Exception
   */
  private int makeCollisionAccount(String optName,
                                   IDominoUserDAO dominoUserDAO,
                                   Map<Integer, Integer> oldToNew,
                                   List<ClientUserDetail> added,
                                   User toImport, String importUserID,
                                   Group group) throws Exception {
    String compoundID = importUserID + USER_LANG_SEPARATOR + optName;
    User userByCompound = dominoUserDAO.getUserByID(compoundID);

    if (userByCompound != null) {
      // so this could happen if we're reloading against the same mongo user db - the user has already been added on a previous import
      logger.warn("copyUsers already added " + compoundID + " : " +
          userByCompound +
          " so moving on...?");
      return userByCompound.getID();
    } else {
      logger.info("copyUsers no user for '" + compoundID + "' so adding one.");

      toImport.setUserID(compoundID);
      ClientUserDetail e = addUser(dominoUserDAO, oldToNew, toImport, optName, group);
      added.add(e);
      return e.getDocumentDBID();
    }
  }

  /**
   * @param dominoUserDAO
   * @param oldToNew
   * @param toImport
   * @param projectName
   * @return
   * @see #copyUsers
   */
  private ClientUserDetail addUser(IDominoUserDAO dominoUserDAO,
                                   Map<Integer, Integer> oldToNew,
                                   User toImport,
                                   String projectName,
                                   Group group) throws Exception {
    logger.info("addUser " + toImport + "\n\twith permissions " + toImport.getPermissions());
    //logger.info("addUser " + toImport.getID()+ " gender " + toImport.getGender() + " " + toImport.getRealGender());
    ClientUserDetail toAdd = dominoUserDAO.toClientUserDetail(toImport, projectName, group);
    ClientUserDetail addedUser = dominoUserDAO.addAndGet(
        toAdd,
        toImport.getPasswordHash()
    );
    if (toAdd.getGender() != addedUser.getGender()) {
      logger.error("for " + addedUser +
          " lost gender after adding " + addedUser.getGender());
    }
    rememberUser(oldToNew, toImport.getID(), addedUser);
    return addedUser;
  }

  private void rememberUser(Map<Integer, Integer> oldToNew, int id, ClientUserDetail addedUser) throws Exception {
    if (addedUser == null) {
      logger.error("addUser no error returned from domino.");
      throw new Exception("rememberUser couldn't import " + id);
    } else {
      oldToNew.put(id, addedUser.getDocumentDBID());
    }
  }

  private void addDefaultUsers(Map<Integer, Integer> oldToNew, IUserDAO dominoUserDAO) {
    oldToNew.put(BaseUserDAO.DEFAULT_USER_ID, dominoUserDAO.getDefaultUser());
    oldToNew.put(BaseUserDAO.DEFAULT_MALE_ID, dominoUserDAO.getDefaultMale());
    oldToNew.put(BaseUserDAO.DEFAULT_FEMALE_ID, dominoUserDAO.getDefaultFemale());
  }

  /**
   * @param slickUserProjectDAO
   * @param projid
   * @param added
   * @see #copyUsers
   */
  private void addUserProjectBinding(IUserProjectDAO slickUserProjectDAO, int projid, Map<Integer, Long> added) {
    // logger.info("addUserProjectBinding adding user->project for " + projid);
    List<SlickUserProject> toAdd = new ArrayList<>();

    slickUserProjectDAO.forgetUsersBulk(added.keySet());

    added.forEach((userID, modified) -> toAdd.add(new SlickUserProject(-1, userID, projid, new Timestamp(modified))));

    slickUserProjectDAO.addBulk(toAdd);
  }
}
