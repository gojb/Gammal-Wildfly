
import java.text.*;
import java.util.*;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/chat")
public class Chatta {
	private static final ArrayList<Person> personer = new ArrayList<>();
	private static int antalOkändaPers = 0;
	private static SimpleDateFormat isoFormat = new SimpleDateFormat("HH:mm:ss");

	private static ArrayList<Person> removelist = new ArrayList<>();
	private Person person;

	@OnOpen
	public void start(Session session) {
		person=new Person(session);
		isoFormat.setTimeZone(TimeZone.getTimeZone("CET"));
	}
	@OnClose
	public void close() {
		removelist.add(person);
		skickaLämna(person.name);
	}
	@OnMessage
	public void inkommande(String message) {
		if (person.name==null) {
			person.name = message.substring(3, message.length());
			if (person.name.equals("")) {
				person.name="Okänd " + ++antalOkändaPers;
			}
			person.lang=message.substring(0,3);
			if (person.isEng()) {
				utgående(person,"*Welcome to GoJbs Chat "+person.name+"! Users online: "+getPersonerNamn());
			}
			else if (person.isSwe()) {
				
				utgående(person,"*Hej och välkommen till GoJbs Chat "+person.name+"! Klockan är "+ isoFormat.format(new Date()) +" och "+getPersonerNamn()+"är online.");
			}
			else {
				utgående(person, "LANGERROR1: " + person.lang);
			}
			skickaAnslut(person.name);
			personer.add(person);

		}
		else {
			skickaAlla("["+isoFormat.format(new Date())+"] "+person.name+": "+ message);
		}
	}

	private static synchronized void utgående(Person person,String message) {
		try {
			synchronized (person.session) {
				person.session.getBasicRemote().sendText(message);
			} 
		}
		catch (Exception e) {
			removelist.add(person);	
		}
	}
	private static void skickaAlla(String message) {
		for (Person person:personer) {
			utgående(person, message);
		}
		removelist();
	}
	public static void skickaAnslut(String namn){
		for (Person person:personer) {
			if (person.isEng()) {
				utgående(person,"*" + namn +" connected.");
			}
			else if (person.isSwe()) {
				utgående(person,"*" + namn +" har anslutit.");
			}
		}
		removelist();
	}
	public static void skickaLämna(String namn){
		for (Person person:personer) {
			if (person.isEng()) {
				utgående(person,"*" + namn + " left.");
			}
			else if (person.isSwe()) {
				utgående(person,"*" + namn + " har lämnat.");
			}
		}
		
		removelist();
	}
	public static void removelist(){
		for (Person person : removelist) {
			personer.remove(person);
		}
		removelist.clear();
	}
	public String getPersonerNamn() {
		String string="";
		if (person.isSwe()) {
			for (Person person : personer) {
				string += person.name + ", ";
			}
			try {
				string = string.substring(0, string.length() - 2) + " ";
			} catch (Exception e) {
				string = "ingen annan ";
			} 
		}
		else if (person.isEng()) {
			for (Person person : personer) {
				string += person.name + ", ";
			}
			try {
				string = string.substring(0, string.length() - 2) + " ";
			} catch (Exception e) {
				string = "none";
			} 
		}

		else {
			string="LANGERROR2 " + person.lang;
		}
		return string;

	}
}
class Person{
	String name;
	Session session;
	String lang;
	public Person(Session session) {
		this.session = session;
	}
	public boolean isEng(){
		return lang.equals("ENG");
	}
	public boolean isSwe(){
		return lang.equals("SWE");
	}
}
