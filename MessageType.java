public enum MessageType {

	DATA(1), ACK(2), NACK(3);

	public long type;
 
	MessageType(long t) {
		this.type = t;
	}

	public long getType() {
		return this.type;
	}
 
	public String toString() {
		String s = "";
		switch((int)this.type) {
			case 1: s = "DATA"; break;
			case 2: s = "ACK"; break;
			case 3: s = "NACK"; break;
		}
		return s;
 	}
 
	public static String toString(long t) {
		String s = "";
		switch((int)t) {
			case 1: s = "DATA"; break;
			case 2: s = "ACK"; break;
			case 3: s = "NACK"; break;
		}
		return s;
 	}
}
