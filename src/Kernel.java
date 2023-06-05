import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class Kernel {

	public String readFile(String name) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(name));
		String st;
		st = br.readLine();
		return st;
	}

	public void writeFile(String name, String data) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(name, true)));
		out.println(data);
		out.close();
	}

	public void printOnScreen(String data) {
		System.out.println("Print  " + data);
	}

	public String userInput() {
		System.out.println("PLEASE ENTER A VALUE");
		Scanner sc = new Scanner(System.in);
		String input = sc.next();
		return input;
	}

}
