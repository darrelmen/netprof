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

  JSONObject getWorstPhonesJson(long userid, Collection<Integer> exids, Map<Integer, String> idToRef);

  PhoneReport getWorstPhonesForResults(long userid, Collection<Integer> ids, Map<Integer, String> idToRef);
}
