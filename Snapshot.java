import java.io.Serializable;
import java.util.*;

public class Snapshot implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8251690269021088830L;
	// static int numSnapshots = 0;
	Integer snapID;
	int snapBal;
	String processID;
	HashMap<String, ArrayList<Integer>> snapChannels;
	HashMap<String, Boolean> recChannels;

	public Snapshot(String ipAddress) {
		snapBal = -1;
		snapID = (Integer) (int)(Math.random() * 99999);
		// snapID = numSnapshots++;
		// numSnapshots++;
		processID = ipAddress;
		snapChannels = new HashMap<String, ArrayList<Integer>>();
		recChannels = new HashMap<String, Boolean>();
	}

	public Integer getID() {
		return snapID;
	}

	public void setID(Integer id) {
		snapID = id;

	}

	public String getProcessID() {
		return processID;
	}

	public int getBal() {
		return snapBal;
	}

	public void setBal(int balance) {
		snapBal = balance;
	}

	// recording Channel STORES MESSAGES
	public void addMessageChannel(String key) {
		System.out.println("in Snapshot Class initing message channel" + key);
		ArrayList<Integer> value = new ArrayList<Integer>();
		snapChannels.put(key, value);
	}

	public void addMessages(String channel, Integer msg) {
		snapChannels.get(channel).add(msg);
	}

	public HashMap<String, ArrayList<Integer>> getChannelMessages() {
		return snapChannels;
	}

	// Channel State STORE STATE
	public void addChannelState(String key, Boolean value) {
		System.out.println("in Snapshot Class initing channel state " + key);

		recChannels.put(key, value);
	}

	public void setRecordingState(String channel, Boolean state) {
		recChannels.put(channel, state);
	}

	public HashMap<String, Boolean> getChannelState() {
		return recChannels;
	}
	
	public boolean getChannelState(String channel) {
		return recChannels.get(channel);
		
	}
	public boolean snapshotFinished() {
		//System.out.println("in Snapshot Class SnapshotFinished()");
		for ( String channel : recChannels.keySet()) {
			boolean recordState = recChannels.get(channel);
			//System.out.println("chan:" + channel + ": " + recordState);
			if (recordState == true) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		String s = "";
		s += "Snapshot ID: " + snapID + "\n";
		s += "Process ID: " + processID + "\n";
		s += "Snapshot balance: " + snapBal + "\n";
		s += "Snapshot of Channels: \n";
		for (String chan : snapChannels.keySet()) {
			s += "Channel " + chan + " to " + processID + " messages :";
			ArrayList<Integer> messages = snapChannels.get(chan);
			for (int i = 0; i < messages.size(); i++) {
				s += "$" + messages.get(i) + " ";
			}
			s += "\n";
		}
		return s;
	}
}