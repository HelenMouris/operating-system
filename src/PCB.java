public class PCB extends Object {
	private int process_id;
	private State state;
	private int pc;
	private int memory_start;
	private int memory_end;
	
	public PCB(State state, int id, int start, int end) {
		process_id = id;
		this.pc=0;
		this.state=state;
		memory_start= start;
		memory_end= end;
	}

	public int getProcess_id() {
		return process_id;
	}

	public void setProcess_id(int process_id) {
		this.process_id = process_id;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public int getPc() {
		return pc;
	}

	public void setPc(int pc) {
		this.pc = pc;
	}

	public int getMemory_start() {
		return memory_start;
	}

	public void setMemory_start(int memory_start) {
		this.memory_start = memory_start;
	}

	public int getMemory_end() {
		return memory_end;
	}

	public void setMemory_end(int memory_end) {
		this.memory_end = memory_end;
	}

}
