/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */
package mitll.langtest.client.domino.common;

import java.util.logging.Logger;

public class CommonValidation {
	
	private static final String EMAIL_REGEX = 
			"^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*" +
			"@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+" + 
			"(?:[A-Z]{2}|com|org|net|edu|gov|mil|biz|info|mobi|name|aero|asia|jobs|museum)\\b"; 
			
	protected static final Logger log = Logger.getLogger("CommonValidation");
	
	//[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+(?:[A-Z]{2}|com|org|net|edu|gov|mil|biz|info|mobi|name|aero|asia|jobs|museum)\b
	//private static final String EMAIL_REGEX = "^[a-z0-9_%+-]+@[a-z0-9-]+\\.[a-z]{2,4}$";
	//private static final String EMAIL2 = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=\\?\\^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+(?:[A-Z]{2}|com|org|net|edu|gov|mil|biz|info|mobi|name|aero|asia|jobs|museum)$";
	
	public static final int MIN_PASS_LENGTH = 8;

	public Result validateEmail(String email) {
		boolean valid = email.toLowerCase().matches(EMAIL_REGEX);
		log.info("Doing email validation for " + email.toLowerCase() + 
				" matches regex " + EMAIL_REGEX + " " + valid);
		return (valid) ? new Result() : new Result("Invalid email format!");
	}
	
	public Result validatePassword(String password) {
		String err = null;
		if (password == null || password.trim().isEmpty()) {
			err = "Password is empty!";
		}
		if (err == null) {
			String tPass = password.trim();
			if (tPass.length() < MIN_PASS_LENGTH) {
				err = "Password length must be at least " + MIN_PASS_LENGTH;
			}
		}
		// TODO add regex to ensure password includes numbers and special chars?
		if (err !=  null) {
			return new Result(err);
		}
		return new Result();
	}
	
	/** 
	 * Validate the a filename is not null, and has a given extension.
	 * @param aType the attachment type.
	 * @param filename The name of the file to check.
	 * @return A validation result.
	 */
/*	public Result validateAttachment(AttachmentType aType, String filename) {

		if (filename == null) { 
			return new Result("No filename provided: " + filename);
		}
		
		String[] validExts = aType.getExtensions();
		for (int i = 0; i < validExts.length; i++) {
			if (filename.trim().toLowerCase().endsWith(validExts[i])) {
				return new Result(); // success
			}
		}
		log.warninging("Invalid attachment filename: " + filename);
		return new Result("File type must be " + aType.getExtensionsString());
	}
	*/
	/**
	 * The result of a validation run. 
	 */
	public static class Result {
		public final String message;
		public final boolean errorFound;
		/** Constructor indicating success.*/
		Result() {
			this(false, "");
		}
		/** Constructor indicating failure.*/
		Result(String message) {
			this(true, message);
		}
			
		Result(boolean errorFound, String message) {
			this.errorFound = errorFound;
			this.message = message;
		}
	}
}
