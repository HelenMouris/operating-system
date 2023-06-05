import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Queue;

public class Handler {

	public static Object[] memory;
	public static Kernel kernel;
	public static Mutex userInputMutex;
	public static Mutex userOutputMutex;
	public static Mutex fileMutex;
	public static int pcbID;
	public static Scheduler scheduler;
	public static int insertCounter;
	public static int burstTemp = 0;

	public static String userInputToBeAssigned;
	public static String readFileToBeAssigned;
	public static boolean userInputIncPcFlag = true;
	public static boolean readFileIncPcFlag = true;

	public Handler() {
		memory = new Object[40];
		kernel = new Kernel();
		userInputMutex = new Mutex();
		userOutputMutex = new Mutex();
		fileMutex = new Mutex();
		pcbID = 0;
		insertCounter = 0;
	}

	public static void createProcess(Process process) throws IOException {
		boolean blocked = false;
		if (spaceForMe()) {
			process.pcb = loadProcesstoMemory(process.path);
			scheduler.processId[process.pcb.getProcess_id()] = pcbID;
			scheduler.burstTime[process.pcb.getProcess_id()] = burstTemp;
		} else {
			// choose which process to remove : 1- check if a process is finished. 2- check if a process is blocked 3- remove any one (the first)
			int index = getIndexOfProcessToRemove();
			System.out.println("Process with ID: "+ memory[index+1] + "swapped into disk");
			// remove the chosen process and put it to the disk
			for (int n = index; n <= (index + 19); n++) {
				String temp = objectToString(memory[n]);
				saveToDisk(temp);
			}
			process.pcb = loadProcesstoMemory(process.path);
			scheduler.processId[process.pcb.getProcess_id()] = pcbID;
			scheduler.burstTime[process.pcb.getProcess_id()] = burstTemp;
		}
	}

	// function that puts the process into memory for the first time (from the program.txt)
	public static PCB loadProcesstoMemory(String path) throws IOException {
		File file = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String st;
		int start = getIndexOfProcessToRemove();
		if (path.contains("1")) {
			pcbID=0;
		}
		else if (path.contains("2")) {
			pcbID=1;
		}
		else if (path.contains("3")) {
			pcbID=2;
		}
		PCB pcb = new PCB(State.READY, pcbID, start, start + 19);

		memory[start] = pcb.getState();
		memory[start + 1] = pcb.getProcess_id();
		memory[start + 2] = pcb.getPc();
		memory[start + 3] = pcb.getMemory_start();
		memory[start + 4] = pcb.getMemory_end();
		memory[start + 5] = new Variable("", null);
		memory[start + 6] = new Variable("", null);
		memory[start + 7] = new Variable("", null);
		int i = 8;
		int burstTime = 0;
		while ((st = br.readLine()) != null) {
			memory[start + i] = st;
			if ((st.contains("assign") && st.contains("readFile"))  || (st.contains("assign") && st.contains("input"))) {
				burstTime++;
			}
			i++;
			burstTime++;
		}
		burstTemp = burstTime;
		return pcb;
	}

	// function that takes a string and write it on the disk
	public static void saveToDisk(String str) throws IOException {
		FileWriter file = new FileWriter("harddisk.txt", true);
		BufferedWriter writer = new BufferedWriter(file);
		PrintWriter out = new PrintWriter(writer);
		out.println(str);
		out.close();
	}

	public static int getIndexOfProcessToRemove() {
		if (memory[0] == null || memory[20] == null) {
			return memory[0] == null ? 0 : 20;
		} else if (memory[0].equals(State.FINISHED) || memory[20].equals(State.FINISHED)) {
			return memory[0].equals(State.FINISHED) ? 0 : 20;
		} else if (memory[0].equals(State.BLOCKED) || memory[20].equals(State.BLOCKED)) {
			return memory[0].equals(State.BLOCKED) ? 0 : 20;
		}
		return 0;
	}

	// functions to swap between memory and hardDisk by taking start index of process
	public static void swapMemoryAndDisk() throws IOException {
		// remove from the disk
		ArrayList<String> diskContents = diskToMemory();
		int index = getIndexOfProcessToRemove();
		// remove the chosen process and put it to the disk
		System.out.println("process with ID: " + memory[index+1] + "swapped into disk");
		for (int n = index; n <= (index + 19); n++) {
			String temp = objectToString(memory[n]);
			saveToDisk(temp);
		}
		// put disk contents to the memory
		int i = 0;
		System.out.println("process with ID: " + diskContents.get(1) + "swapped into memory");
		for (int n = index; n <= (index + 19); n++) {
			if (i < diskContents.size()) {
				if (i == 0) { // pcb state
					memory[n] = stringToState(diskContents.get(i));
				} else if (i == 1 || i == 2 || i == 3 || i == 4) { // pcb int attributes
					memory[n] = Integer.parseInt(diskContents.get(i));
				} else if (i == 5 || i == 6 || i == 7) { // variables
					String[] variable = diskContents.get(i).split("=");
					memory[n] = new Variable(variable[0], variable[1]);
				} else {
					memory[n] = diskContents.get(i);
				}
				i++;
			}
		}
	}

	public static ArrayList<String> diskToMemory() throws IOException {
		ArrayList<String> diskContents = new ArrayList<String>();
		File file = new File("harddisk.txt");
		BufferedReader br = new BufferedReader(new FileReader(file));
		String st;
		while ((st = br.readLine()) != null) {
			diskContents.add(st);
		}
		PrintWriter writer = new PrintWriter("harddisk.txt");
		writer.print("");
		writer.close();
		return diskContents;
	}

	// returns true if the instruction is executed succesfully and false if it is not succesful (blocked)
	public static boolean execute(String instruction, Process process) throws IOException {
		String[] splitted = instruction.split("\\s+");
		switch (splitted[0]) {
		case "print": {
			// if the mutex is taken by me
			if (userOutputMutex.value && userOutputMutex.processID == process.pcb.getProcess_id()) {
				String value = getVariableValue(splitted[1], process);
				kernel.printOnScreen(value);
				return true;
			}
			userOutputMutex.myBlockedQueue.add(process.pcb.getProcess_id());
			System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over userOutputMutex");
			System.out.print("userOutputMutex blocked Queue={");
			for (int i : userOutputMutex.myBlockedQueue) {
				System.out.print(i + ", ");
			}
			System.out.println("}");
			return false;
		}
		case "assign": {
			// check if requires 2 clocks --> in first don't increment pc and excute the read/input
			// in second increment pc and excute the assign
			Variable v;
			if (splitted[2].equals("readFile")) {
				if (readFileIncPcFlag) { // first part (first clock)
					if (fileMutex.value && fileMutex.processID == process.pcb.getProcess_id()) {
						String path = getVariableValue(splitted[3], process);
						readFileToBeAssigned = kernel.readFile(path);
						readFileIncPcFlag = false;
						return true;
					}
					fileMutex.myBlockedQueue.add(process.pcb.getProcess_id());
					System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over fileMutex");
					System.out.print("fileMutex blocked Queue={");
					for (int i : fileMutex.myBlockedQueue) {
						System.out.print(i + ", ");
					}
					System.out.println("}");
					return false;
				} else { // assign part (second clock)
					v = new Variable(splitted[1], readFileToBeAssigned);
					process.variables.add(v);
					addVariableToMemory(v, process);
					readFileIncPcFlag = true;
					return true;
				}
			}
			else if (splitted[2].equals("input")) {
				if (userInputIncPcFlag) { //{first part (first clock)
					if(userInputMutex.value && userInputMutex.processID==process.pcb.getProcess_id()){
						userInputToBeAssigned = kernel.userInput();
						userInputIncPcFlag = false;
						return true;
					}
					userInputMutex.myBlockedQueue.add(process.pcb.getProcess_id());
					System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over userInputMutex");
					System.out.print("userInputMutex blocked Queue={");
					for (int i : userInputMutex.myBlockedQueue) {
						System.out.print(i + ", ");
					}
					System.out.println("}");
					return false;
				} else { // assign part (second clock)
					v = new Variable(splitted[1], userInputToBeAssigned);
					process.variables.add(v);
					addVariableToMemory(v,process);
					userInputIncPcFlag = true;
					return true;
				}
			}
			else {
				v = new Variable(splitted[1], splitted[2]);
				process.variables.add(v);
				addVariableToMemory(v,process);
				return true;
			}
		}
		case "writeFile": {
			if (fileMutex.value && fileMutex.processID == process.pcb.getProcess_id()) {
				String path = getVariableValue(splitted[1], process);
				String value = getVariableValue(splitted[2], process);
				kernel.writeFile(path, value);
				return true;
			}
			fileMutex.myBlockedQueue.add(process.pcb.getProcess_id());
			System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over fileMutex");
			System.out.print("fileMutex blocked Queue={");
			for (int i : userOutputMutex.myBlockedQueue) {
				System.out.print(i + ", ");
			}
			System.out.println("}");
			return false;
		}
		case "readFile": {
			if (fileMutex.value && fileMutex.processID == process.pcb.getProcess_id()) {
				String path = getVariableValue(splitted[1], process);
				kernel.readFile(path);
				return true;
			}
			fileMutex.myBlockedQueue.add(process.pcb.getProcess_id());
			System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over fileMutex");
			System.out.print("fileMutex blocked Queue={");
			for (int i : fileMutex.myBlockedQueue) {
				System.out.print(i + ", ");
			}
			System.out.println("}");
			return false;
		}
		case "printFromTo": {
			if (userOutputMutex.value && userOutputMutex.processID == process.pcb.getProcess_id()) {
				int from = getIntValue(splitted[1], process);
				int to = getIntValue(splitted[2], process);
				for (int i = from; i <= to; i++) {
					kernel.printOnScreen(i + "");
				}
				return true;
			}
			userOutputMutex.myBlockedQueue.add(process.pcb.getProcess_id());
			System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over userOutputMutex");
			System.out.print("userOutputMutex blocked Queue={");
			for (int i : userOutputMutex.myBlockedQueue) {
				System.out.print(i + ", ");
			}
			System.out.println("}");
			return false;
		}
		case "semWait": {
			if (splitted[1].equals("userInput")) {
				if (!userInputMutex.value) {
					userInputMutex.acquire(process.pcb.getProcess_id());
					System.out.println("p" + process.pcb.getProcess_id() + " took the userInputMutex");
					return true;
				}
				userInputMutex.myBlockedQueue.add(process.pcb.getProcess_id());
				System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over userInputMutex");
				System.out.print("userInputMutex blocked Queue={");
				for (int i : userInputMutex.myBlockedQueue) {
					System.out.print(i + ", ");
				}
				System.out.println("}");
				return false;
			} else {
				if (splitted[1].equals("userOutput")) {
					if (!userOutputMutex.value) {
						userOutputMutex.acquire(process.pcb.getProcess_id());
						System.out.println("p" + process.pcb.getProcess_id() + " took the userOutputMutex");
						return true;
					}
					userOutputMutex.myBlockedQueue.add(process.pcb.getProcess_id());
					System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over userOutputMutex");
					System.out.print("userOutputMutex blocked Queue={");
					for (int i : userOutputMutex.myBlockedQueue) {
						System.out.print(i + ", ");
					}
					System.out.println("}");
					return false;
				} else if (splitted[1].equals("file")) {
					if (!fileMutex.value) {
						fileMutex.acquire(process.pcb.getProcess_id());
						System.out.println("p" + process.pcb.getProcess_id() + " took the fileMutex");
						return true;
					}
					fileMutex.myBlockedQueue.add(process.pcb.getProcess_id());
					System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over fileMutex");
					System.out.print("fileMutex blocked Queue={");
					for (int i : fileMutex.myBlockedQueue) {
						System.out.print(i + ", ");
					}
					System.out.println("}");
					return false;
				}
			}
			break;
		}
		case "semSignal": {
			if (splitted[1].equals("userInput")) {
				if (userInputMutex.value && userInputMutex.processID == process.pcb.getProcess_id()) {
					System.out.println("ana 3mlt semsignal");
					userInputMutex.release(process.pcb.getProcess_id());
					System.out.println(userInputMutex.myBlockedQueue.size());
					for (int x : userInputMutex.myBlockedQueue) {
						System.out.println(memory[1] + " " + memory[0] + " " + memory[20] + " " + memory[21]);
						if (memory[1].equals(x) && memory[0].equals("BLOCKED")) {
							memory[0] = "READY";
						} else if (memory[21].equals(x) && memory[20].equals("BLOCKED")) {
							System.out.println("we d5alt hena aho");
							memory[20] = "READY";
						}
						scheduler.readyQueue.add(x);
						userInputMutex.myBlockedQueue.remove(x);
						scheduler.blockedQueue.remove(x);
					}
					return true;
				}
				userInputMutex.myBlockedQueue.add(process.pcb.getProcess_id());
				System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over userInputMutex");
				System.out.print("userInputMutex blocked Queue={");
				for (int i : userInputMutex.myBlockedQueue) {
					System.out.print(i + ", ");
				}
				System.out.println("}");
				return false;
			} else {
				if (splitted[1].equals("userOutput")) {
					if (userOutputMutex.value && userOutputMutex.processID == process.pcb.getProcess_id()) {
						userOutputMutex.release(process.pcb.getProcess_id());
						for (int x : userOutputMutex.myBlockedQueue) {
							scheduler.readyQueue.add(x);
							userOutputMutex.myBlockedQueue.remove(x);
							scheduler.blockedQueue.remove(x);
							if (memory[1].equals(x + "") && memory[0] == State.BLOCKED) {
								memory[0] = State.READY;
							} else if (memory[21].equals(x + "") && memory[20] == State.BLOCKED) {
								memory[20] = State.READY;
							}
						}
						return true;
					}
					userOutputMutex.myBlockedQueue.add(process.pcb.getProcess_id());
					System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over userOutputMutex");
					System.out.print("userOutputMutex blocked Queue={");
					for (int i : userOutputMutex.myBlockedQueue) {
						System.out.print(i + ", ");
					}
					System.out.println("}");
					return false;
				} else if (splitted[1].equals("file")) {
					if (fileMutex.value && fileMutex.processID == process.pcb.getProcess_id()) {
						fileMutex.release(process.pcb.getProcess_id());
						for (int x : fileMutex.myBlockedQueue) {
							scheduler.readyQueue.add(x);
							fileMutex.myBlockedQueue.remove(x);
							scheduler.blockedQueue.remove(x);
							if (memory[1].equals(x + "") && memory[0] == State.BLOCKED) {
								memory[0] = State.READY;
							} else if (memory[21].equals(x + "") && memory[20] == State.BLOCKED) {
								memory[20] = State.READY;
							}
						}
						return true;
					}
					fileMutex.myBlockedQueue.add(process.pcb.getProcess_id());
					System.out.println("p" + process.pcb.getProcess_id() + " got BLOCKED over fileMutex");
					System.out.print("filetMutex blocked Queue={");
					for (int i : fileMutex.myBlockedQueue) {
						System.out.print(i + ", ");
					}
					System.out.println("}");
					return false;
				}
			}
		}
		}
		return false;
	}

	public static boolean spaceForMe() {
		if (memory[0] != (null) && memory[20] != (null)) {
			return false;
		} else
			return true;
	}

	public static String objectToString(Object o) {
		String res = "";
		if (o instanceof State) {
			res = stateToString((State) o);
		} else if (o instanceof Variable) {
			res = ((Variable) o).name + "=" + ((Variable) o).value;
		} else {
			res = "" + o;
		}
		return res;
	}

	public static String stateToString(State state) {
		String res = null;
		if (state.equals(State.READY)) {
			res = "READY";
		}
		if (state.equals(State.BLOCKED)) {
			res = "BLOCKED";
		}
		if (state.equals(State.FINISHED)) {
			res = "FINISHED";
		}
		if (state.equals(State.RUNNING)) {
			res = "RUNNING";
		}
		return res;
	}

	public static State stringToState(String state) {
		State res = null;
		if (state.equals("READY")) {
			res = State.READY;
		}
		if (state.equals("BLOCKED")) {
			res = State.BLOCKED;
		}
		if (state.equals("FINISHED")) {
			res = State.FINISHED;
		}
		if (state.equals("RUNNING")) {
			res = State.RUNNING;
		}
		return res;
	}

	public static void addVariableToMemory(Variable v, Process p) {
		// trying to override the variable if it is already stored in memory
		boolean override = false;
		int start = 1;
		if (memory[1].equals(p.pcb.getProcess_id())) {
			start = 5;
		} else if (memory[21].equals(p.pcb.getProcess_id())) {
			start = 25;
		}
		for (int i = start; i < start + 3; i++) {
			if (((Variable) memory[i]).name.equals(v.name)) {
				((Variable) memory[i]).value = v.value;
				override = true;
			}
		}
		// variable is not already in memory and we assign it for the first time
		if (!override) {
			for (int i = start; i < start + 3; i++) {
				if (((Variable) memory[i]).name.equals("")) {
					((Variable) memory[i]).name = v.name;
					((Variable) memory[i]).value = v.value;
					break;
				}
			}
		}
	}

	public static int getIntValue(String s, Process p) {
		int value = 0;
		try {
			value = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			int start = 1;
			if (memory[1].equals(p.pcb.getProcess_id())) {
				start = 5;
			} else if (memory[21].equals(p.pcb.getProcess_id())) {
				start = 25;
			}
			for (int i = start; i < start + 3; i++) {
				if (((Variable) memory[i]).name.equals(s)) {
					value = Integer.parseInt("" + ((Variable) memory[i]).value);
				}
			}
		}
		return value;
	}

	public static String getVariableValue(String s, Process p) {
		String value = "";
		int start = 1;
		if (memory[1].equals(p.pcb.getProcess_id())) {
			start = 5;
		} else if (memory[21].equals(p.pcb.getProcess_id())) {
			start = 25;
		}
		for (int i = start; i < start + 3; i++) {
			if (((Variable) memory[i]).name.equals(s)) {
				value = "" + ((Variable) memory[i]).value;
			}
		}
		return value;
	}

	public static void main(String[] args) throws IOException {

		// We have the three processes with their arrival time
		Handler hand = new Handler();
		Process process1 = new Process("Program_1.txt", 0);
		Process process2 = new Process("Program_2.txt", 1);
		Process process3 = new Process("Program_3.txt", 4);
		hand.scheduler = new Scheduler(2, process1, process2, process3);
		hand.scheduler.roundRobin();	

	}

}
