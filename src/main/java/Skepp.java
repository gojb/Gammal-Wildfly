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
	String namn, sessionNamn;

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
		anslutna.add(this);
		sessionNamn=this.toString();

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
			namn=scanner.next();
			skicka(namn);
			String allaOnline = null;
			for (Skepp skepp : anslutna) {
				allaOnline+=skepp.namn+",";
			}
			skicka("Alla online = "+allaOnline.substring(0, allaOnline.length()-1));
		}
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
