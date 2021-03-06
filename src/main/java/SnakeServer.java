import java.io.PrintWriter;
import java.io.StringWriter;
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

	private static final Object LOCK = new Object();

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
			int 
			poäng2=o2.length-3,
			poäng1=o1.length-3;
			if (poäng1<0) {
				poäng1=0;
			}
			if (poäng2<0) {
				poäng2=0;
			}
			int one = (poäng2-poäng1);

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
	//	int clear;


	Thread sendloop=new Thread(){
		public void run() {
			while(session.isOpen()){				
				try {
					synchronized(LOCK){
						LOCK.wait();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					sendAll(e.getStackTrace().toString());
				}
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
				starta();
				if (pause) {

					send(Json.createObjectBuilder()
							.add("data",Json.createArrayBuilder().add(Json.createObjectBuilder()
									.add("type", "pause")
									)).build().toString());
					databuild();
					sendAll();
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
					databuild();
					sendAll();
				}
				else {
					arrayBuilder.add(Json.createObjectBuilder()
							.add("type", "unpause"));
					databuild();
					sendAll();
				}
			}
			scanner.close();
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			send("E "+errors.toString());
		}

	}
	@OnClose 
	public void close(){
		removeList.add(this);
	}
	int errtimes;
	public void send(String string,boolean isLast) {
		try {
			session.getBasicRemote().sendText(string,isLast);
			errtimes=0;
		} catch (Exception e) {
			e.printStackTrace();
			if (errtimes++>100) {
				removeList.add(this);
			}

		}
	}
	public void send(String string) {		 
		send(string,true);
	}
	public void starta(){
		length=3;
		x[1]=-1;
		y[1]=-1;
		x[2]=-1;
		y[2]=-1;
	}
	public void reset(){
		//		for (int i = 0; i < x.length; i++) {
		//			x[i]=-1;
		//			y[i]=-1;
		//		}
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
			length = 1;
			x[0]=posx;
			y[0]=posy;
		}
		//		JsonArrayBuilder pixels=Json.createArrayBuilder();
		//		for (int i = 0; i < length; i++) {
		//			pixels.add(Json.createObjectBuilder()
		//					.add("X", x[i])
		//					.add("Y", y[i]));
		//		}
		//		this.pixels=pixels.build();
		highscoreBool=true;
		fördröjning=10;

	}
	void gameover(String orsak){
//		arrayBuilder.add(Json.createObjectBuilder()
//				.add("type", "gameover")
//				.add("namn", namn)
//				.add("orsak", orsak)
//				);
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
			snakeServer.starta();
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

		//			snake.send(message);


		synchronized(LOCK){
			LOCK.notifyAll();
		}
//				sendAll(message);
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
			if(poäng<0)
				poäng=0;
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
		long date = System.currentTimeMillis(),date2,date3 = 0,date4=0,date5=0,date6=0,date7 = 0,date8=0;
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
		date2 = System.currentTimeMillis() ;
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
					if (--snake.fördröjning<=0) {
//						arrayBuilder.add(Json.createObjectBuilder()
//								.add("type", "cleangameover"));
						snake.starta();
					}
				}
				snake.senasteriktning=snake.riktning;
			}
			//Förlustkontroll
			date3 = System.currentTimeMillis();
			for (SnakeServer snake : snakes) {
				dennasnake:if(snake.fördröjning<0){
					//Kolla om munnen åker ur bild
					if (snake.x[0]<0||snake.y[0]<0||snake.x[0]>=width||snake.y[0]>=height) {
						snake.gameover("urBild");
						break dennasnake;
					}

					//Kolla om munnen nuddar egna kroppen
					for (int i = 1; i < snake.length; i++) {
						if((snake.x[0]==snake.x[i]&&snake.y[0]==snake.y[i])) {
							snake.gameover("nuddaKropp");
							break dennasnake;
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
										break dennasnake;
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
					snake.x[snake.length]=-1;
					snake.y[snake.length]=-1;
					snake.length++;
					plupp();
				}
			}
			date5 = System.currentTimeMillis();
			date6 = databuild();
			date7 = System.currentTimeMillis();
			sendAll();

		}
		catch(Exception e){
			sendAll("SERVERUPDATEEXEPTION");
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			sendAll("E "+errors.toString());
		}
		date8 = System.currentTimeMillis();
		long diff=date8-date;
		if (diff>4) {
			arrayBuilder.add(Json.createObjectBuilder().add("type", "delay")
					.add("delay", "Total:"+diff+
							" Rem:"+(date2-date)+
							" Move:"+(date3-date2)+
							" Förl:"+(date4-date3)+
							" Poäng:"+(date5-date4)+
							" BuildLoad:"+(date6-date5)+
							" Build:"+(date7-date6)+
							" Send:"+(date8-date7)));
		}
	}
	//	private JsonArrayBuilder pixels;
	private static long databuild() {
		JsonArrayBuilder array=Json.createArrayBuilder();

		for (SnakeServer snake : snakes) {
//			JsonArrayBuilder X=Json.createArrayBuilder();
//			JsonArrayBuilder Y=Json.createArrayBuilder();
			JsonArrayBuilder player=Json.createArrayBuilder();
			player.add(snake.färg);
			for (int i = 0; i < snake.length; i++) {
				player.add(snake.x[i]);
				player.add(snake.y[i]);
			}
			array.add(player);
			//			if (snake.lastlength=snake.length) {
			//				
			//			}
			//			snake.pixels.set
			//			if (snake.pixels.size()==snake.length) {
			//				snake.pixels.remove(snake.length-1);
			//			}
			//			snake.pixels.add(Json.createObjectBuilder()
			//						.add("X", snake.x[0])
			//						.add("Y", snake.y[0]).build());
			//
			//			pixels.add(Json.createObjectBuilder()
			//					.add("X", snake.x[snake.length-1])
			//					.add("Y", snake.y[snake.length-1]));
//			array.add(Json.createObjectBuilder()
//					.add("färg", snake.färg)
//					.add("X", X)
//					.add("Y", Y));

			//			if (snake.clear==0) {
			//				
			//			}
		}
		arrayBuilder.add(Json.createObjectBuilder()
				.add("type", "players")
				.add("players", array));

		long b=System.currentTimeMillis();
		if (highscoreBool) {
			highscore();
		}
		message=Json.createObjectBuilder().add("data",arrayBuilder).build().toString();
		return b;
	}
}
