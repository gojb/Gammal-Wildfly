import java.util.*;

import javax.json.*;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/snake")
public class SnakeServer {
	public static ArrayList<SnakeServer> snakes = new ArrayList<>(),
			removeList= new ArrayList<>();
	public static Random random = new Random();
	public static final int height = 50;
	public static final int width = 50;
	public static boolean pause;
	public static int pluppX,pluppY;
	public static boolean highscoreBool;
	public static JsonArrayBuilder arrayBuilder=Json.createArrayBuilder();;
	static String message;
	private static final Object lock = new Object();
	public static Thread gameloop=new Thread(){
		@Override
		public void run() {
			while (true) {
				try {
					long i = System.currentTimeMillis();
					if (!pause) {
						update();
					}
					try {
						sleep(i+100-System.currentTimeMillis());
					} 
					catch (IllegalArgumentException e) {
						e.printStackTrace();
						sendAll("E "+e.toString());
					}catch (InterruptedException e) {
						e.printStackTrace();
						sendAll("E "+e.toString());
					}

				} catch (Exception e) {
					e.printStackTrace();
					sendAll("E "+e.toString());
				}
			}
		}

	};
	static Comparator<SnakeServer> comparator = new Comparator<SnakeServer>() {
		@Override
		public int compare(SnakeServer o1, SnakeServer o2) {
			int one = (o2.length-o1.length);

			return one == 0 ? (o2.highscore-o1.highscore) : one;
		}
	};
	static{
		//		timer.start();
		plupp();
		gameloop.start();;
	}

	private Session session;
	private int[] x=new int[1000],y=new int[1000];
	private int length;
	private String riktning,senasteriktning;
	private String färg;
	private String namn;
	private int highscore;
	private int fördröjning;

	Thread sendloop=new Thread(){
		public void run() {
			while(session.isOpen()){				
//				try {
//					synchronized(lock){
//						wait();
//					}
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				send(message);

			}
		};
	};
	@OnOpen
	public void open(Session session){
		this.session=session;
		send("OPEN");

	}
	@OnMessage
	public void in(String message){

		Scanner scanner;
		try {
			scanner = new Scanner(message);
			String string=scanner.next();
			if (string.equals("R")) {
				if (!setRiktning(scanner.next())) {
					if (scanner.hasNext()) {
						setRiktning(scanner.next());
					}
				}

			}
			else if (string.equals("INIT")) {
				sendloop.start();
				färg = scanner.next();
				scanner.useDelimiter("\\z"); 
				namn = scanner.next().substring(1);
				snakes.add(this);
				reset();
				send(Json.createObjectBuilder()
						.add("data",Json.createArrayBuilder().add(Json.createObjectBuilder()
								.add("type", "plupp")
								.add("X", pluppX)
								.add("Y", pluppY)
								)).build().toString());
				send("START");
				fördröjning=-1;
				datasend();
				if (pause) {
					send(Json.createObjectBuilder()
							.add("data",Json.createArrayBuilder().add(Json.createObjectBuilder()
									.add("type", "pause")
									)).build().toString());
				}
			}
			else if (string.equals("RES")) {
				if (pause) {
					update();
				}
				else {
					resetAll();
				}

			}
			else if(string.equals("PAUSE")){
				pause=!pause;
				if (pause) {
					arrayBuilder.add(Json.createObjectBuilder()
							.add("type", "pause"));
					sendAll();
				}
				else {
					arrayBuilder.add(Json.createObjectBuilder()
							.add("type", "unpause"));
					sendAll();
				}
			}
			scanner.close();
		} catch (Exception e) {
			send(e.toString());
		}

	}
	@OnClose 
	public void close(){
		removeList.add(this);
	}

	public void send(String string,boolean isLast) {
		try {
			session.getBasicRemote().sendText(string,isLast);
		} catch (Exception e) {
			e.printStackTrace();
			removeList.add(this);
		}
	}
	public void send(String string) {		 
		send(string,true);
	}
	public void reset(){
		for (int i = 0; i < x.length; i++) {
			x[i]=-1;
			y[i]=-1;
		}
		int posx = random.nextInt(width);
		int posy = random.nextInt(height);

		if (posx>width*0.8||posx<width*0.2||posy>height*0.8||posy<height*0.2) {
			reset();
			return;
		}
		else{		
			String [] arr = {"up", "down", "right", "left"};
			//			setRiktning(arr[random.nextInt(arr.length)]);
			riktning=arr[random.nextInt(arr.length)];
			length = 3;
			x[0]=posx;
			y[0]=posy;
		}
		highscoreBool=true;
		fördröjning=10;

	}
	void gameover(String orsak){
		arrayBuilder.add(Json.createObjectBuilder()
				.add("type", "gameover")
				.add("namn", namn)
				.add("orsak", orsak)
				);
		reset();
	}
	private boolean setRiktning(String nyRiktning) {
		if (!((senasteriktning.equals("up")||senasteriktning.equals("down"))&&(nyRiktning.equals("up")||nyRiktning.equals("down"))||
				(senasteriktning.equals("left")||senasteriktning.equals("right"))&&(nyRiktning.equals("left")||nyRiktning.equals("right")))) {
			riktning=nyRiktning;
			return true;
		}
		else {
			return false;
		}
	}

	public static void resetAll(){
		for (SnakeServer snakeServer : snakes) {
			snakeServer.reset();
			snakeServer.fördröjning=-1;
		}
		plupp();
		pause=false;
	}
	public static void sendAll(String message){
		for (SnakeServer snake : snakes) {
			snake.send(message);
		}
	}
	public static void sendAll(){
		message=Json.createObjectBuilder().add("data",arrayBuilder).build().toString();
			//			snake.send(message);
			synchronized(lock){
				lock.notifyAll();
			}
		arrayBuilder=Json.createArrayBuilder();
	}
	static void plupp(){
		pluppX = random.nextInt(width);
		pluppY = random.nextInt(height);
		arrayBuilder.add(Json.createObjectBuilder()
				.add("type", "plupp")
				.add("X", pluppX)
				.add("Y", pluppY));
		highscoreBool=true;
	}
	static void highscore(){
		ArrayList<SnakeServer> snakes=new ArrayList<>(SnakeServer.snakes);
		snakes.sort(comparator);

		JsonArrayBuilder array=Json.createArrayBuilder();
		for (SnakeServer snake : snakes) {
			int poäng=snake.length-3;
			if (poäng>snake.highscore) {
				snake.highscore=poäng;
			}
			array.add(Json.createObjectBuilder()
					.add("färg",snake.färg)
					.add("namn", snake.namn)
					.add("poäng", poäng)
					.add("highscore", snake.highscore));
		}
		arrayBuilder.add(Json.createObjectBuilder().add("type", "highscore").add("highscore", array));
		highscoreBool=false;
	}
	public static void update() {
		long date = System.currentTimeMillis();
		try {
			if (removeList.size()>0) {
				for (SnakeServer snakeServer : removeList) {

					snakes.remove(snakeServer);
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		long date2 = System.currentTimeMillis(),date3 = 0,date4=0,date5=0,date6=0; 
		try{

			//Gör alla förflyttningar
			for (SnakeServer snake : snakes) {
				if (snake.fördröjning<0) {
					for (int i = snake.length-1 ; i > 0; i--) {
						snake.x[i]=snake.x[i-1];
						snake.y[i]=snake.y[i-1];
					}
					if (snake.riktning.equals("down")) 
						snake.y[0]+=1;
					else if (snake.riktning.equals("up"))
						snake.y[0]-=1;
					else if (snake.riktning.equals("right"))
						snake.x[0]+=1;
					else if (snake.riktning.equals("left"))
						snake.x[0]-=1;
				}
				else{
					if (--snake.fördröjning==0) {
						arrayBuilder.add(Json.createObjectBuilder()
								.add("type", "cleangameover"));
					}
				}
				snake.senasteriktning=snake.riktning;
			}
			//Förlustkontroll
			date3 = System.currentTimeMillis();
			for (SnakeServer snake : snakes) {
				gameoverloop:if(snake.fördröjning<0){
					//Kolla om munnen åker ur bild
					if (snake.x[0]<0||snake.y[0]<0||snake.x[0]>=width||snake.y[0]>=height) {
						snake.gameover("urBild");
						break gameoverloop;
					}

					//Kolla om munnen nuddar egna kroppen
					for (int i = 1; i < snake.length; i++) {
						if((snake.x[0]==snake.x[i]&&snake.y[0]==snake.y[i])) {
							snake.gameover("nuddaKropp");
							break gameoverloop;
						}
					}

					//Kolla om munnen nuddar annans kropp eller mun
					for (SnakeServer snake2 : snakes) {
						if (snake2!=snake) {
							if (snake.x[0]==snake2.x[0]&&snake.y[0]==snake2.y[0]) {
								if (snake2.fördröjning<0) {
									snake.gameover("krock");
								}
								snake2.gameover("krock");
							}
							else if (snake.x[0]==snake2.x[1]&&snake.y[0]==snake2.y[1]&&
									snake.x[1]==snake2.x[0]&&snake.y[1]==snake2.y[0]) {
								snake.gameover("krock");
								snake2.gameover("krock");
							}
							else{
								for (int i = 1; i < snake2.length; i++) {
									if (snake.x[0]==snake2.x[i]&&snake.y[0]==snake2.y[i]){
										snake.gameover("nuddaAnnanKropp");
										break gameoverloop;
									}
								} 
							}
						}
					}
				}
			}
			date4 = System.currentTimeMillis();
			//Poängkontroll
			for (SnakeServer snake : snakes) {
				if (snake.x[0]==pluppX&&snake.y[0]==pluppY) {
					snake.length++;
					plupp();
				}
			}
			date5 = System.currentTimeMillis();
			datasend();

		}
		catch(Exception e){
			sendAll("SERVERUPDATEEXEPTION");
			sendAll("E "+e.toString());
		}
		date6 = System.currentTimeMillis();
		long diff=date6-date;
		if (diff>4) {
			arrayBuilder.add(Json.createObjectBuilder().add("type", "delay")
					.add("delay", "Total"+diff+
							" Rem"+(date2-date)+
							" Move"+(date3-date2)+
							" Förl"+(date4-date3)+
							" Poäng"+(date5-date4)+
							" Send"+(date6-date5)));
		}
	}

	private static void datasend() {
		JsonArrayBuilder array=Json.createArrayBuilder();

		//Skicka data till spelarna
		for (SnakeServer snake : snakes) {
			JsonArrayBuilder pixels=Json.createArrayBuilder();
			for (int i = 0; i < snake.length; i++) {
				pixels.add(Json.createObjectBuilder()
						.add("X", snake.x[i])
						.add("Y", snake.y[i]));
			}
			array.add(Json.createObjectBuilder()
					.add("färg", snake.färg)
					.add("pixels", pixels));
		}
		arrayBuilder.add(Json.createObjectBuilder()
				.add("type", "players")
				.add("players", array));
		if (highscoreBool) {
			highscore();
		}
		sendAll();
	}
}
