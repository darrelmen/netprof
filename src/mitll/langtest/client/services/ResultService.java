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

package mitll.langtest.client.services;

import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.ScoringAudioPanel;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserPassLogin;
import mitll.langtest.shared.ResultAndTotal;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RemoteServiceRelativePath("result-manager")
public interface ResultService extends RemoteService {
  /**
   * @see mitll.langtest.client.result.ResultManager#getTypeaheadUsing(String, TextBox)
   * @param unitToValue
   * @param userid
   * @param flText
   * @param which
   * @return
   */
  Collection<String> getResultAlternatives(Map<String, String> unitToValue, int userid, String flText, String which);


  /**
   * @see ResultManager#showResults
   * @return
   */
  int getNumResults();

  /**
   * @see ResultManager#createProvider(int, CellTable)
   * @param start
   * @param end
   * @param sortInfo
   * @param unitToValue
   * @param userid
   * @param flText
   * @param req
   * @return
   */
  ResultAndTotal getResults(int start, int end, String sortInfo, Map<String, String> unitToValue, int userid,
                            String flText, int req);


  /**
   * @see ScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   * @param resultID
   * @param width
   * @param height
   * @return
   */
 // PretestScore getResultASRInfo(int resultID, int width, int height);

}
