package net.xcordio.vmmanagerservice.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * An abstract interface for basic access to local/remote/virtual filesystems.
 * Includes reading file, writing file, checking file for existence, and listening to filesystem events.
 */
public interface FileAccess {
	
	interface Handler {
		void handleEvents(FileAccess fa, String dir, List<String> actions, String file);
	}
	
	void listenTo(String dir, Handler handler);
	void cancelListen(String dir, Handler handler);
	void cancelListen(String dir);
	//
	InputStream readFile(String filename) throws IOException;
	OutputStream writeFile(String filename) throws IOException;
	boolean isFile(String filename) throws IOException;
	boolean isDir(String filename) throws IOException;
}
