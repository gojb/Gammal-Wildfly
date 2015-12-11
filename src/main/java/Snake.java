import java.awt.Color;

import java.util.*;

import javax.swing.Timer;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/snake")
public class Snake {
	public static ArrayList<Snake> snakes = new ArrayList<Snake>();
	public static Timer timer = new Timer(100,e->update());
	public static Random random = new Random();
	public static final int height = 50;
	public static final int width = 50;
	public static boolean gameover,pause;
	public static int pluppX,pluppY;
	public static String overname;

	public Session session;
	public int[] x=new int[1000],y=new int[1000];
	public int length;
	public String riktning;
	public Color färg;
	public String namn;

	@OnMessage
	public void in(String message){
		Scanner scanner;
		try {
			scanner = new Scanner(message);
			String string=scanner.next();
			if (string.equals("D")) {
				färg = new Color(Integer.parseInt(scanner.next()));
				namn = scanner.next();

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
				reset();
			}

			else if (string.equals("R")) {
				riktning=scanner.next();
				send("ny riktning:" +riktning );
			}
			else if(string.equals("pause")){

			}
			scanner.close();
		} catch (Exception e) {
			send(e.toString());
		}
	}
	@OnOpen
	public void open(Session session){
		this.session=session;
		snakes.add(this);
		plupp();
		try{
			sendAll("OPEN");
		}
		catch(Exception e){

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
	public static void sendAll(String message){
		for (Snake snake : snakes) {
			snake.send(message);
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
			send(posx+"  "+posy);
		}
		gameover=false;
	}
	static void plupp(){
		pluppX = random.nextInt(width);
		pluppY = random.nextInt(height);
	}
	static void gameover(String string){
		sendAll("gameover"+string);
		gameover=true;
		overname=string;
	}
	public static void update() {
		try{
			if (!gameover) {
				//Gör alla förflyttningar
				for (Snake snake : snakes) {
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
				for (Snake snake : snakes) {
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
					for (Snake snake2 : snakes) {
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
				for (Snake snake : snakes) {
					if (snake.x[0]==pluppX&&snake.y[0]==pluppY) {
//						snake.x[snake.length-1]=-1;
//						snake.y[snake.length-1]=-1;
						snake.length++;
						plupp();
					}
				}
				//Skicka data till spelarna
				for (Snake snake : snakes) {
					if (gameover) {
						snake.send("A GAMEOVER " + snake.namn);
					}
					else if (pause) {
						snake.send("A PAUSE");
					}

					snake.send("CLEAR");
					for (Snake snake2 : snakes) {
						String string = "";
						for (int i = 0; i < snake2.length; i++) {
							int x = snake2.x[i];
							int y = snake2.y[i];
							string+=x+" "+y+" ";
						}
						snake.send("B "+snake2.färg.getRGB()+" "+string);
					}
					snake.send("P " + pluppX + " " + pluppY);
				}
			}
		}catch(Exception e){
			snakes.get(0).send("UPDATEEXEPTION ");
			for (Snake snake : snakes) { 
				snake.send("UPDATEEXEPTION ");
			}

		}
	}

}
