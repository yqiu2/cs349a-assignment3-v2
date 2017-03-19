import java.util.ArrayList;
import java.util.Collections;

public class Tester {

	ArrayList<String> neighborIPs;
	String localIP = "a";
	HashMap<String,String> neighbors;

	public Tester() {
		neighborIPs = new ArrayList<String>();
		neighborIPs.add("c");
		neighborIPs.add("a");
		neighborIPs.add("b");
	}

	public String nextIP() {
		ArrayList<String> sortedIPs = new ArrayList<String>();
		sortedIPs.addAll(0, neighborIPs);
		Collections.sort(sortedIPs);

		int currentIndex = sortedIPs.indexOf(localIP);
		if (currentIndex + 1 == sortedIPs.size()) {
			return neighbors.get(sortedIPs.get(0));
		} else {
			return neighbors.get(sortedIPs.get(currentIndex++));
		}
	}

	public static void main(String[] args) {
		Tester test = new Tester();
		System.out.println(test.nextIP());
	}

}
