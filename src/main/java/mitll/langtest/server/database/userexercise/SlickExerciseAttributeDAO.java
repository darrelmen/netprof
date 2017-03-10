package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Pair;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickExerciseAttribute;
import mitll.npdata.dao.userexercise.ExerciseAttributeDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 3/8/17.
 * @deprecated
 */
public class SlickExerciseAttributeDAO extends DAO implements IExerciseAttributeDAO {
  private static final Logger logger = LogManager.getLogger(SlickExerciseAttributeDAO.class);
  private final ExerciseAttributeDAOWrapper dao;

  public SlickExerciseAttributeDAO(DatabaseImpl database, DBConnection dbConnection) {
    super(database);
    dao = new ExerciseAttributeDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  public int insert(SlickExerciseAttribute attribute) {
    return dao.insert(attribute);
  }

  public Collection<SlickExerciseAttribute> getAllByProject(int projid) {
    return dao.allByProject(projid);
  }

  Map<Integer, Pair> getIDToPair(int projid) {
    Map<Integer, Pair> pairMap = new HashMap<>();
    getAllByProject(projid).forEach(p -> pairMap.put(p.id(), new Pair(p.property(), p.value())));
    return pairMap;
  }
}
