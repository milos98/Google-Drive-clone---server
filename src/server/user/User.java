package server.user;

import java.io.Serializable;
import java.util.LinkedList;

public class User implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private String username;
    private String password;
    private String linkDoDrajva;
    private Type tipKorisnika;
    private int brojFajlova = 0;
	private LinkedList<String> pristup = new LinkedList<>();
    
    public User(String username, String password, Type tipKorisnika) {
		super();
		this.username = username;
		this.password = password;
		this.tipKorisnika = tipKorisnika;
	}
	public int getBrojFajlova() {
		return brojFajlova;
	}
	public void setBrojFajlova(int brojFajlova) {
		this.brojFajlova = brojFajlova;
	}
	public String getUsername() {
		return username;
	}
	public String getPassword() {
		return password;
	}
	public String getLinkDoDrajva() {
		return linkDoDrajva;
	}
	public Type getTipKorisnika() {
		return tipKorisnika;
	}
	@Override
	public String toString() {
		return "" + tipKorisnika;
	}
	
	public void dodajPristup(String username) {
		pristup.add(username);
	}
	
	public LinkedList<String> getPristup(){
		return pristup;
	}

}
