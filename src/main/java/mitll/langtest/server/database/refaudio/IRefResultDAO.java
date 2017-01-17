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

package mitll.langtest.server.database.refaudio;

import mitll.langtest.server.audio.DecodeAlignOutput;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.npdata.dao.SlickRefResultJson;
import net.sf.json.JSONObject;

import java.util.Collection;
import java.util.List;

public interface IRefResultDAO extends IDAO {
  boolean removeForExercise(int exid);

  long addAnswer(int userID,
                 int exid,
                 String audioFile,
                 long durationInMillis,
                 boolean correct,

                 DecodeAlignOutput alignOutput,
                 DecodeAlignOutput decodeOutput,

                 DecodeAlignOutput alignOutputOld,
                 DecodeAlignOutput decodeOutputOld,

                 boolean isMale, String speed);

  boolean removeForAudioFile(String audioFile);

  // TODO : add  a simple ex, json pair here
  // get it, parse it, make reverse map of phone->ex
  // add this to pairs when read in exercise
  // add sound to type order...
  // add fancy table widget?
  // have widgets cascade values...?
  // have sort order of items be smarter

  /**
   * @see RefResultDecoder#getDecodedFiles()
   * @return
   */
  List<Result> getResults();

  List<SlickRefResultJson> getJsonResults();

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getPretestScore(int, int, String, String, int, int, boolean, int, boolean)
   * @param exid
   * @param answer
   * @return
   */
  Result getResult(int exid, String answer);

  JSONObject getJSONScores(Collection<Integer> ids);

  /**
   * @see mitll.langtest.server.decoder.RefResultDecoder#trimRef(int, Collection, String)
   * @return
   */
  int getNumResults();
}
