import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AutoMessageSender extends JFrame {
    // 界面组件
    private JButton startBtn;
    private JButton stopBtn;
    private JButton selectFileBtn;
    private JTextField intervalField;
    private JTextArea messageArea;
    private JTextArea logArea;
    private JLabel statusLabel;

    // 功能变量
    private boolean isRunning = false;
    private List<String> messages = new ArrayList<>();
    private int currentIndex = 0;
    private Timer timer;
    private Robot robot;
    private File selectedFile;

    public AutoMessageSender() {
        super("QQ网络大战骂人器 by:草籽");
        initUI();
        initEvents();
        try {
            robot = new Robot();
        } catch (AWTException e) {
            log("初始化失败: 无法创建输入模拟器");
            statusLabel.setText("初始化失败");
        }
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
    }

    private void initUI() {
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部控制面板
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // 文件选择
        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(new JLabel("消息文件:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        selectFileBtn = new JButton("选择TXT文件");
        controlPanel.add(selectFileBtn, gbc);

        // 发送间隔
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        controlPanel.add(new JLabel("发送间隔(毫秒):"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        intervalField = new JTextField("1000");
        intervalField.setToolTipText("每条消息的发送间隔时间");
        controlPanel.add(intervalField, gbc);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        startBtn = new JButton("开始发送");
        stopBtn = new JButton("停止发送");
        stopBtn.setEnabled(false);
        startBtn.setPreferredSize(new Dimension(100, 30));
        stopBtn.setPreferredSize(new Dimension(100, 30));
        buttonPanel.add(startBtn);
        buttonPanel.add(stopBtn);

        // 消息预览区
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("消息预览"));
        messageArea = new JTextArea(5, 30);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        previewPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        // 日志区域
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("运行日志"));
        logArea = new JTextArea(8, 30);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        // 状态标签
        statusLabel = new JLabel("就绪 - 请选择消息文件");
        statusLabel.setForeground(Color.BLUE);

        // 组装界面
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(previewPanel, BorderLayout.CENTER);
        mainPanel.add(logPanel, BorderLayout.SOUTH);
        mainPanel.add(buttonPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void initEvents() {
        // 选择文件按钮
        selectFileBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
                }

                @Override
                public String getDescription() {
                    return "文本文件 (*.txt)";
                }
            });

            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                loadMessagesFromFile(selectedFile);
            }
        });

        // 开始按钮
        startBtn.addActionListener(e -> {
            if (isRunning || messages.isEmpty()) return;

            try {
                int interval = Integer.parseInt(intervalField.getText().trim());
                if (interval < 500) {
                    JOptionPane.showMessageDialog(this, "间隔不能小于500毫秒", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                isRunning = true;
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                statusLabel.setText("正在发送...");
                log("开始自动发送消息，间隔" + interval + "毫秒");

                // 启动定时器
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        sendNextMessage();
                    }
                }, 0, interval);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "请输入有效的数字", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 停止按钮
        stopBtn.addActionListener(e -> {
            if (!isRunning) return;

            isRunning = false;
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            currentIndex = 0;
            statusLabel.setText("已停止");
            log("已停止发送");
        });
    }

    // 从文件加载消息
    private void loadMessagesFromFile(File file) {
        messages.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    messages.add(line);
                }
            }

            // 显示预览
            messageArea.setText("");
            for (int i = 0; i < messages.size() && i < 5; i++) {
                messageArea.append((i + 1) + ". " + messages.get(i) + "\n");
            }
            if (messages.size() > 5) {
                messageArea.append("... 共" + messages.size() + "条消息");
            }

            log("成功加载 " + messages.size() + " 条消息");
            statusLabel.setText("已加载文件: " + file.getName());
        } catch (IOException ex) {
            log("文件读取失败: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "文件读取失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 发送下一条消息
    private void sendNextMessage() {
        if (messages.isEmpty()) return;

        // 循环发送
        if (currentIndex >= messages.size()) {
            currentIndex = 0;
            log("消息列表已循环");
        }

        String message = messages.get(currentIndex);
        log("发送消息: " + message);

        // 模拟输入
        typeMessage(message);

        currentIndex++;
    }

    // 模拟键盘输入
    private void typeMessage(String message) {
        if (robot == null) return;

        // 清除当前输入框内容
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.delay(50);
        robot.keyPress(KeyEvent.VK_BACK_SPACE);
        robot.keyRelease(KeyEvent.VK_BACK_SPACE);
        robot.delay(50);

        // 输入消息内容
        for (char c : message.toCharArray()) {
            typeChar(c);
            robot.delay(20); // 输入间隔，避免过快
        }

        // 模拟回车发送
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.delay(100);
    }

    // 模拟单个字符输入
    private void typeChar(char c) {
        int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);

        // 处理特殊字符
        if (keyCode == KeyEvent.VK_UNDEFINED) {
            // 这里可以扩展处理更多特殊字符
            log("不支持的字符: " + c);
            return;
        }

        // 判断是否需要Shift键
        boolean needShift = Character.isUpperCase(c) ||
                "~!@#$%^&*()_+{}|:\"<>?".indexOf(c) != -1;

        if (needShift) {
            robot.keyPress(KeyEvent.VK_SHIFT);
        }

        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);

        if (needShift) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
    }

    // 日志输出
    private void log(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.util.Date().toString().substring(11, 19) + "] " + text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        // 在事件调度线程中启动UI
        SwingUtilities.invokeLater(AutoMessageSender::new);
    }
}
//by:草籽
