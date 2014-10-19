import java.io.File;

public class recepteur
{
	public static void main(String[] args) throws Exception {
		if( args.length != 4)
			throw new Exception("Example d'appel : recepteur <numero_de_port_en_ecoute> <taille_de_fenetre> <fichier> <pourcentage_perte_segment>");

		int port = Integer.parseInt(args[0]);
		if (port <= 1024) 
			throw new Exception("Erreur : le port d'écoute doit être supérieur à 1024 pour éviter les problèmes de droit.");

		long WIN = Long.parseLong(args[1]);
		if (WIN <= 0 || WIN > 8) 
			throw new Exception("Erreur : la taille de la fenêtre ne peut être nulle ou dépasser la valeur de 8.");

		int lostSequence = 100-Integer.parseInt(args[3]);
		if (lostSequence <= 0 || lostSequence > 100) 
			throw new Exception("Erreur : le pourcentage de perte ne peut être inférieur à 0% et ne doit pas être supérieur à 99%.");

		String filename = args[2];
		if (!new File(System.getProperty("user.dir") + File.separator +filename).exists())
			throw new Exception("Erreur : le fichier demandé est introuvable.");
		
		UDPServer server = new UDPServer(port,WIN,filename,lostSequence);
	}
}
