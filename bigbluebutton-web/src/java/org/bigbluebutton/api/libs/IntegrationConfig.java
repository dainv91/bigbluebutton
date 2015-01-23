/* Created 2014-01-12 16:34 by iadd
* Read config from file for
* integration with lms purpose
*/
package org.bigbluebutton.api.libs;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException; 
import java.io.OutputStream; 
import java.io.InputStream;
import java.util.Properties;
import java.io.File;

public class IntegrationConfig{
	private String file;
	private Properties prop;
	public final String INTEGRATION_CONFIG_LOCATION = "/var/lib/tomcat6/webapps/bigbluebutton/WEB-INF/classes/org/bigbluebutton/api/libs/integration.properties";
	public IntegrationConfig(){
		prop = new Properties();
		setFile(INTEGRATION_CONFIG_LOCATION);
	}
	public IntegrationConfig(String file){
		this();
		setFile(file);
	}
	public void setFile(String file){
		this.file = file;
		File f = new File(file);
		
		if(!f.isFile()){
			//System.out.println("Not exists");
			createDefault();
			return;
		}
	}
	public String getFile(){
		return this.file;
	}
	public void write(String key, String value){
		prop.setProperty(key, value);		
	}
	/* Save changes on finally  action
	*/
	public void saveChanges(){
		OutputStream output = null;
		try{
			output = new FileOutputStream(this.file);
			prop.store(output, null);
		} catch(IOException io){
			io.printStackTrace();
		} finally{
			if(output != null){
                                try{
                                        output.close();
                                }catch(IOException ex){
                                        ex.printStackTrace();
                                }
                        }
		}
	}
	public void createDefault(){
		File f = new File(this.file);
		String vcm = "http://vcm.topica.vn/index.php/log/log_vcr?";
		try{
			f.getParentFile().mkdirs();
			f.createNewFile();

			java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String time = df.format(new java.util.Date()).toString();
			write("iadd", time);
			write("server", vcm);
			saveChanges();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	public String readProperty(String key){
		InputStream input = null;
		String value = null;
		try{
			input = new FileInputStream(getFile());
			prop.load(input);
			value = prop.getProperty(key);
		}catch(IOException ex){
			ex.printStackTrace();
		}finally{
			if(input != null){
				try{
					input.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
		return value;
	}
	/*
	public static void main(String[] args){
		String fileName = "/var/iadd.properties";		
		fileName = args[0];
		IntegrationConfig c = new IntegrationConfig();
		//c.setFile(fileName);
		//return;
		//c.write("iadd","ookii");
		//c.write("abcd", "https://www.google.com/vn/?iadd=abcd");
		//c.saveChanges();
		System.out.println(c.readProperty("abcd"));
	}
	*/
}
