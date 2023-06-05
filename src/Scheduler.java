import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler {
	public final int timeSlice;
	public int counterSlice;
	public Integer[] processId;
	public Integer[] burstTime;
	public Integer[] arrivalTime;
	public Queue<Integer> readyQueue;
	public Queue<Integer> blockedQueue;
	public static int clk;
	public int finished = 0;
	public boolean success=false;

	public Process p0;
	public Process p1;
	public Process p2;
	
	public boolean p0Inserted = false;
	public boolean p1Inserted = false;
	public boolean p2Inserted = false;

	public Scheduler(int timeSlice, Process p0, Process p1, Process p2) {
		processId = new Integer[3];
		burstTime = new Integer[3];
		arrivalTime = new Integer[3];
		this.timeSlice = timeSlice;
		this.counterSlice = timeSlice;
		this.p0 = p0;
		this.p1 = p1;
		this.p2 = p2;
		this.arrivalTime[0]=p0.arrivalTime;
		this.arrivalTime[1]=p1.arrivalTime;
		this.arrivalTime[2]=p2.arrivalTime;
		this.readyQueue = new LinkedList<Integer>();
		this.blockedQueue = new LinkedList<Integer>();
	}

	public void roundRobin() throws IOException {
		while (finished < 3) {
			System.out.println("--------------------------New Cycle----------------------------------");
			System.out.println("clk=" + clk);
			
			createArrivingProcess();
			checkTimeSlice();
			printQueues();
			
			int memoryStartIndex = 0;
			// it is process0 in hand
			if (p0.pcb!=null && readyQueue.peek()==p0.pcb.getProcess_id()) {
				System.out.println("The process is: p0");
				p0.pcb.setState(State.RUNNING);
				updateMyState(p0,State.RUNNING);
				getProcessToMemory(p0);
				if (p0.pcb.getProcess_id() == (int) Handler.memory[1]) {
					memoryStartIndex = 0;
				} else if (p0.pcb.getProcess_id() == (int) Handler.memory[21]) {
					memoryStartIndex = 20;
				}
				startExecution(p0, memoryStartIndex, 0);
			}
			// it is process1 in hand
			else if (p1.pcb!=null && readyQueue.peek()==p1.pcb.getProcess_id()) {
				p1.pcb.setState(State.RUNNING);
				updateMyState(p1,State.RUNNING);
				System.out.println("The process is: p1");
				getProcessToMemory(p1);
				if (p1.pcb.getProcess_id() == (int) Handler.memory[1]) {
					memoryStartIndex = 0;
				}
				else if (p1.pcb.getProcess_id() == (int) Handler.memory[21]) {
					memoryStartIndex = 20;
				}
				startExecution(p1, memoryStartIndex, 1);
			}
			// if process2 is in hand
			else if (p2.pcb!=null && readyQueue.peek()==p2.pcb.getProcess_id()) {
				p2.pcb.setState(State.RUNNING);
				updateMyState(p2,State.RUNNING);
				System.out.println("The process is: p2");
				getProcessToMemory(p2);
				if (p2.pcb.getProcess_id() == (int) Handler.memory[1]) {					
					memoryStartIndex = 0;
				}
				else if (p2.pcb.getProcess_id() == (int) Handler.memory[21]) {
					memoryStartIndex = 20;
				}
				startExecution(p2, memoryStartIndex, 2);
			}
			
			printMemory();
			if (success) {
				clk++;
				counterSlice--;
				System.out.println(success + " " + counterSlice + "");
			}
			printQueues();
			success = false;
		}
	}
	
	public void displayQueue(String s) {
		if (s.equals("ready")) {
			for (Object o : readyQueue) {
				System.out.print(o+", ");
			}
		} else {
			for (Object o : blockedQueue) {
				System.out.print(o+", ");
			}
		}
	}
	
	public void printQueues() {
		System.out.print("Ready queue: ");
		displayQueue("ready");
		System.out.println();
		System.out.print("Blocked queue: ");
		displayQueue("blocked");
		System.out.println();
	}
	
	public void printMemory() {
		System.out.println("______________________________________");
		System.out.println("MEMORY");
		for (Object o : Handler.memory) {
			System.out.println(o);
		}
		System.out.println("______________________________________");
	}
	
	public void createArrivingProcess() throws IOException {
		if (this.arrivalTime[0] == clk && !p0Inserted) {
			Handler.createProcess(p0);
			readyQueue.add(p0.pcb.getProcess_id());
			p0Inserted=true;
		}
		if (this.arrivalTime[1] == clk && !p1Inserted) {
			Handler.createProcess(p1);
			readyQueue.add(p1.pcb.getProcess_id());
			p1Inserted=true;
		}
		if (this.arrivalTime[2]== clk && !p2Inserted) {
			Handler.createProcess(p2);
			readyQueue.add(p2.pcb.getProcess_id());
			p2Inserted=true;
			
		}
	}
	
	public void checkTimeSlice() {
		if (counterSlice == 0) {
			if(p0.pcb!=null && readyQueue.peek()==p0.pcb.getProcess_id()) {
				p0.pcb.setState(State.READY);
				updateMyState(p0,State.READY);
			}
			if(p1.pcb!=null && readyQueue.peek()==p1.pcb.getProcess_id()) {
				p1.pcb.setState(State.READY);
				updateMyState(p1,State.READY);
			}
			if(p2.pcb!=null && readyQueue.peek()==p2.pcb.getProcess_id()) {
				p2.pcb.setState(State.READY);
				updateMyState(p2,State.READY);
			}
			readyQueue.add(readyQueue.remove());
			counterSlice = timeSlice;
		}
	}
	
	public void getProcessToMemory(Process p) throws IOException {
		if (!(p.pcb.getProcess_id() == (int) Handler.memory[1] || p.pcb.getProcess_id() == (int) Handler.memory[21])) {
			// not found in memory and i put it in memory
			System.out.println("not in memory");
			Handler.swapMemoryAndDisk();
			if(!blockedQueue.contains(p.pcb.getProcess_id())) {
				if (p0.pcb.getProcess_id() == (int) Handler.memory[1]) {
					Handler.memory[0]= State.READY;
				}
				else if (p.pcb.getProcess_id() == (int) Handler.memory[21]) {
					Handler.memory[20]= State.READY;
				}
			}
		}
	}
	
	public void startExecution(Process p, int memoryStartIndex, int processNumber) throws IOException {
		if (Handler.execute((String) Handler.memory[memoryStartIndex + 8 + p.pcb.getPc()], p)) {
			System.out.println("The instruction is: "+ Handler.memory[memoryStartIndex + 8 + p.pcb.getPc()]);
			// 3 cases to increment pc
			// contains input and input increment flag is true
			if (((String) Handler.memory[memoryStartIndex + 8 + p.pcb.getPc()]).contains("input") && Handler.userInputIncPcFlag) {
				p.pcb.setPc(p.pcb.getPc() + 1);
				Handler.memory[memoryStartIndex + 2]= p.pcb.getPc();
			}
			// contains readfile and readfile increment flag is true
			else if	(((String) Handler.memory[memoryStartIndex + 8 + p.pcb.getPc()]).contains("readFile") && Handler.readFileIncPcFlag) {
				p.pcb.setPc(p.pcb.getPc() + 1);
				Handler.memory[memoryStartIndex + 2]= p.pcb.getPc();
			// does not contain neither input or readfile
			} else if (!((String) Handler.memory[memoryStartIndex + 8 + p.pcb.getPc()]).contains("input") && !((String) Handler.memory[memoryStartIndex + 8 + p.pcb.getPc()]).contains("readFile")) {
				p.pcb.setPc(p.pcb.getPc() + 1);
				Handler.memory[memoryStartIndex + 2]= p.pcb.getPc();
			}
			this.burstTime[processNumber]--;
			success=true;
			if(this.burstTime[processNumber]==0) {
				p.pcb.setState(State.FINISHED);
				readyQueue.remove(p.pcb.getProcess_id());
				Handler.memory[memoryStartIndex]= "FINISHED";
				finished++;
				counterSlice=timeSlice+1;
			}
		} else {
			blockedQueue.add(p.pcb.getProcess_id());
			readyQueue.remove(p.pcb.getProcess_id());
			Handler.memory[memoryStartIndex]= "BLOCKED";
			p.pcb.setState(State.BLOCKED);
		}
	}
	
	public static void updateMyState(Process p, State s) {
		if (p.pcb.getProcess_id() == (int) Handler.memory[1]) {
			Handler.memory[0]= s;
		}
		else if (p.pcb.getProcess_id() == (int) Handler.memory[21]) {
			Handler.memory[20]= s;
		}
	}

}
