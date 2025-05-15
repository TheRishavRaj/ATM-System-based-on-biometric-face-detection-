import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.sql.*;
import java.text.*;

public class Registration extends JFrame {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField nameField, dobField, userField;
    private JPasswordField passField;

    final String DB_URL = "jdbc:oracle:thin:@localhost:1521:XE";
    final String DB_USER = "RISHAV";
    final String DB_PASS = "1937";

    public Registration() {
        setTitle("ATM Registration");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));

        panel.add(new JLabel("Full Name:"));
        nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("Date of Birth (YYYY-MM-DD):"));
        dobField = new JTextField();
        panel.add(dobField);

        panel.add(new JLabel("Username:"));
        userField = new JTextField();
        panel.add(userField);

        panel.add(new JLabel("Password:"));
        passField = new JPasswordField();
        panel.add(passField);

        JButton registerButton = new JButton("Register & Capture Face");
        registerButton.addActionListener(e -> registerUser());

        panel.add(new JLabel());
        panel.add(registerButton);

        add(panel);
        setVisible(true);
    }

    private void registerUser() {
        String name = nameField.getText().trim();
        String dob = dobField.getText().trim();
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (name.isEmpty() || dob.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int accNo = getNextAccountNumber();

            ProcessBuilder pb = new ProcessBuilder("dist\\capture_face.exe", String.valueOf(accNo));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                JOptionPane.showMessageDialog(this, "Face capture failed.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (insertUser(name, dob, accNo, username, password)) {
                JOptionPane.showMessageDialog(this,
                        "Registration successful!\nAccount No: " + accNo,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error during registration.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getNextAccountNumber() {
        int nextAccNo = 10000001;
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(account_number) FROM BANK_GEU")) {
            if (rs.next()) {
                int max = rs.getInt(1);
                if (max > 0) {
                    nextAccNo = max + 1;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nextAccNo;
    }

    private boolean insertUser(String name, String dob, int accNo, String username, String password) {
        String insertSQL = "INSERT INTO BANK_GEU (name, dob, account_number, username, password, balance, face_image) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String imagePath = Paths.get("dataset", accNo + ".jpg").toString();

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            JOptionPane.showMessageDialog(this, "Image not found at " + imagePath, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            dob = dob.replace("/", "-")  ;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date parsedDate = sdf.parse(dob);
            java.sql.Date sqlDate = new java.sql.Date(parsedDate.getTime());

            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = con.prepareStatement(insertSQL);
                 FileInputStream fis = new FileInputStream(imageFile)) {

                ps.setString(1, name);
                ps.setDate(2, sqlDate);
                ps.setInt(3, accNo);
                ps.setString(4, username);
                ps.setString(5, password);
                ps.setDouble(6, 0.0);  // Default balance
                ps.setBinaryStream(7, fis, fis.available());

                return ps.executeUpdate() > 0;
            }

        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Registration::new);
    }
}
