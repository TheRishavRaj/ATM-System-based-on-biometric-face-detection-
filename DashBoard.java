import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.sql.*;

public class DashBoard extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long accountNumber;
    private String name;
    private double balance;

    final String DB_URL = "jdbc:oracle:thin:@localhost:1521:XE";
    final String DB_USER = "RISHAV";
    final String DB_PASS = "1937";

    public DashBoard(long accountNumber) {
        this.accountNumber = accountNumber;
        if (!loadUserData()) {
            JOptionPane.showMessageDialog(null, "Failed to load user data. Exiting dashboard.");
            dispose();
            return;
        }
        initUI();
    }

    private boolean loadUserData() {
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = con.prepareStatement("SELECT name, balance FROM BANK_GEU WHERE account_number = ?")) {
            ps.setLong(1, accountNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                name = rs.getString("name");
                balance = rs.getDouble("balance");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void initUI() {
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);

        // Setting the icon of the application
        URL iconURL = getClass().getResource("/images/geuLogo.png");
        if (iconURL != null) {
            ImageIcon ic = new ImageIcon(iconURL);
            setIconImage(ic.getImage());
        }
        
        JLabel background = new JLabel(new ImageIcon(getClass().getResource("/images/GeuBank.png")));
        background.setLayout(new BorderLayout());
        setContentPane(background);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel logoLabel = new JLabel(new ImageIcon(getClass().getResource("/images/BankLogo.png")));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(Box.createVerticalStrut(20));
        topPanel.add(logoLabel);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Arial", Font.BOLD, 18));
        logoutBtn.setForeground(Color.BLACK);
        logoutBtn.setBackground(Color.WHITE);
        logoutBtn.setBorder(BorderFactory.createLineBorder(Color.RED));
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setPreferredSize(new Dimension(120, 50));
        logoutBtn.addActionListener(e -> logout());
        logoutBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                logoutBtn.setBackground(Color.RED);
                logoutBtn.setForeground(Color.WHITE);
            }
            public void mouseExited(MouseEvent e) {
                logoutBtn.setBackground(Color.WHITE);
                logoutBtn.setForeground(Color.BLACK);
            }
        });

        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutPanel.setOpaque(false);
        logoutPanel.add(logoutBtn);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(logoutPanel);

        background.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        JPanel optionsPanel = new JPanel(new GridLayout(3, 2, 30, 30)) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                g2.setColor(new Color(12, 28, 61));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        optionsPanel.setOpaque(false);
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        optionsPanel.setPreferredSize(new Dimension(1200, 700));

        String[] labels = {
            "Deposit", "Withdraw", "Transfer",
            "Balance Inquiry", "Change PIN", "Mini Statement"
        };

        for (String text : labels) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Arial", Font.BOLD, 30));
            btn.setBackground(new Color(0, 81, 135));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(700, 120));
            btn.setBorder(BorderFactory.createEmptyBorder());
            btn.addActionListener(e -> handleSelection(text.charAt(0)));
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(new Color(0, 102, 204));
                }
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(new Color(0, 81, 135));
                }
            });
            optionsPanel.add(btn);
        }

        centerPanel.add(optionsPanel);
        background.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(11, 15, 38));
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel nameLabel = new JLabel((name != null ? "Welcome " + name.trim() : "User"));
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);

        JLabel accLabel = new JLabel("Account No: " + accountNumber);
        accLabel.setFont(new Font("Arial", Font.BOLD, 16));
        accLabel.setForeground(Color.WHITE);

        bottomPanel.add(nameLabel, BorderLayout.WEST);
        bottomPanel.add(accLabel, BorderLayout.EAST);

        background.add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void handleSelection(char c) {
        switch (c) {
            case 'D' -> deposit();
            case 'W' -> withdraw();
            case 'T' -> transfer();
            case 'B' -> showBalance();
            case 'C' -> changePIN();
            case 'M' -> miniStatement();
        }
    }

    private void deposit() {
        String input = JOptionPane.showInputDialog(this, "Enter Deposit Amount:");
        try {
            double amount = Double.parseDouble(input);
            if (amount <= 0) throw new NumberFormatException();
            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = con.prepareStatement("UPDATE BANK_GEU SET balance = balance + ? WHERE account_number = ?")) {
                ps.setDouble(1, amount);
                ps.setLong(2, accountNumber);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Deposit Successful.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Input.");
        }
    }

    private void withdraw() {
        String input = JOptionPane.showInputDialog(this, "Enter Withdrawal Amount:");
        try {
            double amount = Double.parseDouble(input);
            if (amount <= 0 || amount > balance) throw new NumberFormatException();
            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = con.prepareStatement("UPDATE BANK_GEU SET balance = balance - ? WHERE account_number = ?")) {
                ps.setDouble(1, amount);
                ps.setLong(2, accountNumber);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Withdrawal Successful.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid or Insufficient Balance.");
        }
    }

    private void transfer() {
        String toAcc = JOptionPane.showInputDialog(this, "Enter recipient account number:");
        String amtStr = JOptionPane.showInputDialog(this, "Enter amount:");
        try {
            long toAccount = Long.parseLong(toAcc);
            double amount = Double.parseDouble(amtStr);
            if (toAccount == accountNumber || amount <= 0 || amount > balance) throw new NumberFormatException();
            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                con.setAutoCommit(false);
                try (
                    PreparedStatement debit = con.prepareStatement("UPDATE BANK_GEU SET balance = balance - ? WHERE account_number = ?");
                    PreparedStatement credit = con.prepareStatement("UPDATE BANK_GEU SET balance = balance + ? WHERE account_number = ?")
                ) {
                    debit.setDouble(1, amount);
                    debit.setLong(2, accountNumber);
                    credit.setDouble(1, amount);
                    credit.setLong(2, toAccount);

                    int rows1 = debit.executeUpdate();
                    int rows2 = credit.executeUpdate();
                    if (rows1 == 1 && rows2 == 1) {
                        con.commit();
                        JOptionPane.showMessageDialog(this, "Transfer Successful.");
                    } else {
                        con.rollback();
                        JOptionPane.showMessageDialog(this, "Transfer Failed. Invalid Account?");
                    }
                } catch (SQLException e) {
                    con.rollback();
                    throw e;
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Transfer Failed: " + e.getMessage());
        }
    }

    private void showBalance() {
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = con.prepareStatement("SELECT balance FROM BANK_GEU WHERE account_number = ?")) {
            ps.setLong(1, accountNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double bal = rs.getDouble("balance");
                JOptionPane.showMessageDialog(this, "Your Balance: Rs. " + bal);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching balance.");
        }
    }

    private void changePIN() {
        String newPIN = JOptionPane.showInputDialog(this, "Enter New PIN:");
        if (newPIN == null || newPIN.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "PIN not changed.");
            return;
        }
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = con.prepareStatement("UPDATE BANK_GEU SET password = ? WHERE account_number = ?")) {
            ps.setString(1, newPIN);
            ps.setLong(2, accountNumber);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "PIN changed successfully.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error changing PIN.");
        }
    }

    private void miniStatement() {
        JOptionPane.showMessageDialog(this, "Mini Statement feature coming soon.");
    }

    private void logout() {
        dispose();
        SwingUtilities.invokeLater(() -> new BankGeu());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DashBoard(10000001));
    }
}
