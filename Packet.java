// common packet class used by both SENDER and RECEIVER

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Packet {
	
	// constants
	private static final int PAYLOADLENGTH = 512;
	private static final int LONGLENGTH = 8;
	private static final int MODULO = 256;
	
	// data
	private byte[] data;
	private long type;
	private long sequence;
	private long WIN;
	private long CRC;
	
	// méthode
	public static Packet Create(MessageType t, long seq, byte[] data, long ws, long CRC) throws Exception
	{
		return new Packet(t.getType(), seq, data, ws, CRC);
	}
	
	public long getType()
	{
		return type;
	}
	
	public long getSequence()
	{
		return (int)(sequence < 0 ? MODULO + sequence : sequence);
	}
	
	public long getLength()
	{
		return (long)data.length;
	}
	
	public byte[] getData()
	{
		return data;
	}
	
	public boolean CRCValid()
	{
		
		Checksum c;
		c = new CRC32();
		c.update(getData(), 0, (int)getLength());
		return ((char)c.getValue() == (char)CRC) ? true : false;
	}

	public byte[] getUDPdata() 
	{
		// créer un bytebuffer de taille PAYLOAD+2
		ByteBuffer bb = ByteBuffer.allocate(PAYLOADLENGTH+LONGLENGTH);
		//System.out.println("taille du bytebuffer : "+ bb.capacity());
		bb.order(ByteOrder.BIG_ENDIAN);
		System.out.println("type ha : "+ type + " WIN : "+  WIN + " Sequence : "+ sequence + " Length : "+  data.length);
		System.out.println("CRC16 : "+  CRC + " - CRC16 version hexadecimal : 0x"+ Integer.toHexString((int)CRC));
		long frame = type << 59 | WIN << 56 | sequence << 48 | (long)data.length << 32 | CRC << 16;
		System.out.println(Long.toBinaryString(frame));
		bb.putLong(frame).put(data);
		//System.out.println("La data encapsulé : " + bb.array());
		return bb.array();
	}
	
	public static Packet parseUDPdata(byte[] UDPdata) throws Exception 
	{
		//System.out.println("La data encapsulé à décapsuler : "+UDPdata);
		ByteBuffer bb = ByteBuffer.wrap(UDPdata);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.rewind();
		long frame = bb.getLong();
		System.out.println(Long.toBinaryString(frame));
		long t = frame >> 59;
		long ws = (frame >> 56) ^ (frame >> 59 << 3);
		long seq = (frame >> 48) ^ (frame >> 56 << 8);
		long size = (frame >> 32) ^ (frame >> 48 << 16);
		long c = (frame >> 16) ^ (frame >> 32 << 16);
		System.out.println("type cou : "+t+" -  WIN : "+ws+" - Sequence : "+seq+" - length : " +size+" - CRC16bit : 0x"+Integer.toHexString((int)c));
		byte d[] = new byte[(int)size];
		//System.out.println("Essaie de récupérer la data : " +bb.capacity());
		bb.get(d,0,(int)size);		
		//System.out.println("Crée le paquet");
		return new Packet(t, seq, d, ws, c);
	}
	// constructeur privé
	private Packet(long t, long seq, byte[] d, long ws, long c) throws Exception
	{
		if (d.length > PAYLOADLENGTH)
			throw new Exception("data too large (max "+PAYLOADLENGTH+" chars); data has length of "+d.length);
		type = t;
		WIN = ws;
		sequence = seq;
		CRC = c;
		data = d;
	}
}
