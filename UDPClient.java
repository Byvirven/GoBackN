import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.BindException;
import java.net.SocketTimeoutException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.Random;
import java.lang.Math;

public class UDPClient
{
	// constante
	private static final int PAYLOADLENGTH = 512;
	private static final int LONGLENGTH = 8;
	private static final int LENGTH = PAYLOADLENGTH+LONGLENGTH;
	private static final int DELAY = 1000;
	private static final int MODULO = 256;
	// configuration socket
	private DatagramSocket dataSocket;
	private DatagramSocket messageSocket;
	private Packet dataPacket;
	private Packet messagePacket;
	private InetAddress hostAddress = null ;
	private int hostPort = 0 ;
	// data
	private byte[] data = new byte[LENGTH];
	private long Sequence = 0;
	private long WIN;
	// erreur crc
	private int error;
	// message
	private byte[] message = new byte[LENGTH];
	// fichier de sortie
	private FileOutputStream file;
	// constructeur
	public UDPClient(String h, int p, long ws, String f, int e) throws Exception
	{
		// configurer socket
		dataSocket =  new DatagramSocket();
		messageSocket = new DatagramSocket();
		hostPort = p;
		hostAddress = InetAddress.getByName(h);
		// configurer la fenêtre
		WIN = ws;
		// pourcentage erreur crc
		error = e;
		// créer le fichier
		createFile(f);
		// démarrer la transaction
		transaction();
	}
	
	public void transaction() throws Exception
	{
		boolean done = false;
		System.out.println("Début de la transaction");
		messagePacket = Packet.Create(MessageType.NACK, Sequence, new byte[0], WIN-1, '0');
		message = messagePacket.getUDPdata();
		tx(dataSocket, message);
		while(!done)
		{	
			try
			{	
				System.out.println("Activer le timeout");
				dataSocket.setSoTimeout(DELAY);
				rx(dataSocket, data);
				System.out.println("Paquet recu");
				if (Sequence%MODULO == dataPacket.getSequence())
				{
					if (dataPacket.CRCValid() && !CRCError(error))
					{
						System.out.println("ecriture des data recu "+Sequence%MODULO+" "+ dataPacket.getSequence());
						System.out.println(dataPacket.getData().toString());
						System.out.println(dataPacket.getData());
						for(int i=0 ; i<dataPacket.getData().length ; i++)
						{
							file.write(dataPacket.getData()[i]);
						}
						if (dataPacket.getData().length != PAYLOADLENGTH)
						{
							done = true;
						}
						//System.out.println("Incrémentation de la Sequence");
						Sequence++;
						//System.out.println("Création de l'ACK");
						messagePacket = Packet.Create(MessageType.ACK, Sequence%MODULO, new byte[0],WIN-1, '0');
						//System.out.println("Encapsulation de l'ACK");
						message = messagePacket.getUDPdata();
						System.out.println("Envoie de l'ACK " + Sequence);
						tx(messageSocket, message);
					}
					else
					{
						System.out.println("CRC erroné !!!");
						NACK();
					}
				}
			}
			catch (Exception e)
			{
				System.err.println("Erreur :" + e);
			}
		}
		file.close();
		messageSocket.close();
		dataSocket.close();
		System.out.println("Transaction terminée");
	}
	// créer le fichier
	private void createFile(String f)
	{
		try {
			file = new FileOutputStream(f);
		} catch (Exception e) {
			System.err.println("Erreur : " + e);
		};
	}

	private void tx(DatagramSocket socket, byte[] data) throws Exception
	{
		try
		{
			System.out.println("Préparation du paquet à envoyer");
			socket.send(
				new DatagramPacket(
					data, 
					data.length, 
					hostAddress, 
					hostPort
				)
			);
			System.out.println("Emission..");
		}
		catch (Exception e)
		{
			System.err.println("Erreur : " + e);
		}
	}

	
	private void rx(DatagramSocket socket, byte[] data) throws Exception 
	{
		try
		{
			DatagramPacket packet=new DatagramPacket(data, data.length);
			//System.out.println("Attente du paquet");
			socket.receive(packet);
			//System.out.println("Parsage du paquet");
			dataPacket = Packet.parseUDPdata(data);
			//System.out.println("Sequence du paquet : " + dataPacket.getSequence());
		}
		catch (SocketTimeoutException e)
		{
			System.out.println("Timeout !!!!");
			NACK();
		}
		catch (Exception e)
		{
			System.err.println("Erreur : " + e);
		}
	}
	
	private void NACK() throws Exception
	{
		messagePacket = Packet.Create(MessageType.NACK, Sequence, new byte[0] ,WIN-1, '0');
		//System.out.println("Encapsulation de NACK");
		message = messagePacket.getUDPdata();
		System.out.println("Envoie de NACK");
		tx(messageSocket, message);
	}

	private boolean CRCError(int percentage)
	{
		boolean b = false;
		Random rand = new Random();
		if (rand.nextInt(percentage) == 0) {
			b = true;
			System.out.println("you got lucky CRC ");
		}
		return b;
	}
}
