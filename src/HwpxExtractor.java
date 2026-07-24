import java.io.*;
import java.util.*; 
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import kr.dogfoot.hwpxlib.reader.HWPXReaderForEncrypted;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.writer.common.ElementWriterManager;
import kr.dogfoot.hwpxlib.writer.common.ElementWriterSort;
import kr.dogfoot.hwpxlib.writer.section_xml.SectionWriter;
import org.xml.sax.InputSource;
import java.io.StringReader;

public class HwpxExtractor {

	public static String extract(File file, String password, Consumer<String> logger) throws PasswordRequiredException, Exception {
		Properties props = new Properties();
		File configFile = new File("config.properties");

		if (configFile.exists()){
			try (InputStream configIs = new FileInputStream(configFile)){
				props.load(configIs);
			}
		}

		String targetString = props.getProperty("target.title", "");
	
		if (logger != null){
			logger.accept("HWPX 압축 해제 및 XML 분석 시작: " + file.getName() + "\n" +
				"검색 대상 문자열: [" + targetString + "]"
				);
		}

		StringBuilder sb = new StringBuilder();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();

		try (ZipFile zipFile = new ZipFile(file)){
			HWPXFile hwpxFile = null;

			if (password != null && !password.trim().isEmpty()){
				hwpxFile = HWPXReaderForEncrypted.fromFile(file, password);
			}

			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				String xmlString = "";

				if (entryName.startsWith("Contents/section") && entryName.endsWith(".xml")){
					if (logger != null){
						logger.accept("섹션 XML를 읽고 있습니다...: " + entryName);
					}

					try (InputStream is = zipFile.getInputStream(entry)){
						Document doc= null;
						
						try {
							if (hwpxFile != null){
								int sectionIndex = 0;
								
								if (sectionIndex < hwpxFile.sectionXMLFileList().count()){
									SectionXMLFile sectionXMLFile = hwpxFile.sectionXMLFileList().get(sectionIndex);
									ElementWriterManager manager = new ElementWriterManager();
									SectionWriter sectionWriter = (SectionWriter) manager.get(ElementWriterSort.Section);
									sectionWriter.write(sectionXMLFile);

									xmlString = manager.xsb().toString();

									doc = builder.parse(new InputSource(new StringReader(xmlString)));
								} else {
									doc = builder.parse(is);
								}
							} else {
								doc = builder.parse(is);
							}
						} catch (Exception xmlEx) {
							if (logger != null){
								logger.accept("암호화된 XML섹션을 감지하였습니다. ");
							}
							throw new PasswordRequiredException("HWPX 파일이 암호화되어 있습니다. ");
						}

						NodeList pNodes = doc.getElementsByTagName("hp:p");

						if (pNodes.getLength() == 0){
							pNodes = doc.getElementsByTagNameNS("*", "p");
						}

						for (int i = 0; i < pNodes.getLength(); i++){
							Node pNode = pNodes.item(i);
							String pText = pNode.getTextContent().trim();

							if (!targetString.isEmpty() && pText.contains(targetString)){
								if (logger != null){
									logger.accept("pText 출력: " + pText + "\n" +
										"Target Keyword 발견: [" + targetString + "] \n아랫줄 표 탐색 시작...");
								}

								Node nextNode = pNode.getNextSibling();

								while (nextNode != null){
									String nodeName = nextNode.getNodeName();

									if ("hp:p".equals(nextNode.getNodeName())){
										Element pElement = (Element) nextNode;
										NodeList tables = pElement.getElementsByTagName("hp:tbl");

										if (tables.getLength() > 0) {
											
											Node tblNode = tables.item(0);
											for (int tb = 0; tb < tables.getLength(); tb++){
												Element tableElem = (Element) tables.item(tb);
												NodeList trList = tableElem.getElementsByTagName("hp:tr");
												Node trNode = trList.item(0);

												// logger.accept(trNode.getNodeName());

												if (trList.getLength() == 0){
													trList = tableElem.getElementsByTagNameNS("*", "tr");
												}
												
												for (int r = 0; r < trList.getLength(); r++){
													Element trElem = (Element) trList.item(r);
													NodeList tcList
														= trElem.getElementsByTagName("hp:tc");
													if (tcList.getLength() == 0){
														tcList = trElem.getElementsByTagNameNS("*", "tc");
													}
													
													for (int c = 0; c < tcList.getLength(); c++) {

//														Node tcNode = tcList.item(c);
//
//														if (tcNode.getNodeType() == Node.ELEMENT_NODE){
//															Element tcElem = (Element) tcNode;
//															NodeList pList = tcElem.getElementsByTagName("hp:p");
//
//															for (int p = 0; p < pList.getLength(); p++){
//																Node hppNode = pList.item(p);
//
//																if (hppNode.getNodeType() == Node.ELEMENT_NODE){
//																	Element pElem = (Element) hppNode;
//
//																	NodeList tList = pElem.getElementsByTagName("hp:t");
//
//																	for (int t = 0; t < tList.getLength(); t++){
//																		String tText = tList.item(t).getTextContent().trim();
//
//																		if (!tText.isEmpty()){
//																			sb.append(tText).append("\n");
//																		}
//																	}
//																	sb.append("\n");
//																}
//															}
//														}
														String cellText = tcList.item(c).getTextContent().trim();
														sb.append(cellText).append("\t");
														sb.append("\t");
													}
													sb.append("\n");
												}
												sb.append("\n");
												break;
											}
										}
									}
//									if (nodeName.contains("tbl")){
//										Element tableElem = (Element) nextNode;
//										NodeList trList = tableElem.getElementsByTagName("hp:tr");
//
//										if (trList.getLength() == 0){
//											trList = tableElem.getElementsByTagNameNS("*", "tr");
//										}
//										
//										for (int r = 0; r < trList.getLength(); r++){
//											Element trElem = (Element) trList.item(r);
//											NodeList tcList
//												= trElem.getElementsByTagName("hp:tc");
//											if (tcList.getLength() == 0){
//												tcList = trElem.getElementsByTagNameNS("*", "tc");
//											}
//											
//											for(int c = 0; c < tcList.getLength(); c++) {
//												String cellText = tcList.item(c).getTextContent().trim();
//												sb.append(cellText).append("\t");
//											}
//											sb.append("\n");
//										}
//										break;
//									}
									// nextNode = nextNode.getNextSibling();
									nextNode = null;
								}
							} else {
								continue;
							}
						}
						sb.append("\n");
					}
				}
			}
		}
		String fullText = sb.toString();

		return fullText;
	}

	
}