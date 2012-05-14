/**
 * 
 */
package mitll.langtest.server;

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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author gregbramble
 *
 */
public class UploadServlet extends HttpServlet implements Servlet{
/*	private static final String url = "jdbc:mysql://localhost:3306/", dbOptions = "?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
		driver = "com.mysql.jdbc.Driver";*/
	private static final long serialVersionUID = -611668719240096732L;

	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    //System.err.println("got doPost!");

		String tomcatWriteDirectory = getServletContext().getInitParameter("tomcatWriteDirectoryFullPath");
		String pretestFilesRelativePath = getServletContext().getInitParameter("pretestFilesRelativePath");  // likely = pretest_files
		String testsRelativePath = "tests";
		
		response.setContentType("application/json");
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);

		try{
			List<FileItem> items = upload.parseRequest(request);
			Iterator<FileItem> it = items.iterator();
			
			String currentPlanName = null, currentTestName = null;

			while(it.hasNext()){
				FileItem item = it.next();
				String fieldName = item.getFieldName();
       // System.err.println("Got " + fieldName + "=" + item.getString());
				if(fieldName.equals("currentPlanName")){	
					currentPlanName = item.getString();
				}
				else if(fieldName.equals("currentTestName")){	
					currentTestName = item.getString();
				}
				else{
					String exercise_name = item.getName();
				//	String base = exercise_name + maxTestId;
          System.err.println("Got " + currentPlanName + " and " + currentTestName);

          String planAndTestPath = currentPlanName + File.separator + testsRelativePath + File.separator + currentTestName;
          String currentTestDir = tomcatWriteDirectory + File.separator + pretestFilesRelativePath + File.separator + planAndTestPath;
          File audioFilePath = new File(currentTestDir);
          audioFilePath.mkdirs();

          writeAudioFile(item, "", audioFilePath);
				}
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}

  private void writeAudioFile(FileItem item, String base, File audioFilePath) throws Exception {
    System.out.println("got " + item);
    //item.write(new File(audioFilePath.getPath() + File.separator + base + ".wav"));
  }
}