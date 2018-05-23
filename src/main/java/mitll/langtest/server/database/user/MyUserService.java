package mitll.langtest.server.database.user;

import com.mongodb.client.MongoCollection;
import mitll.hlt.domino.server.user.MongoUserServiceDelegate;
import mitll.hlt.domino.server.util.Mailer;
import mitll.hlt.domino.server.util.Mongo;
import mitll.hlt.domino.server.util.UserServiceProperties;
import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.user.*;
import mitll.langtest.server.mail.MailSupport;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static mitll.hlt.domino.shared.Constants.CHANGE_PW_PNM;
import static mitll.hlt.domino.shared.Constants.RESET_PW_HASH;

public class MyUserService extends MongoUserServiceDelegate {

  private static final String ID_F = "_id";

  private static final String ENC_EMAIL_TOKEN_F = "encEmailToken";
  private static final String EMAIL_TOKEN_EXP_F = "emailTokenExp";
  private static final String PASS_F = "pass";

  private static final String ACTIVE_F = "active";

  private final MailSupport mailSupport;

  private Clock theClock = Clock.systemUTC();

   MyUserService(UserServiceProperties props, Mailer mailer, String acctTypeName, Mongo mongoPool, MailSupport mailSupport) {
    super(props, mailer, acctTypeName, mongoPool);
    this.mailSupport = mailSupport;
  }

  //  @Override
  LoginResult addUserNoEmail(User currUser, ClientUserDetail addUser, String urlBase) {
    log.info("Adding a user: " + addUser);
    if (addUser == null || addUser.getUserId() == null || addUser.getUserId().isEmpty() ||
        addUser.getFirstName() == null || addUser.getLastName() == null ||
        addUser.getEmail() == null || addUser.getEmail().isEmpty()) {
      log.error("Add attempt with incomplete user! " + addUser);
      return new LoginResult(new SResult<>("Missing user details."), "");
    }

    Date now = Date.from(Instant.now(theClock));
    AccountDetail acctDtl = new AccountDetail(currUser, now);
    ClientUserDetail cleanAddUser = new ClientUserDetail(-1, addUser.getUserId().trim(), addUser.getFirstName().trim(),
        addUser.getLastName().trim(), addUser.getEmail().trim(), addUser.getAffiliation(), addUser.getGender(),
        addUser.getRoleAbbreviations(), addUser.getPrimaryGroup(), addUser.getSecondaryGroups(), acctDtl, addUser.getApplicationAbbreviations());
    if (addUser.getAcctDetail() != null) {
      AccountDetail.Device d = addUser.getAcctDetail().getDevice();
      cleanAddUser.getAcctDetail().setDevice(d);
    }

    // When creating for a group admin, enforce additional constraints.
    // user is created in the same group as the user's creator.
    // user has no secondary groups.
    // user initially inactive, and has a pending Unlock request.
    boolean isPendingRequest = currUser.hasRole(Role.GrAM);

    if (currUser.hasRole(Role.GrAM)) {
      cleanAddUser.setPrimaryGroup(currUser.getPrimaryGroup());
      cleanAddUser.clearSecondaryGroups();

      // ensure you can't add a read-only role.
      Set<String> newRoleAbbrs = new LinkedHashSet<>();
      for (String roleAbbr : cleanAddUser.getRoleAbbreviations()) {
        Role role = getRoleDAO().getByAbbreviation(roleAbbr);
        if (role.isGamEditable()) {
          newRoleAbbrs.add(roleAbbr);
        } else {
          // this should happen.
          log.warn("Ignoring external manager " + currUser.toLogString() +
              " attempt to add non-editable role to " +
              cleanAddUser.toLogString());
        }
      }
      cleanAddUser.setRoleAbbreviations(newRoleAbbrs);
      // attach a pending request to the user.
      UserEvent createEvent = new UserEvent(UserEventType.Create, cleanAddUser, now, currUser, true, Collections.emptyList());
      log.info("Adding a user in pending state! " + cleanAddUser.toLogString());
      cleanAddUser.getAcctDetail().setPendingRequest(createEvent);
      cleanAddUser.setActive(false);
    }

    // With a pending request, don't create an email validation key. Instead, wait until the
    // request is approved before creating the key.
    String emailToken = null;
    String encEmailToken = null;
    if (!isPendingRequest) {
      try {
        emailToken = generateEmailValidationKey();
        MyMongoUserServiceDelegate myMongoUserServiceDelegate = new MyMongoUserServiceDelegate();
        encEmailToken = myMongoUserServiceDelegate.encodeNewUserPass(emailToken);
      } catch (Exception ex) {
        log.warn("Can not create key for new user " + addUser.getUserId() + "! Aborting create!", ex);
        return null;
      }
    }

    prepareUserForSave(cleanAddUser);
    SResult<ClientUserDetail> result = doAddUser(cleanAddUser, encEmailToken);
    if (result != null && !result.isError()) {
      // Either email user, and log create event, or a pending request.
      if (result.get().getAcctDetail().getPendingRequest() == null) {
        getEventDAO().logEvent(currUser, UserEventType.Create, result.get(), false, Collections.emptyList());
        sendNewUserEmails(cleanAddUser, emailToken, urlBase);
      } else {
        getEventDAO().logEvent(currUser, result.get().getAcctDetail().getPendingRequest());
      }
    } else {
      log.warn("Error when adding user " + ((result != null) ? result.getResponseMessage() : " no result!"));
    }
    log.info("Adding a user: " + cleanAddUser);
    return new LoginResult(result, emailToken);
  }

  private void sendNewUserEmails(ClientUserDetail newUser, String emailToken, String urlBase) {
    String fullName = newUser.getFirstName() + " " + newUser.getLastName();
    log.info("Adding {} : {}", fullName, newUser.getEmail());

    String passLink = urlBase + "?" + CHANGE_PW_PNM + "=" + emailToken + RESET_PW_HASH;

    mailSupport.email(newUser.getEmail(),
        acctTypeName + " Account Activation", fullName + ",\n"
            + "You have been registered for a new " + acctTypeName
            + " account. You will receive your username in a separate email.\n"
            + "You can use the following link set your password:\n\n" + passLink
            + "\n\nIf you don’t use this link within " + EMAIL_VERIFY_LINK_HRS
            + " hours, it will expire. To get a new password reset link, visit "
            + urlBase + RESET_PW_HASH + "\n\nThanks,\n   " + acctTypeName + " Administrator");

    mailSupport.email(newUser.getEmail(),
        acctTypeName + " Account Activation Username", fullName + ",\n"
            + "You have been registered for a new " + acctTypeName +
            " account. You will receive a link to reset your password in a separate email.\n"
            + "Your username is: " + newUser.getUserId()
            + "\n\nThanks,\n   " + acctTypeName + " Administrator");
  }

  static class LoginResult {
    SResult<ClientUserDetail> result;
    String emailToken;

    public LoginResult(SResult<ClientUserDetail> result, String emailToken) {
      this.result = result;
      this.emailToken = emailToken;
    }
  }

  /**
   * Update the user password expiration and account expiration times.
   */
  private void prepareUserForSave(ClientUserDetail user) {
    if (user != null && user.getPrimaryGroup() != null && user.getAcctDetail() != null) {
      Date pwExpires = null;
      int passPeriod = user.getPrimaryGroup().getPasswordPeriodDays();
      if (passPeriod > 0) {
        Calendar cal = Calendar.getInstance();
        if (user.getAcctDetail().getLastPassChange() != null) {
          cal.setTime(user.getAcctDetail().getLastPassChange());
        }
        cal.add(Calendar.DAY_OF_MONTH, passPeriod);
        pwExpires = cal.getTime();
      }
      user.getAcctDetail().setPassExpireDate(pwExpires);
      user.getAcctDetail().setAcctExpireDate(user.getPrimaryGroup().getAccountExpireDate());
    } else {
      log.info("Skip user prepare for save. No primary group or actDtl. This is probably a self-update. User: " + user);
    }
  }

  @Override
  public boolean changePassword(String userId, String providedToken, String newPassword, String urlBase) {
    log.info("Changing password for {}.", userId);
    boolean success = false;
    DBUser changeUser = getDBUser(userId);
    UserCredentials cred = null;
    String encodedSavedToken = null;
    if (changeUser != null) {
      cred = getMyUserCredentials(changeUser.getDocumentDBID());
      if (cred != null) {
        encodedSavedToken = cred.encEmailToken;
      }
    }

    // ensure we go through the motions for unmatched tokens/ids
    // to avoid returning too quickly and providing information about user name validity.
    if (encodedSavedToken == null) {
      encodedSavedToken = makeFakeEncodedPass();
    }
    String encodedProvidedToken = null;
    try {
      encodedProvidedToken = new MyMongoUserServiceDelegate().encodePass(encodedSavedToken, providedToken, PasswordEncoding.common_v1);
    } catch (Exception ex) {
      log.error("Exception decoding password for " + userId);
    }
    if (encodedSavedToken.equals(encodedProvidedToken)) {
      log.info("Password Token match for {}!", changeUser.getNameWithId());

      Instant expiresTime = cred.emailTokenExp.toInstant().plusSeconds(60 * 60 * EMAIL_VERIFY_LINK_HRS);
      if (expiresTime.isAfter(Instant.now(theClock))) {
        success = (doChangePassword(changeUser, cred.encPass, newPassword) != null);
        // log event and send notification to user.
        if (success) {
          getEventDAO().logEvent(changeUser, UserEventType.PWChange, changeUser, false);

          new Thread(new Runnable() {
            @Override
            public void run() {
              try {
                mailSupport.email(changeUser.getEmail(), acctTypeName + " Password Changed",
                    "Hello " + changeUser.getUserId() + ",\nYour " + acctTypeName + " password has been changed.\n\n" +
                        "If you did not change your password, you can recover access by resetting your password" +
                        " at the following link:\n\n" + urlBase + RESET_PW_HASH +
                        "\n\nThanks,\n   " + acctTypeName + " Administrator");
              } catch (Exception e) {
                log.warn("couldn't send email " + e, e);
              }
            }
          }).start();

        }
      } else {
        log.info("Password Token Expired for {}!", changeUser.getNameWithId());
        doClearResetLink(changeUser);
      }
    } else {
      log.info("Token mismatch for {}.", userId);
    }
    return success;
  }

  private UserCredentials getMyUserCredentials(int userDBId) {
    return getUserCredentials(eq(ID_F, userDBId));
  }

  private UserCredentials getUserCredentials(Bson query) {
    UserCredentials cred = null;
    Document user = users().find(query).projection(include(PASS_F, ACTIVE_F, ENC_EMAIL_TOKEN_F, EMAIL_TOKEN_EXP_F)).first();

    if (user != null) {
      String encodedPass = user.getString(PASS_F);
      boolean active = user.getBoolean(ACTIVE_F, false);
      String emailVKey = user.getString(ENC_EMAIL_TOKEN_F);
      Date emailVExp = user.getDate(EMAIL_TOKEN_EXP_F);

      cred = new UserCredentials(encodedPass, active, emailVKey, emailVExp);
    } else {
      log.warn("User not found in DB! Query: " + query);
    }
    return cred;
  }

  private String makeFakeEncodedPass() {
    char[] enc = new char[ENCODED_PASS_LEN];
    Arrays.fill(enc, '0');
    return new String(enc);
  }

  private MongoCollection<Document> users() {
    return mongoPool.getMongoCollection(USERS_C);
  }

  // package access for testing.
  static class UserCredentials {
    public final String encPass;
    public final boolean active;
    public final String encEmailToken;
    public final Date emailTokenExp;

    public UserCredentials(String encodedPass, boolean active, String encEmailToken, Date emailTokenExp) {
      this.encPass = encodedPass;
      this.active = active;
      this.encEmailToken = encEmailToken;
      this.emailTokenExp = emailTokenExp;
    }

  }


}
