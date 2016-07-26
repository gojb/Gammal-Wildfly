import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value="/skepp")
public class Skepp {
	static ArrayList<Skepp> anslutna = new ArrayList<>();
	Scanner scanner;
	Session session;
	String namn, sessionNamn, annansSession;
	Skepp andra;
	boolean kopplad=false;


	@OnClose
	public void close() {
		anslutna.remove(this);
	}
	@OnError
	public void error(Throwable	throwable) {

	}
	@OnOpen
	public void open(Session session) {
		this.session=session;
		skicka(session.getId());
	}

	@OnMessage
	public void taemot(String message) {

		message=message.toLowerCase();

		scanner = new Scanner(message);
		String string=scanner.next();

		if(string.equals("starta")){
			skicka("starta");
			skicka(anslutna.get(0).toString());
		}
		else if(string.toLowerCase().equals("namn")){
			namn=scanner.next().toLowerCase();
			String allaOnline = "";
			String allaOnlineID="";
			if(!namn.equals("ettnammsomaldrigskrivs")){
				anslutna.add(this);
				for (Skepp skepp : anslutna) {
					if(skepp!=this&&skepp.kopplad==false){
						allaOnline+=skepp.namn+",";
					}
				}
				for (Skepp skepp : anslutna) {
					if(skepp!=this&&skepp.kopplad==false){
						allaOnlineID+=skepp.session.getId()+",";
					}
				}
			}
			else{
				allaOnline="  ";
			}
			if(allaOnline.length()>0){
				skicka("Alla online ="+allaOnline.substring(0, allaOnline.length()-1)+";"+allaOnlineID.substring(0, allaOnlineID.length()-1)+";"+namn);
			}
			else{
				skicka("Ingen online");
			}
		}
		else if (string.toLowerCase().equals("annan")) {
			for (Skepp skepp : anslutna) {
				String string2 = new String(skepp.session.getId().toLowerCase());
				String string3 = new String(message.split(" ")[1].toString().toLowerCase());

				if (string2.equals(string3)) {
					skepp.andra=this;
					andra=skepp;
					kopplad=true;
					andra.kopplad=true;
					skickaTillAndra("Ihopkopplad " + namn);
					skicka("Ihopkopplad " + andra.namn);
				}
			}
		}

	}
	public void skickaTillAndra(String message) {
		andra.skicka(message);
	}
	public void skicka(String message){
		try {
			session.getBasicRemote().sendText(message);
		} catch (IOException e) {
			System.err.println("Senderror");
		}
	}
	public static void skickaAlla(String message){
		for (Skepp skepp : anslutna) {
			skepp.skicka(message);
		}
	}

}
