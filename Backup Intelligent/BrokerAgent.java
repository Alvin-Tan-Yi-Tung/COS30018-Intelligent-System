package IntelligentProject;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.HashMap;
import java.util.Map;

public class BrokerAgent extends Agent {
	// Data storage for dealer car listings (DealerName -> "CarType,Price")
    private Map<String, String> carListings = new HashMap<>();
    
    // Commission tracking fields
    private int totalCommission = 0;
    private int automatedCommission = 0;	// Automated commission
    private int manualCommission = 0;		// Manual commission
    
    // Agent initialization
    protected void setup() {
        GUI.logMessage(getLocalName(), "🟦 Broker started - Ready for registrations");
        
        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.or(
                    MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
                    ),
                    MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
                        MessageTemplate.MatchPerformative(ACLMessage.CONFIRM)
                    )
                );
                
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    switch (msg.getPerformative()) {
                        case ACLMessage.INFORM:
                        	if (msg.getContent().startsWith("DEAL_CONFIRMED")) {
                                handleDealConfirmation(msg);
                            } else if (msg.getContent().startsWith("DEAL_REJECTED")) {
                                handleDealRejection(msg);
                            } else {
                                handleDealerListing(msg);
                            }
                            break;
                        case ACLMessage.REQUEST:
                            handleBuyerRequest(msg);
                            break;
                        case ACLMessage.QUERY_IF:
                            handleManualQuery(msg);
                            break;
                        case ACLMessage.CONFIRM:
                            // Reserved for future extensions
                            break;
                    }
                } else {
                    block();
                }
            }
        });
    }
    
    /**
     * Handles dealer registration messages
     * @param msg INFORM message containing "CarType,Price"
     * - Stores dealer offering in carListings
     * - Logs registration in GUI
     */
    private void handleDealerListing(ACLMessage msg) {
        try {
            String content = msg.getContent();
            String[] parts = content.split(",");
            
            if (parts.length == 2) {
                carListings.put(msg.getSender().getLocalName(), content);
                GUI.logMessage(getLocalName(), 
                    "📥 Registered " + parts[0] + " from " + msg.getSender().getLocalName() + 
                    " @ $" + parts[1]);
                
                if (msg.getSender().getLocalName().startsWith("M.Dealer")) {
                    GUI.logInteraction("BrokerAgent", msg.getSender().getLocalName(), 
                        "ACK", "Registered " + parts[0] + "@$" + parts[1]);
                }
            }
        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Error processing dealer listing: " + e.getMessage());
        }
    }

    /**
     * Processes buyer requests for specific car types
     * @param msg REQUEST message with desired car type
     * - Finds lowest price from registered dealers
     * - Returns best offer (REFUSE if none)
     * - Logs matching in GUI
     */
    private void handleBuyerRequest(ACLMessage msg) {
        try {
            GUI.logInteraction(
                msg.getSender().getLocalName(), 
                getLocalName(), 
                "REQUEST", 
                msg.getContent()
            );
            
            String requestedCar = msg.getContent();
            ACLMessage reply = msg.createReply();
            
            String bestDealer = null;
            int bestPrice = Integer.MAX_VALUE;

            for (Map.Entry<String, String> entry : carListings.entrySet()) {
                String[] parts = entry.getValue().split(",");
                if (parts[0].equals(requestedCar)) {
                    int price = Integer.parseInt(parts[1]);
                    if (price < bestPrice) {
                        bestPrice = price;
                        bestDealer = entry.getKey();
                    }
                }
            }

            if (bestDealer != null) {
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(bestDealer + "," + bestPrice);
                GUI.logMessage(getLocalName(), 
                    "🤝 Matched " + msg.getSender().getLocalName() + 
                    " with " + bestDealer + " @ $" + bestPrice);
                GUI.logInteraction(getLocalName(), msg.getSender().getLocalName(), 
                    "INFORM", bestDealer + "," + bestPrice);
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("No matching dealers");
                GUI.logMessage(getLocalName(), "❌ No dealers found for " + requestedCar);
                GUI.logInteraction(getLocalName(), msg.getSender().getLocalName(), 
                    "REFUSE", "No matching dealers");
            }
            send(reply);
        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Error processing buyer request: " + e.getMessage());
        }
    }
    
    /**
     * Handles manual buyer queries with price limits
     * @param msg QUERY_IF message with "CarType,MaxPrice"
     * - Returns list of dealers matching criteria
     * - Used for manual negotiation process
     */
    private void handleManualQuery(ACLMessage msg) {
        try {
            GUI.logInteraction(
                msg.getSender().getLocalName(), 
                getLocalName(), 
                "QUERY_IF", 
                msg.getContent()
            );

            String[] criteria = msg.getContent().split(",");
            String buyerCarType = criteria[0].trim().toLowerCase();
            int maxPrice = Integer.parseInt(criteria[1].trim());

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            
            StringBuilder dealers = new StringBuilder();
            for (Map.Entry<String, String> entry : carListings.entrySet()) {
                String[] parts = entry.getValue().split(",");
                String dealerCarType = parts[0].trim().toLowerCase();
                int price = Integer.parseInt(parts[1].trim());
                
                if (dealerCarType.equals(buyerCarType) && price <= maxPrice) {
                    dealers.append(entry.getKey()).append(",").append(price).append(";");
                }
            }
            
            reply.setContent(dealers.toString());
            send(reply);

            GUI.logInteraction(
                getLocalName(), 
                msg.getSender().getLocalName(), 
                "INFORM", 
                dealers.toString()
            );
        } catch (Exception e) {
            GUI.logMessage(getLocalName(), "⚠️ Manual query error: " + e.getMessage());
        }
    }

    /**
     * Finalizes successful deals from manual negotiations
     * @param msg CONFIRM message with "DEAL_CONFIRMED,..."
     * - Updates commission totals
     * - Removes dealer from listings
     * - Notifies both parties
     */
    private void handleDealConfirmation(ACLMessage msg) {
        try {
            String[] parts = msg.getContent().split(",");
            if (parts.length != 5 || !parts[0].equals("DEAL_CONFIRMED")) {
                GUI.logMessage(getLocalName(), "Invalid deal confirmation: " + msg.getContent());
                return;
            }

            String buyer = parts[1];
            String dealer = parts[2];
            String carType = parts[3];
            int price = Integer.parseInt(parts[4]);
            
            // Calculate commission
            int commission = 500;
            totalCommission += commission;
            
            // Determine commission type
            if(buyer.startsWith("M.Buyer") || dealer.startsWith("M.Dealer")) {
                manualCommission += commission;
            } else {
                automatedCommission += commission;
            }

            // Update GUI
            GUI.updateCommissionDisplay(automatedCommission, manualCommission, totalCommission);
            
         // Original functionality
            carListings.remove(dealer);
            GUI.logMessage(getLocalName(), 
                "✅ Deal Confirmed - " + buyer + " ↔ " + dealer + 
                " | " + carType + " @ $" + price +
                " | Commission: RM" + commission);

            // 1. Remove dealer from active listings
            carListings.remove(dealer);

            // 2. Log the successful deal
            GUI.logMessage(getLocalName(), 
                "✅ Manual Deal Confirmed - " + buyer + " ↔ " + dealer + 
                " | " + carType + " @ $" + price);

            // 3. Notify both parties
            notifyDealCompletion(buyer, dealer, carType, price);

            // 4. Update GUI interactions
            GUI.logInteraction(buyer, getLocalName(), 
                "DEAL_CONFIRMED", dealer + "," + price);
            GUI.logInteraction(dealer, getLocalName(), 
                "DEAL_CONFIRMED", buyer + "," + price);

        } catch (Exception e) {
            GUI.logMessage(getLocalName(), 
                "⚠️ Deal confirmation error: " + e.getMessage());
        }
    }
    
    /**
     * Processes failed manual negotiations
     * @param msg CONFIRM message with "DEAL_REJECTED,..."
     * - Cleans up dealer listings
     * - Logs rejection in GUI
     */
    private void handleDealRejection(ACLMessage msg) {
        try {
            String[] parts = msg.getContent().split(",");
            if (parts.length != 4 || !parts[0].equals("DEAL_REJECTED")) return;
            
            String buyer = parts[1];
            String dealer = parts[2];
            String carType = parts[3];
            
            // Remove both from listings
            carListings.remove(dealer);
            
            // Log interactions
            GUI.logMessage(getLocalName(), 
                "❌ Deal Rejected - " + buyer + " ↔ " + dealer + " | " + carType);
            GUI.logInteraction(buyer, "BrokerAgent", "REJECT", 
                "Rejected " + dealer + "'s offer");
            GUI.logInteraction(dealer, "BrokerAgent", "REJECT", 
                buyer + " rejected the deal");

        } catch (Exception e) {
            GUI.logMessage(getLocalName(), 
                "⚠️ Rejection handling error: " + e.getMessage());
        }
    }

    /**
     * Sends completion notifications to both parties
     * @param buyer Buyer agent name
     * @param dealer Dealer agent name
     * @param carType Negotiated car type
     * @param price Final agreed price
     */
    private void notifyDealCompletion(String buyer, String dealer, String carType, int price) {
        try {
            // Notify buyer
            ACLMessage buyerMsg = new ACLMessage(ACLMessage.CONFIRM);
            buyerMsg.addReceiver(new AID(buyer, AID.ISLOCALNAME));
            buyerMsg.setContent("DEAL_COMPLETED," + dealer + "," + carType + "," + price);
            send(buyerMsg);

            // Notify dealer
            ACLMessage dealerMsg = new ACLMessage(ACLMessage.CONFIRM);
            dealerMsg.addReceiver(new AID(dealer, AID.ISLOCALNAME));
            dealerMsg.setContent("DEAL_COMPLETED," + buyer + "," + carType + "," + price);
            send(dealerMsg);

        } catch (Exception e) {
            GUI.logMessage(getLocalName(), 
                "⚠️ Completion notify failed: " + e.getMessage());
        }
    }
}