package IntelligentProject;

import jade.lang.acl.ACLMessage;

/**
 * Interface defining manual negotiation capabilities for dealer agents
 * Enables human-in-the-loop decision making for offers
 */
public interface IManualDealerAgent {
    /**
     * Queues an outgoing message for delivery
     * @param msg Complete ACLMessage to be sent
     *        Should include receiver, performative, and content
     */
	void sendMessage(ACLMessage msg);

    /**
     * Finalizes agreement with specific buyer
     * @param buyerName Target buyer agent name
     * @param offer Accepted offer amount
     * @throws Exception If deal confirmation fails
     *         (e.g., invalid buyer, stale negotiation)
     */
	void acceptOffer(String buyerName, int offer) throws Exception;

    /**
     * Terminates negotiation with buyer
     * @param buyerName Target buyer agent name
     * @throws Exception If rejection processing fails
     */
    void rejectOffer(String buyerName) throws Exception;

    /**
     * Retrieves next incoming message from queue
     * @return ACLMessage or null if none available
     *         Messages ordered by arrival time
     */
    ACLMessage getNextMessage();
}