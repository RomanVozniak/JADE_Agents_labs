package ontology;

import jade.content.AgentAction;
import jade.content.Concept;
import jade.util.leap.List;

public class Sell implements AgentAction {
	private Book item;
	
	public Book getItem() {
		return item;
	}
	public void setItem(Book item) {
		this.item = item;
	}
}
