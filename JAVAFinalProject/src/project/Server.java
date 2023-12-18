package project;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Server {
	private static ServerSocket serverSocket;
    private static boolean running = true;
    private static JTextArea textArea;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10); // 创建固定大小的线程池

    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame("Server Control Panel");
        frame.setSize(300, 200);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setEditable(false);
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running = false;
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                    textArea.append("Server shutting down.\n");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                frame.dispose();
            }
        });


        frame.setVisible(true);

        // start the servers
        int port = 9898;
        serverSocket = new ServerSocket(port);
        textArea.append("Server listening on port " + port + "\n"); // Display on GUI
        
     // thread pools
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket)); // 使用线程池执行任务
            } catch (IOException e) {
                if (!running) {
                    textArea.append("Server shutting down.\n");
                    break;
                }
                e.printStackTrace();
            }
        }

        threadPool.shutdown();
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
        	
        	textArea.append("New client connected: " + clientSocket.getInetAddress().getHostAddress() + "\n");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String request;
                while ((request = in.readLine()) != null) {
                	System.out.println("Received request from client: " + request);

                	if (request.startsWith("Register:")) {
                        String[] credentials = request.substring(9).split(":");
                        String username = credentials[0];
                        String password = credentials[1];

                        if (registerUser(username, password)) {
                        	textArea.append("Registration successful");
                        	out.println("Registration successful");
                        } else {
                        	textArea.append("Registration failed");
                        	out.println("Registration failed");
                        }
                    } else if (request.startsWith("Login:")) {
                        String[] credentials = request.substring(6).split(":");
                        String username = credentials[0];
                        String password = credentials[1];
                        int userId = validateUser(username, password);
                        if (userId != -1) {
                            textArea.append("Login successful" + userId); //for test
                            out.println("Login successful:" + userId); 
                        } else {
                            textArea.append("Invalid credentials"); //for test
                            out.println("Invalid credentials");
                        }
                    } else if (request.startsWith("View Reservation")) {
                        String[] requestParts = request.split(":");
                        //get userid here
                        int userId = Integer.parseInt(requestParts[1]);
                        System.out.println("Received User ID from client: " + userId); //for test
                        if (userId != -1) {
                            String response = queryReservations(userId);
                            out.println(response);
                            out.println("END_OF_RESPONSE");
                        } else {
                            out.println("Invalid credentials");
                        }
                    } else if (request.startsWith("Book:")) {
                        String[] bookingParts = request.split(":");
                        int userId = Integer.parseInt(bookingParts[1]);
                        String date = bookingParts[2];
                        String description = bookingParts[3];
                        String bookingInfo = date + ":" + description;

                        String response = addBooking(bookingInfo, userId);
                        out.println(response);
                        out.println("END_OF_RESPONSE");
                    } else if (request.startsWith("Delete Booking:")) {
                        String[] parts = request.split(":");
                        int userId = Integer.parseInt(parts[1]);
                        int bookingId = Integer.parseInt(parts[2]);
                        String response = deleteBooking(userId, bookingId);
                        out.println(response);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private static String deleteBooking(int userId, int bookingId) {
            String url = "jdbc:sqlite:bookingMessage.db";
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM appointments WHERE id = ? AND userId = ?")) {
                deleteStmt.setInt(1, bookingId);
                deleteStmt.setInt(2, userId);
                int affectedRows = deleteStmt.executeUpdate();
                return affectedRows > 0 ? "Booking successfully deleted" : "No booking found with the given ID for this user";
            } catch (SQLException e) {
                e.printStackTrace();
                return "Error while deleting booking";
            }
        }
        
        private boolean registerUser(String username, String password) {
            String url = "jdbc:sqlite:bookingMessage.db";
            try {
				Class.forName("org.sqlite.JDBC");
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement checkUserStmt = conn.prepareStatement("SELECT COUNT(*) FROM User WHERE username = ?");
                 PreparedStatement registerStmt = conn.prepareStatement("INSERT INTO User (username, password) VALUES (?, ?)")) {

                // check if there already have a user
                checkUserStmt.setString(1, username);
                ResultSet rs = checkUserStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return false;
                }

                // 注册新用户
                registerStmt.setString(1, username);
                registerStmt.setString(2, password);
                registerStmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        
        //check the username and password
        private int validateUser(String username, String password) {
            String url = "jdbc:sqlite:bookingMessage.db";
            try {
                Class.forName("org.sqlite.JDBC");
                try (Connection conn = DriverManager.getConnection(url);
                     PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM User WHERE username = ? AND password = ?")) {
                    
                    pstmt.setString(1, username);
                    pstmt.setString(2, password);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                    return -1;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
    }
        
    //query function
    private static String queryReservations(int userId) {
        String url = "jdbc:sqlite:bookingMessage.db";
        StringBuilder result = new StringBuilder();
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM appointments WHERE userId = ?")) {
                
                pstmt.setInt(1, userId); // 使用userId过滤结果
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String date = rs.getString("date");
                    String description = rs.getString("description");
                    result.append("ID: ").append(id).append(", Date: ").append(date).append(", Description: ").append(description).append("\n");
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return "Error querying the database.";
        }

        return result.toString();
    }
    
    //booking function
    private static String addBooking(String bookingInfo, int userId) {
        // Split bookingInfo to get date and description
        String[] parts = bookingInfo.split(":");
        String date = parts[0];
        String description = parts[1];
        
        System.out.println("即将添加预约：");
        System.out.println("用户ID: " + userId);
        System.out.println("日期: " + date);
        System.out.println("描述: " + description);

        String url = "jdbc:sqlite:bookingMessage.db";
        try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM appointments WHERE date = ?");
             PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO appointments (userId, date, description) VALUES (?, ?, ?)")) {

            // Check if date is already booked
            checkStmt.setString(1, date);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "Date already booked";
            }

            // Insert new booking
            insertStmt.setInt(1, userId);
            insertStmt.setString(2, date);
            insertStmt.setString(3, description);
            insertStmt.executeUpdate();
            return "Booking successfully added";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error while adding booking";
        }
    }
}