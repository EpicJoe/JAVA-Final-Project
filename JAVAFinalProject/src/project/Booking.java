package project;

import javax.swing.*;
import javax.swing.text.AbstractDocument;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import javax.swing.text.DocumentFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;


public class Booking {
	private static int userId = -1;
	private static boolean showLoginDialog() {
		final JDialog loginDialog = new JDialog();
	    loginDialog.setModal(true);
	    loginDialog.setLayout(new BoxLayout(loginDialog.getContentPane(), BoxLayout.Y_AXIS));
	    loginDialog.setSize(300, 500);


	    //booking interface
	    JPanel loginPanel = new JPanel(new FlowLayout());
	    JTextField usernameField = new JTextField(20);
	    JPasswordField passwordField = new JPasswordField(20);
	    JButton loginButton = new JButton("Login");

	    final boolean[] loginSuccess = {false};
	    loginButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            String username = usernameField.getText();
	            String password = new String(passwordField.getPassword());
	            
	            try (Socket socket = new Socket("localhost", 9898);
	                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
	                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
	                 
	                out.println("Login:" + username + ":" + password);
	                String response = in.readLine();
	                String[] responseParts = response.split(":");
	                if (responseParts[0].equals("Login successful")) {
	                    userId = Integer.parseInt(responseParts[1]);
	                    loginSuccess[0] = true;
	                    loginDialog.dispose();
	                } else {
	                    JOptionPane.showMessageDialog(loginDialog, "The username or password is incorrect", "Error", JOptionPane.ERROR_MESSAGE);
	                }
	            } catch (IOException ex) {
	                ex.printStackTrace();
	            }
	        }
	    });
	    
	    loginPanel.add(new JLabel("Username:"));
	    loginPanel.add(usernameField);
	    loginPanel.add(new JLabel("Password:"));
	    loginPanel.add(passwordField);
	    loginPanel.add(loginButton);
	    
	    JPanel registerPanel = new JPanel(new FlowLayout());
	    JTextField registerUsernameField = new JTextField(20);
	    JPasswordField registerPasswordField = new JPasswordField(20);
	    JPasswordField confirmRegisterPasswordField = new JPasswordField(20);
	    JButton registerButton = new JButton("Register");
	    
	    //register here
	    registerButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            String registerUsername = registerUsernameField.getText();
	            String registerPassword = new String(registerPasswordField.getPassword());
	            String confirmRegisterPassword = new String(confirmRegisterPasswordField.getPassword());
	            
	            if (!registerPassword.equals(confirmRegisterPassword)) {
	                JOptionPane.showMessageDialog(loginDialog, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
	                return;
	            }
	            registerUser(registerUsername, registerPassword);
	        }
	    });

	    registerPanel.add(new JLabel("Register Username:"));
	    registerPanel.add(registerUsernameField);
	    registerPanel.add(new JLabel("Register Password:"));
	    registerPanel.add(registerPasswordField);
	    registerPanel.add(new JLabel("Confirm Password:"));
	    registerPanel.add(confirmRegisterPasswordField);
	    registerPanel.add(registerButton);

	    loginDialog.add(loginPanel);
	    loginDialog.add(registerPanel);

	    loginDialog.setVisible(true);
	    return loginSuccess[0];
	}
	
	private static void registerUser(String username, String password) {
	    try (Socket socket = new Socket("localhost", 9898);
	         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
	         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

	        // Sending registration details to the server
	        out.println("Register:" + username + ":" + password);

	        // Reading response from the server
	        String response = in.readLine();
	        System.out.println("Server response: " + response);
	    } catch (IOException e) {
	        e.printStackTrace();
	        System.out.println("Error connecting to the server.");
	    }
	}

	
    private static void addComponentsToPane(Container pane) {
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 10, 10, 10);

        JButton bookButton = new JButton("Booking");
        bookButton.setPreferredSize(new Dimension(150, 40));
        bookButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	createBookingDialog();
            }
        });
        pane.add(bookButton, c);

        JButton viewButton = new JButton("View Reservation");
        viewButton.setPreferredSize(new Dimension(150, 40));
        viewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try (Socket socket = new Socket("localhost", 9898);
                             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        	out.println("View Reservation:" + userId); // send the message to servers
                            String response;
                            StringBuilder responseBuilder = new StringBuilder();
                            while (!(response = in.readLine()).equals("END_OF_RESPONSE")) {
                                responseBuilder.append(response).append("\n");
                            }
                            publish(responseBuilder.toString());
                        } catch (IOException ex) {
                        	ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Failed to connect to server: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<String> chunks) {
                        for (String chunk : chunks) {
                            JOptionPane.showMessageDialog(null, chunk, "Reservations", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                };
                worker.execute();
            }
        });
        pane.add(viewButton, c);
        
        JButton deleteButton = new JButton("Delete Reservation");
        deleteButton.setPreferredSize(new Dimension(150, 40));
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String bookingIdStr = JOptionPane.showInputDialog("Enter Booking ID to delete:");
                if (bookingIdStr != null && !bookingIdStr.isEmpty()) {
                    try {
                        int bookingId = Integer.parseInt(bookingIdStr);
                        deleteBooking(bookingId);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Invalid Booking ID", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        pane.add(deleteButton, c);
    }
    
    //delete function
    private static void deleteBooking(int bookingId) {
        try (Socket socket = new Socket("localhost", 9898);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("Delete Booking:" + userId + ":" + bookingId);

            String response = in.readLine();
            JOptionPane.showMessageDialog(null, response);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to connect to server: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void createBookingDialog() {
        JDialog bookingDialog = new JDialog();
        bookingDialog.setLayout(new FlowLayout());
        bookingDialog.setSize(500, 300);

        //limit to the text box
        JTextField yearField = new JTextField(4);
        ((AbstractDocument) yearField.getDocument()).setDocumentFilter(new LengthDocumentFilter(4));
        
        JTextField monthField = new JTextField(2);
        ((AbstractDocument) monthField.getDocument()).setDocumentFilter(new LengthDocumentFilter(2));
        
        JTextField dayField = new JTextField(2);
        ((AbstractDocument) dayField.getDocument()).setDocumentFilter(new LengthDocumentFilter(2));


        bookingDialog.add(new JLabel("Year:"));
        bookingDialog.add(yearField);
        bookingDialog.add(new JLabel("Month:"));
        bookingDialog.add(monthField);
        bookingDialog.add(new JLabel("Day:"));
        bookingDialog.add(dayField);
        
        JTextField descriptionField = new JTextField(30);
        bookingDialog.add(new JLabel("Description:"));
        bookingDialog.add(descriptionField);


        JButton submitButton = new JButton("Submit");
        
        //submit listener
        submitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String date = yearField.getText() + "-" + monthField.getText() + "-" + dayField.getText();
                String description = descriptionField.getText();

                try (Socket socket = new Socket("localhost", 9898);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                     
                	out.println("Book:" + userId + ":" + date + ":" + description);

                   
                    String response = in.readLine();
                    if ("Booking successfully added".equals(response)) {
                        JOptionPane.showMessageDialog(bookingDialog, "Successfully Added");
                    } else {
                        JOptionPane.showMessageDialog(bookingDialog, response, "Booking Fail", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(bookingDialog, "Cannot connect to server", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        bookingDialog.add(submitButton);

        bookingDialog.setVisible(true);
    }
    static class LengthDocumentFilter extends DocumentFilter {
        private final int maxCharacters;

        public LengthDocumentFilter(int maxChars) {
            maxCharacters = maxChars;
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            int currentLength = fb.getDocument().getLength();
            int overLimit = (currentLength + text.length()) - maxCharacters - length;
            if (overLimit > 0) {
                text = text.substring(0, text.length() - overLimit);
            }
            if (text.length() > 0) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }

    public static void main(String[] args) {
    	if (showLoginDialog()) {
    		//if login, show the menu
    		JFrame frame = new JFrame("Online Reservation System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            addComponentsToPane(frame.getContentPane());
            frame.setVisible(true);
        }
    }
}