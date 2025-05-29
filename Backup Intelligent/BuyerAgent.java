package IntelligentProject;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import java.util.Date;

public class BuyerAgent extends Agent {
	// Negotiation parameters
	protected String carType;		// Desired car model
    protected int initialOffer;		// First offer amount
    protected int reservePrice;		// Maximum budget (won't exceed this)
    protected boolean negotiationComplete = false;
    protected int minRounds = 3;	// Minimum negotiation rounds before accepting
    protected int currentRound = 0;	// Current negotiation round

    protected void setup() {
        try {
            Object[] args = getArguments();
            if (args == null || args.length != 3) {
                GUI.logMessage(getLocalName(), "❌ Usage: <CarModel> <InitialOffer> <MaxBudget>");
                doDelete();
                return;
            }

            carType = args[0].toString();
            initialOffer = Integer.parseInt(args[1].toString());
            reservePrice = Integer.parseInt(args[2].toString());

            GUI.logMessage(getLocalName(), 
                "🛒 Started for " + carType + 
                "\n   Initial Offer: $" + initialOffer + 
                "\n   Max Budget: $" + reservePrice);

            addBehaviour(new NegotiationStarter());

        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Initialization failed: " + e.getMessage());
            doDelete();
        }
    }

    /**
     * Initial negotiation starter behavior (one-time)
     * - Contacts BrokerAgent to find dealers
     * - Handles broker response timeout
     */
    private class NegotiationStarter extends OneShotBehaviour { // Not cyclic
        public void action() {
            try {
                ACLMessage findDealer = new ACLMessage(ACLMessage.REQUEST);
                findDealer.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                findDealer.setContent(carType);
                myAgent.send(findDealer);
                GUI.logInteraction(getLocalName(), "BrokerAgent", "REQUEST", carType);
                
                // Handle response
                MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchSender(new AID("BrokerAgent", AID.ISLOCALNAME)),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                );
                ACLMessage response = myAgent.blockingReceive(mt, 30000);
                handleBrokerResponse(response);
            } catch (Exception e) {
                GUI.logMessage(getLocalName(), "⚠️ Broker contact error: " + e.getMessage());
            }
        }
        
        private void handleBrokerResponse(ACLMessage response) {
            try {
                if (response != null) {
                    String[] dealerInfo = response.getContent().split(",");
                    if (dealerInfo.length >= 2) {
                        AID dealer = new AID(dealerInfo[0], AID.ISLOCALNAME);
                        GUI.logMessage(getLocalName(), 
                            "🤝 Connected with " + dealer.getLocalName() + " @ $" + dealerInfo[1]);
                        // Log received dealer info
                        GUI.logInteraction("BrokerAgent", getLocalName(), 
                            "INFORM", dealerInfo[0] + "," + dealerInfo[1]);
                        myAgent.addBehaviour(new PriceNegotiator(dealer));
                    }
                }
            } catch (Exception e) {
                GUI.logMessage(getLocalName(), "⚠️ Response handling error: " + e.getMessage());
                myAgent.doDelete();
            }
        }
    }

    /**
     * Core negotiation behavior (cyclical until completion)
     * Manages:
     * - Offer/counter-offer exchange
     * - Round counting
     * - Response processing
     */
    private class PriceNegotiator extends Behaviour {
        private final AID dealer;
        private int currentOffer;
        private boolean negotiationDone = false;

        public PriceNegotiator(AID dealer) {
            super();
            this.dealer = dealer;
            this.currentOffer = initialOffer;
        }

        /**
         * Main negotiation loop
         * - Sends offers
         * - Processes dealer responses
         * - Updates offer strategy
         */
        public void action() {
            if (!negotiationDone) {
                try {
                    currentRound++;
                    GUI.logMessage(getLocalName(), "🔄 ROUND " + currentRound);
                    sendOffer();
                    handleResponse();
                } catch (Exception e) {
                    GUI.logMessage(getLocalName(), "⚠️ Error: " + e.getMessage());
                    negotiationDone = true;
                }
            }
        }

        /**
         * Sends current offer to dealer
         * Uses PROPOSE message format: "CarType,Price"
         */
        private void sendOffer() {
            ACLMessage offer = new ACLMessage(ACLMessage.PROPOSE);
            offer.addReceiver(dealer);
            offer.setContent(carType + "," + currentOffer);
            myAgent.send(offer);
            GUI.logMessage(getLocalName(), "📤 OFFER: $" + currentOffer);
            // Log interaction
            GUI.logInteraction(getLocalName(), dealer.getLocalName(), 
                "PROPOSE", carType + "," + currentOffer);
        }

        /**
         * Handles dealer responses with 15s timeout
         * Processes: Accept/Reject/Counter offers
         */
        private void handleResponse() {
            try {
                MessageTemplate mtAccept = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                MessageTemplate mtReject = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
                MessageTemplate mtCounter = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                
                MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchSender(dealer),
                    MessageTemplate.or(mtAccept, MessageTemplate.or(mtReject, mtCounter))
                );
                
                ACLMessage response = myAgent.blockingReceive(mt, 15000);
                if (response != null) {
                    processResponse(response);
                } else {
                    GUI.logMessage(getLocalName(), "⌛ Response timeout");
                    negotiationDone = true;
                }
            } catch (Exception e) {
                GUI.logMessage(getLocalName(), "⚠️ Response handling error: " + e.getMessage());
                negotiationDone = true;
            }
        }

        /**
         * Processes different response types:
         * - ACCEPT_PROPOSAL: Finalizes deal
         * - REJECT_PROPOSAL: Ends negotiation
         * - PROPOSE: Processes counter-offer
         */
        private void processResponse(ACLMessage response) {
            try {
                if (response.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    negotiationComplete = true;
                    negotiationDone = true;
                    GUI.logMessage(getLocalName(), "✅ Deal accepted at $" + currentOffer);
                    
                    // Log acceptance
                    GUI.logInteraction(dealer.getLocalName(), getLocalName(), 
                        "ACCEPT", carType + "," + currentOffer);
                } 
                else if (response.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                    negotiationDone = true;
                    GUI.logMessage(getLocalName(), "❌ Deal rejected");
                    
                 // Log rejection
                    GUI.logInteraction(dealer.getLocalName(), getLocalName(), 
                        "REJECT", carType);
                }
                else {
                    String[] parts = response.getContent().split(",");
                    int dealerCounter = Integer.parseInt(parts[1]);
                    GUI.logMessage(getLocalName(), "📥 COUNTER: $" + dealerCounter);
                    
                    // Log counter offer
                    GUI.logInteraction(dealer.getLocalName(), getLocalName(), 
                        "PROPOSE", carType + "," + dealerCounter);

                    if (dealerCounter <= reservePrice) {
                        if (currentRound < minRounds) {
                            currentOffer = (int)(dealerCounter * 0.97);
                            GUI.logMessage(getLocalName(), 
                                "💡 New offer: $" + currentOffer + " (97% of counter)");
                        } else {
                            acceptOffer(response);
                        }
                    } else {
                        currentOffer = Math.min(currentOffer + (dealerCounter - currentOffer)/2, reservePrice);
                        if (currentOffer >= reservePrice) {
                            GUI.logMessage(getLocalName(), "⛔ Max budget reached");
                            negotiationDone = true;
                        }
                    }
                }
            } catch (Exception e) {
                GUI.logMessage(getLocalName(), "⚠️ Offer processing error: " + e.getMessage());
                negotiationDone = true;
            }
        }

        /**
         * Finalizes successful negotiation:
         * - Sends acceptance to dealer
         * - Notifies BrokerAgent
         * - Updates GUI status
         */
        private void acceptOffer(ACLMessage response) {
            ACLMessage accept = response.createReply();
            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            myAgent.send(accept);
            negotiationComplete = true;
            negotiationDone = true;
            GUI.logMessage(getLocalName(), "✅ Deal accepted!");
            
            // Notify BrokerAgent of the successful deal
            ACLMessage brokerMsg = new ACLMessage(ACLMessage.INFORM);
            brokerMsg.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
            brokerMsg.setContent("DEAL_CONFIRMED," + getLocalName() + "," + dealer.getLocalName() + "," + carType + "," + currentOffer);
            send(brokerMsg);
        }

        public boolean done() {
            if (negotiationDone) {
                if (!negotiationComplete) {
                    // Notify broker of failure
                    ACLMessage failure = new ACLMessage(ACLMessage.FAILURE);
                    failure.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
                    failure.setContent("NEGOTIATION_FAILED," + 
                        getLocalName() + "," + 
                        dealer.getLocalName() + "," + 
                        carType);
                    myAgent.send(failure);
                    
                    GUI.logMessage(getLocalName(), "⚠️ Negotiation failed");
                }
                myAgent.doDelete();
                return true;
            }
            return false;
        }
    }
}