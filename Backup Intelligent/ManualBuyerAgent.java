package IntelligentProject;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.LinkedList;
import java.util.Queue;
import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.FlowLayout;

public class ManualBuyerAgent extends BuyerAgent implements IManualBuyerAgent {
    private DefaultTableModel dealerMatchesModel;
    private JLabel statusLabel;
    private final Queue<Integer> offerQueue = new LinkedList<>();
    private String carType;
    private int reservePrice;
    private int initialOffer;
    private final Queue<ACLMessage> messageQueue = new LinkedList<>();
    private final Queue<ACLMessage> outgoingMessages = new LinkedList<>();
    private Map<String, Boolean> dealStatus = new ConcurrentHashMap<>();
    private Map<String, Boolean> buyerAcceptances = new ConcurrentHashMap<>();
    
    public void putO2AObject(int offer) {
        synchronized(offerQueue) {
            offerQueue.add(offer);
        }
    }
    
    @Override
    public void sendMessage(ACLMessage msg) {
        super.send(msg);
    }
    
    @Override
    public ACLMessage getNextMessage() {
        synchronized(messageQueue) {
            return messageQueue.poll();
        }
    }
    
    private class BuyerButtonEditor extends DefaultCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout());
        private final JButton acceptBtn = new JButton("Accept");
        private final JButton rejectBtn = new JButton("Reject");
        private String targetAgent;
        private int tableRow;

        public BuyerButtonEditor() {
            super(new JCheckBox());
            
            acceptBtn.addActionListener(e -> {
                try {
                    buyerAcceptances.put(targetAgent, true);
                    checkMutualAcceptance(targetAgent);
                    
                    ACLMessage accept = new ACLMessage(ACLMessage.CONFIRM);
                    accept.addReceiver(new AID(targetAgent, AID.ISLOCALNAME));
                    accept.setContent("BUYER_ACCEPT");
                    send(accept);
                    
                    dealerMatchesModel.setValueAt("Accepted", tableRow, 3);
                    fireEditingStopped();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            rejectBtn.addActionListener(e -> {
                try {
                    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    reject.addReceiver(new AID(targetAgent, AID.ISLOCALNAME));
                    reject.setContent("BUYER_REJECT");
                    send(reject);
                    
                    dealerMatchesModel.setValueAt("Rejected", tableRow, 3);
                    fireEditingStopped();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            panel.add(acceptBtn);
            panel.add(rejectBtn);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            targetAgent = (String) table.getValueAt(row, 0);
            tableRow = table.convertRowIndexToModel(row);
            return panel;
        }
    }

    private void checkMutualAcceptance(String dealerName) {
        if (buyerAcceptances.getOrDefault(dealerName, false) &&
            GUI.getDealerAcceptance(dealerName, getLocalName())) {
            
            ACLMessage brokerMsg = new ACLMessage(ACLMessage.INFORM);
            brokerMsg.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
            brokerMsg.setContent(getLocalName() + "," + dealerName + "," + 
                               carType + "," + reservePrice);
            send(brokerMsg);
            
            GUI.logInteraction(getLocalName(), "BrokerAgent", "INFORM", 
                "Deal finalized with " + dealerName);
        }
    }
    
    private class ChatHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.not(MessageTemplate.MatchSender(new AID("BrokerAgent", AID.ISLOCALNAME))
            ));
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();
                
                // Add to message queue for chat window
                synchronized (messageQueue) {
                    messageQueue.add(msg);
                }
                
                // Update receiver's chat area via GUI
                SwingUtilities.invokeLater(() -> {
                    GUI.updateChatSession(sender, myAgent.getLocalName(), content);
                });
            } else {
                block();
            }
        }
    }
    
    private class ManualInteractionLogger extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String performative = ACLMessage.getPerformative(msg.getPerformative());
                GUI.logInteraction(msg.getSender().getLocalName(), 
                                 getLocalName(),
                                 performative,
                                 msg.getContent());
            }
            block();
        }
    }
    
    

    @Override
    protected void setup() {
        try {
        	setEnabledO2ACommunication(true, 100);
            Object[] args = getArguments();
            if (args == null || args.length != 3) {
                GUI.logMessage(getLocalName(), "❌ Invalid initialization parameters");
                doDelete();
                return;
            }
            
            // Register with broker
            ACLMessage registration = new ACLMessage(ACLMessage.INFORM);
            registration.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
            registration.setContent("REGISTER");
            send(registration);

            // Parse and validate parameters
            carType = args[0].toString();
            initialOffer = Integer.parseInt(args[1].toString());
            reservePrice = Integer.parseInt(args[2].toString());

            if (initialOffer <= 0 || reservePrice <= 0 || initialOffer >= reservePrice) {
                GUI.logMessage(getLocalName(), "❌ Invalid offer/budget values");
            }

            // Initialize table model
            dealerMatchesModel = new DefaultTableModel(
            	    new Object[][]{}, 
            	    new String[]{"Dealer", "Price", "Action", "Status", "Confirm"} // Add all 4 columns
            	) {
            	    @Override
            	    public boolean isCellEditable(int row, int column) {
            	        return column == 2 || column == 4; // Only Action column is editable
            	    }
            	    
            	    @Override
            	    public Class<?> getColumnClass(int columnIndex) {
            	        return columnIndex == 3 ? String.class : Object.class;
            	    }
            	};

            // Initialize GUI components
            SwingUtilities.invokeLater(() -> {
                try {
                    GUI.createManualBuyerTab(
                        getLocalName(),
                        carType,
                        String.valueOf(initialOffer),
                        String.valueOf(reservePrice),
                        dealerMatchesModel
                    );
                    
                    
                } catch (Exception e) {
                    GUI.logMessage(getLocalName(), "⚠️ GUI initialization failed: " + e.getMessage());
                }
            });
            
            addBehaviour(new ChatHandler());

            // Automatically query broker on startup
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    queryBrokerForDealers();
                }
            });
            
            // Add response listener
            addBehaviour(new CyclicBehaviour(this) {
                public void action() {
                    MessageTemplate mt = MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                        MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                    );
                    ACLMessage msg = receive(mt);
                    if (msg != null) {
                        handleDealerResponse(msg);
                    } else {
                        block();
                    }
                }
            });
            
            addBehaviour(new DealResponseHandler()); 
            
            // Accept button
            addBehaviour(new CyclicBehaviour(this) {
                public void action() {
                    Object obj = getO2AObject();
                    if (obj != null && obj instanceof Object[]) {
                        Object[] cmd = (Object[]) obj;
                        if ("NOTIFY_BROKER".equals(cmd[0])) {
                            String dealerName = (String) cmd[1];
                            for (int i = 0; i < dealerMatchesModel.getRowCount(); i++) {
                                if (dealerName.equals(dealerMatchesModel.getValueAt(i, 0))) {
                                    String price = dealerMatchesModel.getValueAt(i, 1).toString();
                                    ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                                    inform.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                                    inform.setContent("DEAL_CONFIRMED," + getLocalName() + "," + dealerName + "," + carType + "," + price);
                                    send(inform);
                                    break;
                                }
                            }
                        }
                    } else {
                        block();
                    }
                }
            });
            
            // Reject button
            addBehaviour(new CyclicBehaviour(this) {
                public void action() {
                    Object obj = getO2AObject();
                    if (obj instanceof Object[]) {
                        Object[] cmd = (Object[]) obj;
                        if ("NOTIFY_BROKER_REJECT".equals(cmd[0])) {
                            String dealer = (String) cmd[1];
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                            msg.setContent("DEAL_REJECTED," + getLocalName() + "," + dealer + "," + carType);
                            send(msg);
                        }
                    }
                    block();
                }
            });
            
            addBehaviour(new CyclicBehaviour(this) {
                public void action() {
                	ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    if (msg != null) {
                        synchronized(messageQueue) {
                            messageQueue.add(msg);
                        }
                        SwingUtilities.invokeLater(() -> {
                            GUI.updateChatSession(
                                msg.getSender().getLocalName(),
                                getLocalName(),
                                msg.getContent()
                            );
                        });
                    }
                    block(1000);
                }
            });

            GUI.logMessage(getLocalName(), "🛒 Ready for " + carType + " (Offer: $" + initialOffer + ", Max: $" + reservePrice + ")");

        } catch (NumberFormatException e) {
            GUI.logMessage(getLocalName(), "❌ Invalid number format in parameters");
            doDelete();
        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Critical initialization error: " + e.getMessage());
            doDelete();
        }
    }
    
    private class DealResponseHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
            );
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String dealerName = msg.getSender().getLocalName();
                String response = msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL 
                    ? "accepted" : "rejected";
                
                GUI.logMessage(getLocalName(), "Deal " + response + " by " + dealerName);
                
                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    // Record dealer's acceptance
                    GUI.recordAcceptance(dealerName, getLocalName(), true);
                    
                    // Check if buyer has accepted this dealer
                    if (GUI.getBuyerAcceptance(getLocalName(), dealerName)) {
                        // Notify broker
                        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                        inform.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                        
                        // Find the dealer's price from the model
                        for (int i = 0; i < dealerMatchesModel.getRowCount(); i++) {
                            if (dealerMatchesModel.getValueAt(i, 0).equals(dealerName)) {
                                String price = dealerMatchesModel.getValueAt(i, 1).toString();
                                inform.setContent(getLocalName() + "," + dealerName + "," + price);
                                break;
                            }
                        }
                        
                        send(inform);
                        GUI.logInteraction(getLocalName(), "BrokerAgent", "INFORM", 
                            "Deal confirmed with " + dealerName);
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    updateDealerStatus(dealerName, 
                        msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL 
                            ? "Accepted" 
                            : "Rejected");
                });
            }
            else {
                block();
            }
        }
        
        private void updateDealerStatus(String dealerName, String status) {
            for(int i=0; i<dealerMatchesModel.getRowCount(); i++) {
                if(dealerMatchesModel.getValueAt(i, 0).equals(dealerName)) {
                    dealerMatchesModel.setValueAt(status, i, 3);
                    break;
                }
            }
        }
    }

    public void updateDealStatus(String participant, boolean accepted) {
        dealStatus.put(participant, accepted);
    }

    public boolean getDealStatus(String participant) {
        return dealStatus.getOrDefault(participant, false);
    }
    
    // Add O2A interface provider
    @Override
    public Object getO2AInterface(Class c) {
        if (c.equals(IManualBuyerAgent.class)) {
            return this;
        }
        return null;
    }
    
    public void sendRequestToDealer(String dealerName) {
        try {
            ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
            propose.addReceiver(new AID(dealerName, AID.ISLOCALNAME));
            propose.setContent("MANUAL_PROPOSE:" +carType + "," + initialOffer);
            send(propose);
            
            // Add explicit interaction logging
            GUI.logInteraction(getLocalName(), dealerName, "PROPOSE", 
                              carType + "," + initialOffer);
            updateStatus("Request sent to " + dealerName);
        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Request failed: " + e.getMessage());
        }
    }
    
    private void handleDealerResponse(ACLMessage msg) {
        String dealerName = msg.getSender().getLocalName();
        String status;
        switch (msg.getPerformative()) {
            case ACLMessage.ACCEPT_PROPOSAL:
                status = "Negotiating...";
                break;
            case ACLMessage.REJECT_PROPOSAL:
                status = "Rejected";
                break;
            default:
                status = "Unknown";
        }

        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < dealerMatchesModel.getRowCount(); i++) {
                if (dealerMatchesModel.getValueAt(i, 0).equals(dealerName)) {
                    dealerMatchesModel.setValueAt(status, i, 3);
                    // Force full refresh of the row
                    dealerMatchesModel.fireTableRowsUpdated(i, i);
                    dealerMatchesModel.setValueAt("", i, 4);
                    break;
                }
            }
        });
    }

    @Override
    public void queryBrokerForDealers() {
        try {
            ACLMessage query = new ACLMessage(ACLMessage.QUERY_IF);
            query.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
            query.setContent(carType.trim().toLowerCase() + "," + reservePrice);
            send(query);
            
            ACLMessage msg = new ACLMessage(ACLMessage.QUERY_REF);
            msg.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
            msg.setContent("REQUEST_DEALERS:" + carType);
            send(msg);
            
            // Log the interaction
            GUI.logInteraction(getLocalName(), "BrokerAgent", "REQUEST", "Dealer query for " + carType);

            ACLMessage response = blockingReceive(
                MessageTemplate.and(
                    MessageTemplate.MatchSender(new AID("BrokerAgent", AID.ISLOCALNAME)),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                ), 
                3000 // 3-second timeout
            );

            if (response != null) {
                processDealerMatches(response.getContent());
                GUI.logInteraction("BrokerAgent", getLocalName(), "INFORM", response.getContent());
            } else {
                updateStatus("No broker response - Retrying...");
            }
        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Broker query failed: " + e.getMessage());
        }
    }

    private void processDealerMatches(String content) {
        SwingUtilities.invokeLater(() -> {
            try {
                dealerMatchesModel.setRowCount(0);
                
                if (content == null || content.isEmpty()) {
                    updateStatus("No matching dealers found");
                    return;
                }

                String[] dealers = content.split(";");
                for (String dealer : dealers) {
                    String[] parts = dealer.split(",");
                    if (parts.length == 2) {
                        try {
                            String dealerName = parts[0];
                            int price = Integer.parseInt(parts[1]);
                            if (price <= reservePrice) {
                            	// Add all 4 columns with initial status
                                dealerMatchesModel.addRow(new Object[]{
                                    dealerName, 
                                    price, 
                                    "Send Request",  // Action column
                                    ""        // Initial status
                                });
                            }
                        } catch (NumberFormatException e) {
                            GUI.logMessage(getLocalName(), "⚠️ Invalid price format: " + parts[1]);
                        }
                    }
                }
                
                if (dealerMatchesModel.getRowCount() > 0) {
                    updateStatus("Found " + dealerMatchesModel.getRowCount() + " matching dealers");
                } else {
                    updateStatus("No dealers within budget");
                }
            } catch (Exception e) {
                GUI.logMessage(getLocalName(), "⚠️ Dealer match processing failed: " + e.getMessage());
            }
        });
    }

    @Override
    public void startNegotiation(String dealerName) {
        try {
            ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
            propose.addReceiver(new AID(dealerName, AID.ISLOCALNAME));
            propose.setContent(carType + "," + initialOffer);
            send(propose);
            
            GUI.logInteraction(getLocalName(), dealerName, "PROPOSE", carType + "," + initialOffer);
            updateStatus("Negotiating with " + dealerName);
        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Negotiation start failed: " + e.getMessage());
        }
    }
    
    private void sendAcceptMessage(String counterpart) {
        try {
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.addReceiver(new AID(counterpart, AID.ISLOCALNAME));
            accept.setContent("DEAL_ACCEPTED");
            send(accept);
            
            // Debug log
            System.out.println("[DEBUG] Sent ACCEPT_PROPOSAL to " + counterpart);
        } catch (Exception e) {
            // Error handling
        }
    }

    @Override
    public DefaultTableModel getDealerMatchesModel() {
        return dealerMatchesModel;
    }

    @Override
    public void setStatusLabel(JLabel label) {
        this.statusLabel = label;
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + message));
        }
    }

    @Override
    protected void takeDown() {
        GUI.logMessage(getLocalName(), "🔚 Terminating buyer agent");
        super.takeDown();
    }
    
    @Override
    protected void beforeMove() {
        GUI.logMessage(getLocalName(), "🔄 Agent moving containers");
        super.beforeMove();
    }

    @Override
    protected void afterMove() {
        GUI.logMessage(getLocalName(), "🔄 Agent moved successfully");
        super.afterMove();
    }
}