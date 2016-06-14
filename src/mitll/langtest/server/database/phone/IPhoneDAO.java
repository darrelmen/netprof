package mitll.langtest.server.database.phone;

import mitll.langtest.shared.analysis.PhoneReport;
import net.sf.json.JSONObject;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 3/29/16.
 */
public interface IPhoneDAO<T> {
  boolean addPhone(T phone);

  JSONObject getWorstPhonesJson(long userid, List<String> exids, Map<String, String> idToRef);

  PhoneReport getWorstPhonesForResults(long userid, List<Integer> ids, Map<String, String> idToRef) throws SQLException;
}
