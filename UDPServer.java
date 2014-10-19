import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.BindException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.Random;
import java.lang.Math;

public class UDPServer
{
	// constante
	private static final int PAYLOADLENGTH = 512;
	private static final int LONGLENGTH = 8;
	private static final int LENGTH = PAYLOADLENGTH+LONGLENGTH;
	private static final int MODULO = 256;
	// socket
	private DatagramSocket dataSocket;
	private DatagramSocket messageSocket;
	private Packet dataPacket;
	private Packet messagePacket;
	private DatagramPacket ackPacket;
	private InetAddress clientAddress = null ;
	private int hostPort = 0 ;
	private int clientPort = 0 ;
	// data
	private long Sequence = 0;
	private long nextSequence=1;
	private long WIN;
	// message
	private byte[] message = new byte[LENGTH];
	// fichier
	private Vector<byte[]> fileSequences;
	private int nbSequence;
	private int lost;
	// CRC
	private Checksum CRC;
	// constructeur
	public UDPServer(int p, long ws, String f, int l) throws Exception
	{
		// configurer socket
		dataSocket =  new DatagramSocket(p);
		messageSocket = new DatagramSocket();
		hostPort = p;
		// configurer la fenêtre
		WIN = ws;
		// Découper le fichier
		fileSequences = segmentFile(f);
		// pourcentage de perte et d'erreur
		lost = l;
		// démarrer les transactions
		transaction();
	}
	// public
	public void transaction() throws Exception
	{
		while (true)
		{
			System.out.println("Attente du destinataire (couple IP/port)");
			rx(dataSocket, message);
			System.out.println("Configuration destinataire réussi...");
			try
			{
				while(Sequence != nbSequence)
				{
					System.out.println("Début de la transmission DATA " + Sequence + " " + nextSequence + " " +nbSequence);
					// envoie des paquets
					for (long i=Sequence;i<((Sequence+WIN>nbSequence)?nbSequence:Sequence+WIN);i++) 
					{
						if (lost == 100 || !sequenceLost(lost))
						{
							sendData(fileSequences.get((int)i), i);
						}
					}
					System.out.println("Attente des ACKs dans l'ordre' " + Sequence + " " + nextSequence);
					// reception des ack
					waitAnswer();
				}
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				// si la derniere sequence a la meme longueur que le payload
				if (fileSequences.lastElement().length == PAYLOADLENGTH) 
				{
					sendData(new byte[0], Sequence);
					waitAnswer();
				}
				reset();
			}
			reset();
		}
	}

	private void waitAnswer() throws Exception
	{
		for (long i=Sequence;i<((Sequence+WIN>nbSequence)?nbSequence:Sequence+WIN);i++) 
		{
			DatagramPacket packet=new DatagramPacket(new byte[PAYLOADLENGTH], PAYLOADLENGTH);
			rx(dataSocket, new byte[PAYLOADLENGTH]);					
			System.out.println("verifier si c'est un ACK" +messagePacket.getSequence()+" "+(Sequence+1)%MODULO);
			if (messagePacket.getType() == MessageType.ACK.getType()
				&& messagePacket.getSequence() == (nextSequence+1)%MODULO) 
			{
		
				System.out.println("ACK détecté; glisser la fenêtre'");
				nextSequence++;
			}
			else
			{
				break;
			}
		}
		Sequence = nextSequence;
	}

	private void reset() throws Exception
	{
		System.out.println("Réinitialiser la séquence pour le client suivant");
		Sequence = 0;
		nextSequence = 1;
		System.out.println("Réinitialiser le destinataire");
		clientAddress = null;
		clientPort = 0;
		System.out.println("Transaction terminée");
	}

	private void tx(DatagramSocket socket, byte[] data) throws Exception
	{
		try
		{
			socket.send(
				new DatagramPacket(
					data, 
					data.length, 
					clientAddress, 
					clientPort
				)
			);
		}
		catch (IOException e)
		{
			System.err.println("Erreur : " + e);
		}
	}

	
	private void rx(DatagramSocket socket, byte[] data) throws Exception 
	{
		try
		{
			DatagramPacket packet=new DatagramPacket(data, data.length);
			socket.receive(packet);
			messagePacket = Packet.parseUDPdata(data);
			if (clientAddress == null)
			{
				clientAddress = packet.getAddress();
				clientPort = packet.getPort();
			}
		}
		catch (IOException e)
		{
			System.err.println("Erreur : " + e);
		}
	}
	private void sendData(byte[] data, long seq) throws Exception
	{
		
			CRC = new CRC32();
			CRC.update(data, 0, data.length);
			tx(
				dataSocket, 
				Packet.Create
				(
					MessageType.DATA, 
					seq%MODULO, 
					data, 
					WIN-1, 
					(char)CRC.getValue()
				).getUDPdata()
			);
	}
	// private
	private Vector<byte[]> segmentFile(String f) throws Exception
	{
		Vector<byte[]> sequenceList = new Vector<byte[]>();

		try
		{
			 //Open the input and out files for the streams
			FileInputStream file= new FileInputStream(f);
			while (file.available() > PAYLOADLENGTH) 
			{
				byte[] b = new byte[PAYLOADLENGTH];
				file.read(b);
				sequenceList.add(b);
			}
			byte[] b = new byte[file.available()];
			file.read(b);
			sequenceList.add(b);
			file.close();
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Erreur : le fichier "+f+" est introuvable");
		}
		catch (Exception e)
		{
			System.err.println("Erreur : " + e);
		}
		nbSequence = sequenceList.size();
		return sequenceList;
	}

	private boolean sequenceLost(int percentage)
	{
		boolean b = false;
		Random rand = new Random();
		if (rand.nextInt(percentage) == 0) {
			System.out.println("you got lucky Lost " + nbSequence);
		}
		return b;
	}
}
