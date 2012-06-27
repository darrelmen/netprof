/**
 * 
 */
package mitll.langtest.server;

import mitll.langtest.client.recorder.UploadForm;
import mitll.langtest.server.database.DatabaseImpl;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author gregbramble
 *
 */
public class UploadServlet extends HttpServlet implements Servlet{
	private static final long serialVersionUID = -611668719240096732L;
  private DatabaseImpl db;
  private AudioCheck audioCheck = new AudioCheck();

  /**
   * Don't do this earlier, since we need the servlet context to be set.
   */
  @Override
  public void init() {
    db = new DatabaseImpl(this);
  }

  @SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		String tomcatWriteDirectory = getServletContext().getInitParameter("tomcatWriteDirectoryFullPath");
	//	String pretestFilesRelativePath = getServletContext().getInitParameter("pretestFilesRelativePath");  // likely = pretest_files
    if (tomcatWriteDirectory == null) tomcatWriteDirectory = "answers";

    File test = new File(tomcatWriteDirectory);
    if (!test.exists()) test.mkdirs();
    if (!test.exists()) {
      tomcatWriteDirectory = "answers";
    }
    response.setContentType("application/json");
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);

		try{
			List<FileItem> items = upload.parseRequest(request);
			Iterator<FileItem> it = items.iterator();
			
			String plan = null, exercise = null, question=null, user=null;

			while(it.hasNext()){
				FileItem item = it.next();
				String fieldName = item.getFieldName();
				if(fieldName.equals(UploadForm.PLAN)){
					plan = item.getString();
				}
        else if(fieldName.equals(UploadForm.EXERCISE)){
          exercise = item.getString();
        }
        else if(fieldName.equals(UploadForm.QUESTION)){
          question = item.getString();
        }
        else if(fieldName.equals(UploadForm.USER)){
          user = item.getString();
        }
				else{
          String planAndTestPath = plan + File.separator + exercise + File.separator + question + File.separator + "subject-"+user;
          String currentTestDir = tomcatWriteDirectory + File.separator  + planAndTestPath;
          File audioFilePath = new File(currentTestDir);
          audioFilePath.mkdirs();

          File file = writeAudioFile(item, "answer", audioFilePath);
          if (!file.exists()) {
            System.err.println("huh? can't find " + file.getAbsolutePath());
          }
          boolean valid = isValid(file);
      /*    if (!valid) {
            System.err.println("audio file " + file.getAbsolutePath() + " is *not* valid");
          }
          else {
            System.out.println("audio file " + file.getAbsolutePath() + " is valid");
          }*/
          db.answerDAO.addAnswer(Integer.parseInt(user), plan,exercise,Integer.parseInt(question),"",file.getPath(), valid, db);
				}
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}

  private File writeAudioFile(FileItem item, String base, File audioFilePath) throws Exception {
    File file = new File(audioFilePath.getPath() + File.separator + base + ".wav");
    item.write(file);
    //isValid(file);
    return file;
  }

  private boolean isValid(File file) {
    try {
      return audioCheck.checkWavFile(file);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}