import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.net.URL;
import java.io.*;
import java.util.concurrent.*;


public class BankGeu extends JFrame {
	private static final long serialVersionUID = 1L;
    JTextField userField;
    JPasswordField passField;

    final String DB_URL = "jdbc:oracle:thin:@localhost:1521:XE";
    final String DB_USER = "RISHAV";
    final String DB_PASS = "1937";

    public BankGeu() {
        setTitle("Bank Of GEU");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        // Setting the icon of the application
        URL iconURL = getClass().getResource("/images/geuLogo.png");
        if (iconURL != null) {
            ImageIcon ic = new ImageIcon(iconURL);
            setIconImage(ic.getImage());
        }

        // Setting background image
        ImageIcon bgIcon;
        URL bgURL = getClass().getResource("/images/GeuBank.png");
        if (bgURL != null) {
            bgIcon = new ImageIcon(bgURL);
        } else {
            bgIcon = new ImageIcon();
        }

        JLabel background = new JLabel(bgIcon);
        background.setLayout(new BorderLayout());
        setContentPane(background);
        
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel logoLabel = new JLabel(new ImageIcon(getClass().getResource("/images/BankLogo.png")));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(Box.createVerticalStrut(20));
        topPanel.add(logoLabel);
        
        background.add(topPanel, BorderLayout.NORTH);
        
        JPanel box = new JPanel();
        box.setPreferredSize(new Dimension(325, 300));
        box.setBackground(new Color(255, 255, 255, 200));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        Font labelFont = new Font("Times New Roman", Font.BOLD, 16);


        // Username and Password fields and labels

        JLabel user = new JLabel(new ImageIcon(getClass().getResource("/images/username.png")));
        user.setAlignmentX(CENTER_ALIGNMENT);
        
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(labelFont);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        userField = new JTextField();
        userField.setMaximumSize(new Dimension(200, 30));
        userField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel pass = new JLabel(new ImageIcon(getClass().getResource("/images/password.png")));
        pass.setAlignmentX(CENTER_ALIGNMENT);
        
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(labelFont);
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        passField = new JPasswordField();
        passField.setMaximumSize(new Dimension(200, 30));
        passField.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons
        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("Arial", Font.BOLD, 16));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.addActionListener(e -> loginUser());

        JButton registerBtn = new JButton("Register");
        registerBtn.setFont(new Font("Arial", Font.BOLD, 16));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.addActionListener(e -> new Registration());

        JButton faceLoginBtn = new JButton("Face Login");
        faceLoginBtn.setFont(new Font("Arial", Font.BOLD, 16));
        faceLoginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        faceLoginBtn.addActionListener(e -> faceLoginUser());

        // Adding components to the panel
        box.add(Box.createVerticalStrut(10));
        
        box.add(user);
//        box.add(userLabel);
        box.add(userField);
        box.add(Box.createVerticalStrut(10));
        box.add(pass);
//        box.add(passLabel);
        box.add(passField);
        box.add(Box.createVerticalStrut(20));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(registerBtn);
        buttonRow.add(loginBtn);
        box.add(buttonRow);

        box.add(Box.createVerticalStrut(20));
        box.add(faceLoginBtn);
        box.add(Box.createVerticalStrut(40));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(box);
        background.add(centerPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    void loginUser() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.");
            return;
        }

        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String query = "SELECT account_number FROM BANK_GEU WHERE username = ? AND password = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, user);
            ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long accNo = rs.getLong("ACCOUNT_NUMBER"); // Use long for account number
                new DashBoard(accNo);  // Pass long account number to Dashboard
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error during login.");
        }
    }

    void faceLoginUser() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("dist\\face_login.exe");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String accNo = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Python Output: " + line);
                    accNo = line.trim();  // Capture account number
                }

                int exitCode = process.waitFor();

                if (exitCode == 0 && accNo != null && !accNo.isEmpty()) {
                    try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                        PreparedStatement ps = con.prepareStatement(
                            "SELECT account_number FROM BANK_GEU WHERE account_number = ?");
                        ps.setLong(1, Long.parseLong(accNo));  // Use long here as well
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            long accNoParsed = rs.getLong("ACCOUNT_NUMBER");
                            new DashBoard(accNoParsed);  // Pass long account number to Dashboard
                        } else {
                            JOptionPane.showMessageDialog(this, "User not found.");
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Face not recognized.");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Face login failed.");
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BankGeu::new);
    }
}
