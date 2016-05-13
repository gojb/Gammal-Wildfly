import java.awt.Color;

import java.util.*;

import javax.swing.Timer;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/snake")
public class SnakeServer {
	public static ArrayList<SnakeServer> snakes = new ArrayList<SnakeServer>();
	public static Timer timer = new Timer(100,e->update());
	public static Random random = new Random();
	public static final int height = 50;
	public static final int width = 50;
	public static boolean gameover,pause;
	public static int pluppX,pluppY;
	public static String overname;
	static{
		timer.start();
	}

	public Session session;
	public int[] x=new int[1000],y=new int[1000];
	public int length;
	public String riktning;
	public Color färg;
	public String namn;
	public int inactive;


	@OnOpen
	public void open(Session session){
		this.session=session;
		send("OPEN");

	}
	@OnMessage
	public void in(String message){
		inactive=0;
		Scanner scanner;
		try {
			scanner = new Scanner(message);
			String string=scanner.next();
			if (string.equals("R")) {
				String string2 = scanner.next();
				if (!((riktning.equals("up")||riktning.equals("down"))&&(string2.equals("up")||string2.equals("down"))||
						(riktning.equals("left")||riktning.equals("right"))&&(string2.equals("left")||string2.equals("right")))) {
					riktning=string2;
				}

			}
			
			
			
			else if (string.equals("INIT")) {
				färg = new Color(Integer.parseInt(scanner.next()));
				namn = scanner.next();
				snakes.add(this);
				reset();
				send("START");

			}
			else if (string.equals("START")) {
				timer.start();
			}
			else if (string.equals("STOP")) {
				timer.stop();
			}
			else if (string.equals("RES")) {
				resetAll();
			}
			else if(string.equals("pause")){

			}
			scanner.close();
		} catch (Exception e) {
			send(e.toString());
		}
	}
	@OnClose 
	public void close(){
		snakes.remove(this);
	}
	public void send(String string) {
		try {
			session.getBasicRemote().sendText(string);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		}
		else{		
			String [] arr = {"up", "down", "right", "left"};

			riktning=arr[random.nextInt(arr.length)];
			length = 3;
			x[0]=posx;
			y[0]=posy;
		}
		plupp();
		send("A");
		gameover=false;
	}
	public static void resetAll(){
		for (SnakeServer snakeServer : snakes) {
			snakeServer.reset();
		}
	}
	public static void sendAll(String message){
		for (SnakeServer snake : snakes) {
			snake.send(message);
		}
	}
	static void plupp(){
		pluppX = random.nextInt(width);
		pluppY = random.nextInt(height);
	}
	static void gameover(String string){
		sendAll("A GAMEOVER "+string);
		gameover=true;
		overname=string;
	}
	public static void update() {
		try{
			if (!gameover) {
				//Gör alla förflyttningar
				for (SnakeServer snake : snakes) {
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
				//Förlustkontroll
				for (SnakeServer snake : snakes) {
					for (int i = 1; i < snake.length; i++) {
						//Kolla om munnen nuddar egna kroppen
						if((snake.x[0]==snake.x[i]&&snake.y[0]==snake.y[i])) {
							gameover(snake.namn);
							//							sendAll("1");
							return;
						}
					}
					//Kolla om munnen åker ur bild
					if ((snake.x[0]<0||snake.y[0]<0)||snake.x[0]>=width||snake.y[0]>=height) {
						gameover(snake.namn);
						//						sendAll("2");
						return;
					}
					//Kolla om munnen nuddar annans kropp eller mun
					for (SnakeServer snake2 : snakes) {
						if (snake2!=snake) {
							for (int i = 0; i < snake2.length; i++) {
								if (snake.x[0]==snake2.x[i]&&snake.y[0]==snake2.y[i]){
									gameover(snake.namn);
									//									sendAll("3");
									return;
								}
							}
						}
					}
				}
				//Poängkontroll
				for (SnakeServer snake : snakes) {
					if (snake.x[0]==pluppX&&snake.y[0]==pluppY) {
						snake.length++;
						plupp();
					}
				}
				//Skicka data till spelarna
				for (SnakeServer snake : snakes) {
					if (snake.inactive++==100) {
						snake.session.close();
						return;
					}
					if (pause) {
						snake.send("A PAUSE");
					}
					else{
//						snake.send("CLEAR");
						snake.send("P " + pluppX + " " + pluppY);
						for (SnakeServer snake2 : snakes) {
							String string = "";
							for (int i = 0; i < snake2.length; i++) {
								int x = snake2.x[i];
								int y = snake2.y[i];
								string+=x+" "+y+" ";
							}
							snake.send("B "+snake2.färg.getRGB()+" "+string);
						}
						
					}
				}
			}
		}
		catch(Exception e){
			sendAll("UPDATEEXEPTION");
		}
	}

}
