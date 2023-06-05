import java.util.LinkedList;
import java.util.Queue;

public class Mutex {
	public boolean value=false;
	public int processID;
	public Queue<Integer> myBlockedQueue = new LinkedList<Integer>();
	
	
	public void acquire(int processID) {
		if (!value) {
			value=true;
			this.processID= processID;
		}
		
	}
	
	public void release(int processID) {
		if (this.processID==processID) {
			value=false;
		}
	}
	
	

}
