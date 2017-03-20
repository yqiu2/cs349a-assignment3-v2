import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

public interface AccountInt extends Remote {
	// 1) bootstrapping the cue to start communicating with other clients
	void receiveIPs(ArrayList<String> ipAddress) throws RemoteException;

	// 2) sending money
	void receiveMoney(int amount, String senderIP) throws RemoteException;

	// 3) leader election
	void receiveBallot(String candidate, int numMessagesPassed, boolean leaderConfirmed) throws RemoteException;

	// 4) snapshotting
	void receiveStartSnapshot(String leader, String sender, String recipient) throws RemoteException;

	void receiveSnapshot(String sender, int amount, ArrayList<ArrayList<Integer>> channels) throws RemoteException;

	// void receiveServerMessage(String serverMessage) throws RemoteException;
	void sendSnapshot(String sender, int amount, ArrayList<ArrayList<Integer>> channels) throws RemoteException;
}