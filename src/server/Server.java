package server;
 
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import server.user.ClientHandler;
import server.user.User;
 
public class Server {
   
    // Definisemo i cuvamo listu online korisnika koji su se zakacili na server
    public static LinkedList<User> dataBase = new LinkedList<>();

	public static void main(String[] args) {
       
        // Definisanje porta za osluskivanje, ServerSocket za prihvatanje novih korisnika i soket za komunikaciju sa istima
        int port = 9000;
        ServerSocket serverSoket = null;
        Socket soketZaKomunikaciju = null;
        
//----------------------------------------------------------------------------------------------------------------------------------------------------------
//														Ucitavanje baze podataka u listu.
//----------------------------------------------------------------------------------------------------------------------------------------------------------
        dataBase.clear();
        try(FileInputStream fIn = new FileInputStream("drive/baza.mj");
        		BufferedInputStream bIn = new BufferedInputStream(fIn);
        		ObjectInputStream oIn = new ObjectInputStream(bIn)) {
        			try {
        				while(true) {
        					User u = (User)(oIn.readObject());
        					dataBase.add(u);
        				}
        			} catch (EOFException e) {}
        } catch (Exception e) {
        	System.out.println("Greska pri ucitavanju baze!");
        }
       
        try {
            // Konstruktor za ServerSocket
            serverSoket = new ServerSocket(port);
//----------------------------------------------------------------------------------------------------------------------------------------------------------
//          					Prihvatanje novih korisnika i pokretanje nove NITi (ClientHandler) za svakog pojedinacno.
//         					Sva dalja komunikacija se obavlja kroz nit.Kao parametar za NIT se prosledjuje soketZaKomunikaciju.
//----------------------------------------------------------------------------------------------------------------------------------------------------------
            while (true) {
               
                System.out.println("Cekam na konekciju...");
                soketZaKomunikaciju = serverSoket.accept();
                
                System.out.println("Doslo je do konekcije!");
                ClientHandler klijent = new ClientHandler(soketZaKomunikaciju);
               
                // POkretanje NITi
                klijent.start();
               
            }
//----------------------------------------------------------------------------------------------------------------------------------------------------------
//														Obrada greske pri pokretanju servera
//----------------------------------------------------------------------------------------------------------------------------------------------------------           
        } catch (IOException e) {
            System.out.println("Greska prilikom pokretanja servera!");
        }
 
    }
 
}