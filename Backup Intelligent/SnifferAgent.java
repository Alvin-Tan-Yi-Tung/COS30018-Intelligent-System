package IntelligentProject;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.Iterator;
import jade.core.AID;

/**
 * Monitoring agent that logs all system communications
 * Provides real-time message tracing for debugging and analysis
 */
public class SnifferAgent extends Agent {
	protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
				// Capture ALL messages in the system
                ACLMessage msg = receive(MessageTemplate.MatchAll());
                if (msg != null) {
                    // Handle multiple receivers
                    Iterator it = msg.getAllReceiver();
                    while (it.hasNext()) {
                        AID receiver = (AID) it.next();
						// Log interaction to GUI
                        GUI.logInteraction(
                            msg.getSender().getLocalName(),
                            receiver.getLocalName(),
                            getPerformative(msg),	// Get readable performative
                            msg.getContent()
                        );
                    }
                }
                block();	// Wait for next message
            }
        });
    }

    /**
     * Translates ACLMessage performatives to human-readable labels
     * Handles both standard and custom message formats
     * @param msg Message to analyze
     * @return String representation of message type
     */
	private String getPerformative(ACLMessage msg) {
		// Handle special QUERY_IF case first
		if (msg.getPerformative() == ACLMessage.QUERY_IF) {
	        return "QUERY_IF"; // Explicitly map QUERY_IF
	    }
	    
	    int performative = msg.getPerformative();
	    String content = msg.getContent();

	    // Detect manual negotiation messages
	    if (content != null) {
	        if (content.startsWith("MANUAL_ACCEPT:")) {
	            return "ACCEPT";
	        }
	        if (content.startsWith("MANUAL_PROPOSE:")) {
	            return "PROPOSE";
	        }
	        if (content.startsWith("MANUAL_REJECT:")) {
	            return "REJECT";
	        }
	        if (content.startsWith("CHAT_START")) {
	            return "INFORM";	// Special chat initialization
	        }
	    }

	    // Map standard FIPA performatives
	    switch(performative) {
	        case ACLMessage.REQUEST: return "REQUEST";
	        case ACLMessage.QUERY_IF: return "QUERY_IF";
	        case ACLMessage.INFORM: return "INFORM";
	        case ACLMessage.PROPOSE: return "PROPOSE";
	        case ACLMessage.ACCEPT_PROPOSAL: return "ACCEPT";
	        case ACLMessage.REJECT_PROPOSAL: return "REJECT";
	        default: return "UNKNOWN";	// Fallback for unmapped types
	    }
	}
}