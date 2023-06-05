
public class Variable extends Object {
	String name;
	Object value;
	
	public Variable(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	public String toString(){
		return name + " = " + value;
	}

}
