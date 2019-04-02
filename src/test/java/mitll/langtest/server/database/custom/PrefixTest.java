package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.project.Project;
import org.junit.Test;

public class PrefixTest extends BaseTest {
  @Test
 public void test(){

      DatabaseImpl french = getDatabase();
      int projectid = 5;
      Project project = french.getProject(projectid,true);
      project.getAudioFileHelper().makeOneEditMap();



    }


}
