import java.util.ArrayList;

public class Process {
	public String path;
	public int arrivalTime;
	public ArrayList<Variable> variables;
	public PCB pcb;
	
	
	public Process(String path, int arrivalTime) {
		this.path=path;
		this.arrivalTime=arrivalTime;
		variables = new ArrayList<Variable>();
	}

}
