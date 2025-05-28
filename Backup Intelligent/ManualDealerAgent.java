package IntelligentProject;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;
import javax.swing.*;
import java.util.*;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Collections;
import javax.swing.table.DefaultTableModel;
import java.util.concurrent.ConcurrentHashMap;
import jade.core.behaviours.TickerBehaviour;
import jade.core.Profile;
import jade.util.leap.Properties;
import java.awt.FlowLayout;
import java.awt.Component;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

/**
 * Manual dealer agent with GUI interaction capabilities
 * Implements negotiation protocols for human-supervised car sales
 */
public class ManualDealerAgent extends Agent implements IManualDealerAgent {
    // Agent state
    private String carType;
    private int listPrice;
    private final Queue<ACLMessage> messageQueue = new LinkedList<>();
    private DefaultTableModel requestModel;
    private final Map<String, ACLMessage> pendingMessages = new ConcurrentHashMap<>();
    private Map<String, Boolean> dealerAcceptances = new ConcurrentHashMap<>();
    
    // Interface implementation - Basic message sending
    @Override
    public void sendMessage(ACLMessage msg) {
        super.send(msg);  // Call the Agent's native send() method
    }
    
    /**
     * Custom cell editor for dealer response buttons (Accept/Reject)
     * Handles GUI interaction events for manual negotiations
     */
    private class DealerButtonEditor extends DefaultCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout());
        private final JButton acceptBtn = new JButton("Accept");
        private final JButton rejectBtn = new JButton("Reject");
        private String targetAgent;
        private int tableRow;

        public DealerButtonEditor() {
            super(new JCheckBox());
            
            // Accept button action
            acceptBtn.addActionListener(e -> {
                try {
                    dealerAcceptances.put(targetAgent, true);
                    checkMutualAcceptance(targetAgent);
                    
                    ACLMessage accept = new ACLMessage(ACLMessage.CONFIRM);
                    accept.addReceiver(new AID(targetAgent, AID.ISLOCALNAME));
                    accept.setContent("DEALER_ACCEPT");
                    send(accept);
                    
                    requestModel.removeRow(tableRow);
                    fireEditingStopped();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            rejectBtn.addActionListener(e -> {
                try {
                    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    reject.addReceiver(new AID(targetAgent, AID.ISLOCALNAME));
                    reject.setContent("DEALER_REJECT");
                    send(reject);
                    
                    requestModel.removeRow(tableRow);
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

    /**
     * Verifies mutual acceptance between dealer and buyer
     * @param buyerName The buyer agent name
     */
    private void checkMutualAcceptance(String buyerName) {
        if (dealerAcceptances.getOrDefault(buyerName, false) &&
            GUI.getBuyerAcceptance(buyerName, getLocalName())) {
            
            ACLMessage brokerMsg = new ACLMessage(ACLMessage.INFORM);
            brokerMsg.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
            brokerMsg.setContent(buyerName + "," + getLocalName() + "," + 
                               carType + "," + listPrice);
            send(brokerMsg);
            
            GUI.logInteraction(getLocalName(), "BrokerAgent", "INFORM", 
                "Deal finalized with " + buyerName);
        }
    }
    
    /**
     * Custom renderer for action buttons in request table
     */
    private class ButtonPanelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JButton button = new JButton("Respond");
            return button;
        }
    }
    
    /**
     * Agent initialization routine
     */
    @Override
    public void setup() {
        try {        	
        	setEnabledO2ACommunication(true, 100);
        	
            // Initialize agent parameters
            Object[] args = getArguments();
            if (args != null && args.length == 2) {
                carType = args[0].toString();
                listPrice = Integer.parseInt(args[1].toString());
            } else {
                GUI.logMessage(getLocalName(), "❌ Invalid initialization parameters");
                doDelete();
                return;
            }

            // Initialize GUI components
            SwingUtilities.invokeLater(() -> {
                requestModel = new DefaultTableModel(
                    new Object[][]{}, 
                    new String[]{"Buyer", "Car Type", "Offer", "Action"}
                );
                GUI.createManualDealerTab(getLocalName(), carType, String.valueOf(listPrice), requestModel);
            });

            // Add behaviors
            addBehaviour(new ChatMessageHandler());
            addBehaviour(new ProposalHandler());
//            addBehaviour(new RegistrationHandler());
            // Replace the RegistrationHandler with this:
            addBehaviour(new OneShotBehaviour() {
                public void action() {
                    ACLMessage registration = new ACLMessage(ACLMessage.INFORM);
                    registration.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                    registration.setContent(carType.toLowerCase() + "," + listPrice);
                    send(registration);
                    GUI.logInteraction(getLocalName(), "BrokerAgent", "INFORM", 
                            carType + "," + listPrice);
                }
            });
            addBehaviour(new ConnectionMonitor());
            addBehaviour(new DealHandler());     
            
            // Accept button
            addBehaviour(new CyclicBehaviour(this) {
                public void action() {
                    Object obj = getO2AObject();
                    if (obj != null && obj instanceof Object[]) {
                        Object[] cmd = (Object[]) obj;
                        if ("NOTIFY_BROKER".equals(cmd[0])) {
                            String buyerName = (String) cmd[1];
                            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                            inform.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                            inform.setContent("DEAL_CONFIRMED," + buyerName + "," + getLocalName() + "," + carType + "," + listPrice);
                            send(inform);
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
                    if (obj != null && obj instanceof Object[]) {
                        Object[] cmd = (Object[]) obj;
                        if ("NOTIFY_BROKER_REJECT".equals(cmd[0])) {
                            String buyerName = (String) cmd[1];
                            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                            inform.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                            inform.setContent("DEAL_REJECTED," + buyerName + "," + getLocalName() + "," + carType);
                            send(inform);
                        }
                    } else {
                        block();
                    }
                }
            });

            GUI.logMessage(getLocalName(), "🏪 Dealer initialized for " + carType + " @ $" + listPrice);
        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Initialization failed: " + e.getMessage());
            doDelete();
        }
    }
    
    /**
     * Behavior for handling deal acceptance/rejection
     */
    private class DealHandler extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive(MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
            ));
            
            if (msg != null) {
                String buyerName = msg.getSender().getLocalName();
                String status = msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL 
                    ? "Accepted" : "Rejected";
                
                GUI.logMessage(getLocalName(), "Deal " + status.toLowerCase() + " by " + buyerName);
                
                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    // Record buyer's acceptance
                    GUI.recordAcceptance(buyerName, getLocalName(), true);
                    
                    // Check if dealer has accepted this buyer
                    if (GUI.getDealerAcceptance(getLocalName(), buyerName)) {
                        // Notify broker
                        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                        inform.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                        
                        // Find the buyer's offer from the model
                        for (int i = 0; i < requestModel.getRowCount(); i++) {
                            if (requestModel.getValueAt(i, 0).equals(buyerName)) {
                                String price = requestModel.getValueAt(i, 2).toString();
                                inform.setContent(buyerName + "," + getLocalName() + "," + price);
                                break;
                            }
                        }
                        
                        send(inform);
                        GUI.logInteraction(getLocalName(), "BrokerAgent", "INFORM", 
                            "Deal confirmed with " + buyerName);
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    for(int i=0; i<requestModel.getRowCount(); i++) {
                        if(requestModel.getValueAt(i, 0).equals(buyerName)) {
                            requestModel.setValueAt(status, i, 3);
                            break;
                        }
                    }
                });
            }
            else {
                block();
            }
        }
        
        private void removeBuyerRequest(String buyerName) {
            for(int i=0; i<requestModel.getRowCount(); i++) {
                if(requestModel.getValueAt(i, 0).equals(buyerName)) {
                    requestModel.removeRow(i);
                    break;
                }
            }
        }
    }
    
    // Deal status tracking
    private Map<String, Boolean> dealStatus = new ConcurrentHashMap<>();

    /**
     * Updates deal acceptance status
     * @param participant Agent name
     * @param accepted Acceptance state
     */
    public void updateDealStatus(String participant, boolean accepted) {
        dealStatus.put(participant, accepted);
    }

    /**
     * Retrieves deal status
     * @param participant Agent name
     * @return Acceptance state
     */
    public boolean getDealStatus(String participant) {
        return dealStatus.getOrDefault(participant, false);
    }
    
    /**
     * Behavior for handling chat messages
     */
    private class ChatMessageHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.not(MessageTemplate.MatchSender(new AID("BrokerAgent", AID.ISLOCALNAME))
            ));
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();
                
                synchronized (messageQueue) {
                    messageQueue.add(msg);
                }
                
                SwingUtilities.invokeLater(() -> {
                    GUI.updateChatSession(sender, myAgent.getLocalName(), content);
                });
            } else {
                block();
            }
        }
    }

    /**
     * Behavior for handling purchase proposals
     */
    private class ProposalHandler extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
            if (msg != null) {
                try {
                    String content = msg.getContent();
                    
                    // Handle manual proposal format
                    if (content.startsWith("MANUAL_PROPOSE:")) {
                        content = content.substring("MANUAL_PROPOSE:".length());
                        
                        String[] parts = content.split(",");
                        if (parts.length == 2) {
                            String receivedCarType = parts[0];
                            int offer = Integer.parseInt(parts[1]);
                            
                            if (receivedCarType.equalsIgnoreCase(carType)) {
                                String buyer = msg.getSender().getLocalName();
                                
                                SwingUtilities.invokeLater(() -> {
                                    requestModel.addRow(new Object[]{
                                        buyer,
                                        receivedCarType,
                                        offer,
                                        "Pending", // Initial status
                                        ""
                                    });
                                });
                                
                                GUI.logMessage(getLocalName(), "Received request from " + buyer);
                                return;
                            }
                        }
                    }
                    GUI.logMessage(getLocalName(), "⚠️ Invalid manual proposal format");
                } catch (Exception e) {
                    GUI.logMessage(getLocalName(), "⚠️ Invalid proposal format: " + msg.getContent());
                }
            }
            block();
        }
    }

    // Handle registration with broker
    private class RegistrationHandler extends CyclicBehaviour {
        @Override
        public void action() {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                msg.setContent(carType.toLowerCase() + "," + listPrice);
                send(msg);
                GUI.logInteraction(getLocalName(), "BrokerAgent", "INFORM",
                        carType + "," + listPrice);
            } catch (Exception e) {
                GUI.logMessage(getLocalName(), "⚠️ Broker registration failed: " + e.getMessage());
            }
            block(60000); // Re-register every 60 seconds
        }
    }
    
    /**
     * Network connection monitoring behavior
     */
    private class ConnectionMonitor extends TickerBehaviour {
        public ConnectionMonitor() {
            super(ManualDealerAgent.this, 5000);
        }

        protected void onTick() {
            if (!isConnected()) {
                GUI.logMessage(getLocalName(), "❌ Connection lost!");
                doDelete();
            }
        }
        
        private boolean isConnected() {
            try {
                return getContainerController().getContainerName() != null;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Retrieves next incoming message
     * @return ACLMessage or null if empty
     */
    @Override
    public ACLMessage getNextMessage() {
        synchronized(messageQueue) {
            return messageQueue.poll();
        }
    }

    /**
     * Accepts a buyer's offer
     * @param buyerName The buyer agent name
     * @param offer The accepted price
     */
    @Override
    public void acceptOffer(String buyerName, int offer) {
        try {
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.addReceiver(new AID(buyerName, AID.ISLOCALNAME));
            accept.setContent("MANUAL_ACCEPT:" + offer);
            send(accept);
            
            GUI.logInteraction(getLocalName(), buyerName, "ACCEPT", 
                    String.valueOf(offer));
            
            GUI.logMessage(getLocalName(), "✅ Accepted " + buyerName + "'s offer of $" + offer);
            GUI.logInteraction(getLocalName(), buyerName, "ACCEPT", String.valueOf(offer));

            // Initiate chat session
            SwingUtilities.invokeLater(() -> {
                GUI.createChatWindow(getLocalName(), buyerName);
            });

            // Notify buyer to start chat
            ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
            notify.addReceiver(new AID(buyerName, AID.ISLOCALNAME));
            notify.setContent("CHAT_START");
            send(notify);

        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Error accepting offer: " + e.getMessage());
        }
    }

    /**
     * Rejects a buyer's offer
     * @param buyerName The buyer agent name
     */
    @Override
    public void rejectOffer(String buyerName) {
        try {
            ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            reject.addReceiver(new AID(buyerName, AID.ISLOCALNAME));
            reject.setContent("Offer rejected by dealer");
            send(reject);
            
            GUI.logMessage(getLocalName(), "❌ Rejected " + buyerName + "'s offer");
            GUI.logInteraction(getLocalName(), buyerName, "REJECT", "Offer rejected");

        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Error rejecting offer: " + e.getMessage());
        }
    }
    
    /**
     * Sends acceptance notification
     * @param counterpart The buyer agent name
     */
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

    /**
     * Provides O2A interface access
     */
    @Override
    public Object getO2AInterface(Class c) {
        return c.equals(IManualDealerAgent.class) ? this : null;
    }

    /**
     * Cleanup before agent termination
     */
    @Override
    protected void takeDown() {
        GUI.logMessage(getLocalName(), "🔴 Agent terminating");
        setEnabledO2ACommunication(false, 0);
        pendingMessages.clear();
        super.takeDown();
    }
    
    /**
     * Pre-move preparation
     */
    @Override
    protected void beforeMove() {
        GUI.logMessage(getLocalName(), "🔄 Agent moving containers");
        super.beforeMove();
    }

    /**
     * Post-move initialization
     */
    @Override
    protected void afterMove() {
        GUI.logMessage(getLocalName(), "🔄 Agent moved successfully");
        super.afterMove();
    }
}