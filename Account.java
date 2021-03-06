import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.util.*;
import java.rmi.server.UnicastRemoteObject;

public class Account implements AccountInt {

	boolean leaderConfirmed;
	boolean isLeader;
	boolean startCommunication;

	// array list of stubs of neighbors
	HashMap<String, AccountInt> neighbors;
	ArrayList<String> neighborIPs;
	ArrayList<AccountInt> neighborStubs;
	AccountInt nextNeighborStub;

	int balance;
	String localIP;

	HashMap<Integer, HashMap<String, String>> globalSnaps;
	HashMap<Integer, Snapshot> ownSnaps;

	public Account() {
		isLeader = false;
		leaderConfirmed = false;
		startCommunication = false;
		localIP = "";
		neighborIPs = new ArrayList<String>();
		neighborStubs = new ArrayList<AccountInt>();
		neighbors = new HashMap<String, AccountInt>();
		balance = 200;
		nextNeighborStub = null;
		ownSnaps = new HashMap<Integer, Snapshot>();
		globalSnaps = new HashMap<Integer, HashMap<String, String>>();

	}

	// 1) bootstrapping:
	public void receiveIPs(ArrayList<String> ipAddresses) {
		System.out.println("I am " + localIP);
		try {
			for (int i = 0; i < ipAddresses.size(); i++) {
				String remoteHost = ipAddresses.get(i);
				if (!remoteHost.equals(localIP)) {
					Registry remoteRegistry = LocateRegistry.getRegistry(remoteHost);
					AccountInt remoteStub = (AccountInt) remoteRegistry.lookup("Account");
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
		for (Integer snapshotID : ownSnaps.keySet()) {
			if (ownSnaps.get(snapshotID).getChannelState(senderIP)) {
				ownSnaps.get(snapshotID).addMessages(senderIP, amount);
			}
		}
	}

	private void sendMoney() throws RemoteException {
		// while (true) {
		if (startCommunication) {

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

			System.out.println("sending $" + m + " to " + recipientIP);
			balance -= m;
			recipientStub.receiveMoney(m, localIP);
			System.out.println("\t this account balance is $" + balance);
		}
		// }

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
		try {
			System.out.println("sleeping for 2.5 seconds");
			Thread.sleep(2500);
		} catch (Exception e) {
			System.err.println("error in sleeping in receiveballot " + e.toString());
			e.printStackTrace();
		}
		numMessagesPassed++;
		if (candidate.equals(localIP) && leaderConfirmed) {
			this.isLeader = true;
			System.out.println("\n***The leader is " + candidate + "!***");
			numMessagesPassed--;
			System.out.println("\tThis was confirmed after " + numMessagesPassed + " messages were passed.");
		} else if (candidate.equals(localIP) && !leaderConfirmed) {
			this.leaderConfirmed = true;
			System.out.println("the leader is " + candidate + " and I'm sending out the confirmations");
			sendBallot(candidate, numMessagesPassed, true);
		} else if (!candidate.equals(localIP) && leaderConfirmed) {
			this.leaderConfirmed = true;
			System.out.println("passing on the confirmation that " + candidate + " is the leader");
			sendBallot(candidate, numMessagesPassed, true);
		} else {
			if (candidate.compareTo(localIP) < 0) {
				// set candidate to be local IP
				System.out.println("leader not confirmed yet, recieved " + candidate + " new candidate" + localIP);
				sendBallot(localIP, numMessagesPassed, false);
			} else {
				// candidate is still candidate
				System.out
						.println("leader not confirmed yet, recieved " + candidate + " new candidate remains the same");
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
		int currentIndex = sortedIPs.indexOf(localIP);
		if (currentIndex + 1 == sortedIPs.size()) {
			nextNeighborStub = neighbors.get(sortedIPs.get(0));
		} else {
			currentIndex++;
			nextNeighborStub = neighbors.get(sortedIPs.get(currentIndex));
		}
	}

	// 4) snapshotting -- DONT FORGET TO UPDATE THE INTERFACE

	// only run if leader
	private void initSnap() {

		// init new own snapshot
		Snapshot ownSnap = new Snapshot(this.localIP);
		// snap balance
		ownSnap.setBal(this.balance);
		// stores own snap because multiple snaps can occur at once
		ownSnaps.put(ownSnap.getID(), ownSnap);
		System.out.println("created, set, and stored own snapshot" + ownSnap.getID() + ": ");

		System.out.println("Initializing global snapshot storage");
		HashMap<String, String> channelSnapshots = new HashMap<String, String>();
		for (String neighborIP : neighborIPs) {
			channelSnapshots.put(neighborIP, null);
			ownSnap.addMessageChannel(neighborIP);
			
		}
		channelSnapshots.put(localIP, null);
		// adding the snapshot to the global snapshot storage
		System.out.println("adding the snapshot to the global snapshot storage");
		globalSnaps.put(ownSnap.getID(), channelSnapshots);
		System.out.println("initialized global snapshots to be: " + globalSnaps.toString());
		
		
		for (String neighborIP : neighbors.keySet()) {
			try {
				// start recording channel
				System.out.println("start recording channels");
				ownSnap.setRecordingState(neighborIP, true);
				// send markers
				System.out.println("initially sending markers to " + neighborIP + " to start snap: " + ownSnap.getID());
				AccountInt recipientStub = neighbors.get(neighborIP);
				recipientStub.receiveMarker(localIP, localIP, ownSnap.getID());
			} catch (Exception e) {
				System.err.println("error in sending markers in initSnap()" + e.toString());
				e.printStackTrace();
			}
		}
	}

	public void receiveMarker(String leader, String sender, Integer snapID) {
		System.out.println(localIP + " has received marker from " + sender + "id of " + snapID);

		if (!ownSnaps.containsKey(snapID)) {// upon receiving first marker
			System.out.println("Receiving marker for the first time for: " + snapID);

			// init new own snapshot
			Snapshot ownSnap = new Snapshot(this.localIP);
			// snap balance
			ownSnap.setBal(this.balance);
			ownSnap.setID(snapID);

			// stores own snap because multiple snaps can occur at once
			ownSnaps.put(snapID, ownSnap);

			for (String neighborIP : neighbors.keySet()) {

				// send markers to everyone
				System.out.println("upon receiving snap " + snapID + " sending markers to " + neighborIP + " ");
				System.out.println("start recording channels");
				ownSnap.addMessageChannel(neighborIP);
				// start recording on all neighbors except the one that sent
				// you the message
				if (!sender.equals(neighborIP)) {
					ownSnap.setRecordingState(neighborIP, true);
				} else {
					ownSnap.setRecordingState(neighborIP, false);
				}
				try {
					AccountInt recipientStub = neighbors.get(neighborIP);
					recipientStub.receiveMarker(leader, localIP, snapID);
				} catch (Exception e) {
					System.err
							.println("error in sending MARKER to " + neighborIP + " in receiveMarker()" + e.toString());
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("Receiving marker for the first time for: " + snapID);
			// upon receiving another marker
			// check if has snapshot
			// stop recording on sender channel
			ownSnaps.get(snapID).setRecordingState(sender, false);
			// store channel state of sender channel
			// if all channels have stop recording then send snapshot to leader
			if (ownSnaps.get(snapID).snapshotFinished()) {
				if (leader.equals(localIP)) {
					globalSnaps.get(snapID).put(localIP, ownSnaps.get(snapID).toString());
				} else {
					try {
						AccountInt leaderStub = neighbors.get(leader);
						leaderStub.receiveSnapshot(snapID, localIP, ownSnaps.get(snapID).toString());
					} catch (Exception e) {
						System.err.println("error in sending SNAPSHOT back to leader " + leader + " in receiveMarker()"
								+ e.toString());
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void receiveSnapshot(Integer snapID, String sender, String snap) {
		globalSnaps.get(snapID).put(sender, snap);
		// check if snapshot storage has snapshots for all
		System.out.println("Checking if snapshot storage has snapshots for all");
		HashMap<String, String> existingSnaps = globalSnaps.get(snapID);
		boolean receivedAll = true;
		for (String snapfrom : existingSnaps.values()) {
			if (snapfrom == null) {
				receivedAll = false;
			}
		}
		// if all snapshots are received then we can print out snapshots
		if (receivedAll && this.isLeader) {
			System.out.println("all snapshots have been received");
			for (String snapfrom : existingSnaps.values()) {
				System.out.println(snapfrom.toString());
			}
		}
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
						obj.neighborStubs.add(remoteStub);
						remoteStub.receiveIPs(obj.neighborIPs);
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
		int numOps = 0;

		while (true) {
			if (numOps < 2 || !obj.isLeader) {
				try {
					obj.sendMoney();
					numOps++;
				} catch (Exception e) {
					System.err.println("error in sendMoney() in main" + e.toString());
					e.printStackTrace();
				}
				System.out.println("numOps: " + numOps);
			} else {
				// conditional check is leader is localIP, if yes send out
				// initSnap()
				// and initialize snapshot storage
				if (obj.isLeader) {
					obj.initSnap();
					numOps = 0;
				}
			}
			// wait time to start transfer
			int r = 5000 + (int) (Math.random() * 50000);
			// sending money to another account
			try {
				System.out.println("\n***Waiting for " + r / 1000 + " seconds...***\n");
				Thread.sleep(r);
			} catch (Exception e) {
				System.err.println("error with sending money/initSnapshot" + e.toString());
				e.printStackTrace();
			}

		}
	}
}