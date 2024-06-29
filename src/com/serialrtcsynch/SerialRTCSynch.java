package com.serialrtcsynch;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.util.Locale;
import com.fazecast.jSerialComm.SerialPort;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SerialRTCSynch {

    private static JComboBox<String> portComboBox;
    private static SerialPort comPort;
    private static JButton setTimeButton;
    private static JTextArea logArea;
    private static JScrollPane scrollPane;
    private static JLabel clockLabel;
    private static JLabel connectionStatusDot;     
    private static Timer clockTimer;
    private static Timer comPortRefreshTimer;

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        JFrame frame = new JFrame(" ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        
        ImageIcon icon = new ImageIcon(SerialRTCSynch.class.getResource("/ico64.png"));
        frame.setIconImage(icon.getImage());
        
        frame.setLocationRelativeTo(null); 

        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel);

        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel) {
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Arduino COM Port Communication");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); 
        topPanel.add(titleLabel);

        JPanel comPanel = new JPanel();
        comPanel.setLayout(new FlowLayout());

        JLabel portLabel = new JLabel("Select COM Port:");
        portLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        comPanel.add(portLabel);

        portComboBox = new JComboBox<>();
        comPanel.add(portComboBox);

        setTimeButton = new JButton("Set Time");
        setTimeButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        setTimeButton.setBackground(new Color(70, 130, 180));
        setTimeButton.setForeground(Color.WHITE);
        comPanel.add(setTimeButton);

        connectionStatusDot = new JLabel("\u25CF");
        connectionStatusDot.setFont(new Font("Arial", Font.BOLD, 20));
        connectionStatusDot.setForeground(Color.RED); 
        comPanel.add(connectionStatusDot);

        topPanel.add(comPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFocusable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); 
        logArea.setBackground(new Color(50, 50, 50));
        logArea.setForeground(Color.LIGHT_GRAY); 
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setVisible(false);
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE); /

        scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY)); 
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        clockLabel = new JLabel(" ", JLabel.RIGHT);
        clockLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        clockLabel.setBorder(new EmptyBorder(10, 3, 10, 3)); 
        bottomPanel.add(clockLabel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        setTimeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setTimeOnArduino();
            }
        });

        // Timer to update clock display
        clockTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clockLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
        });
        clockTimer.start();

        // Refresh COM ports every 2 seconds
        comPortRefreshTimer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshComPorts();
            }
        });
        comPortRefreshTimer.start();

        // Initial population of COM ports
        refreshComPorts();
    }

    private static void refreshComPorts() {
        String currentSelection = (String) portComboBox.getSelectedItem();
        portComboBox.removeAllItems();
        SerialPort[] ports = SerialPort.getCommPorts();
        boolean selectionExists = false;
        for (SerialPort port : ports) {
            portComboBox.addItem(port.getSystemPortName());
            if (port.getSystemPortName().equals(currentSelection)) {
                selectionExists = true;
            }
        }
        if (selectionExists) {
            portComboBox.setSelectedItem(currentSelection);
        }
    }

    private static void setTimeOnArduino() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String selectedPort = (String) portComboBox.getSelectedItem();
                comPort = SerialPort.getCommPort(selectedPort);

                comPort.setComPortParameters(9600, 8, 1, 0); // Set com parameters
                comPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);

                if (comPort.openPort()) {
                    logArea.append("> Port opened.\n");
                    comPort.setDTR();
                    comPort.setRTS();
                    // Allow some time for the Arduino to reset and initialize
                    logArea.append("> Waiting for board reset. \n");
                    
                    Thread.sleep(5000);
                    // Send current time to Arduino
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter arduinoFormatter = DateTimeFormatter.ofPattern("yy, M, d, H, m, s", Locale.ENGLISH);
                    String arduinoFormattedDateTime = now.format(arduinoFormatter);

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d uuuu 'at' HH:mm", Locale.ENGLISH);
                    String formattedDateTime = now.format(formatter);

                    try {
                        comPort.setDTR();
                        comPort.setRTS();
                        comPort.getOutputStream().write(arduinoFormattedDateTime.getBytes());
                        comPort.getOutputStream().flush();
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    logArea.append("> Sent : " + formattedDateTime + "\n");

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            connectionStatusDot.setForeground(Color.GREEN);
                        }
                    });
                    
                    Thread.sleep(5000);
                    // Close the port
                    comPort.closePort();
                    logArea.append("> Port closed.\n");

                    Thread.sleep(2000);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            connectionStatusDot.setForeground(Color.RED);
                        }
                    });
                } else {
                    logArea.append("> Failed to open port.\n");
                }

                return null;
            }
        }.execute();
    }
}
