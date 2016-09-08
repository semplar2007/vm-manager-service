package net.xcordio.vmmanagerservice;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import net.semplar.log.Logger;
import net.xcordio.vmmanagerservice.model.InstanceConfig;
import net.xcordio.vmmanagerservice.util.FileAccess;
import net.xcordio.vmmanagerservice.util.ReflectiveConfig;
import net.xcordio.vmmanagerservice.util.RemoteFileAccess;

/**
 * This class listens to remote directories and handle JSON changes.
 * @author xcordio
 */
public class RemoteConfigHandler extends RemoteFileAccess {
	
	private static final Logger log = Logger.getc();
	
	public List<String> dirs = new ArrayList<String>();
	
	public void onDirsChange(List<String> newDirs) {
		for (String d : dirs) cancelListen(d, handler);
		for (String d : newDirs) listenTo(d, handler);
	}
	
	/** Single preHandler instance used for all directories. */
	protected transient final Handler handler = new Handler() {
		@Override
		public void handleEvents(FileAccess fa, String dir, List<String> actions, String file) {
			for (String action : actions) {
				if ("CREATE".equals(action) || "MODIFIED".equals(action)) {
					String configFilepath = dir + file;
					InputStream is = null;
					String jsonString;
					try {
						is = fa.readFile(configFilepath);
						jsonString = ServiceConfig.readUtfStringFromStream(is);
					} catch (IOException e) {
						handleConfigError(configFilepath, e);
						continue;
					} finally {
						if (is != null) try {
							is.close();
						} catch (IOException ioe) { }
					}
					handleConfigData(file, new JSONObject(jsonString));
				} else if ("DELETE".equals(action)) {
					handleConfigDeleted(file);
				} else {
					// TODO: handle RENAME_FROM and RENAME_TO actions, and drop other actions
				}
			}
		}
	};
	
	protected ServiceMain main;
	
	public RemoteConfigHandler() throws IOException {
		super();
	}
	
	public RemoteConfigHandler setMain(ServiceMain m) {
		if (m != null) {
			main = m;
		} else try {
			sshClient.close();
		} catch (IOException ioe) {
			log.error("failed to close ssh connection", ioe);
		}
		return this;
	}

	protected void handleConfigData(String filepath, JSONObject object) {
		InstanceConfig ic = main.instanceMap.get(filepath);
		if (ic == null) main.instanceMap.put(filepath, ic = new InstanceConfig());
		ReflectiveConfig.getInstance().applyConfig(ic, object);
	}
	
	protected void handleConfigDeleted(String filepath) {
		
	}
	
	protected void handleConfigError(String filepath, Throwable e) {
		log.error("failed to load and parse config file: " + filepath, e);
	}
}
