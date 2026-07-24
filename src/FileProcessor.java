import java.io.*;
import java.util.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class FileProcessor {

	public static String processFile(File file, String password, Consumer<String> logger) throws Exception {
		String fileName = file.getName().toLowerCase();

		if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")){
			return processExcel(file, password, logger);
		} else if (fileName.endsWith(".hwpx")){
			return processHwpx(file, password, logger);	
		} else {
			throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + fileName);
		}
	}

	// 엑셀 처리
	private static String processExcel(File file, String password, Consumer<String> logger) throws Exception {
		if (logger != null){
			logger.accept("엑셀파일 분석 시작: " + file.getName());			
		}
		
		String textContent = ExcelExtractor.extract(file, password, logger);

		File savedFile = saveToTxtFile(file, textContent, logger);

		return savedFile.getAbsolutePath();
	}

	// hwpx 처리
	private static String processHwpx(File file, String password, Consumer<String> logger) throws Exception {
		if (logger != null){
			logger.accept("HWPX 파일 분석 시작: " + file.getName());			
		}

		String textContent = HwpxExtractor.extract(file, password, logger);
		File savedFile = saveToTxtFile(file, textContent, logger);
		return savedFile.getAbsolutePath();
	}

	// 파일 저장 메서드
	public static File saveToTxtFile(File originalFile, String extractedText, Consumer<String> logger) throws Exception {
		String userHome = System.getProperty("user.home");
		File baseDir = new File(userHome + File.separator + "Downloads");

		if (!baseDir.exists()){
			baseDir.mkdirs();
		}

		String originalName = originalFile.getName();
		String baseName = originalName.contains(".")
			? originalName.substring(0, originalName.lastIndexOf("."))
			: originalName;

		File txtFile = new File(baseDir, baseName + "_extracted.txt");

		Files.writeString(
			txtFile.toPath(),
			extractedText,
			StandardCharsets.UTF_8
		);

		if (logger != null){
			logger.accept("파일 저장 완료: " + txtFile.getAbsolutePath());
		}
		
		return txtFile;
	}
}