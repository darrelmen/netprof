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
  DatabaseImpl db = new DatabaseImpl(this);

	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		String tomcatWriteDirectory = getServletContext().getInitParameter("tomcatWriteDirectoryFullPath");
	//	String pretestFilesRelativePath = getServletContext().getInitParameter("pretestFilesRelativePath");  // likely = pretest_files
	//	String testsRelativePath = "tests";
//    System.out.println("got " + request.getSession());

    if (tomcatWriteDirectory == null) tomcatWriteDirectory = "answers";

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
       // System.err.println("Got " + fieldName + "=" + item.getString());
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
//          System.err.println("Got " + plan + " and " + exercise);

          String planAndTestPath = plan + File.separator + exercise + File.separator + question + File.separator + "subject-"+user;
          String currentTestDir = tomcatWriteDirectory + File.separator  + planAndTestPath;
          File audioFilePath = new File(currentTestDir);
          audioFilePath.mkdirs();

          File file = writeAudioFile(item, "answer", audioFilePath);
          db.addAnswer(0, plan,exercise,Integer.parseInt(question),"",file.getPath());
				}
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}

  private File writeAudioFile(FileItem item, String base, File audioFilePath) throws Exception {
  //  System.out.println("got " + item);
    File file = new File(audioFilePath.getPath() + File.separator + base + ".wav");
    item.write(file);
    return file;
  }
}