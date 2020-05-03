package ua.agentlab;

import java.util.Hashtable;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class OfferRequestsServer extends CyclicBehaviour {
	
	private Hashtable _catalogue;
	private MessageTemplate _msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.CFP);
	
	OfferRequestsServer(Hashtable catalogue){
		_catalogue = catalogue;
	}
	
	public void action() {
		ACLMessage msg = myAgent.receive(_msgTemplate);
		
		System.out.println("OfferRequestsServer");

		if (msg != null) {
			String title = msg.getContent();
			ACLMessage reply = msg.createReply();
			Integer price = (Integer) _catalogue.get(title);

			if (price != null) {
				reply.setPerformative(ACLMessage.PROPOSE);
				reply.setContent(String.valueOf(price.intValue()));
			}
			else {
				reply.setPerformative(ACLMessage.REFUSE);
				reply.setContent("not-available");
			}
			myAgent.send(reply);
		}
		else {
			block();
		}
	}
}
