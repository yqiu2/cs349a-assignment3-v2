
//import java.rmi.Remote;
//import java.rmi.RemoteException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.util.*;
import java.rmi.server.UnicastRemoteObject;

public class Account implements AccountInt {
	boolean startCommunication;
	String localIP;
	// array list of stubs of neighbors
	ArrayList<String> neighborIPs;
	ArrayList<AccountInt> neighborStubs;
	// balance
	int balance;

	public Account() {
		startCommunication = false;
		localIP = "";
		neighborIPs = new ArrayList<String>();
		neighborStubs = new ArrayList<AccountInt>();
		balance = 200;
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

	//PLZ CHECK LOGIC
	public int sendBallot(String candidate, int numMessagesPassed, boolean leaderConfirmed) {
		if (startCommunication) {
			while (!leaderConfirmed) {
				//DID I DO THIS RIGHT?
				Account recipientStub = (Account)neighborStubs.get(numMessagesPassed);
				
				// parses to int, replaces all punctuation, creates a substring of
				// the last three characters so that IPs are comparable :p
				int candidateInt = Integer.parseInt(candidate.replaceAll("[^a-zA-Z ]", "")
						.substring(candidate.length() - 3, candidate.length()));
				int runnerUp = Integer.parseInt(neighborIPs.get(numMessagesPassed).replaceAll("[^a-zA-Z ]", "")
						.substring(neighborIPs.get(numMessagesPassed).length() - 3, neighborIPs.get(numMessagesPassed).length()));
				
				if (numMessagesPassed == neighborIPs.size()){ 
					//because you can only pass as many messages as there are clients in the list
					leaderConfirmed = true;
				}
				else if (candidateInt < runnerUp){
					recipientStub.sendBallot(neighborIPs.get(numMessagesPassed), numMessagesPassed++, false);
				}
				else if (candidateInt > runnerUp || candidateInt == runnerUp){
					recipientStub.sendBallot(candidate, numMessagesPassed++, false);
				}
			}
		}	
		System.out.println("The leader is " + candidate + "!");
		return numMessagesPassed - 1; //-1 because you will always check yourself
		//is this what we want to return or should we be returning something else?
	}

	/*
	 * // 4) snapshotting void receiveStartSnapshot(String leader, String
	 * sender, String recipient) throws RemoteException; void
	 * receiveSnapshot(String sender, int amount, ArrayList<ArrayList<Integer>>
	 * channels) throws RemoteException;
	 */

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

		obj.localIP = args[0];
		for (int i = 0; i < args.length; i++) {
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

		obj.neighborIPs.remove(0);
		// start to send money
		while (!obj.startCommunication) {
			try {
				System.out.println("waiting for startCommunication to become true");
				Thread.sleep(10000);
			} catch (Exception e) {
				System.err.println("error with waiting " + e.toString());
				e.printStackTrace();
			}
		}

		try {
			obj.sendMoney();
		} catch (Exception e) {
			System.err.println("Client exception(could not send money): " + e.toString());
			e.printStackTrace();
		}

	}

}