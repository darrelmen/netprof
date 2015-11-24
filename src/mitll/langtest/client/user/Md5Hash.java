/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Hash {
  /**
   * Generate MD5 digest.
   *
   * @param input input data to be hashed.
   * @return MD5 digest.
   */
  private static byte[] getMd5Digest(byte[] input) {
    MessageDigest md5;

    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      return new byte[1];
    }
    md5.reset();
    md5.update(input);
    return md5.digest();
  }

  private static char[] HEX_CHARS =
      new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  private static String toHexString(byte[] bytes) {
    char[] hexString = new char[2 * bytes.length];
    int j = 0;
    for (byte aByte : bytes) {
      hexString[j++] = HEX_CHARS[(aByte & 0xF0) >> 4];
      hexString[j++] = HEX_CHARS[aByte & 0x0F];
    }
    return new String(hexString);
  }

  public static String getHash(String toHash) {
    return toHexString(Md5Hash.getMd5Digest(toHash.getBytes()));

  }
/*  public static void main(String []arg) {
    String bueller = getHash("bueller");
    System.out.println("hash " + bueller + " " + bueller.length());
    String first = "074036D6CAEB34B3A23BEA17A449B05C";
    System.out.println("hash " + first + " " + first.length());

     first = "398E13BF1B80EC28F41E73AB791E276B";
    System.out.println("hash " + first + " " + first.length());
  }*/
}