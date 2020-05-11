package ua.agentlab;

import java.util.Date;
import java.util.Hashtable;

import jade.content.ContentElementList;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSContractNetResponder;
import ontology.Book;
import ontology.BookTradingOntology;
import ontology.Costs;
import ontology.Sell;
import jade.content.onto.*;
import jade.content.onto.basic.Action;
import jade.content.schema.*;

public class BookSellerAgent extends Agent {
	private Hashtable catalogue;
	private BookSellerGui myGui;
	
	private Codec codec = new SLCodec();
	private Ontology ontology = (Ontology) BookTradingOntology.getInstance();
	
	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology((jade.content.onto.Ontology) ontology);

		catalogue = new Hashtable();
		catalogue.put("A_storm_of_swords", 3);
		
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
		
		addBehaviour(new CallForOfferServer(this));
		addBehaviour(new PurchaseOrdersServer(catalogue));
		putForSale("A_storm_of_swords", 10);
		
		System.out.println("Seller-agent done: " + getAID());
	}

	protected void takeDown() {
		myGui.dispose();
		System.out.println("Seller-agent" + getAID().getName() + " terminating.");
	}
	
	public void putForSale(String title, int price){   
        addBehaviour(new PriceManager(this, title, price));   
    }

	// This is invoked by the GUI when the user adds a new book for sale
	public void updateCatalogue(final String title, final int price) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				catalogue.put(title, new Integer(price));
			}
		});
	}
	
	private class PriceManager extends TickerBehaviour{   
		private String title;   
        private int price;
           
        private PriceManager(Agent agent, String title, int price){   
            super(agent, 600);    // tick every minute   
            this.title = title;   
            this.price = price;  
        }
           
        public void onStart(){   
            // Insert the book in the catalogue of books available for sale   
            catalogue.put(title, this);   
            super.onStart();   
        }   
           
        public void onTick(){   
            long currentTime = System.currentTimeMillis(); 
            // Compute the current price
            this.price = 10;
        }   
           
        public int getCurrentPrice(){
            return (int) this.price;
        }
    }
	
	private class CallForOfferServer extends CyclicBehaviour{
        private MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));
           
        public CallForOfferServer(Agent a) {
            super(a);
        }
           
        public void action(){   
        	//System.out.println("CallForOfferServer");
            ACLMessage cfp = myAgent.receive(mt);   
            if(cfp != null) {   
                // Message received.Process it   
                myAgent.addBehaviour(new SSContractNetResponder(myAgent, cfp){   
                    protected ACLMessage handleCfp(ACLMessage cfp) {   
                        ACLMessage reply = cfp.createReply();   
                        try {   
                            ContentManager cm = myAgent.getContentManager();   
                            Action act = (Action)cm.extractContent(cfp);   
                            Sell sellAction = (Sell)act.getAction();   
                            Book book = sellAction.getItem();   
                            PriceManager pm = (PriceManager)catalogue.get(book.getTitle());   
                            if(pm != null) {   
                                // The requested book is available for sale. Reply with the price   
                                reply.setPerformative(ACLMessage.PROPOSE);   
                                ContentElementList cel = new ContentElementList();   
                                cel.add(act);   
                                Costs costs = new Costs();   
                                costs.setItem(book);   
                                costs.setPrice(pm.getCurrentPrice());   
                                cel.add(costs);   
                                cm.fillContent(reply, cel);                        
                            }   
                            else{   
                                // The requested book is NOT available for sale .   
                                reply.setPerformative(ACLMessage.REFUSE);   
                            }   
                        }   
                        catch (OntologyException oe) {   
                            oe.printStackTrace();   
                            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);   
                        }   
                        catch (CodecException ce) {   
                            ce.printStackTrace();   
                            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);   
                        }   
                        return reply;   
                    }   
                       
                    protected ACLMessage handleAcceptProposal(   
                        ACLMessage cfp, ACLMessage propose, ACLMessage accept) {   
                        ACLMessage reply = accept.createReply();   
                        try {   
                            ContentManager cm = myAgent.getContentManager();   
                            ContentElementList cel = (ContentElementList)cm.extractContent(accept);   
                            Costs costs = (Costs)cel.get(1);   
                            Book book = costs.getItem();   
                            float price = costs.getPrice();   
                               
                            PriceManager pm = (PriceManager)catalogue.get(book.getTitle());   
                            if(pm != null && pm.getCurrentPrice() <= price){   
                                // The requested book is available for sale. Reply with the price   
                                catalogue.remove(book.getTitle());   
                                pm.stop();
                                System.out.println("Book " + book.getTitle() + " has been sent to " + accept.getSender() + ".");
                                reply.setPerformative(ACLMessage.INFORM);   
                                reply.setContent(accept.getContent());   
                            }   
                            else{   
                                // The requested book is NOT available for sale .   
                                reply.setPerformative(ACLMessage.FAILURE);   
                            }   
                        }   
                        catch (OntologyException oe) {   
                            oe.printStackTrace();   
                            reply.setPerformative(ACLMessage.FAILURE);
                            System.out.println("accept proposal from " + accept.getSender() + " can't be understood.");
                        }   
                        catch (CodecException ce) {   
                            ce.printStackTrace();   
                            reply.setPerformative(ACLMessage.FAILURE);
                            System.out.println("accept proposal from " + accept.getSender() + " can't be understood.");
                        }   
                        return reply;   
                    }   
                });                
            }   
            else {   
                block();   
            }   
        }   
    }//End of inner class CallForOfferServer   
}
