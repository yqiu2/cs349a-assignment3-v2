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
	void receiveMarker(String leader, String sender, Integer snapID) throws RemoteException;
	void receiveSnapshot(Snapshot snap) throws RemoteException;
	String ping() throws RemoteException;
}