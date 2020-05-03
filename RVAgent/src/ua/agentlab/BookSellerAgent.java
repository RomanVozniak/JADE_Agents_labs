package ua.agentlab;

import java.util.Hashtable;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BookSellerAgent extends Agent {
	private Hashtable catalogue;
	private BookSellerGui myGui;

	protected void setup() {
		catalogue = new Hashtable();
		catalogue.put("A_storm_of_swords", 3);
		
		myGui = new BookSellerGui(this);
		myGui.show();
		
		// Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-selling");
        sd.setName("JADE-book-trading");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
		
		addBehaviour(new OfferRequestsServer(catalogue));
		addBehaviour(new PurchaseOrdersServer(catalogue));
		
		System.out.println("Seller-agent done: " + getAID());
	}

	protected void takeDown() {
		myGui.dispose();
		System.out.println("Seller-agent" + getAID().getName() + " terminating.");
	}

	// This is invoked by the GUI when the user adds a new book for sale
	public void updateCatalogue(final String title, final int price) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				catalogue.put(title, new Integer(price));
			}
		});
	}
}
