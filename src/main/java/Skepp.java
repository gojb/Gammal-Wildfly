//N�r push av servern fungerar, testa:
//Fungerar servern i avseende att kolla om b�da �r klara?
//Fungerar klick och bombning?
//M�las rutorna vid tr�ff och miss?
//--- Lyckas ej pusha tror jag. Nya �ndringar i koden h�nder det inget med


import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
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
	boolean kopplad=false, klar=false;


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
		
		skicka("message --> "+message);

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
				if(skepp!=this&&skepp.kopplad==false){
					allaOnline+=skepp.namn+",";
				}
			}
			for (Skepp skepp : anslutna) {
				if(skepp!=this&&skepp.kopplad==false){
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
		else if (string.toLowerCase().equals("refresh")) {
			String allaOnline1 = "";
			String allaOnlineID1="";
			for (Skepp skepp : anslutna) {
				if(skepp!=this&&skepp.kopplad==false){
					allaOnline1+=skepp.namn+",";
				}
			}
			for (Skepp skepp : anslutna) {
				if(skepp!=this&&skepp.kopplad==false){
					allaOnlineID1+=skepp.session.getId()+",";
				}
			}
			if(allaOnline1.length()>0){
				skicka("refresh ="+allaOnline1.substring(0, allaOnline1.length()-1)+";"+allaOnlineID1.substring(0, allaOnlineID1.length()-1));
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
		else if (string.toLowerCase().equals("klar".toLowerCase())) {
			klar=true;
			skickaTillAndra("klar");
			if(andra.klar==true){
				Random random = new Random();
				int rand = random.nextInt(1);
				//Vem b�rjar
				if(rand==0){
					skickaTillAndra("b�daklar start");
					skicka("b�daklar inte");
				}
				else{
						skicka("b�daklar start");
						skickaTillAndra("b�daklar inte");
				}
			}
			skicka("--klar: "+klar + " --- andra.klar: "+andra.klar);
		}
		else if (string.toLowerCase().equals("skjut")||string.toLowerCase().equals("skott")) {
			skickaTillAndra(message);
		}
		else{
			skicka("ERROR! Hittade ingen if-sats med " + string);
			skicka("MESSAGE = --"+message+"--");
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
