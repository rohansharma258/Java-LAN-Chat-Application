import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private DefaultListModel<String> userListModel;
    private PrintWriter out;
    private String username;

    public ChatClientGUI(String serverAddress, int port) {
        frame = new JFrame("Java Chat");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        frame.add(chatScroll, BorderLayout.CENTER);

        // Online Users List
        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        frame.add(userScroll, BorderLayout.EAST);

        // Input Field
        inputField = new JTextField();
        frame.add(inputField, BorderLayout.SOUTH);

        inputField.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                out.println(message);
                inputField.setText("");
            }
        });

        // Username dialog
        username = JOptionPane.showInputDialog(frame, "Enter username:");
        if (username == null || username.trim().isEmpty()) {
            System.exit(0);
        }

        try {
            Socket socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(username);

            // Read thread
            new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("/users ")) {
                            userListModel.clear();
                            String[] users = line.substring(7).split(",");
                            for (String user : users) {
                                if (!user.isEmpty()) userListModel.addElement(user);
                            }
                        } else {
                            chatArea.append(line + "\n");
                            chatArea.setCaretPosition(chatArea.getDocument().getLength()); // auto scroll
                        }
                    }
                } catch (IOException ex) {
                    chatArea.append("❌ Connection closed.\n");
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Failed to connect to server", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClientGUI("localhost", 12345));
    }
}
