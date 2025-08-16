import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcast(String message, ClientHandler excludeUser) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != excludeUser) {
                    client.sendMessage(message);
                }
            }
        }
    }

    static void updateUserList() {
        StringBuilder userList = new StringBuilder("/users ");
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                userList.append(client.username).append(",");
            }
        }
        for (ClientHandler client : clientHandlers) {
            client.sendMessage(userList.toString());
        }
    }

    static void removeUser(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        broadcast("🔴 " + clientHandler.username + " left the chat", clientHandler);
        updateUserList();
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                username = in.readLine();
                broadcast("🔵 " + username + " joined the chat", this);
                updateUserList();

                String msg;
                while ((msg = in.readLine()) != null) {
                    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

                    if (msg.startsWith("/msg ")) {
                        String[] split = msg.split(" ", 3);
                        if (split.length >= 3) {
                            String targetUser = split[1];
                            String privateMsg = split[2];
                            boolean found = false;

                            synchronized (clientHandlers) {
                                for (ClientHandler client : clientHandlers) {
                                    if (client.username.equalsIgnoreCase(targetUser)) {
                                        client.sendMessage("💌 [Private from " + username + " at " + timestamp + "]: " + privateMsg);
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                out.println("❌ User " + targetUser + " not found.");
                            }
                        } else {
                            out.println("❌ Usage: /msg <username> <message>");
                        }
                    } else {
                        broadcast("[" + timestamp + "] " + username + ": " + msg, this);
                    }
                }
            } catch (IOException e) {
                System.out.println(username + " disconnected.");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                removeUser(this);
            }
        }
    }
}
