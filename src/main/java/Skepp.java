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
			anslutna.add(this);
			namn=scanner.next();
			String allaOnline = "";
			String allaOnlineID="";
			for (Skepp skepp : anslutna) {
				if(skepp!=this){
					allaOnline+=skepp.namn+",";
				}
			}
			for (Skepp skepp : anslutna) {
				if(skepp!=this){
					allaOnlineID+=skepp.session.getId()+",";
				}
			}
			if(allaOnline.length()>0){
				skicka("Alla online ="+allaOnline.substring(0, allaOnline.length()-1)+";"+allaOnlineID.substring(0, allaOnlineID.length()-1));
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
					skickaTillAndra("Ihopkopplad " + namn);
					skicka("Ihopkopplad " + andra.namn);
				}
			}
		}
		//		andra=anslutna.get(1);
		//		anslutna.get(1).andra=this;

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
