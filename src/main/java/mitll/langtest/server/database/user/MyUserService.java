package mitll.langtest.server.database.user;

import mitll.hlt.domino.server.user.MongoUserServiceDelegate;
import mitll.hlt.domino.server.util.Mailer;
import mitll.hlt.domino.server.util.Mongo;
import mitll.hlt.domino.server.util.UserServiceProperties;
import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.user.*;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

public class MyUserService extends MongoUserServiceDelegate {
  public MyUserService(UserServiceProperties props, Mailer mailer, String acctTypeName, Mongo mongoPool) {
    super(props, mailer, acctTypeName, mongoPool);
  }

  //  @Override
  public LoginResult addUserNoEmail(User currUser, ClientUserDetail addUser, String urlBase) {
    log.info("Adding a user: " + addUser);
    if (addUser == null || addUser.getUserId() == null || addUser.getUserId().isEmpty() ||
        addUser.getFirstName() == null || addUser.getLastName() == null ||
        addUser.getEmail() == null || addUser.getEmail().isEmpty()) {
      log.error("Add attempt with incomplete user! " + addUser);
      return new LoginResult(new SResult<>("Missing user details."), "");
    }
    Clock theClock = Clock.systemUTC();

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
        //sendNewUserEmails(cleanAddUser, emailToken, urlBase);
      } else {
        getEventDAO().logEvent(currUser, result.get().getAcctDetail().getPendingRequest());
      }
    } else {
      log.warn("Error when adding user " + ((result != null) ? result.getResponseMessage() : " no result!"));
    }
    log.info("Adding a user: " + cleanAddUser);
    return new LoginResult(result, emailToken);
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


}
