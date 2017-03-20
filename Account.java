import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.util.*;
import java.rmi.server.UnicastRemoteObject;

public class Account implements AccountInt {
	boolean leaderConfirmed;
	boolean startCommunication;
	String localIP;
	// array list of stubs of neighbors
	HashMap<String, AccountInt> neighbors;
	ArrayList<String> neighborIPs;
	ArrayList<AccountInt> neighborStubs;
	AccountInt nextNeighborStub;
	// balance
	int balance;

	public Account() {
		leaderConfirmed = false;
		startCommunication = false;
		localIP = "";
		neighborIPs = new ArrayList<String>();
		neighborStubs = new ArrayList<AccountInt>();
		neighbors = new HashMap<String, AccountInt>();
		balance = 200;
		nextNeighborStub = null;
	}

	// 1) bootstrapping:
	public void receiveIPs(ArrayList<String> ipAddresses) {
		System.out.println("I got a recieveIPs message!");
		System.out.println("I am " + localIP);
		try {
			for (int i = 0; i < ipAddresses.size(); i++) {
				String remoteHost = ipAddresses.get(i);
				System.out.println("is " + remoteHost + "my neighbor?");
				if (!remoteHost.equals(localIP)) {
					System.out.println("Yes, " + remoteHost + " is my neighbor");
					Registry remoteRegistry = LocateRegistry.getRegistry(remoteHost);
					AccountInt remoteStub = (AccountInt) remoteRegistry.lookup("Account");
					System.out.println("I have found the stub of the remote host: " + remoteHost);
					// save the stubs
					neighborIPs.add(remoteHost); // save the identification of
													// the neighbors
					neighborStubs.add(remoteStub);
					System.out.println(remoteHost + " has been added as a neighbor");
				}
			}
			startCommunication = true; // the cue to start communicating with
										// other clients
			System.out.println("start communicating with other machines " + neighborIPs.toString());
		} catch (Exception e) {
			System.err.println("Client exception(receiveIPs could not find remote Account): " + e.toString());
			e.printStackTrace();
		}
	}

	// 2) sending and receiving money between clients
	public void receiveMoney(int amount, String senderIP) {
		System.out.println("You've received $" + amount + " from " + senderIP + ": ");
		balance += amount;
		System.out.println("\tYour new balance is $" + balance);
	}

	private void sendMoney() throws RemoteException, InterruptedException {
		while (true) {
			if (startCommunication) {
				// wait time to start transfer
				int r = 5000 + (int) (Math.random() * 50000);
				// amount transferred
				int m = 1 + (int) (Math.random() * balance);
				if (m < 0)
					m = 0;
				int p = -1;
				String recipientIP;
				AccountInt recipientStub;
				do {
					p = (int) (Math.random() * neighborIPs.size());
					recipientIP = neighborIPs.get(p);
					recipientStub = neighborStubs.get(p);
				} while (recipientIP.equals(localIP));

				// sending money to another account
				System.out.println("\n***Waiting for " + r / 1000 + " seconds...***\n");
				Thread.sleep(r);
				System.out.println("sending $" + m + " to " + recipientIP);
				balance -= m;
				recipientStub.receiveMoney(m, localIP);
				System.out.println("\t this account balance is $" + balance);
			}
		}
	}

	// 3) leader election
	private void sendBallot(String candidate, int numMessagesPassed, boolean leaderConfirmed) {
		try {
			if (nextNeighborStub == null) {
				// add items into hashmap for convenience
				for (int i = 0; i < neighborIPs.size(); i++) {
					neighbors.put(neighborIPs.get(i), neighborStubs.get(i));
				}
				// let's find your next neighbor for #convenience
				nextStub();
			}

			AccountInt recipientStub = this.nextNeighborStub;
			recipientStub.receiveBallot(candidate, numMessagesPassed, leaderConfirmed);
		} catch (Exception e) {
			System.err.println("Client exception(sendBallot() could not find stub): " + e.toString());
			e.printStackTrace();
		}
	}

	public void receiveBallot(String candidate, int numMessagesPassed, boolean leaderConfirmed) throws RemoteException {
		try{
		Thread.sleep(5000);
		}
		catch (Exception e) {
			System.err.println("Sleeping in receiveballot " + e.toString());
			e.printStackTrace();
		}
		numMessagesPassed++;
		if (leaderConfirmed && candidate.equals(localIP)) {
			System.out.println("The leader is " + candidate + "!");
			numMessagesPassed--;
			System.out.println("This was confirmed after " + numMessagesPassed + " messages were passed.");

		} else if (candidate.equals(localIP)) {
			System.out.println("the leader is " + candidate + "and I'm sending out the confirmations");
			sendBallot(candidate, numMessagesPassed, true);
		} else { // received candidate != local IP
			System.out.println(candidate.compareTo(localIP));
			if (candidate.compareTo(localIP) < 0) {
				// set candidate to be local IP
				System.out.println("leader not confirmed yet, recieved " + candidate + "new candidate" + localIP);
				sendBallot(localIP, numMessagesPassed, false);
			} else {
				// candidate is still candidate
				System.out
						.println("leader not confirmed yet, recieved " + candidate + "new candidate remains the same");
				sendBallot(candidate, numMessagesPassed, false);
			}
		}
	}

	// helper method to find next neighbor of current IP
	public void nextStub() {
		ArrayList<String> sortedIPs = new ArrayList<String>();
		sortedIPs.addAll(neighborIPs);
		sortedIPs.add(localIP);
		Collections.sort(sortedIPs);
//		System.out.println("***\n***");
//		System.out.println("sortedIPs");
//		System.out.println(sortedIPs.toString());
//		System.out.println("neighbors hashmap");
//		System.out.println(neighbors.toString());

		int currentIndex = sortedIPs.indexOf(localIP);
		System.out.println("currentIndex is " + currentIndex + " and the size is " + sortedIPs.size());

		if (currentIndex + 1 == sortedIPs.size()) {
//			System.out.println("current index is 0");
//			System.out.println("sortedIPs.get(0) " + sortedIPs.get(0));
			// System.out.println("setting nextneighborstub to " +
			// neighbors.get(sortedIPs.get(0)).toString());
			nextNeighborStub = neighbors.get(sortedIPs.get(0));
		} else {
//			System.out.println("current index is " + currentIndex + " +1");
			currentIndex++;
			// System.out.println("sortedIPs.get(currentIndex) " +
			// sortedIPs.get(currentIndex));
			nextNeighborStub = neighbors.get(sortedIPs.get(currentIndex));
		}

//		System.out.println("the nxt neighbor stub " + nextNeighborStub.toString());
//		if (neighborStubs.contains(nextNeighborStub)) {
//			System.out.println("yes, we found a stub");
//		}
	}

	// 4) snapshotting
	public void receiveStartSnapshot(String leader, String sender, String recipient) {
		// records the state of C as an empty set
		// execute the marker sending rule
	}

	public void receiveSnapshot(String sender, int amount, ArrayList<ArrayList<Integer>> channels) {
		// Record the state of C as the set of messages received along C after
		// pj's state was recorded and before pj received the marker along C

	}

	public void sendSnapshot(String sender, int amount, ArrayList<ArrayList<Integer>> channels) {
		// process records its state
		// sends a marker along C before sending further message down C
	}

	public static void main(String[] args) {
		Account obj = new Account();

		try {
			AccountInt stub = (AccountInt) UnicastRemoteObject.exportObject(obj, 0);
			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry();
			registry.bind("Account", stub);
			System.out.println("Account has been bound to RMI registry");
		} catch (Exception e) {
			System.err.println("Client exception(could not register itself): " + e.toString());
			e.printStackTrace();

		}

		obj.localIP = args[1];

		for (int i = 1; i < args.length; i++) {
			obj.neighborIPs.add(args[i]);
		}

		if (obj.neighborIPs.size() > 1) {
			System.out.println("this should only happen on last client");
			try {
				// send out all of the IP addresses to all the other accounts
				for (int i = 0; i < obj.neighborIPs.size(); i++) {
					String remoteHost = obj.neighborIPs.get(i);
					if (!remoteHost.equals(obj.localIP)) {
						Registry remoteRegistry = LocateRegistry.getRegistry(remoteHost);
						AccountInt remoteStub = (AccountInt) remoteRegistry.lookup("Account");
						System.out.println(remoteHost + " has been bound to a stub");
						obj.neighborStubs.add(remoteStub);
						System.out.println("calling receiveIPs on " + remoteHost);
						remoteStub.receiveIPs(obj.neighborIPs);
					} else {
						System.out.println("not calling receiveIP on myself");
					}
				}
				obj.startCommunication = true;
			} catch (Exception e) {
				System.err.println("Client exception(could not find remote Account): " + e.toString());
				e.printStackTrace();
			}
		}
		// remove yourself
		obj.neighborIPs.remove(0);

		// waiting for start communication
		while (!obj.startCommunication) {
			try {
				System.out.println("waiting for startCommunication to become true");
				Thread.sleep(10000);

			} catch (Exception e) {
				System.err.println("error with waiting " + e.toString());
				e.printStackTrace();
			}
		}

		if (args[0].equals("1")) {
			System.out.println("This process is the leader initiator");
			try {
				obj.sendBallot(obj.localIP, 0, false);
			} catch (Exception e) {
				System.err.println("Unable to send ballot " + e.toString());
				e.printStackTrace();
			}
		} else {
			System.out.println("This process is NOT the leader initiator.");
		}

		// waiting for leader confirmed
		while (!obj.leaderConfirmed) {
			try {
				System.out.println("waiting for leaderConfirmed to become true");
				Thread.sleep(10000);

			} catch (Exception e) {
				System.err.println("error with waiting for leader election" + e.toString());
				e.printStackTrace();
			}
		}

		if (obj.leaderConfirmed) {
			try {
				obj.sendMoney();
			} catch (Exception e) {
				System.err.println("Client exception(could not send money): " + e.toString());
				e.printStackTrace();
			}
		}

	}

}