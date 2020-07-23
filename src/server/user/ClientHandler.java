package server.user;

import java.io.*;
import java.net.Socket;
import java.util.*;

import server.Server;

//U ovom slucaju NIT je realizovana nasledjivanjem klase THREAD

public class ClientHandler extends Thread {

	// Definisani ulazni i izlazni tok, soket za komunikaciju, korisnicko ime,
	// sifra, tip korisnika i pomocne promenljive

	private BufferedReader clientInput = null;
	private PrintStream clientOutput = null;
	private Socket soketZaKomunikaciju = null;
	private File baza = new File("drive/baza.mj");
	private File izabraniF;
	private File destinacija;
	private User user;
	private String username;
	private String password;
	private Type tipKorisnika;
	private String pom = "";
	private String userInput;
	boolean isValid = false;

	// Konstruktor za NIT
	public ClientHandler(Socket soket) {
		soketZaKomunikaciju = soket;
	}

	// Ispis fajlova
	public void printFiles(String path) {
		File root = new File(path);
		File[] list = root.listFiles();

		if (list == null)
			return;

		for (File f : list) {
			if (f.isDirectory()) {
				clientOutput.println(">>> Dir: " + f.getPath());
				printFiles(f.getPath());
			} else {
				clientOutput.println(">>> File:" + f.getPath());
			}
		}
	}

	// Ispis menija
	public void printMenu() {
		clientOutput.println(">>> Izaberite opciju sa liste (potrebno je uneti samo broj opcije"
				+ " - u svakom trenutku mozete uneti ***quit za izlaz)");
		clientOutput.println("\n");

		// Ispis svih fajlova/foldera na disku
		printFiles("drive/" + username);

		// Ispis fajlova kojima klijent ima pristup na tudjim diskovima
		for (User u : Server.dataBase)
			if (username.equals(u.getUsername())) {
				for (String s : u.getPristup())
					printFiles("drive/" + s);
			}

		clientOutput.println("\n");
		clientOutput.println(">>> 1. Open file");
		clientOutput.println(">>> 2. Upload file");
		clientOutput.println(">>> 3. Share drive");
		clientOutput.println(">>> 4. Link Share"); // DONE!!!
		if (tipKorisnika.toString().equals("PREMIUM")) {
			clientOutput.println(">>> 5. Make folder"); // DONE!!!
			clientOutput.println(">>> 6. Rename folder"); // DONE!!!
			clientOutput.println(">>> 7. Move file"); // DONE!!!
			clientOutput.println(">>> 8. Delete folder"); // DONE!!!
		}
		clientOutput.println(">>> 9. Delete folder");

	}

	// Registracija korisnika
	public void register() throws IOException {

		// Unos korisnickog imena i provera da li je ispravno
		do {
			clientOutput.println(">>> Unesite korisnicko ime:");
			username = clientInput.readLine();

			if (username.contains(" ")) {
				clientOutput.println(">>> Korisnicko ime ne sme da sadrzi prazno mesto!");
			} else {
				for (User u : Server.dataBase)
					if (username.equals(u.getUsername())) {
						clientOutput.println(">>> Korisnicko ime vec postoji u bazi!");
						pom = "postoji";
					}
				if (!(pom.equals("postoji")))
					isValid = true;
			}

		} while (!isValid);

		// UNos sifre
		clientOutput.println("Unesite password:");
		password = clientInput.readLine();

		isValid = false;

		// Izbor tipa naloga
		do {
			clientOutput.println(">>> Unesite vrstu korisnika (upisati pocetno slovo opcije):");
			clientOutput.println(">>> Standard");
			clientOutput.println(">>> Premium");

			userInput = clientInput.readLine();

			if (userInput.toLowerCase().equals("s")) {
				isValid = true;
				tipKorisnika = Type.STANDARD;
				clientOutput.println(">>> Dobrodosao/la " + username + " (" + tipKorisnika
						+ " korisnik)\nZa izlazak unesite ***quit");
			} else {
				if (userInput.toLowerCase().equals("p")) {
					isValid = true;
					tipKorisnika = Type.PREMIUM;
					clientOutput.println(">>> Dobrodosao/la " + username + " (" + tipKorisnika
							+ "korisnik)\nZa izlazak unesite ***quit");
				} else {
					clientOutput.println(">>> Pogresna opcija! Morate izabrati jednu od ponudjenih opcija.");
				}
			}
		} while (!isValid);

		// Ubacivanje u listu svih korisnika
		user = new User(username, password, tipKorisnika);
		Server.dataBase.add(user);

		// Serializacija liste
		try (FileOutputStream fOut = new FileOutputStream(baza.getAbsolutePath());
				BufferedOutputStream bOut = new BufferedOutputStream(fOut);
				ObjectOutputStream out = new ObjectOutputStream(bOut)) {
			for (int i = 0; i < Server.dataBase.size(); i++)
				out.writeObject(Server.dataBase.get(i));
		} catch (Exception e) {
			System.out.println("Greska pri upisu u bazu!");
		}

		// Pravljenje foldera za korisnika
		destinacija = new File("drive/" + username);
		destinacija.mkdir();
	}

	// Prijavljivanje korisnika
	public void login() throws IOException {
		do {

			clientOutput.println(">>> Unesite username: ");
			username = clientInput.readLine();

			clientOutput.println(">>> Unesite password: ");
			password = clientInput.readLine();

			// Provera u listi da li postoji username, pa ako postoji provera sifre
			for (User u : Server.dataBase)
				if (username.equals(u.getUsername()))
					if (password.equals(u.getPassword())) {
						tipKorisnika = u.getTipKorisnika();
						isValid = true;
					}

			if (!(isValid))
				clientOutput.println(">>> Pogresan username i/ili password");

		} while (!isValid);
	}

	// Otvaranje/download fajla sa drajva
	public void openFile() throws IOException {
		clientOutput.println(">>> Unesite putanju do fajla fajla koji zelite da otvorite: ");
		userInput = clientInput.readLine();

		izabraniF = new File(userInput);

		// Provera tipa fajla (.txt ili binarni)
		if (izabraniF.getName().endsWith(".txt")) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(userInput));

				boolean kraj = false;

				while (!kraj) {
					pom = br.readLine();
					if (pom == null)
						kraj = true;
					else
						clientOutput.println(pom);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			pom = null;
			try {
				FileInputStream fIs = new FileInputStream(izabraniF);
				byte[] b = new byte[(int) izabraniF.length()];
				fIs.read(b);
				pom = new String(Base64.getEncoder().encode(b), "UTF-8");
				clientOutput.println(pom);
				fIs.close();
			} catch (FileNotFoundException e) {
				clientOutput.println(">>> Fajl nije pronadjen!");
			}
		}
	}

	// Upload fajla na drive
	public void uploadFile() throws IOException {

		// Provera da li ima jos mesta za upload (samo za standard korisnike -
		// ogranicenje je 4 fajla)
		for (User u : Server.dataBase)
			if (username.equals(u.getUsername()))
				if (u.getTipKorisnika().toString().equals("STANDARD"))
					if (u.getBrojFajlova() > 3)
						pom = "***ogranicenje";

		if (pom.equals("***ogranicenje")) {
			clientOutput.println("Dostignut je maksimalan broj fajlova!");
			return;
		} else {

			clientOutput.println(
					">>> Unesite relativnu putanju do fajla koji zelite da upload-ujete, zajedno sa imenom fajla: ");
			userInput = clientInput.readLine();

			clientOutput.println(
					">>> Unesite relativnu putanju do mesta gde zelite da upload-ujete fajl, zajedno sa imenom fajla: ");
			userInput = clientInput.readLine();

			userInput = "drive/" + username + "/" + userInput;
			destinacija = new File(userInput);
			destinacija.createNewFile();

			if (destinacija.getName().endsWith(".txt")) {
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(userInput)));
				pom = null;

				do {
					userInput = clientInput.readLine();
					if (userInput.equals("***kraj"))
						break;
					if (pom != null)
						pw.print("\n");
					pw.print(userInput);
					pom = "upis";
					clientOutput.println(userInput);
				} while (true);
				pw.close();
				for (User u : Server.dataBase)
					if (username.equals(u.getUsername()))
						u.setBrojFajlova(u.getBrojFajlova() + 1);
			} else {
				userInput = clientInput.readLine();
				try {
					FileOutputStream fOut = new FileOutputStream(destinacija);
					byte[] b = Base64.getDecoder().decode(userInput);
					fOut.write(b);
					fOut.close();
					for (User u : Server.dataBase)
						if (username.equals(u.getUsername()))
							u.setBrojFajlova(u.getBrojFajlova() + 1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Deljenje drajva sa korisnikom
	public void shareDrive() throws IOException {
		clientOutput.println(">>> Unestite username korisnika koje zelite da date pristup svom drajvu: ");
		userInput = clientInput.readLine();
		for (User u : Server.dataBase) {
			if (userInput.equals(u.getUsername())) {
				u.dodajPristup(username);
				clientOutput.println(">>> Uspesno ste podelili svoj drajv sa korisnikom " + username);
				return;
			}
		}
		clientOutput.println(">>> Uneti korisnik ne postoji!");
	}

	// Deljenje drajva pomocu linka
	public void linkShare() {
		clientOutput.println(">>> Link do vaseg drajva je: " + new File("drive").getAbsolutePath() + "\\" + username);
	}

	// Pravljenje foldera na drajvu
	public void makeFolder() throws IOException {
		clientOutput.println(
				">>> Unesite relativnu putanju do destinacije na kojoj pravite folder zajedno sa imenom foldera: ");
		userInput = clientInput.readLine();

		userInput = "drive/" + username + "/" + userInput;
		destinacija = new File(userInput);
		destinacija.mkdir();
	}

	// Promena imena foldera
	public void renameFolder() throws IOException {
		clientOutput.println(">>> Unesite relativnu putanju do foldera koji zelite da preimenujete: ");
		userInput = clientInput.readLine();

		userInput = "drive/" + username + "/" + userInput;
		izabraniF = new File(userInput);

		clientOutput.println(">>> Unesite novo ime foldera: ");
		userInput = clientInput.readLine();

		userInput = izabraniF.getAbsoluteFile().getParent() + "\\" + userInput;
		destinacija = new File(userInput);
		izabraniF.renameTo(destinacija);
	}

	// Premestanje fajlova
	public void moveFile() throws IOException {
		clientOutput.println(
				">>> Unesite relativnu putanju do fajla koji zelite da prebacite (zajedno sa nazivom fajla na kraju): ");
		userInput = clientInput.readLine();

		userInput = "drive/" + username + "/" + userInput;
		izabraniF = new File(userInput);

		clientOutput.println(
				">>> Unesite relativnu putanju do foldera u koji zelite da prebacite fajl (zajedno sa nazivom fajla na kraju): ");
		userInput = clientInput.readLine();

		userInput = "drive/" + username + "/" + userInput;
		destinacija = new File(userInput);
		izabraniF.renameTo(destinacija);
	}

	// Brisanje foldera (samo za prazne radi) i fajlova
	public void deleteFolder() throws IOException {
		clientOutput.println(">>> Unesite relativnu putanju do foldera koji zelite da izbrisete: ");
		userInput = clientInput.readLine();

		userInput = "drive/" + username + "/" + userInput;
		izabraniF = new File(userInput);

		try {
			izabraniF.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Metoda run u kojoj se izvrsava NIT
	@Override
	public void run() {

		try {

			// ----------------------------------------------------------------------------------------------------------------------------------------------------------
			// Inicijalizacija ulazno-izlaznih tokova
			// ----------------------------------------------------------------------------------------------------------------------------------------------------------

			clientInput = new BufferedReader(new InputStreamReader(soketZaKomunikaciju.getInputStream()));
			clientOutput = new PrintStream(soketZaKomunikaciju.getOutputStream());

			// ----------------------------------------------------------------------------------------------------------------------------------------------------------
			// Prijavljivanje/registracija korisnika
			// ----------------------------------------------------------------------------------------------------------------------------------------------------------

			do {
				clientOutput.println(
						">>> Dobrodosli na Gdrive!\n>>> Za izbor opcije unesite pocetno slovo\n\n>>> Registracija\n>>> Prijavljivanje");
				userInput = clientInput.readLine();
				switch (userInput.toLowerCase()) {
				case "r":
					register();
					isValid = true;
					break;
				case "p":
					login();
					isValid = true;
					break;
				default:
					clientOutput.println(">>> Pogresan izbor!");
					break;
				}
			} while (!isValid);

			// ----------------------------------------------------------------------------------------------------------------------------------------------------------
			// Interakcija sa korisnikom
			// ----------------------------------------------------------------------------------------------------------------------------------------------------------

			while (true) {

				// Ispis glavnog menija i cekanje odgovora korisnika
				printMenu();
				userInput = clientInput.readLine();

				// Ako poruka sadrzi niz karaktera koji ukazuju na izlaz, izlazi se iz petlje,
				// korisnik se izbacuje sa servera
				if (userInput.startsWith("***quit")) {
					break;
				}

				// Izvrsavanje korisnikove komande
				if (!userInput.equals("9")) {
					switch (userInput) {
					case "1":
						openFile();
						break;

					case "2":
						uploadFile();
						break;

					case "3":
						shareDrive();
						break;

					case "4":
						linkShare();
						break;

					case "5":
						makeFolder();
						break;

					case "6":
						renameFolder();
						break;

					case "7":
						moveFile();
						break;
					case "8":
						deleteFolder();
						break;
					default:
						clientOutput.println("Pogresna opcija!");
					}
				}
			}
			
			// Korisniku koji napusta chat se salje pozdravna poruka
			clientOutput.println(">>> Dovidjenja " + username);
			System.out.println("Korisnik " + username + " se odjavio!");

			// Zatvaramo soket za komunikaciju
			soketZaKomunikaciju.close();

			// Ovde je obradjen izuzetak u slucaju da korisnik nasilno prekine konekciju.
			// U smislu da ne otkuca ***quit nego da samo ugasi klijentsku aplikaciju.
			// Ili da mu nestane struje npr.
		} catch (IOException e) {
			System.out.println("Korisnik " + username + " je nasilno prekinuo konekciju!");
		}
	}

}