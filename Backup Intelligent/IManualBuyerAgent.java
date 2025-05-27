package IntelligentProject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import jade.lang.acl.ACLMessage;

public interface IManualBuyerAgent {
    void queryBrokerForDealers();
    DefaultTableModel getDealerMatchesModel();
    void startNegotiation(String dealerName);
    void sendRequestToDealer(String dealerName);
    void setStatusLabel(JLabel label);
    void sendMessage(ACLMessage msg);
    ACLMessage getNextMessage();
}