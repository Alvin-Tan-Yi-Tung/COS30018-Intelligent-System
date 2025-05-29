package IntelligentProject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.LinkedList;
import java.util.Queue;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.table.*;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import java.awt.Component;
import jade.core.AID;
import javax.swing.RowFilter;
import java.util.Arrays;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController; 
import IntelligentProject.IManualDealerAgent;
import java.awt.GridLayout;
import javax.swing.JButton;
import java.util.concurrent.ConcurrentHashMap;
import IntelligentProject.IManualBuyerAgent;

/**
 * Main GUI class for the Car Negotiation System
 * Handles all user interface components and agent visualization
 */
public class GUI {
    // UI components and data structures
	private static Map<String, JTextPane> agentTextPanes = new HashMap<>();
	private static Map<String, JPanel> negotiationPanels = new HashMap<>();
	private static Map<String, List<String>> messageFlows = new HashMap<>();
    private static JFrame frame;
    private static JTabbedPane tabbedPane;
    private static JTabbedPane negotiationTabbedPane;
    private static JSplitPane mainSplitPane;
    private static JTextPane flowTextPane;
    private static Map<String, Queue<String>> interactionQueues = new HashMap<>();
    private static SequenceDiagramPanel diagramPanel;
    private static SequenceDiagramPanel sequenceDiagramPanel;
    private static Map<String, List<String>> agentInteractions = new HashMap<>();
    private static Map<String, DefaultTableModel> buyerTableModels = new HashMap<>();
    private DefaultTableModel dealerMatchesModel;
    private static Map<String, JFrame> chatWindows = new HashMap<>();
    private static Map<String, JTextArea> agentChatAreas = new HashMap<>();
    private static int manualBuyerCount = 1;
    private static int manualDealerCount = 1;
    private static Map<String, Map<String, Boolean>> acceptances = new ConcurrentHashMap<>();
    private static Map<String, DefaultTableModel> dealerTableModels = new ConcurrentHashMap<>();
    private static Map<String, String> buyerCarTypes = new ConcurrentHashMap<>();
    private static JLabel autoCommissionLabel;
    private static JLabel manualCommissionLabel;
    private static JLabel totalCommissionLabel;
    private static JPanel commissionPanel;
    private static Map<String, Map<String, Boolean>> buyerAcceptances = new ConcurrentHashMap<>();
    private static Map<String, Map<String, Boolean>> dealerAcceptances = new ConcurrentHashMap<>();
    
    /**
     * Initializes the main GUI components
     */
    public static void initialize() {
        try {
            // Main window setup
            frame = new JFrame("Car Negotiation System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);
            
            // Set consistent font
            Font defaultFont = new Font("Arial", Font.PLAIN, 16);
            UIManager.put("Button.font", defaultFont);
            UIManager.put("Label.font", defaultFont);
            UIManager.put("TextField.font", defaultFont);
            UIManager.put("TextArea.font", defaultFont);
            UIManager.put("Table.font", defaultFont);
            UIManager.put("TabbedPane.font", defaultFont);
            
            // Create toolbar with agent creation buttons
            JToolBar toolbar = new JToolBar();
            JButton addBuyerBtn = new JButton("+ Add Buyer Agent");
            JButton addDealerBtn = new JButton("+ Add Dealer Agent");
            
            addBuyerBtn.addActionListener(e -> showBuyerForm());
            addDealerBtn.addActionListener(e -> showDealerForm());
            
            toolbar.add(addBuyerBtn);
            toolbar.add(addDealerBtn);
            frame.add(toolbar, BorderLayout.NORTH);
            
            // Main content area
            mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            mainSplitPane.setResizeWeight(0.6);
            
            // Agent tabs
            tabbedPane = new JTabbedPane() {
                @Override
                public void setSelectedIndex(int index) {
                    super.setSelectedIndex(index);
                    if (index >= 0) {
                        String agentName = getTitleAt(index);
                        showAgentInteractions(agentName);
                    }
                }
            };
            
            // Initialize sequence diagram panel
            sequenceDiagramPanel = new SequenceDiagramPanel();
            JScrollPane diagramScrollPane = new JScrollPane(sequenceDiagramPanel);
            
            // Negotiation tabs
            negotiationTabbedPane = new JTabbedPane();
            negotiationTabbedPane.addTab("Sequence Diagram", diagramScrollPane);
            
            // Final layout
            mainSplitPane.setTopComponent(tabbedPane);
            mainSplitPane.setBottomComponent(negotiationTabbedPane);
            
            frame.add(mainSplitPane, BorderLayout.CENTER);
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            frame.setVisible(true);
            
            // Commission panel
            commissionPanel = new JPanel(new GridLayout(3, 1));
            autoCommissionLabel = new JLabel("Automated Commission: RM 0");
            manualCommissionLabel = new JLabel("Manual Commission: RM 0");
            totalCommissionLabel = new JLabel("Total Earned Commission: RM 0");
            
            commissionPanel.add(autoCommissionLabel);
            commissionPanel.add(manualCommissionLabel);
            commissionPanel.add(totalCommissionLabel);
            
            negotiationTabbedPane.addTab("Commission", commissionPanel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static JTabbedPane getAgentTabbedPane() {
        return tabbedPane;  // Ensure 'tabbedPane' is your main tab container
    }
    
    /* AGENT MANAGEMENT METHODS */
    
    /**
     * Shows buyer creation form dialog
     */
    private static void showBuyerForm() {
        BuyerFormDialog form = new BuyerFormDialog(frame);
        form.setVisible(true);
        String[] data = form.getData();
        if(data != null) {
            createManualBuyer(data[0], data[1], data[2]);
        }
    }
    
    /**
     * Shows dealer creation form dialog
     */
    private static void showDealerForm() {
        DealerFormDialog form = new DealerFormDialog(frame);
        form.setVisible(true);
        String[] data = form.getData();
        if(data != null) {
            createManualDealer(data[0], data[1]);
        }
    }
    
    /**
     * Creates a new manual buyer agent
     * @param carType The desired car type
     * @param offer Initial offer amount
     * @param budget Maximum budget
     */
    private static void createManualBuyer(String carType, String offer, String budget) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    String name = "M.Buyer" + manualBuyerCount++;
                    Object[] args = new Object[]{carType, offer, budget};
                    
                    jade.wrapper.AgentController ac = Main.getContainer().createNewAgent(
                        name, 
                        "IntelligentProject.ManualBuyerAgent", 
                        args
                    );
                    
                    ac.start();
                    
                    // Wait for agent to be ready
                    int retries = 0;
                    while (retries < 5) {
                        try {
                            ac.getO2AInterface(IManualBuyerAgent.class);
                            break;
                        } catch (Exception e) {
                            Thread.sleep(200);
                            retries++;
                        }
                    }
                    
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(frame, "Agent Creation Failed: " + e.getMessage())
                    );
                }
                return null;
            }
        }.execute();
    }
    
    /**
     * Creates a new manual dealer agent
     * @param carType Car model being sold
     * @param price Asking price
     */
    private static void createManualDealer(String carType, String price) {
        try {
            String name = "M.Dealer" + manualDealerCount++;
            Object[] args = new Object[]{carType, price};
            
            // Create agent
            jade.wrapper.AgentController ac = Main.getContainer().createNewAgent(
                name, 
                "IntelligentProject.ManualDealerAgent", 
                args
            );
            
            // Start agent
            ac.start();
            
            // Verify agent creation
            System.out.println("Dealer agent " + name + " created successfully");
            
            // Create GUI tab after agent is fully started
            SwingUtilities.invokeLater(() -> {
                DefaultTableModel model = new DefaultTableModel(
                    new Object[][]{}, 
                    new String[]{"Buyer", "Car Type", "Offer", "Status", "Confirm"}
                );
//                GUI.createManualDealerTab(name, carType, price, model);
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Failed to create dealer: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /* CHAT AND NEGOTIATION METHODS */
    
    /**
     * Creates chat session between two agents
     * @param participant1 First agent
     * @param participant2 Second agent
     */
    public static void createChatSession(String participant1, String participant2) {
    	// Sort names to ensure unique session ID
        String[] sorted = {participant1, participant2};
        Arrays.sort(sorted);
        String tabName = sorted[0] + " ↔ " + sorted[1];
        
        if(negotiationPanels.containsKey(tabName)) return;

        JPanel panel = new JPanel(new BorderLayout());
        
        // Chat history
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        
        // Control panel with buttons
        JPanel controlPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton acceptButton = new JButton("Accept Deal");
        JButton rejectButton = new JButton("Reject Deal");
        
        // Input panel
        JPanel inputPanel = new JPanel();
        JTextField messageField = new JTextField(25);
        JButton sendButton = new JButton("Send");
        
        // Add action listeners
        acceptButton.addActionListener(e -> handleDealAction(participant1, participant2, true, chatArea));
        rejectButton.addActionListener(e -> handleDealAction(participant1, participant2, false, chatArea));
        
        controlPanel.add(acceptButton);
        controlPanel.add(rejectButton);
        
        sendButton.addActionListener(e -> {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                try {
                	// Get agent controller
                    jade.wrapper.AgentController ac = Main.getContainer().getAgent(participant1);
                    
                    // Get O2A interface
                    Object agentObj = participant1.startsWith("M.Buyer")
                        ? ac.getO2AInterface(IManualBuyerAgent.class)
                        : ac.getO2AInterface(IManualDealerAgent.class);

                    if (agentObj == null) {
                        throw new Exception("Agent interface not available");
                    }

                    // Create and send message
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(new AID(participant2, AID.ISLOCALNAME));
                    msg.setContent(message);

                    if (agentObj instanceof IManualBuyerAgent) {
                        ((IManualBuyerAgent) agentObj).sendMessage(msg);
                    } else if (agentObj instanceof IManualDealerAgent) {
                        ((IManualDealerAgent) agentObj).sendMessage(msg);
                    }

                    GUI.updateChatSession(participant1, participant2, message);
                    messageField.setText("");
                } catch(Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, 
                        "Error sending message: " + ex.getMessage(),
                        "Send Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        inputPanel.add(messageField);
        inputPanel.add(sendButton);
        
        panel.add(chatScroll, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        negotiationPanels.put(tabName, panel);
        negotiationTabbedPane.addTab(tabName, panel);
        negotiationTabbedPane.setSelectedComponent(panel);
    }
    
    private static void handleDealAction(String sender, String receiver, boolean isAccept, JTextArea chatArea) {
        try {
            ACLMessage msg = new ACLMessage(isAccept ? ACLMessage.ACCEPT_PROPOSAL : ACLMessage.REJECT_PROPOSAL);
            msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
            msg.setContent(isAccept ? "DEAL_ACCEPTED" : "DEAL_REJECTED");
            
            Main.getContainer().getAgent(sender).putO2AObject(msg, true);
            
            chatArea.append(sender + ": " + (isAccept ? "✅ Accepted the deal!" : "❌ Rejected the deal!") + "\n");
            GUI.logInteraction(sender, receiver, 
                isAccept ? "ACCEPT" : "REJECT", 
                isAccept ? "Deal accepted" : "Deal rejected");
            
        } catch(Exception e) {
            JOptionPane.showMessageDialog(null, "Error sending response: " + e.getMessage());
        }
    }

    public static void updateChatSession(String sender, String receiver, String message) {
        String[] participants = {sender, receiver};
        Arrays.sort(participants);
        String tabName = participants[0] + " ↔ " + participants[1];
        
        JPanel panel = negotiationPanels.get(tabName);
        if (panel != null) {
            Component[] comps = panel.getComponents();
            for (Component c : comps) {
                if (c instanceof JScrollPane) {
                    JTextArea chatArea = (JTextArea) ((JScrollPane) c).getViewport().getView();
                    chatArea.append(sender + ": " + message + "\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                }
            }
        }
        
        // Update sender's agent-specific chat area
        if (agentChatAreas.containsKey(sender)) {
            agentChatAreas.get(sender).append(sender + ": " + message + "\n");
        }
    }
    
    /**
     * Creates tab for manual buyer agent
     * @param name Agent name
     * @param carType Desired car type
     * @param offerStr Initial offer
     * @param budgetStr Maximum budget
     * @param model Table model for dealer matches
     */
    public static void createManualBuyerTab(String name, String carType, String offerStr, String budgetStr, DefaultTableModel model) {
        JPanel panel = new JPanel(new BorderLayout());
        buyerCarTypes.put(name, carType);
        buyerTableModels.put(name, model);
        
        // Info Panel
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.add(new JLabel("Car Type: " + carType));
        infoPanel.add(new JLabel("Initial Offer: $" + offerStr));
        infoPanel.add(new JLabel("Max Budget: $" + budgetStr));
        
        // Matches Table - simplified with only "Send Request" button
        JTable matchesTable = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 4; // Only Action column (index 2) is editable
            }
        };
        
        JScrollPane tableScroll = new JScrollPane(matchesTable);
        
        // Proper column configuration
        TableColumnModel columnModel = matchesTable.getColumnModel();
        columnModel.getColumn(2).setCellRenderer(new ButtonRenderer("Send Request"));
        columnModel.getColumn(2).setCellEditor(new ButtonEditor("Send Request", model, name, matchesTable));
        
        // Enable sorting without breaking button functionality
        matchesTable.setAutoCreateRowSorter(true);
        matchesTable.setRowHeight(25); // Ensure button fits
        matchesTable.setFocusable(true);
        matchesTable.setShowGrid(true);
        matchesTable.setRowHeight(30);
        
        // Chat Area
        JTextArea buyerchatArea = new JTextArea();
        buyerchatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(buyerchatArea);
        chatScroll.setPreferredSize(new Dimension(400, 150));
        agentChatAreas.put(name, buyerchatArea); // Store chat area
        
        // Split pane for table and chat
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(tableScroll);
        splitPane.setBottomComponent(chatScroll);
        splitPane.setDividerLocation(0.6);
        
        model.setColumnIdentifiers(new String[]{"Dealer", "Price", "Action", "Status", "Confirm"});
        
        // With proper column index configuration:
        TableColumn actionColumn = matchesTable.getColumnModel().getColumn(2);
        actionColumn.setCellEditor(new ButtonEditor(
        	    "Send Request", model, name, matchesTable)  // Add matchesTable parameter
        	);
        
        TableColumn confirmColumn = matchesTable.getColumnModel().getColumn(4);
        confirmColumn.setCellRenderer(new ConfirmButtonRenderer());
        confirmColumn.setCellEditor(new ConfirmButtonEditor("buyer", model, matchesTable));
        
        // Ensure table properly handles sorting
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        matchesTable.setRowSorter(sorter);
        
        // Configure Confirm column
//        model.setColumnIdentifiers(new String[]{"Dealer", "Price", "Action", "Status", "Confirm"});
        matchesTable.getColumn("Confirm").setCellRenderer(new ConfirmButtonRenderer());
        matchesTable.getColumn("Confirm").setCellEditor(new ConfirmButtonEditor("buyer", model, matchesTable));
        matchesTable.setName(name);
        
        // Status & Controls
        JPanel controlPanel = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Status: Querying broker for dealers...");
        JButton refreshBtn = new JButton("Refresh Matches");
        
        refreshBtn.addActionListener(e -> {
            try {
                IManualBuyerAgent buyerAgent = (IManualBuyerAgent) Main.getContainer().getAgent(name)
                    .getO2AInterface(IManualBuyerAgent.class);
                buyerAgent.queryBrokerForDealers(); // Manual refresh triggered here
            } catch(Exception ex) {
                JOptionPane.showMessageDialog(panel, "Error refreshing: " + ex.getMessage());
            }
        });
        
        controlPanel.add(refreshBtn, BorderLayout.NORTH);
        controlPanel.add(statusLabel, BorderLayout.SOUTH);
        
        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        tabbedPane.addTab(name, panel);
        applyAgentColors(name, null, (JPanel) tabbedPane.getTabComponentAt(tabbedPane.getTabCount()-1));
        
        // Add this line to auto-select the new tab
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        
        // Final Confirm column configuration
        matchesTable.getColumn("Confirm").setCellRenderer(new ConfirmButtonRenderer());
        matchesTable.getColumn("Confirm").setCellEditor(new ConfirmButtonEditor("buyer", model, matchesTable));
    }
    
    // Retrieve buyer's car type
    public static String getBuyerCarType(String buyerName) {
        return buyerCarTypes.get(buyerName);
    }

    /**
     * Creates tab for manual dealer agent
     * @param name Agent name
     * @param carType Car model being sold
     * @param priceStr Asking price
     * @param model Table model for buyer requests
     */
    public static void createManualDealerTab(String name, String carType, String priceStr, DefaultTableModel model) {
    	dealerTableModels.put(name, model);
        JPanel panel = new JPanel(new BorderLayout());
        
        // Dealer Info
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.add(new JLabel("Car Type: " + carType));
        infoPanel.add(new JLabel("List Price: $" + priceStr));
        
        // Requests Table
        JTable requestsTable = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4 || column == 5; // Only Action column is editable
            }
        };
        
        JScrollPane tableScroll = new JScrollPane(requestsTable);
        
        // Chat Area
        JTextArea dealerchatArea = new JTextArea();
        dealerchatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(dealerchatArea);
        chatScroll.setPreferredSize(new Dimension(400, 150));
        agentChatAreas.put(name, dealerchatArea); // Store chat area
        
        // Split pane for table and chat
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(tableScroll);
        splitPane.setBottomComponent(chatScroll);
        splitPane.setDividerLocation(0.6);
        
        // Configure Action column - Fixed version
        model.setColumnIdentifiers(new String[]{"Buyer", "Car Type", "Offer", "Status", "Action", "Confirm"});
        requestsTable.getColumn("Action").setCellRenderer(new ButtonPanelRenderer());
        requestsTable.getColumn("Action").setCellEditor(new ButtonPanelEditor(name, model));
        
        // Configure Confirm column
        requestsTable.getColumn("Confirm").setCellRenderer(new ConfirmButtonRenderer());
        requestsTable.getColumn("Confirm").setCellEditor(new ConfirmButtonEditor("dealer", model, requestsTable));
        requestsTable.setName(name);
        requestsTable.setRowHeight(40);
        
        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        
        tabbedPane.addTab(name, panel);
        applyAgentColors(name, null, (JPanel) tabbedPane.getTabComponentAt(tabbedPane.getTabCount()-1));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        
        // Final Confirm column configuration
        requestsTable.getColumn("Action").setCellEditor(new ButtonPanelEditor(name, model));
        requestsTable.getColumn("Confirm").setCellEditor(new ConfirmButtonEditor("dealer", model, requestsTable));
        
        requestsTable.setRowHeight(35); // Increased row height
        requestsTable.setIntercellSpacing(new Dimension(0, 0));
        
        TableColumn actionColumn = requestsTable.getColumn("Action");
        actionColumn.setMinWidth(130);  // Wider column
        actionColumn.setMaxWidth(130);
        
        TableColumn confirmColumn = requestsTable.getColumn("Confirm");
        confirmColumn.setMinWidth(140); // Wider column
        confirmColumn.setMaxWidth(140);
    }
    
    /**
     * Creates standalone chat window
     * @param participant1 First agent
     * @param participant2 Second agent
     */
    public static void createChatWindow(String participant1, String participant2) {
    	String[] participants = {participant1, participant2};
        Arrays.sort(participants);
        String chatId = participant1 + "_" + participant2;
        if(chatWindows.containsKey(chatId)) return;

        JFrame chatFrame = new JFrame("Negotiation: " + participant1 + " ↔ " + participant2);
        chatFrame.setSize(400, 300);
        chatFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        // Chat history
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        
        // Control panel with buttons
        JPanel controlPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton acceptButton = new JButton("Accept Deal");
        JButton rejectButton = new JButton("Reject Deal");
        JButton closeBtn = new JButton("Close");
        
        // Add action listener for close button
        closeBtn.addActionListener(e -> {
            chatFrame.dispose();
            chatWindows.remove(chatId);
        });
        
        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        
        // Add action listeners for deal buttons
        acceptButton.addActionListener(e -> {
            try {
                ACLMessage acceptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                acceptMsg.addReceiver(new AID(participant2, AID.ISLOCALNAME));
                Main.getContainer().getAgent(participant1).putO2AObject(acceptMsg, true);
                chatArea.append("\n[System] You accepted the deal!\n");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Error accepting deal: " + ex.getMessage());
            }
        });
        
        rejectButton.addActionListener(e -> {
            try {
                ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                rejectMsg.addReceiver(new AID(participant2, AID.ISLOCALNAME));
                Main.getContainer().getAgent(participant1).putO2AObject(rejectMsg, true);
                chatArea.append("\n[System] You rejected the deal!\n");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Error rejecting deal: " + ex.getMessage());
            }
        });
        
        controlPanel.add(acceptButton);
        controlPanel.add(rejectButton);
        controlPanel.add(closeBtn);
        
        // Add window listener HERE
        chatFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                chatFrame.dispose();
                chatWindows.remove(chatId);
            }
            
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                chatWindows.remove(chatId);
            }
        });
        
        sendButton.addActionListener(e -> {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                try {
                    // Determine agent type
                    boolean isBuyer = participant1.startsWith("M.Buyer");
                    
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(new AID(participant2, AID.ISLOCALNAME));
                    msg.setContent(message);

                    if(isBuyer) {
                        IManualBuyerAgent buyerAgent = (IManualBuyerAgent) Main.getContainer().getAgent(participant1)
                            .getO2AInterface(IManualBuyerAgent.class);
                        buyerAgent.sendMessage(msg);
                    } else {
                        IManualDealerAgent dealerAgent = (IManualDealerAgent) Main.getContainer().getAgent(participant1)
                            .getO2AInterface(IManualDealerAgent.class);
                        dealerAgent.sendMessage(msg);
                    }

                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(participant1 + ": " + message + "\n");
                        messageField.setText("");
                    });
                    
                    // Add direct interaction logging
                    GUI.logInteraction(participant1, participant2, 
                                      "CHAT", message);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, "Send error: " + ex.getMessage());
                }
            }
        });

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        panel.add(chatScroll, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        chatFrame.add(panel);
        chatFrame.setVisible(true);
        chatWindows.put(chatId, chatFrame);
        
        // Add window listener to clean up when closed
        chatFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                chatWindows.remove(chatId);
            }
        });
        
        // Start message listener thread
        new Thread(() -> {
            try {
                Object agent = null;
                int retries = 0;
                boolean isBuyer = participant1.startsWith("M.Buyer");
                
                // Get the appropriate agent interface
                while (retries < 5 && agent == null) {
                    try {
                        jade.wrapper.AgentController ac = Main.getContainer().getAgent(participant1);
                        if (isBuyer) {
                            agent = ac.getO2AInterface(IManualBuyerAgent.class);
                        } else {
                            agent = ac.getO2AInterface(IManualDealerAgent.class);
                        }
                    } catch (Exception e) {
                        Thread.sleep(500);
                        retries++;
                    }
                }
                
                if (agent == null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(chatFrame, 
                            "Connection failed after retries", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                        chatFrame.dispose();
                    });
                    return;
                }

                // Message listening loop
                while (chatFrame.isVisible()) {
                    try {
                        ACLMessage msg = null;
                        if (isBuyer) {
                            msg = ((IManualBuyerAgent) agent).getNextMessage();
                        } else {
                            msg = ((IManualDealerAgent) agent).getNextMessage();
                        }
                        
                        if (msg != null) {
                            String content = msg.getContent();
                            String senderName = msg.getSender().getLocalName();
                            
                            SwingUtilities.invokeLater(() -> {
                                chatArea.append(senderName + ": " + content + "\n");
                            });
                        }
                        Thread.sleep(100); // Prevent CPU overuse
                    } catch (Exception e) {
                        // Log error or handle interruptions
                        if (!(e instanceof InterruptedException)) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(chatFrame,
                                    "Error receiving messages: " + e.getMessage(),
                                    "Connection Error",
                                    JOptionPane.WARNING_MESSAGE);
                            });
                        }
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> chatFrame.dispose());
            }
        }).start();
    }
    
    public static boolean hasChatWindow(String chatId) {
        return chatWindows.containsKey(chatId);
    }
    
    public static JFrame getChatWindow(String chatId) {
        return chatWindows.get(chatId);
    }
    
    private static class ButtonPanelRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new GridLayout(1, 1));
        private final JButton negotiateBtn = new JButton("Start Negotiate");
        
        public ButtonPanelRenderer() {
        	negotiateBtn.setMargin(new Insets(3, 6, 3, 6));
        	negotiateBtn.setFont(new Font("Arial", Font.BOLD, 14));
            negotiateBtn.setFocusPainted(false);
            panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            panel.add(negotiateBtn);
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
            return panel;
        }
    }

    private static class ButtonPanelEditor extends DefaultCellEditor {
        private final JPanel btnPanel;
        private final JButton startNegotiateBtn;
        private int currentRow;
        private String currentBuyer;
        private int currentOffer;
        private DefaultTableModel model;
        private String dealerName;

        public ButtonPanelEditor(String dealerName, DefaultTableModel model) {
            super(new JCheckBox());
            this.dealerName = dealerName;
            this.model = model;
            
            startNegotiateBtn = new JButton("startNegotiateBtn");
            startNegotiateBtn.setMargin(new Insets(3, 6, 3, 6));
            startNegotiateBtn.setFont(new Font("Arial", Font.BOLD, 11));
            startNegotiateBtn.setFocusPainted(false);
            
            // Configure panel
            btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            btnPanel.add(startNegotiateBtn);
            
            // Add listeners
            startNegotiateBtn.addActionListener(e -> handlestartNegotiateBtn());

            setClickCountToStart(1);
        }

        private void handlestartNegotiateBtn() {
            try {
            	// Send ACCEPT_PROPOSAL to buyer
                ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                accept.addReceiver(new AID(currentBuyer, AID.ISLOCALNAME));
                accept.setContent("Start Negotiating...");
                
                jade.wrapper.AgentController ac = Main.getContainer().getAgent(dealerName);
                IManualDealerAgent dealerAgent = (IManualDealerAgent) ac.getO2AInterface(IManualDealerAgent.class);
                dealerAgent.sendMessage(accept);       
                
                SwingUtilities.invokeLater(() -> {
                	GUI.createChatWindow(dealerName, currentBuyer);  // Dealer's window
                    GUI.createChatWindow(currentBuyer, dealerName);  // Buyer's window
                    
                	// Update status instead of removing row
                    model.setValueAt("Negotiating...", currentRow, 3); // Assuming status is column 3
                    model.fireTableCellUpdated(currentRow, 3);
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, 
                    "Error: " + ex.getMessage(), 
                    "Operation Failed", 
                    JOptionPane.ERROR_MESSAGE);
            }
            fireEditingStopped();
        }

        private void handleReject() {
            try {
                ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                reject.addReceiver(new AID(currentBuyer, AID.ISLOCALNAME));
                reject.setContent("REJECTED");
                
                jade.wrapper.AgentController ac = Main.getContainer().getAgent(dealerName);
                IManualDealerAgent dealerAgent = (IManualDealerAgent) ac.getO2AInterface(IManualDealerAgent.class);
                dealerAgent.sendMessage(reject);

                SwingUtilities.invokeLater(() -> {
                	model.setValueAt("Rejected", currentRow, 3); // Update status
                    model.setValueAt("", currentRow, 4);         // Clear action buttons
                    model.fireTableCellUpdated(currentRow, 3);
                    model.fireTableCellUpdated(currentRow, 4);;
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, 
                    "Error: " + ex.getMessage(), 
                    "Operation Failed", 
                    JOptionPane.ERROR_MESSAGE);
            }
            fireEditingStopped();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, 
                                                     boolean isSelected, int row, int column) {
            currentRow = table.convertRowIndexToModel(row);
            currentBuyer = (String) model.getValueAt(currentRow, 0);
            return btnPanel;
        }
        
        // Add this to handle immediate interaction
        @Override
        public boolean stopCellEditing() {
            fireEditingStopped();
            return super.stopCellEditing();
        }
    }

    private static void createManualNegotiationTab(String buyer, String dealer) {
        String tabName = buyer + " ↔ " + dealer;
        if(negotiationPanels.containsKey(tabName)) return;
        
        JPanel panel = new JPanel(new BorderLayout());
        
        // Chat history
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        
        // Input panel
        JPanel inputPanel = new JPanel();
        JTextField offerField = new JTextField(10);
        JButton sendBtn = new JButton("Send Offer");
        
        sendBtn.addActionListener(e -> {
            try {
                int offer = Integer.parseInt(offerField.getText());
                
                // Get agent reference
                try {
                    jade.wrapper.AgentController ac = Main.getContainer().getAgent(buyer);
                    ManualBuyerAgent buyerAgent = (ManualBuyerAgent) ac.getO2AInterface(ManualBuyerAgent.class);
                    
                    if (buyerAgent != null) {
                        buyerAgent.putO2AObject(offer);
                        chatArea.append("You offered: $" + offer + "\n");
                        offerField.setText("");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, "Agent communication error: " + ex.getMessage());
                }
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Invalid offer amount!");
            }
        });
        
        inputPanel.add(offerField);
        inputPanel.add(sendBtn);
        
        panel.add(chatScroll, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        negotiationPanels.put(tabName, panel);
        negotiationTabbedPane.addTab(tabName, panel);
        negotiationTabbedPane.setSelectedComponent(panel);
    }
    
    private static ManualBuyerAgent getAgentReference(String name) {
        try {
            jade.wrapper.AgentController ac = Main.getContainer().getAgent(name);
            return (ManualBuyerAgent) ac.getO2AInterface(ManualBuyerAgent.class);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Agent communication error: " + e.getMessage());
            return null;
        }
    }

    /* LOGGING AND MESSAGE HANDLING */
    
    /**
     * Logs a message to an agent's tab
     * @param agentName Agent name
     * @param message Message text
     */
    public static synchronized void logMessage(String agentName, String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (!agentTextPanes.containsKey(agentName)) {
                    createAgentTab(agentName);
                }
                appendStyledMessage(agentTextPanes.get(agentName), agentName, message);
            } catch (Exception e) {
                System.err.println("Error logging message: " + e.getMessage());
            }
        });
    }

    public static synchronized void logMessageFlow(String sender, String receiver, String performative, String content) {
        SwingUtilities.invokeLater(() -> {
            String interaction = String.format("%s -> %s: [%s] %s", sender, receiver, performative, content);
            
            // Store interaction for both sender and receiver
            interactionQueues.computeIfAbsent(sender, k -> new LinkedList<>()).add(interaction);
            interactionQueues.computeIfAbsent(receiver, k -> new LinkedList<>()).add(interaction);
            
            // Keep only the last 20 interactions per agent
            if (interactionQueues.get(sender).size() > 20) {
                interactionQueues.get(sender).remove();
            }
            if (interactionQueues.get(receiver).size() > 20) {
                interactionQueues.get(receiver).remove();
            }
        });
    }
    
    /**
     * Logs an interaction between agents
     * @param sender Sending agent
     * @param receiver Receiving agent
     * @param performative Message type
     * @param content Message content
     */
    public static synchronized void logInteraction(String sender, String receiver, 
            String performative, String content) {
        String interaction;
        
        // Format broker requests specifically
        
        interaction = String.format("%s -> %s: [%s] %s", 
                sender, receiver, performative, content);
        
        synchronized (agentInteractions) {
            agentInteractions.computeIfAbsent(sender, k -> Collections.synchronizedList(new ArrayList<>())).add(interaction);
            agentInteractions.computeIfAbsent(receiver, k -> Collections.synchronizedList(new ArrayList<>())).add(interaction);
        }
        
        if ("CHAT".equals(performative)) {
            // Update the negotiation chat window
            SwingUtilities.invokeLater(() -> {
                String tabName = sender + " ↔ " + receiver;
                JPanel panel = negotiationPanels.get(tabName);
                if (panel != null) {
                    JTextArea chatArea = (JTextArea) ((JScrollPane) panel.getComponent(0)).getViewport().getView();
                    chatArea.append(sender + ": " + content + "\n");
                }
            });
            
            // Also update agent-specific chat areas
            String formattedMessage = sender + ": " + content + "\n";
            SwingUtilities.invokeLater(() -> {
                if (agentChatAreas.containsKey(sender)) {
                    agentChatAreas.get(sender).append(formattedMessage);
                }
                if (agentChatAreas.containsKey(receiver)) {
                    agentChatAreas.get(receiver).append(formattedMessage);
                }
            });
        } else {
            // Existing logic for other performatives
            String displayContent = content.replaceAll("^(MANUAL_\\w+:|CHAT_START)", "").trim();
            String formattedMessage = sender + ": " + displayContent + "\n";
            
            SwingUtilities.invokeLater(() -> {
                if (agentChatAreas.containsKey(sender)) {
                    agentChatAreas.get(sender).append(formattedMessage);
                }
                if (agentChatAreas.containsKey(receiver)) {
                    agentChatAreas.get(receiver).append(formattedMessage);
                }
            });
        }
    }

    public static synchronized void logNegotiation(String buyerName, String dealerName, 
                                                 String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String negotiationId = buyerName + " ↔ " + dealerName;
                
                if (!negotiationPanels.containsKey(negotiationId)) {
                    createNegotiationPanel(buyerName, dealerName);
                }
                
                JPanel panel = negotiationPanels.get(negotiationId);
                JSplitPane splitPane = (JSplitPane)panel.getComponent(0);
                
                JTextPane targetPane = sender.equals(buyerName) 
                    ? (JTextPane)((JScrollPane)((JPanel)splitPane.getLeftComponent()).getComponent(1)).getViewport().getView()
                    : (JTextPane)((JScrollPane)((JPanel)splitPane.getRightComponent()).getComponent(1)).getViewport().getView();
                
                appendStyledMessage(targetPane, sender, message);
                negotiationTabbedPane.setSelectedComponent(panel);
            } catch (Exception e) {
                System.err.println("Error logging negotiation: " + e.getMessage());
            }
        });
    }

    private static void updateFlowDisplay() {
        if (flowTextPane != null) {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                String agentName = tabbedPane.getTitleAt(selectedIndex);
                showAgentFlows(agentName);
            }
        }
    }

    public static void showAgentFlows(String agentName) {
        if (flowTextPane == null) {
            flowTextPane = createStyledTextPane();
            JScrollPane scrollPane = new JScrollPane(flowTextPane);
            negotiationTabbedPane.addTab("Message Flows", scrollPane);
        }
        
        StyledDocument doc = flowTextPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            
            List<String> flows = messageFlows.getOrDefault(agentName, Collections.<String>emptyList());
            if (flows.isEmpty()) {
                SimpleAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setItalic(attr, true);
                StyleConstants.setForeground(attr, Color.GRAY);
                doc.insertString(0, "No message flows for " + agentName + "\n", attr);
            } else {
                for (String flow : flows) {
                    appendFlowMessage(flowTextPane, flow);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void appendFlowMessage(JTextPane pane, String message) {
        try {
            StyledDocument doc = pane.getStyledDocument();
            
            SimpleAttributeSet timeAttr = new SimpleAttributeSet();
            StyleConstants.setForeground(timeAttr, Color.GRAY);
            StyleConstants.setItalic(timeAttr, true);
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            doc.insertString(doc.getLength(), "[" + time + "] ", timeAttr);
            
            SimpleAttributeSet msgAttr = new SimpleAttributeSet();
            if (message.contains("ACCEPT")) {
                StyleConstants.setForeground(msgAttr, new Color(0, 100, 0));
            } else if (message.contains("REJECT")) {
                StyleConstants.setForeground(msgAttr, new Color(150, 0, 0));
            } else if (message.contains("PROPOSE")) {
                StyleConstants.setForeground(msgAttr, new Color(0, 0, 150));
            } else if (message.contains("REQUEST")) {
                StyleConstants.setForeground(msgAttr, new Color(128, 0, 128));
            }
            doc.insertString(doc.getLength(), message + "\n", msgAttr);
            
            pane.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createAgentTab(String agentName) {
        JTextPane textPane = createStyledTextPane();
        JScrollPane scrollPane = new JScrollPane(textPane);
        
        JPanel tabPanel = createTabHeader(agentName, () -> removeTab(agentName));
        
        agentTextPanes.put(agentName, textPane);
        tabbedPane.addTab(agentName, scrollPane);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount()-1, tabPanel);
        applyAgentColors(agentName, textPane, tabPanel);
    }

    private static void createNegotiationPanel(String buyerName, String dealerName) {
        String negotiationId = buyerName + " ↔ " + dealerName;
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        JTextPane buyerPane = createStyledTextPane();
        JTextPane dealerPane = createStyledTextPane();
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createAgentMessagePanel(buyerName, buyerPane));
        splitPane.setRightComponent(createAgentMessagePanel(dealerName, dealerPane));
        splitPane.setDividerLocation(0.5);
        
        JLabel statusBar = new JLabel(" Negotiation started...", JLabel.LEFT);
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        negotiationPanels.put(negotiationId, mainPanel);
        negotiationTabbedPane.addTab(negotiationId, mainPanel);
        
        applyAgentColors(buyerName, buyerPane, null);
        applyAgentColors(dealerName, dealerPane, null);
    }

    private static JPanel createAgentMessagePanel(String agentName, JTextPane textPane) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel header = new JLabel(agentName + " Messages:");
        header.setFont(new Font("Arial", Font.BOLD, 12));
        
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(textPane), BorderLayout.CENTER);
        
        return panel;
    }

    private static JTextPane createStyledTextPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setFont(new Font("Consolas", Font.PLAIN, 16));
        pane.setBackground(new Color(240, 240, 240));
        return pane;
    }

    private static void appendStyledMessage(JTextPane textPane, String sender, String message) {
        try {
            StyledDocument doc = textPane.getStyledDocument();
            
            SimpleAttributeSet timeAttr = new SimpleAttributeSet();
            StyleConstants.setForeground(timeAttr, Color.GRAY);
            StyleConstants.setItalic(timeAttr, true);
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            doc.insertString(doc.getLength(), "[" + time + "] ", timeAttr);
            
            if (!sender.isEmpty()) {
                SimpleAttributeSet senderAttr = new SimpleAttributeSet();
                StyleConstants.setBold(senderAttr, true);
                doc.insertString(doc.getLength(), sender + ": ", senderAttr);
            }
            
            SimpleAttributeSet msgAttr = new SimpleAttributeSet();
            applyMessageStyle(msgAttr, message);
            doc.insertString(doc.getLength(), message + "\n", msgAttr);
            
            textPane.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            System.err.println("Error appending message: " + e.getMessage());
        }
    }

    private static void applyMessageStyle(SimpleAttributeSet attr, String message) {
        if (message.contains("ACCEPT") || message.contains("✅")) {
            StyleConstants.setForeground(attr, new Color(0, 100, 0));
            StyleConstants.setBold(attr, true);
        } else if (message.contains("COUNTER") || message.contains("🔄")) {
            StyleConstants.setForeground(attr, new Color(0, 0, 150));
        } else if (message.contains("REJECT") || message.contains("❌")) {
            StyleConstants.setForeground(attr, new Color(150, 0, 0));
            StyleConstants.setBold(attr, true);
        } else if (message.contains("OFFER") || message.contains("📤")) {
            StyleConstants.setForeground(attr, new Color(30, 30, 30));
        } else if (message.contains("INFORM") || message.contains("📥")) {
            StyleConstants.setForeground(attr, new Color(70, 70, 70));
        } else {
            StyleConstants.setForeground(attr, Color.DARK_GRAY);
        }
    }

    private static JPanel createTabHeader(String title, Runnable closeAction) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        
        JButton closeButton = new JButton("×");
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setContentAreaFilled(false);
        closeButton.addActionListener(e -> closeAction.run());
        
        panel.add(label, BorderLayout.CENTER);
        panel.add(closeButton, BorderLayout.EAST);
        
        return panel;
    }

    /* HELPER METHODS */
    
    /**
     * Applies color scheme based on agent type
     * @param agentName Agent name
     * @param pane Optional text pane to color
     * @param tabPanel Optional tab panel to color
     */
    private static void applyAgentColors(String agentName, JTextPane pane, JPanel tabPanel) {
        Color bgColor = Color.WHITE;
        Color fgColor = Color.BLACK;
        
        if (agentName.startsWith("A.Buyer")) {
            bgColor = new Color(240, 248, 255); // Alice Blue
            fgColor = new Color(0, 0, 128); // Navy
        } else if (agentName.startsWith("A.Dealer")) {
            bgColor = new Color(255, 245, 238); // Seashell
            fgColor = new Color(128, 0, 0); // Maroon
        } else if (agentName.equals("BrokerAgent")) {
            bgColor = new Color(240, 255, 240); // Honeydew
            fgColor = new Color(0, 100, 0); // Dark Green
        } else if (agentName.startsWith("M.Buyer")) {
            bgColor = new Color(240, 248, 255);  // Light blue
            fgColor = Color.BLUE;
        } else if (agentName.startsWith("M.Dealer")) {
            bgColor = new Color(255, 245, 238);  // Light coral
            fgColor = new Color(178, 34, 34);
        }
        
        if (pane != null) {
            pane.setBackground(bgColor);
        }
        
        if (tabPanel != null) {
            for (Component comp : tabPanel.getComponents()) {
                if (comp instanceof JLabel) {
                    ((JLabel)comp).setForeground(fgColor);
                }
            }
        }
    }

    private static void removeTab(String agentName) {
        try {
            int index = tabbedPane.indexOfTab(agentName);
            if (index != -1) {
                tabbedPane.remove(index);
                agentTextPanes.remove(agentName);
            }
        } catch (Exception e) {
            System.err.println("Error removing tab: " + e.getMessage());
        }
    }
    
    private static void showAgentInteractions(String agentName) {
        if (sequenceDiagramPanel != null) {
            List<String> allInteractions = new ArrayList<>();
            
            synchronized (agentInteractions) {
                // Get both sent and received interactions
                List<String> sent = agentInteractions.getOrDefault(agentName, Collections.emptyList());
                List<String> received = agentInteractions.getOrDefault(agentName, Collections.emptyList());
                
                allInteractions.addAll(sent);
                allInteractions.addAll(received);
            }
            
            // Filter out duplicate interactions
            Set<String> uniqueInteractions = new LinkedHashSet<>(allInteractions);
            sequenceDiagramPanel.setInteractions(new ArrayList<>(uniqueInteractions), agentName);
            sequenceDiagramPanel.repaint();
        }
    }
    
    /* INNER CLASSES */

    /**
     * Custom panel for visualizing sequence diagrams
     */
    private static class SequenceDiagramPanel extends JPanel {
        private List<String> interactions = new ArrayList<>();
        private String currentAgent;
        
        @Override
        public Dimension getPreferredSize() {
            // Calculate required height based on number of interactions
            int numInteractions = (int) interactions.stream()
                .filter(this::shouldDisplayInteraction)
                .count();
            int height = 50 + (numInteractions * 40) + 100; // StartY + interactions*spacing + margin
            
            // Width based on 3 participants (Buyer, Broker, Dealer)
            int width = 900; 
            
            return new Dimension(width, height);
        }
        
        /**
         * Sets interactions to display
         * @param interactions List of interaction strings
         * @param agent Current focused agent
         */
        public void setInteractions(List<String> interactions, String agent) {
            this.interactions = interactions;
            this.currentAgent = agent;
            revalidate();
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw participant lanes
            int laneWidth = 400;
            int startX = 50;
            int startY = 50;
            
            // Draw participant lines
            String[] participants = {"Buyer", "Broker", "Dealer"};
            Map<String, Integer> xPositions = new HashMap<>();
            for (int i = 0; i < participants.length; i++) {
                int x = startX + i * laneWidth;
                xPositions.put(participants[i], x);
                g2d.drawLine(x, startY, x, getHeight() - 50);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.drawString(participants[i], x - 20, startY - 10);
            }
            
            // Draw interactions with colored arrows
            int y = startY + 30;
            for (String interaction : interactions) {
                if (shouldDisplayInteraction(interaction)) {
                    String[] parts = interaction.split(" -> ");
                    if (parts.length == 2) {
                        String from = parts[0].trim();
                        String to = parts[1].split(":")[0].trim();
                        
                        // Filter out Sniffer
                        if (from.contains("Sniffer") || to.contains("Sniffer")) continue;
                        
                        String fromType = resolveAgentType(from);
                        String toType = resolveAgentType(to);
                        
                        if (xPositions.containsKey(fromType) && xPositions.containsKey(toType)) {
                            // Get performative and set color
                            String performative = extractPerformative(interaction);
                            g2d.setColor(getPerformativeColor(performative));
                            
                            // Set line style (dashed for failure, solid otherwise)
                            if ("FAILURE".equals(performative)) {
                                float[] dashPattern = {5, 5}; // Dash pattern
                                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, 
                                    BasicStroke.JOIN_MITER, 10, dashPattern, 0));
                            } else {
                                g2d.setStroke(new BasicStroke(1)); // Solid line
                            }
                            
                            int fromX = xPositions.get(fromType);
                            int toX = xPositions.get(toType);
                            
                            // Draw the interaction line
                            drawInteraction(g2d, fromX, toX, y, 
                                interaction.substring(interaction.indexOf(":") + 1).trim());
                            y += 40;
                        }
                    }
                }
            }
        }
        
        private String resolveAgentType(String agentName) {
            // Handle both manual and automated agents
            if (agentName.startsWith("M.Buyer") || agentName.startsWith("A.Buyer")) {
                return "Buyer";
            }
            if (agentName.startsWith("M.Dealer") || agentName.startsWith("A.Dealer")) {
                return "Dealer";
            }
            if (agentName.equals("BrokerAgent") || agentName.equals("Broker")) {
                return "Broker";
            }
            return agentName;
        }
        
        private boolean shouldDisplayInteraction(String interaction) {
            // Show interactions involving current agent
            return interaction.contains(currentAgent) && 
                       !interaction.contains("Sniffer");
        }
        
        private String extractPerformative(String interaction) {
            int start = interaction.indexOf("[") + 1;
            int end = interaction.indexOf("]");
            return (start > 0 && end > start) ? 
                   interaction.substring(start, end) : 
                   "UNKNOWN";
        }
        
        private Color getPerformativeColor(String performative) {
            switch (performative.toUpperCase()) {
                case "INFORM":
                    return new Color(34, 139, 34); // ForestGreen
                case "REQUEST":
                    return new Color(0, 0, 205); // MediumBlue
                case "PROPOSE":
                    return new Color(148, 0, 211); // DarkViolet
                case "ACCEPT":
                    return new Color(0, 100, 0); // DarkGreen
                case "REJECT":
                    return new Color(255, 165, 0); // Orange
                case "FAILURE":
                    return new Color(220, 20, 60); // Crimson
                case "CONFIRM":
                    return new Color(0, 139, 139); // DarkCyan
                default:
                    return Color.DARK_GRAY;
            }
        }
        
        private void drawInteraction(Graphics2D g2d, int fromX, int toX, int y, String message) {
            // Draw the line
            g2d.drawLine(fromX, y, toX, y);
            
            // Draw arrowhead at the end (toX)
            int arrowSize = 5;
            if (fromX < toX) {
                // Right arrow
                g2d.drawLine(toX, y, toX - arrowSize, y - arrowSize);
                g2d.drawLine(toX, y, toX - arrowSize, y + arrowSize);
            } else {
                // Left arrow
                g2d.drawLine(toX, y, toX + arrowSize, y - arrowSize);
                g2d.drawLine(toX, y, toX + arrowSize, y + arrowSize);
            }
            
            // Draw the message text above the line
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            int textWidth = g2d.getFontMetrics().stringWidth(message);
            int textX = (fromX + toX) / 2 - textWidth / 2;
            g2d.drawString(message, textX, y - 10);
        }
    }
    
    /**
     * Custom button renderer for table cells
     */
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer(String text) {
            super(text);
            setOpaque(true);
            setBorderPainted(true);
            setContentAreaFilled(true);
            setFont(new Font("Arial", Font.PLAIN, 14));
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
                
            // Visual feedback for selection
            if (isSelected) {
               setForeground(table.getSelectionForeground());
               setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(UIManager.getColor("Button.background"));
            }
            return this;
        }
    }
    
    public static void recordAcceptance(String acceptor, String target, boolean accepted) {
        acceptances
            .computeIfAbsent(acceptor, k -> new ConcurrentHashMap<>())
            .put(target, accepted);
    }

    public static boolean getBuyerAcceptance(String buyer, String dealer) {
        return acceptances.getOrDefault(buyer, new ConcurrentHashMap<>())
                         .getOrDefault(dealer, false);
    }

    public static boolean getDealerAcceptance(String dealer, String buyer) {
        return acceptances.getOrDefault(dealer, new ConcurrentHashMap<>())
                         .getOrDefault(buyer, false);
    }

    /**
     * Custom button editor for table cells
     */
    private static class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button;
        private String dealerName;
        private int modelRow;
        private final DefaultTableModel model;
        private final String buyerName;
        private final JTable table;

        public ButtonEditor(String buttonText, DefaultTableModel model, String buyerName, JTable table) {
            this.model = model;
            this.buyerName = buyerName;
            this.table = table;
            button = new JButton(buttonText);
            
            button.addActionListener(e -> {
            	int row = table.convertRowIndexToModel(table.getEditingRow());
//                String dealerName = (String) model.getValueAt(row, 0);
            	// Use stored modelRow instead of getting from table
                String dealerName = (String) model.getValueAt(row, 0);
                
                try {    	                   
                    IManualBuyerAgent buyerAgent = (IManualBuyerAgent) Main.getContainer()
                        .getAgent(buyerName).getO2AInterface(IManualBuyerAgent.class);
                    buyerAgent.sendRequestToDealer(dealerName);
                    
                    SwingUtilities.invokeLater(() -> {
                        model.setValueAt("Pending", row, 3);
                        model.fireTableCellUpdated(row, 3);
                    });
                } catch(Exception ex) {
                	JOptionPane.showMessageDialog(null, 
                            "Error sending request: " + ex.getMessage(),
                            "Operation Failed", 
                            JOptionPane.ERROR_MESSAGE);
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        	this.modelRow = table.convertRowIndexToModel(row);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }
    }
    
    private static class ConfirmButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton acceptBtn = new JButton("✅ Accept");
        private final JButton rejectBtn = new JButton("❌ Reject");
        private String currentAgent;
        private String counterpart;
        private int currentRow;
        private DefaultTableModel model;
        private boolean isBuyer;
        
        public ConfirmButtonRenderer() {
        	setLayout(new GridLayout(1, 2, 5, 0)); // 5px horizontal gap
            acceptBtn.setMargin(new Insets(3, 5, 3, 5));
            rejectBtn.setMargin(new Insets(3, 5, 3, 5));
            acceptBtn.setFont(new Font("Arial", Font.BOLD, 14));
            rejectBtn.setFont(new Font("Arial", Font.BOLD, 14));
            acceptBtn.setForeground(new Color(0, 100, 0));
            rejectBtn.setForeground(new Color(150, 0, 0));
            add(acceptBtn);
            add(rejectBtn);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String status = table.getValueAt(row, 3).toString();
            setVisible(status.equals("Negotiating..."));
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }
    
    // Add these methods to the GUI class
    public static synchronized void recordBuyerAcceptance(String buyer, String dealer) {
        buyerAcceptances
            .computeIfAbsent(buyer, k -> new ConcurrentHashMap<>())
            .put(dealer, true);
    }

    public static synchronized void recordDealerAcceptance(String dealer, String buyer) {
        dealerAcceptances
            .computeIfAbsent(dealer, k -> new ConcurrentHashMap<>())
            .put(buyer, true);
    }

    public static synchronized boolean checkMutualAcceptance(String buyer, String dealer) {
        return buyerAcceptances.getOrDefault(buyer, Collections.emptyMap())
                               .getOrDefault(dealer, false)
            && dealerAcceptances.getOrDefault(dealer, Collections.emptyMap())
                                .getOrDefault(buyer, false);
    }

    private static class ConfirmButtonEditor extends AbstractCellEditor implements TableCellEditor {
    	private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        private final JButton acceptBtn = new JButton("✅ Accept");
        private final JButton rejectBtn = new JButton("❌ Reject");
        private final DefaultTableModel model;
        private final boolean isBuyer;
        private final JTable table;
        private String currentAgent;
        private String counterpart;
        private int currentRow;

        public ConfirmButtonEditor(String agentType, DefaultTableModel model, JTable table) {
            this.model = model;
            
            acceptBtn.setMargin(new Insets(3, 5, 3, 5));
            rejectBtn.setMargin(new Insets(3, 5, 3, 5));
            acceptBtn.setFont(new Font("Arial", Font.BOLD, 11));
            rejectBtn.setFont(new Font("Arial", Font.BOLD, 11));
            acceptBtn.setForeground(new Color(0, 100, 0));
            rejectBtn.setForeground(new Color(150, 0, 0));
            
            this.isBuyer = agentType.equals("buyer");
            this.table = table;
            
            acceptBtn.addActionListener(e -> handleAccept());
            rejectBtn.addActionListener(e -> handleReject());
            
            panel.add(acceptBtn);
            panel.add(rejectBtn);
        }

        private void handleAccept() {
            try {
            	// Record acceptance
                if(isBuyer) {
                    GUI.recordBuyerAcceptance(currentAgent, counterpart);
                } else {
                    GUI.recordDealerAcceptance(currentAgent, counterpart);
                }

                // Check mutual acceptance
                if(GUI.checkMutualAcceptance(
                    isBuyer ? currentAgent : counterpart, 
                    isBuyer ? counterpart : currentAgent)) {
                    
                    notifyBroker();
                    
                    SwingUtilities.invokeLater(() -> {
                        // Update current agent's table
                        model.setValueAt("Confirmed", currentRow, 3);
                        
                        // Update counterpart's table
                        if (isBuyer) {
                            // Current agent is buyer, update dealer's table
                            DefaultTableModel dealerModel = GUI.dealerTableModels.get(counterpart);
                            if (dealerModel != null) {
                                for (int i = 0; i < dealerModel.getRowCount(); i++) {
                                    if (dealerModel.getValueAt(i, 0).equals(currentAgent)) {
                                        dealerModel.setValueAt("Confirmed", i, 3);
                                        dealerModel.fireTableCellUpdated(i, 3);
                                        break;
                                    }
                                }
                            }
                        } else {
                            // Current agent is dealer, update buyer's table
                            DefaultTableModel buyerModel = GUI.buyerTableModels.get(counterpart);
                            if (buyerModel != null) {
                                for (int i = 0; i < buyerModel.getRowCount(); i++) {
                                    if (buyerModel.getValueAt(i, 0).equals(currentAgent)) {
                                        buyerModel.setValueAt("Confirmed", i, 3);
                                        buyerModel.fireTableCellUpdated(i, 3);
                                        break;
                                    }
                                }
                            }
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        model.setValueAt("Awaiting Counterpart", currentRow, 3);
                    });
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                    "Acceptance failed: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            } finally {
                fireEditingStopped();
            }
        }

        private void updateCounterpartTable(String counterpart) {
            if (isBuyer) {
                DefaultTableModel dealerModel = GUI.dealerTableModels.get(counterpart);
                if (dealerModel != null) {
                    for (int i = 0; i < dealerModel.getRowCount(); i++) {
                        if (currentAgent.equals(dealerModel.getValueAt(i, 0))) {
                            dealerModel.setValueAt("Confirmed", i, 3);
                            break;
                        }
                    }
                }
            }
        }

        private void handleReject() {
            try {
            	// 1. Send REJECT to counterpart
                ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                reject.addReceiver(new AID(counterpart, AID.ISLOCALNAME));
                reject.setContent("DEAL_REJECTED");
                
                // 2. Send notification to broker via O2A - REMOVE THE LOG HERE
                Object[] brokerCmd = new Object[]{"NOTIFY_BROKER_REJECT", counterpart};
                Main.getContainer().getAgent(currentAgent).putO2AObject(brokerCmd, true);

                // 3. GUI updates - LOG ONLY ONCE HERE
                model.setValueAt("Rejected", currentRow, 3);
                GUI.logInteraction(currentAgent, counterpart, "REJECT", "Deal rejected");
                
                // 4. Remove the broker notification log from the agent's behavior
                fireEditingStopped();

                // 5. Close chat window
                String chatId = currentAgent + "_" + counterpart;
                if (GUI.hasChatWindow(chatId)) {
                    GUI.getChatWindow(chatId).dispose();
                }

            } catch(Exception ex) {
                JOptionPane.showMessageDialog(null, 
                    "Rejection failed: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }

        private void notifyBroker() {
            try {
            	ACLMessage brokerMsg = new ACLMessage(ACLMessage.INFORM);
                brokerMsg.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                
                // Get deal details
                String carType = "";
                int price = 0;
                
                if (isBuyer) {
                    carType = GUI.getBuyerCarType(currentAgent);
                    for (int i=0; i<model.getRowCount(); i++) {
                        if (model.getValueAt(i, 0).equals(counterpart)) {
                            price = (Integer) model.getValueAt(i, 1);
                            break;
                        }
                    }
                } else {
                    for (int i=0; i<model.getRowCount(); i++) {
                        if (model.getValueAt(i, 0).equals(counterpart)) {
                            carType = (String) model.getValueAt(i, 1);
                            price = (Integer) model.getValueAt(i, 2);
                            break;
                        }
                    }
                }
                
                brokerMsg.setContent("DEAL_CONFIRMED," + 
                        (isBuyer ? currentAgent : counterpart) + "," + 
                        (isBuyer ? counterpart : currentAgent) + "," + 
                        carType + "," + price);
                
                // Send through the appropriate agent
                if(isBuyer) {
                    IManualBuyerAgent buyerAgent = (IManualBuyerAgent) Main.getContainer()
                        .getAgent(currentAgent).getO2AInterface(IManualBuyerAgent.class);
                    buyerAgent.sendMessage(brokerMsg);
                } else {
                    IManualDealerAgent dealerAgent = (IManualDealerAgent) Main.getContainer()
                        .getAgent(currentAgent).getO2AInterface(IManualDealerAgent.class);
                    dealerAgent.sendMessage(brokerMsg);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Broker notification failed: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
            currentRow = table.convertRowIndexToModel(row);
            currentAgent = table.getName();
            counterpart = (String) model.getValueAt(currentRow, 0);
            
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return null; // No specific value needed for button editors
        }
    }
    
    public static void createBrokerTab() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        
        autoCommissionLabel = new JLabel("Automated Commission: RM 0");
        manualCommissionLabel = new JLabel("Manual Commission: RM 0");
        totalCommissionLabel = new JLabel("Total Earned: RM 0");
        
        panel.add(autoCommissionLabel);
        panel.add(manualCommissionLabel);
        panel.add(totalCommissionLabel);
        
        tabbedPane.addTab("Broker Commission", panel);
    }

    /**
     * Updates commission display
     * @param auto Automated commission amount
     * @param manual Manual commission amount
     * @param total Total commission amount
     */
    public static void updateCommissionDisplay(int auto, int manual, int total) {
        SwingUtilities.invokeLater(() -> {
            autoCommissionLabel.setText("Automated Commission: RM " + auto);
            manualCommissionLabel.setText("Manual Commission: RM " + manual);
            totalCommissionLabel.setText("Total Earned Commission: RM " + total);
        });
    }
}