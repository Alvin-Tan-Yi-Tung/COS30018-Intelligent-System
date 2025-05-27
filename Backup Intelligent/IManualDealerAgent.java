package IntelligentProject;

import jade.lang.acl.ACLMessage;

public interface IManualDealerAgent {
	void sendMessage(ACLMessage msg);
	void acceptOffer(String buyerName, int offer) throws Exception;
    void rejectOffer(String buyerName) throws Exception;
    ACLMessage getNextMessage();
}