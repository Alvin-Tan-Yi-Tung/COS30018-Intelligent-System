package IntelligentProject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import jade.lang.acl.ACLMessage;

/**
 * Interface defining required capabilities for manual buyer agents
 * Enables GUI-driven negotiation management
 */
public interface IManualBuyerAgent {
    /**
     * Queries BrokerAgent for matching dealers
     * Populates dealer matches table model
     */
    void queryBrokerForDealers();

    /**
     * Provides table model for displaying dealer matches
     * @return DefaultTableModel containing dealer listings
     *         Columns: [Dealer Name, Car Type, Price]
     */
    DefaultTableModel getDealerMatchesModel();

    /**
     * Initiates negotiation sequence with selected dealer
     * @param dealerName Target dealer for negotiation
     */
    void startNegotiation(String dealerName);
    
    /**
     * Sends custom proposal/request to specific dealer
     * @param dealerName Target dealer for direct communication
     */
    void sendRequestToDealer(String dealerName);
    
    /**
     * Connects agent status updates to GUI component
     * @param label Reference to UI status label
     */
    void setStatusLabel(JLabel label);

    /**
     * Queues outgoing message for delivery
     * @param msg Complete ACLMessage to send
     */
    void sendMessage(ACLMessage msg);

    /**
     * Retrieves next incoming message from queue
     * @return ACLMessage or null if none available
     *         Note: May block in threaded implementations
     */
    ACLMessage getNextMessage();
}