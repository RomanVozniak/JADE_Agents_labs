package ua.agentlab;

import java.util.Hashtable;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class PurchaseOrdersServer extends CyclicBehaviour {
	private Hashtable _catalogue;
	private MessageTemplate _msgTemplate = 
			MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
	
	PurchaseOrdersServer(Hashtable catalogue){
		_catalogue = catalogue;
	}
	
    public void action() {
        ACLMessage msg = myAgent.receive(_msgTemplate);
        if (msg != null) {
            String bookTitle = msg.getContent();
            ACLMessage reply = msg.createReply();
            
            Integer price = (Integer) _catalogue.remove(bookTitle);
            if (price != null) {
                reply.setPerformative(ACLMessage.INFORM);
                System.out.println(bookTitle + " has been sent to agent " + msg.getSender().getName());
            } else {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("no book found");
            }
            myAgent.send(reply);
        } else {
            block();
        }
    }
}
