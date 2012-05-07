package mitll.langtest.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:50 PM
 * To change this template use File | Settings | File Templates.
 */
@RemoteServiceRelativePath("langtestdatabase")
public interface LangTestDatabase extends RemoteService {
	void test();
}
