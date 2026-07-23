import java.io.*;
import java.util.*; 
import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.ss.usermodel.*;

public class ExcelExtractor {

	public static String extract(File file, String password, Consumer<String> logger) throws Exception {
		
		StringBuilder sb = new StringBuilder();
		DataFormatter formatter = new DataFormatter();
		Workbook workbook = null;

		try {
			try (POIFSFileSystem fs = new POIFSFileSystem(file)) {
				EncryptionInfo info = new EncryptionInfo(fs);
				Decryptor decryptor = Decryptor.getInstance(info);

				if (password == null || password.trim().isEmpty() || !decryptor.verifyPassword(password)){
					throw new PasswordRequiredException("비밀번호가 필요하거나 틀렸습니다.");
				}

				InputStream dataStream = decryptor.getDataStream(fs);
				workbook = WorkbookFactory.create(dataStream);

				if (logger != null){
					logger.accept("엑셀 암호 해제 성공");
				}
			} catch (PasswordRequiredException e) {
				throw e;
			} catch (Exception e) {
				if (logger != null){
					logger.accept("일반 엑셀 파일로 읽기 시도 중...");
				}
				workbook = WorkbookFactory.create(file);
			}

			for (Sheet sheet : workbook){
				sb.append("=== 시트명: ").append(sheet.getSheetName()).append(" ===\n");

				for (Row row : sheet){
					for (Cell cell : row){
						String cellValue = (cell != null)
							? formatter.formatCellValue(cell).trim()
							: "";
						
						if (cellValue.isEmpty()){
							cellValue = "\t";
						}

						sb.append(cellValue).append("\t");
					}
					sb.append("\n");
				}
				sb.append("\n\n");
			}

			if (logger != null){
				logger.accept("엑셀에서 테스트 추출 완료");
			}

			
		} finally {
			if (workbook != null){
				workbook.close();
			}
		}
		return sb.toString();

		
		// String fileName = file.getName().toLowerCase();
	}

	
}