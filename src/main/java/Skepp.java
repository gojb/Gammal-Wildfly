import java.io.IOException;
import java.util.ArrayList;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value="/skepp")
public class Skepp {
	static ArrayList<Skepp> ansutna = new ArrayList<>();
	Session session;
	
	@OnClose
	public void close() {
		ansutna.remove(this);
	}
	@OnError
	public void error(Throwable	throwable) {
		
	}
	@OnOpen
	public void open(Session session) {
		this.session=session;
		ansutna.add(this);
	}

	@OnMessage
	public void taemot(String message) {
		
	}
	public void skicka(String message){
		try {
			session.getBasicRemote().sendText(message);
		} catch (IOException e) {
			System.err.println("Senderror");
		}
	}
	public static void skickaAlla(String message){
		for (Skepp skepp : ansutna) {
			skepp.skicka(message);
		}
	}

}
