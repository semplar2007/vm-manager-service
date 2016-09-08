package net.xcordio.vmmanagerservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;

import net.semplar.log.Logger;

public class ServiceConfig {
	
	private static Logger log = Logger.getc();
	
	public static JSONObject loadConfig() throws IOException {
		File configFile = ServiceConfig.findConfigFile();
		if (configFile == null) throw new IOException("configuration file not found");
		log.info("using config: " + configFile);
		FileInputStream fin = new FileInputStream(configFile);
		JSONObject configJson = new JSONObject(ServiceConfig.readUtfStringFromStream(fin));
		fin.close(); // TODO: add try-catch
		return configJson;
	}
	
	protected final static String confFN = "vm-manager-service.conf";
	protected static File[] configPaths = {
			new File("./" + confFN),
			new File("dist/" + confFN),
			new File("/etc/vm-manager-service/" + confFN),
	};
	
	protected static File findConfigFile() {
		for (File f : configPaths)
			if (f.isFile())
				return f;
		return null;
	}
	
	public static String readUtfStringFromStream(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] cb = new char[4096];
		InputStreamReader isr = new InputStreamReader(is, "utf-8");
		for (int read; ; ) {
			read = isr.read(cb);
			if (read < 0) break;
			if (read == 0) try { // in case stream returns 0
				Thread.sleep(50);
				continue;
			} catch (InterruptedException ie) {
				throw new RuntimeException("stream is interrupted");
			}
			sb.append(cb, 0, read);
		}
		return sb.toString();
	}
}
