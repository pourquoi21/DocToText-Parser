import java.io.*;
import java.util.*; 
import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.ss.usermodel.*;


public class HwpxExtractor {

	public String userHome = System.getProperty("user.home");
	public String baseDir = userHome + File.separator + "Downloads" + File.separator;

	public static String extract(File file, String password, Consumer<String> logger) throws Exception {
		String fileName = file.getName().toLowerCase();

		return "";
	}

	
}