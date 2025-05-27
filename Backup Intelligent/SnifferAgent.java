package IntelligentProject;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.Iterator;
import jade.core.AID;

public class SnifferAgent extends Agent {
	protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchAll());
                if (msg != null) {
                    // Log all messages
                    Iterator it = msg.getAllReceiver();
                    while (it.hasNext()) {
                        AID receiver = (AID) it.next();
                        GUI.logInteraction(
                            msg.getSender().getLocalName(),
                            receiver.getLocalName(),
                            getPerformative(msg),
                            msg.getContent()
                        );
                    }
                }
                block();
            }
        });
    }

    // Updated method to parse performative from message content
	private String getPerformative(ACLMessage msg) {
		if (msg.getPerformative() == ACLMessage.QUERY_IF) {
	        return "QUERY_IF"; // Explicitly map QUERY_IF
	    }
	    
	    int performative = msg.getPerformative();
	    String content = msg.getContent();

	    // Enhanced manual agent handling
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
	            return "INFORM";
	        }
	    }

	    // Standard performative mapping
	    switch(performative) {
	        case ACLMessage.REQUEST: return "REQUEST";
	        case ACLMessage.QUERY_IF: return "QUERY_IF";
	        case ACLMessage.INFORM: return "INFORM";
	        case ACLMessage.PROPOSE: return "PROPOSE";
	        case ACLMessage.ACCEPT_PROPOSAL: return "ACCEPT";
	        case ACLMessage.REJECT_PROPOSAL: return "REJECT";
	        default: return "UNKNOWN";
	    }
	}
}