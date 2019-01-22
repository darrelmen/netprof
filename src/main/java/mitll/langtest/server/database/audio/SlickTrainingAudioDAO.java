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

package mitll.langtest.server.database.audio;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.scoring.ASR;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickAudio;
import mitll.npdata.dao.SlickTrainingAudio;
import mitll.npdata.dao.audio.TrainingAudioDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SlickTrainingAudioDAO extends DAO implements ITrainingAudioDAO {
  private static final Logger logger = LogManager.getLogger(SlickTrainingAudioDAO.class);
  private final TrainingAudioDAOWrapper dao;

  public SlickTrainingAudioDAO(Database database, DBConnection dbConnection, IUserDAO userDAO) {
    super(database);
    dao = new TrainingAudioDAOWrapper(dbConnection);
  }


  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }


  public List<SlickTrainingAudio> getAll(int projid) {
    return dao.getAll(projid);
  }


  public void addBulk(List<SlickTrainingAudio> bulk) {
    dao.addBulk(bulk);
  }

  /**
   * Smarter would be to check if the audio and training audio tables are out of sync
   *
   * @param projects
   * @param audioDAO
   */
  @Override
  public void checkAndAddAudio(Collection<Project> projects, IAudioDAO audioDAO) {
    for (Project project : projects) {
      if (project.getStatus() != ProjectStatus.DELETED && project.getStatus() != ProjectStatus.RETIRED) {
        int id = project.getID();
        int numFor = dao.getNumFor(id);
        if (numFor == 0) {
          logger.info("checkAndAddAudio no training audio for " + project);

          List<SlickAudio> all = audioDAO.getAll(id);
          List<SlickTrainingAudio> trainingAudios = new ArrayList<>();
          ASR asr = project.getAudioFileHelper().getASR();

          all.forEach(slickAudio -> {
            String normTranscript = asr.getNormTranscript(slickAudio.transcript(), "");
            trainingAudios.add(new SlickTrainingAudio(-1,
                slickAudio.userid(),
                slickAudio.exid(),
                slickAudio.modified(),
                slickAudio.audioref(),
                slickAudio.audiotype(),
                slickAudio.transcript(),
                normTranscript,
                id,
                slickAudio.gender()
            ));
          });

          dao.addBulk(trainingAudios);
        }
      }
    }
  }
}
