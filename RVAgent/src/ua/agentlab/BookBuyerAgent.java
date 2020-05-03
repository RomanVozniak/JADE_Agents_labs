package ua.agentlab;

import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import ua.agentlab.BookSellerGui;
import jade.lang.acl.ACLMessage;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class BookBuyerAgent extends Agent {

	private static final long serialVersionUID = 1L;
	
	private String targetBookTitle;
	private String AID1_Name = "sellect1"; 
	private String AID2_Name = "sellect2"; 
	private AID[] sellerAgents = {
			new AID(AID1_Name, AID.ISLOCALNAME), 
			new AID(AID2_Name, AID.ISLOCALNAME)
		};
	
	protected void setup() {
		System.out.println("Hello! Buyer-agent " + getAID().getName() + " is ready");
		
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			targetBookTitle = (String) args[0];
			System.out.println("Trying to buy: " + targetBookTitle);
	        addBehaviour(new TickerBehaviour(this, 10000) {
	            protected void onTick() {
	                // Update the list of seller agents
	                DFAgentDescription template = new DFAgentDescription();
	                ServiceDescription sd = new ServiceDescription();
	                sd.setType("book-selling");
	                template.addServices(sd);
	                
	                try {
	                    DFAgentDescription[] result = DFService.search(myAgent, template);
	                    System.out.println("Found the following seller agents:");
	                    sellerAgents = new AID[result.length];
	                    for (int i = 0; i < result.length; ++i) {
	                        sellerAgents[i] = result[i].getName();
	                        System.out.println(sellerAgents[i].getName());
	                    }
	                } catch (FIPAException fe) {
	                    fe.printStackTrace();
	                }
	
	                // Perform the request
	                myAgent.addBehaviour(new RequestPerformer(sellerAgents, targetBookTitle));
	            }
	        } );
		}
		else {
			// Негайно завершити роботу агента 
			System.out.println("No book title specified");
			doDelete();
		}
		
		System.out.println("Buyer-agent done: " + getAID().getName());
	}
	
	protected void takeDown() {
		System.out.println("Buyer-agent " + getAID().getName() + " terminating.");
	}
}






