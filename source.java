import java.util.BitSet;

public class source {

	public static void main(String[] args)  throws Exception 
	{
		if( args.length != 5)
			throw new Exception("Example d'appel : source <nom_DNS_du_destinataire> <numero_de_port_destination> <taille_de_fenetre> <fichier> <pourcentage_erreur_crc>");
		// configurer le client

		int port = Integer.parseInt(args[1]);
		if (port <= 1024)
			throw new Exception("Erreur : le port d'écoute doit être supérieur à 1024 pour éviter les problèmes de droit");

		long WIN = Long.parseLong(args[2]);
		if (WIN <= 0 || WIN > 8) 
			throw new Exception("Erreur : la taille de la fenêtre ne peut être nulle ou dépasser la valeur de 8.");

		int errorCRC = 100-Integer.parseInt(args[4]);
		if (errorCRC <= 0 || errorCRC > 100) 
			throw new Exception("Erreur : le pourcentage d'erreur ne peut être inférieur à 0% et ne doit pas être supérieur à 99%.");

		byte[] buffer = new byte[1024];
		// instancier le client
		UDPClient client = new UDPClient(args[0],port, WIN, args[3], errorCRC);
	}
}
