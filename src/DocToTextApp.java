import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.*;
import java.awt.datatransfer.DataFlavor;
import java.util.prefs.Preferences;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DocToTextApp extends JFrame {

	// --- [엑셀 탭용] ---
	// 엑셀파일 경로
	private JTextField excelTargetPath;
	// 엑셀 비밀번호 입력
	private JPasswordField excelTargetPw;
	// 엑셀파일 업로드 버튼
	private JButton btnExcelSelect;
	// 엑셀파일 -> TXT 변환 버튼
	private JButton btnExcelStart;

	// --- [HWPX 탭용] ---
	// HWPX 경로
	private JTextField hwpxTargetPath;
	// HWPX 비밀번호 입력
	private JPasswordField hwpxTargetPw;
	// HWPX 업로드 버튼
	private JButton btnHwpxSelect;
	// HWPX (내부의 특정 표) -> TXT 변환 버튼
	private JButton btnHwpxStart;

	// --- [공통 로그용] ---
	private JTextArea logArea;

	// 라이트/다크모드 관련 변수
	private boolean isDarkMode = false;
	private JButton btnThemeToggle;
	private ImageIcon sunIcon;
	private ImageIcon moonIcon;

	// 프로그래스바
	private JProgressBar progressBar;

	public DocToTextApp() {
		Font mainFont = new Font("맑은 고딕", Font.PLAIN, 12);
		Font btnFont =  new Font("맑은 고딕", Font.PLAIN, 11);
		UIManager.put("Label.font", mainFont);
		UIManager.put("Button.Font", mainFont);
		UIManager.put("TextField.font", mainFont);
		UIManager.put("CheckBox.font", mainFont);
		UIManager.put("ComboBox.font", mainFont);

		setTitle("문서파일 텍스트 추출 - docToText v1.0");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(550, 550);
		setResizable(false);
		setLocationRelativeTo(null);

		// 프로그램 아이콘 로딩
		try {
			java.net.URL iconURL = getClass().getResource("/images/fox.png");
			if (iconURL != null){
				ImageIcon icon = new ImageIcon(iconURL);
				setIconImage(icon.getImage());
			} else {
			ImageIcon icon = new ImageIcon("images/fox.png");
			setIconImage(icon.getImage());
			}
		} catch (Exception e) {
			System.err.println("아이콘 로딩 실패: " + e.getMessage());
		}

		// ==================================================
		//                  라이트 / 다크모드용
		// ==================================================

		java.net.URL sunURL = getClass().getResource("/images/sun.png");
		java.net.URL moonURL = getClass().getResource("/images/moon.png");

		ImageIcon rawSun = new ImageIcon(sunURL);
		ImageIcon rawMoon = new ImageIcon(moonURL);

		Image scaledSun = rawSun.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
		Image scaledMoon = rawMoon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);

		sunIcon = new ImageIcon(scaledSun);
		moonIcon = new ImageIcon(scaledMoon);

		btnThemeToggle = new JButton(moonIcon);
		btnThemeToggle.setBorderPainted(false);
		btnThemeToggle.setContentAreaFilled(false);

		btnThemeToggle.putClientProperty("themeMode", "light");

		// ========================================================
		//           BoxLayout과 GridBagLayout 패널 조합
		// ========================================================
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		JPanel topBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		topBarPanel.setOpaque(false);

		JButton btnHelp = new JButton();
		btnHelp.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,
			com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_HELP);
		
		btnHelp.setToolTipText("사용 설명서를 읽어보세요!");

		topBarPanel.add(btnHelp);
		topBarPanel.add(btnThemeToggle);

		mainPanel.add(topBarPanel);

		JPanel phasePanel = new JPanel(new GridBagLayout());
		phasePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 6, 10, 6);
		gbc.fill = GridBagConstraints.HORIZONTAL;


		// ==================================================
		//                       탭 생성
		// ==================================================

		 JTabbedPane tabbedPane = new JTabbedPane();
		 tabbedPane.addTab("엑셀(.xlsx)", createExcelTabPanel());
		 tabbedPane.addTab("한글(.hwpx)", createHwpxTabPanel());

		 phasePanel.add(tabbedPane, gbc);

		// ==================================================
		//                     하단패널 생성
		// ==================================================

		JPanel bottomPanel = createBottomPanel();

		// ----------------------------------------------------
		// 최종 JFrame 본체 화면에 안착
		// ----------------------------------------------------
		mainPanel.add(phasePanel);
		setLayout(new BorderLayout());
		this.add(mainPanel, BorderLayout.CENTER);
		this.add(bottomPanel, BorderLayout.SOUTH);

		SwingUtilities.updateComponentTreeUI(this);

		// 엑셀파일 변환시작 버튼 리스너
		btnExcelStart.addActionListener(e -> {
			String path = excelTargetPath.getText();

			if (path == null || path.trim().isEmpty()){
				appendLog("엑셀 파일을 먼저 선택해 주세요.");
				return;
			}

			File targetFile = new File(path);

			if (!targetFile.exists() || !targetFile.isFile()){
				appendLog("해당 경로에 실제 파일이 존재하지 않습니다: " + path);
				return;
			}

			btnExcelStart.setEnabled(false);
			progressBar.setForeground(javax.swing.UIManager.getColor("ProgressBar.foreground"));

			new Thread(() -> {
				try {
					onExcelStartClicked();
				} finally {
					SwingUtilities.invokeLater(() -> {
						progressBar.setForeground(new Color(0, 0, 0, 0));
						btnExcelStart.setEnabled(true);
					});
				}
			}).start();
		});

		// 한글파일 변환시작 버튼 리스너
		btnHwpxStart.addActionListener(e -> {
			String path = hwpxTargetPath.getText();

			if (path == null || path.trim().isEmpty()){
				appendLog("HWPX 파일을 먼저 선택해 주세요.");
				return;
			}

			File targetFile = new File(path);

			if (!targetFile.exists() || !targetFile.isFile()){
				appendLog("해당 경로에 실제 파일이 존재하지 않습니다: " + path);
				return;
			}

			btnHwpxStart.setEnabled(false);
			progressBar.setForeground(javax.swing.UIManager.getColor("ProgressBar.foreground"));

			new Thread(() -> {
				try {
					onHwpxStartClicked();
				} finally {
					SwingUtilities.invokeLater(() -> {
						progressBar.setForeground(new Color(0, 0, 0, 0));
						btnHwpxStart.setEnabled(true);
					});
				}
			}).start();
		});

		// 도움말버튼 눌렀을때 리스너
		btnHelp.addActionListener(e -> {
			showHelpDialog(false, isDarkMode);
		});

		// 라이트/다크모드 버튼 리스너
		btnThemeToggle.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (!isDarkMode){
						com.formdev.flatlaf.FlatDarculaLaf.setup();
						btnThemeToggle.setIcon(sunIcon);
						btnThemeToggle.putClientProperty("themeMode", "dark");
						// lblStep1Title.setForeground(Color.WHITE);
						// lblStep2Title.setForeground(Color.WHITE);
						isDarkMode = true;
					} else {
						com.formdev.flatlaf.FlatIntelliJLaf.setup();
						btnThemeToggle.setIcon(moonIcon);
						btnThemeToggle.putClientProperty("themeMode", "light");
						// lblStep1Title.setForeground(Color.BLUE);
						// lblStep2Title.setForeground(Color.BLUE);
						isDarkMode = false;
					}
					SwingUtilities.updateComponentTreeUI(DocToTextApp.this);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			
			}
		});

		// 도움말
		Preferences prefs = Preferences.userNodeForPackage(DocToTextApp.class);
		boolean hideHelp = prefs.getBoolean("HIDE_HELP_POPUP", false);

		if (!hideHelp){
			SwingUtilities.invokeLater(() -> {
				showHelpDialog(true, false);
			});
		}
	}

	// 엑셀탭 눌렀을때
	private void onExcelStartClicked() {
		String path = excelTargetPath.getText().trim();
		if (path.isEmpty()){
			appendLog("엑셀 파일을 선택해 주세요.");
			return;
		}

		if (!path.toLowerCase().endsWith(".xlsx") && !path.toLowerCase().endsWith(".xls")){
			JOptionPane.showMessageDialog(this,
				"[엑셀]파일만 선택 가능합니다.",
				"확장자 오류", JOptionPane.WARNING_MESSAGE);
			appendLog("잘못된 파일 형식: 엑셀 파일이 아닙니다.");
			return;
		}
		processFile(new File(path));
	}

	// HWPX탭 눌렀을때
	private void onHwpxStartClicked() {
		String path = hwpxTargetPath.getText().trim();
		if (path.isEmpty()){
			appendLog("HWPX 파일을 선택해 주세요.");
			return;
		}

		if (!path.toLowerCase().endsWith(".hwpx")){
			JOptionPane.showMessageDialog(this,
				"[HWPX]파일만 선택 가능합니다.",
				"확장자 오류", JOptionPane.WARNING_MESSAGE);
			appendLog("잘못된 파일 형식: HWPX 파일이 아닙니다.");
			return;
		}
		processFile(new File(path));
	}


	// drag-and-drop 감지
	class FileDropTargetListener extends DropTargetAdapter {
		// 경로 글자가 들어갈 대상 textField
		private JTextField targetTextField;

		public FileDropTargetListener(JTextField targetTextField) {
			this.targetTextField = targetTextField;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void drop(DropTargetDropEvent dtde) {
			try {
				if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					dtde.acceptDrop(DnDConstants.ACTION_COPY);
					
					List<File> droppedFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					if (droppedFiles != null && !droppedFiles.isEmpty()) {
						File file = droppedFiles.get(0);
						
						String path = file.getAbsolutePath();
						targetTextField.setText(path);
						
					}
					dtde.dropComplete(true);
				} else {
					dtde.rejectDrop();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				dtde.rejectDrop();
			}
		}
	}

	// action 감지
	class FileSelectListener implements ActionListener {
		private JTextField targetTextField;

		public FileSelectListener(JTextField targetTextField) {
			this.targetTextField = targetTextField;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String targetPath = openFileChooser();
			if (targetPath != null) {
				targetTextField.setText(targetPath);
			}
		}
	}

	// 파일 탐색기 띄우기
	private String openFileChooser() {
		JFileChooser fileChooser = new JFileChooser();

		fileChooser.setCurrentDirectory(new File("."));

		int result = fileChooser.showOpenDialog(this);

		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			return selectedFile.getAbsolutePath();
		}

		return null;
	}

	// [탭 1] 엑셀 전용 패널 생성
	private JPanel createExcelTabPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(25, 15, 15, 15));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
		panel.add(new JLabel("엑셀 파일:"), gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
		excelTargetPath = new JTextField(20);
		excelTargetPath.setEditable(false);
		panel.add(excelTargetPath, gbc);

		gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
		btnExcelSelect = new JButton("파일 선택");
		panel.add(btnExcelSelect, gbc);

		gbc.insets = new Insets(25, 10, 10, 10);
		gbc.gridx = 0; gbc.gridy = 4;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipady = 25; // 두껍게
		gbc.weightx = 1.0;
		btnExcelStart = new JButton("시작");
		panel.add(btnExcelStart, gbc);

		// 엑셀업로드 파일찾기 클릭감지
		btnExcelSelect.addActionListener(new FileSelectListener(excelTargetPath));

        // 엑셀 파일 필드에서 드래그 앤 드롭 감지
        excelTargetPath.setDropTarget(
			new DropTarget(excelTargetPath
			, DnDConstants.ACTION_COPY
			, new FileDropTargetListener(excelTargetPath)));

		return panel;
	}


	// [탭 2] HWPX 전용 패널 생성
	private JPanel createHwpxTabPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(25, 15, 15, 15));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
		panel.add(new JLabel("HWPX 파일:"), gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
		hwpxTargetPath = new JTextField(20);
		hwpxTargetPath.setEditable(false);
		panel.add(hwpxTargetPath, gbc);

		gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
		btnHwpxSelect = new JButton("파일 선택");
		panel.add(btnHwpxSelect, gbc);

		gbc.insets = new Insets(25, 10, 10, 10);
		gbc.gridx = 0; gbc.gridy = 4;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipady = 25; // 두껍게
		gbc.weightx = 1.0;
		btnHwpxStart = new JButton("시작");
		panel.add(btnHwpxStart, gbc);

		// HWPX업로드 파일찾기 클릭감지
		btnHwpxSelect.addActionListener(new FileSelectListener(hwpxTargetPath));

        // HWPX 파일 필드에서 드래그 앤 드롭 감지
        hwpxTargetPath.setDropTarget(
			new DropTarget(hwpxTargetPath
			, DnDConstants.ACTION_COPY
			, new FileDropTargetListener(hwpxTargetPath)));

		return panel;
	}

	// 하단 패널 생성
	private JPanel createBottomPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 15, 0, 15));


		JPanel logPanel = new JPanel(new BorderLayout());
		Border titled = BorderFactory.createTitledBorder("진행상황 및 로그");
		Border padding = BorderFactory.createEmptyBorder(5, 8, 8, 8);
		logPanel.setBorder(BorderFactory.createCompoundBorder(titled, padding));

		logArea = new JTextArea(6, 50);
		logArea.setEditable(false);
		logArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
		JScrollPane scrollPane = new JScrollPane(logArea);

		logPanel.add(scrollPane, BorderLayout.CENTER);

		// 프로그레스바
		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);

		progressBar.setVisible(true);
		progressBar.setOpaque(false);
		progressBar.setStringPainted(false);
		progressBar.setForeground(new Color(0, 0, 0, 0));

		progressBar.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PROGRESS_BAR_SQUARE, true);

		progressBar.putClientProperty("JProgressBar.largeHeight", false);
		progressBar.putClientProperty("JProgressBar.height", 6);

		panel.add(logPanel, BorderLayout.CENTER);
		panel.add(progressBar, BorderLayout.SOUTH);

		return panel;
	}

	// 시작 버튼 클릭시 메서드
	private void processFile(File file) {
		String password = null;

		try {
			FileProcessor.processFile(file, password, this::appendLog);
		} catch (PasswordRequiredException e) {
			appendLog("암호화된 파일입니다. 비밀번호 입력을 요청합니다.");

			password = showPasswordDialog(file.getName());

			if (password != null){
				try {
					FileProcessor.processFile(file, password, this::appendLog);
				} catch (Exception ex) {
					appendLog("비밀번호 오류 또는 추출 실패: " + ex.getMessage());
				}
			} else {
				appendLog("비밀번호 입력이 취소되었습니다.");
			}
		} catch (Exception e) {
			appendLog("에러 발생: " + e.getMessage());
		}
	}

	// 비밀번호 입력 팝업창
	private String showPasswordDialog(String fileName) {
		JPasswordField pf = new JPasswordField();

		// 팝업창 뜨자마자 textfield에 focus
		pf.addAncestorListener(new javax.swing.event.AncestorListener(){
			@Override
			public void ancestorAdded(javax.swing.event.AncestorEvent event) {
				pf.requestFocusInWindow();
			}

			@Override
			public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}

			@Override
			public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
		});

		Object[] message = {
			"'" + fileName + "' 파일은 암호화되어 있습니다.",
			"비밀번호를 입력하세요: ",
			pf
		};
		
		int option = JOptionPane.showConfirmDialog(
			this,
			message,
			"비밀번호 입력",
			JOptionPane.OK_CANCEL_OPTION
		);

		if (option == JOptionPane.OK_OPTION){
			return new String(pf.getPassword());
		}

		return null;
	}

	// 로그출력 메서드
	public void appendLog(String message) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String timeStr = LocalTime.now().format(formatter);

		SwingUtilities.invokeLater(() -> {
			if (logArea != null){
				logArea.append("[" + timeStr  + "] " + message + "\n");
				logArea.setCaretPosition(logArea.getDocument().getLength());
			}
		});
	}


	public static void main(String[] args) {
		System.out.println("구동 중 ...");
		try {
			com.formdev.flatlaf.FlatIntelliJLaf.setup();
		} catch (Exception ex) {
			System.err.println("FlatLaf 테마 적용 실패, 기본 Swing 테마로 구동");
		}

		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					DocToTextApp frame = new DocToTextApp();

					frame.setVisible(true);

				} catch (Exception e) {
					System.out.println("UI생성 중 오류 발생");
					e.printStackTrace();
				}
			}
		});
	}

	// 도움말 팝업
	private void showHelpDialog(boolean isAutoPopup, boolean isDarkMode) {
		String bodyColor, strongColor, noticeColor, warningColor, hrColor;

		if (isDarkMode){
			bodyColor = "#E6E6FA";
			strongColor = "#F8F8FF";
			warningColor = "#FFA07A";
			noticeColor = "#B0E0E6";
			hrColor = "#E6E6FA";
		} else {
			bodyColor = "#333333";
			strongColor = "#2c3e50";
			warningColor = "#e74c3c";
			noticeColor = "#00BFFF";
			hrColor = "#E0E0E0";
		}


		JDialog helpDialog = new JDialog(this, "사용 설명서", true);
		helpDialog.setLayout(new BorderLayout(15, 15));
		((JPanel)helpDialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		JLabel titleLabel = new JLabel("사용 방법", JLabel.CENTER);
		titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
		helpDialog.add(titleLabel, BorderLayout.NORTH);

		java.net.URL sunIconUrl = DocToTextApp.class.getResource("/images/sun.png");
		java.net.URL moonIconUrl = DocToTextApp.class.getResource("/images/moon.png");	
		java.net.URL lightBulbUrl = DocToTextApp.class.getResource("/images/bulb.png");

		JEditorPane textPane = new JEditorPane();
		textPane.setContentType("text/html");
		textPane.setEditable(false);
		textPane.setOpaque(false);
		textPane.setText("<html><head><style>"
			+ " body {font-family:맑은 고딕; font-size: 12pt; line-height:1.6; padding: 10px; color: " + bodyColor + ";}"
			+ " strong {font-size: 13pt; font-weight: bold; color: " + strongColor + ";}"
			+ " hr {height:1px; border: 0px; color: " + hrColor + ";}"
			+ " .warning {display: inline-block; font-weight: bold; padding-right: 15px; color: " + warningColor + ";}"
			+ " .notice {color: " + noticeColor + ";}"
			+ " ol, ul {margin-left: 20px}"
			+ "</style></head><body>"
			+ "<strong>제목1</strong>"
			+ "<ol><li>순서있는 리스트 1</li>"
			+ "<li>순서있는 리스트 2<br>"
			+ "<span class='warning'><b>유의사항</b></span>　어쩌고 저쩌고 "
			+ "어쩌고 저쩌고 어쩌고 저쩌고</li>" 
			+ "</ol>"
			+ "<img src='" + lightBulbUrl + "' width='16' height='16'> "
			+ "<span class='notice'>　notice입니다 "
			+ "안녕하세요.</span><br><br>"
			+ "<ul><li>순서없는 리스트 1</li>"
			+ "<li>순서없는 리스트 2</li></ul>"
			+ "<br><hr><br>우측 상단의 <img src='" + moonIconUrl + "' width='16' height='16'> 버튼과 <img src='" + sunIconUrl + "' width='16' height='16'> 버튼을 누르면 다크모드/라이트 모드로 이용이 가능합니다."
			+ "</body></html>"
		);

		textPane.setCaretPosition(0);
		helpDialog.add(new JScrollPane(textPane), BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new BorderLayout());

		JCheckBox hideNextTimeCheck = new JCheckBox("이 창을 앞으로 다시 열지 않기");
		hideNextTimeCheck.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
		if (isAutoPopup){
			bottomPanel.add(hideNextTimeCheck, BorderLayout.WEST);
		}

		JButton closeBtn = new JButton("확인 및 닫기");
		closeBtn.addActionListener(e -> {
			if (hideNextTimeCheck.isSelected()){
				Preferences prefs = Preferences.userNodeForPackage(DocToTextApp.class);
				prefs.putBoolean("HIDE_HELP_POPUP", true);
			}
			helpDialog.dispose();
		});
		bottomPanel.add(closeBtn, BorderLayout.EAST);

		helpDialog.add(bottomPanel, BorderLayout.SOUTH);

		helpDialog.setSize(500, 380);
		helpDialog.setLocationRelativeTo(this);
		helpDialog.setVisible(true);
	}
}