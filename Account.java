//import java.rmi.Remote;
//import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.util.*;
import java.rmi.server.UnicastRemoteObject;

public class Account implements AccountInt {
	boolean startCommunication;
	String localIP;
	// array list of stubs of neighbors
	ArrayList<AccountInt> neighborStubs;

	public Account() {
		startCommunication = false;
		localIP = "";
		neighborStubs = new ArrayList<AccountInt>();
	}

	// 1) bootstrapping the cue to start communicating with other clients
	public void recieveIPs(ArrayList<String> ipAddress) {

	}
	// 2) leader election
	/*
	 * void sendBallot(String candidate, int numMessagesPassed, boolean
	 * leaderConfirmed) throws remoteException; // 3) sending money void
	 * receiveMoney(int amount, String sender) throws RemoteException; // void
	 * receiveServerMessage(String serverMessage) throws RemoteException;
	 * 
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
		
		ArrayList<String> ipAddresses = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			ipAddresses.add(args[i]);
		}
		obj.localIP = args[0];

		if (ipAddresses.size() > 1) {
			try {
				// send out all of the IP addresses to all the other accounts
				for (int i = 1; i < ipAddresses.size(); i++) {
					String remoteHost = ipAddresses.get(i);

					Registry remoteRegistry = LocateRegistry.getRegistry(remoteHost);
					AccountInt remoteStub = (AccountInt) remoteRegistry.lookup("Account");
					// save the stubs
					obj.neighborStubs.add(remoteStub);
					System.out.println(remoteHost + " has been bound to a stub");
				}

			} catch (Exception e) {
				System.err.println("Client exception(could not find remote Account): " + e.toString());
				e.printStackTrace();
			}
		}

		
	}
}