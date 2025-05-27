package IntelligentProject;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import java.util.HashMap;
import java.util.Map;

public class DealerAgent extends Agent {
	protected String carType;	// Car model being sold
    protected int listPrice;	// Initial asking price
    private Map<AID, Integer> buyerRounds = new HashMap<>();	// Tracks negotiation rounds per buyer

    protected void setup() {
        try {
            Object[] args = getArguments();
            carType = (String) args[0];
            listPrice = Integer.parseInt(args[1].toString());

            registerWithBroker();
            setupNegotiationHandler();

            GUI.logMessage(getLocalName(), "🏪 Registered " + carType + " @ $" + listPrice);
        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "❌ Initialization failed: " + e.getMessage());
            doDelete();
        }
    }

    /**
     * Registers car listing with BrokerAgent
     * - Sends INFORM message with "CarType,Price" format
     * - Essential for appearing in broker's marketplace
     */
    protected void registerWithBroker() {
        try {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
            String content = carType.trim() + "," + listPrice; // Standardized format
            msg.setContent(content);
            send(msg);
            GUI.logInteraction(getLocalName(), "BrokerAgent", "INFORM", content);
        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Failed to register: " + e.getMessage());
        }
    }

    /**
     * Sets up continuous negotiation handler
     * - Uses AchieveREResponder for FIPA protocol compliance
     * - Listens for PROPOSE messages (buyer offers)
     */
    private void setupNegotiationHandler() {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
        addBehaviour(new AchieveREResponder(this, mt) {
        	/**
             * Core negotiation logic for handling offers
             * @param propose Buyer's PROPOSE message with "CarType,Offer"
             * @return Response message (ACCEPT/COUNTER/FAILURE)
             */
            protected ACLMessage prepareResponse(ACLMessage propose) {
                try {
                    String[] parts = propose.getContent().split(",");
                    int offer = Integer.parseInt(parts[1]);
                    AID buyer = propose.getSender();

                    int rounds = buyerRounds.getOrDefault(buyer, 0) + 1;
                    buyerRounds.put(buyer, rounds);

                    double discount = Math.pow(0.95, rounds);
                    int counter = Math.max((int)(listPrice * discount), (int)(listPrice * 0.7));

                    GUI.logMessage(getLocalName(), 
                        "📨 Offer from " + buyer.getLocalName() + ": $" + offer);
                    // Log received offer
                    GUI.logInteraction(buyer.getLocalName(), getLocalName(), 
                        "PROPOSE", carType + "," + offer);

                    if (offer >= counter) {
                        GUI.logMessage(getLocalName(), "✅ Accepting offer");
                        // Log acceptance
                        GUI.logInteraction(getLocalName(), buyer.getLocalName(), 
                            "ACCEPT", carType + "," + offer);
                        return createAcceptMessage(propose);
                    } else {
                        GUI.logMessage(getLocalName(), "🔄 Countering with $" + counter);
                        // Log counter offer
                        GUI.logInteraction(getLocalName(), buyer.getLocalName(), 
                            "PROPOSE", carType + "," + counter);
                        return createCounterMessage(propose, counter);
                    }
                } catch (Exception e) {
                    GUI.logMessage(getLocalName(), "⚠️ Error handling offer: " + e.getMessage());
                    return createFailureMessage(propose);
                }
            }

            /**
             * Creates acceptance message and notifies broker
             * - Finalizes deal at agreed price
             * - Removes dealer from broker listings
             */
            private ACLMessage createAcceptMessage(ACLMessage original) {
                ACLMessage accept = original.createReply();
                accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

                // Notify BrokerAgent of the accepted deal
                ACLMessage brokerMsg = new ACLMessage(ACLMessage.INFORM);
                brokerMsg.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                brokerMsg.setContent("DEAL_CONFIRMED," + original.getSender().getLocalName() + "," + getLocalName() + "," + carType + "," + listPrice);
                send(brokerMsg);

                return accept;
            }

            /**
             * Generates counter-offer message
             * @param price Calculated counter offer amount
             */
            private ACLMessage createCounterMessage(ACLMessage original, int price) {
                return createReply(original, ACLMessage.PROPOSE, carType + "," + price);
            }

            /**
             * Handles negotiation failures/errors
             */
            private ACLMessage createFailureMessage(ACLMessage original) {
                return createReply(original, ACLMessage.FAILURE, null);
            }

            private ACLMessage createReply(ACLMessage original, int performative, String content) {
                ACLMessage reply = original.createReply();
                reply.setPerformative(performative);
                if (content != null) reply.setContent(content);
                return reply;
            }
        });
    }
}