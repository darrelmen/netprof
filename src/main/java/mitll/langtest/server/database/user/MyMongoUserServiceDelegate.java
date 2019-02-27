/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.user;

import mitll.hlt.domino.server.user.UserServiceDelegateBase;
import mitll.hlt.domino.server.util.LegacyMd5Hash;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

/**
 * Created by go22670 on 2/1/17.
 */
class MyMongoUserServiceDelegate {//extends MongoUserServiceDelegate {
  private static final Logger log = LogManager.getLogger(MyMongoUserServiceDelegate.class);
  private static final UserServiceDelegateBase.PasswordEncoding DEFAULT_ENC = UserServiceDelegateBase.PasswordEncoding.common_v1;

  private final boolean DEBUG = false;
//  MyMongoUserServiceDelegate(UserServiceProperties props, Mailer mailer, String appName, Mongo mongoPool) {
//    super(props, mailer, appName, mongoPool);
//  }

  boolean isMatch(String encoded, String attempt) {
//      String encodedAttemptedPass = LegacyMd5Hash.getHash(attempt);
//      return encodedAttemptedPass.equals(encoded);
    return authenticate(encoded, attempt);
  }

  String encodeNewUserPass(String txtPass) throws Exception {
    byte[] salt = generateUserSalt(DEFAULT_ENC);
    return encodePass(txtPass, salt, DEFAULT_ENC);
  }

  private byte[] generateUserSalt(UserServiceDelegateBase.PasswordEncoding pEnc) throws Exception {
    SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
    byte[] salt = new byte[pEnc.saltLength];
    random.nextBytes(salt);
    return salt;
  }

  private boolean authenticate(String encodedCurrPass, String attemptedTxtPass) {
    try {
      // ensure we go through the motions for unmatched usernames
      // to avoid returning too quickly and
      // providing information about user name validity.
      if (encodedCurrPass == null) {
        return false;
      }

      String encodedAttemptedPass = encodePass(encodedCurrPass, attemptedTxtPass, UserServiceDelegateBase.PasswordEncoding.common_v1);
      if (encodedAttemptedPass.equals(encodedCurrPass)) {
        log.info("Decoded using SHA-512.");
        return true;
      } else if (DEBUG) log.debug("1 no match " +
          "\n\tcurrent " + encodedCurrPass +
          "\n\tattempt " + encodedAttemptedPass);

      // Handle Domino Encoding
      encodedAttemptedPass = encodePass(encodedCurrPass, attemptedTxtPass, UserServiceDelegateBase.PasswordEncoding.domino);
      if (encodedAttemptedPass.equals(encodedCurrPass)) {
        log.info("Decoded using SHA1.");
        return true;
      } else if (DEBUG) log.debug("2 no match " +
          "\n\tcurrent " + encodedCurrPass +
          "\n\tattempt " + encodedAttemptedPass);

      // Handle NetProF Encoding
      encodedAttemptedPass = LegacyMd5Hash.getHash(attemptedTxtPass);
      if (encodedAttemptedPass.equals(encodedCurrPass)) {
        log.info("Decoded using NetProF-MD5.");
        return true;
      } else if (DEBUG) log.debug("3 no match " +
          "\n\tcurrent " + encodedCurrPass +
          "\n\tattempt " + encodedAttemptedPass);

    } catch (Exception ex) {
      log.warn("Can not authenticate user!", ex);
    }
    if (DEBUG) log.info("Authentication Failed for " + encodedCurrPass);
    return false;
  }

  public String encodePass(String encodedCurrPass, String txtPass, UserServiceDelegateBase.PasswordEncoding pEnc) throws Exception {
    return encodePass(txtPass, extractSalt(encodedCurrPass, pEnc), pEnc);
  }

  private static final String PASS_PREFIX = "{SSHA}";

  private String encodePass(String txtPass, byte[] salt, UserServiceDelegateBase.PasswordEncoding pEnc) throws Exception {
    byte[] encryptedPass = encryptPass(txtPass, salt, pEnc);
    // once encrypted, encode the password
    // to simplify storage when LDAP is used.
    return PASS_PREFIX + new String(Base64.encodeBase64(encryptedPass));
  }

  private byte[] encryptPass(String txtPass, byte[] salt, UserServiceDelegateBase.PasswordEncoding pEnc) throws Exception {
    // See this link for more info on encryption options.
    // http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html
    int iterations = 20000;
    KeySpec spec = new PBEKeySpec(txtPass.toCharArray(), salt, iterations,
        pEnc.derivedKeyLength);
    SecretKeyFactory f = SecretKeyFactory.getInstance(pEnc.algorithm);
    byte[] encPass = f.generateSecret(spec).getEncoded();
    return ArrayUtils.addAll(encPass, salt);
  }

  private byte[] extractSalt(String encodedPass, UserServiceDelegateBase.PasswordEncoding pEnc) {
    String encodedPassNoPrefix = encodedPass.substring(PASS_PREFIX.length());
    byte[] hashAndSalt = Base64.decodeBase64(encodedPassNoPrefix.getBytes());
    int shaLen = hashAndSalt.length - pEnc.saltLength + 1;
    return ArrayUtils.subarray(hashAndSalt, shaLen - 1, hashAndSalt.length);
  }
}
