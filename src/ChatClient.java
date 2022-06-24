import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.CardLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import java.awt.BorderLayout;
import javax.swing.JTextArea;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.event.ActionEvent;

public class ChatClient {

	private JFrame frmChatClient;
	private JTextField textFieldIPAddress;
	private JTextField textFieldPort;
	private JTextField textFieldMyName;
	private JList listOnlineUsers = new JList();
	JTextArea textAreaChat = new JTextArea();
	private Socket socket = null;
	private Timer receivingTimer = null;
	private int status = 0;  // 0 for offline, 1 for idle, 2 for talking.

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ChatClient window = new ChatClient();
					window.frmChatClient.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ChatClient() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmChatClient = new JFrame();
		frmChatClient.setTitle("Chat Client");
		frmChatClient.setBounds(100, 100, 450, 300);
		frmChatClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmChatClient.getContentPane().setLayout(new CardLayout(0, 0));
		frmChatClient.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try  {
					if (receivingTimer!=null) {
						receivingTimer.cancel();
					}
					if (socket!=null) {
						socket.close();
						socket = null;
					}
					frmChatClient.dispose();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		
		JPanel panelConnect = new JPanel();
		frmChatClient.getContentPane().add(panelConnect, "connect");
		panelConnect.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("IP Address");
		lblNewLabel.setBounds(10, 11, 71, 14);
		panelConnect.add(lblNewLabel);
		
		textFieldIPAddress = new JTextField();
		textFieldIPAddress.setText("127.0.0.1");
		textFieldIPAddress.setBounds(78, 8, 228, 20);
		panelConnect.add(textFieldIPAddress);
		textFieldIPAddress.setColumns(10);
		
		JLabel lblNewLabel_1 = new JLabel("Port");
		lblNewLabel_1.setBounds(10, 39, 46, 14);
		panelConnect.add(lblNewLabel_1);
		
		textFieldPort = new JTextField();
		textFieldPort.setText("5555");
		textFieldPort.setBounds(78, 36, 86, 20);
		panelConnect.add(textFieldPort);
		textFieldPort.setColumns(10);
		
		JButton btnOnLine = new JButton("On Line");
		btnOnLine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					String ip = textFieldIPAddress.getText();
					int port = Integer.parseInt(textFieldPort.getText());
					socket = new Socket(ip, port);
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					writer.write("req:[online;"+textFieldMyName.getText()+"]");
					writer.newLine();
					String resp=reader.readLine();
					if (resp.equals("res:[ok]")) {
						
						
						receivingTimer = new Timer();
						receivingTimer.schedule(new TimerTask() {
							public void run() {
								try {
									BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
									String line = reader.readLine();
									if (isRequestResponse(line)) {
										//do something
									} else {
										if (status == 2)
											textAreaChat.append(line+"\r\n");
									}
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						}, 0, 20);
						
						writer.write("req:[getUserList]");
						writer.newLine();
						resp=reader.readLine(); // res:[userList: "name1", "name2", "name3"]
						String respData = parseResponse("userList");
						String[] list = parseStringList(respData);
						
						DefaultListModel<String> listModel = new DefaultListModel();
						listModel.addAll(Arrays.asList(list));
						listOnlineUsers.setModel(listModel);
						
						CardLayout layout = (CardLayout)frmChatClient.getContentPane().getLayout();
						layout.show(frmChatClient.getContentPane(), "idle");
						status = 1;
					}
					
					
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		btnOnLine.setBounds(10, 113, 89, 23);
		panelConnect.add(btnOnLine);
		
		JLabel lblNewLabel_2 = new JLabel("My Name");
		lblNewLabel_2.setBounds(10, 70, 46, 14);
		panelConnect.add(lblNewLabel_2);
		
		textFieldMyName = new JTextField();
		textFieldMyName.setBounds(78, 67, 228, 20);
		panelConnect.add(textFieldMyName);
		textFieldMyName.setColumns(10);
		
		JPanel panelIdle = new JPanel();
		frmChatClient.getContentPane().add(panelIdle, "idle");
		panelIdle.setLayout(new BorderLayout(0, 0));
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (listOnlineUsers.isSelectionEmpty())
						return;
					String user = (String)listOnlineUsers.getSelectedValue();
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					writer.write("req:[connect;"+user+"]");
					writer.newLine();
					String resp =reader.readLine();
					if (parseResponse("ok").equals("ok")) {
						
						CardLayout layout = (CardLayout)frmChatClient.getContentPane().getLayout();
						layout.show(frmChatClient.getContentPane(), "chat");
						status = 2;
						
					}
					
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		panelIdle.add(btnConnect, BorderLayout.SOUTH);
		
		
		panelIdle.add(listOnlineUsers, BorderLayout.CENTER);
		
		JPanel panelChatBox = new JPanel();
		frmChatClient.getContentPane().add(panelChatBox, "chat");
		panelChatBox.setLayout(null);
		
		
		textAreaChat.setEditable(false);
		textAreaChat.setBounds(0, 0, 434, 145);
		panelChatBox.add(textAreaChat);
		
		JTextArea textAreaMessage = new JTextArea();
		textAreaMessage.setBounds(0, 156, 434, 60);
		panelChatBox.add(textAreaMessage);
		
		JButton btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					String message = textAreaMessage.getText();
					if (!message.isEmpty()) {
						BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
						writer.write(message);
						writer.newLine();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		btnSend.setBounds(69, 227, 89, 23);
		panelChatBox.add(btnSend);
		
		JButton btnDiconnect = new JButton("Exit");
		btnDiconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					writer.write("req:[disconnect]");
					writer.newLine();

					((CardLayout)frmChatClient.getContentPane().getLayout()).show(frmChatClient.getContentPane(), "idle");
					status = 1;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		btnDiconnect.setBounds(257, 227, 89, 23);
		panelChatBox.add(btnDiconnect);
	}
}
