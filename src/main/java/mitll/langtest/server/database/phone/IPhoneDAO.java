package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.IDAO;
import mitll.langtest.shared.analysis.PhoneReport;
import net.sf.json.JSONObject;

import java.util.Collection;
import java.util.Map;

/**
 * Created by go22670 on 3/29/16.
 */
public interface IPhoneDAO<T>  extends IDAO {
  boolean addPhone(T phone);

  /**
   * @see mitll.langtest.server.database.JsonSupport#getJsonPhoneReport
   * @param userid
   * @param exids
   * @param idToRef
   * @param language
   * @return
   */
  JSONObject getWorstPhonesJson(long userid, Collection<Integer> exids, Map<Integer, String> idToRef, String language);

  /**
   * @see mitll.langtest.server.database.analysis.Analysis#getPhoneReport
   * @param userid
   * @param ids
   * @param idToRef
   * @param language
   * @return
   */
  PhoneReport getWorstPhonesForResults(long userid, Collection<Integer> ids, Map<Integer, String> idToRef, String language);

  void removeForResult(int resultid);
}
