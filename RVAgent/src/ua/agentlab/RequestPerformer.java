package ua.agentlab;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.Behaviour;

public class RequestPerformer extends Behaviour {
	private static final long serialVersionUID = 203850710920024205L;
	private AID bestOfferSeller; // The agent who provides the best offer
	private int bestOfferPrice; // The best offered price
	private int repliesCount = 0; // The counter of replies from seller agents
	private MessageTemplate _msgTemplate; // The template to receive replies
	private int step = 0;
	
	private AID[] _sellerAgents;
	private String _targetBookTitle;
	
	RequestPerformer(AID[] sellerAgents, String targetBookTitle){
		_sellerAgents = sellerAgents;
		_targetBookTitle = targetBookTitle;
	}
	
	public void action() {
		switch (step) {
			case 0:
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < _sellerAgents.length; ++i) {
					cfp.addReceiver(_sellerAgents[i]);
				}
				cfp.setContent(_targetBookTitle);
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				
				// Prepare the template to get proposals
				_msgTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(_msgTemplate);
				if (reply != null) {
					
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						
						// This is an offer
						int price = Integer.parseInt(reply.getContent());
						if (bestOfferSeller == null || price < bestOfferPrice) {
							
							// This is the best offer at present
							bestOfferPrice = price;
							bestOfferSeller = reply.getSender();
						}
					}
					repliesCount++;
					if (repliesCount >= _sellerAgents.length) {
						// We received all replies
						step = 2;
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the seller that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestOfferSeller);
				order.setContent(_targetBookTitle);
				order.setConversationId("book-trade");
				order.setReplyWith("order" + System.currentTimeMillis());
				myAgent.send(order);
				
				// Prepare the template to get the purchase order reply
				_msgTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:
				// Receive the purchase order reply
				reply = myAgent.receive(_msgTemplate);
				if (reply != null) {
					
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						
						// Purchase successful. We can terminate
						System.out.println(_targetBookTitle + " successfully purchased.");
						System.out.println("Price = " + bestOfferPrice);
						myAgent.doDelete();
					}
					step = 4;
				}
				else {
					block();
				}
				break;
		}
	}
	public boolean done() {
		return ((step == 2 && bestOfferSeller == null) || step == 4);
	}
}

