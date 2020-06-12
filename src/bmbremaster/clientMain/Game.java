package bmbremaster.clientMain;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.io.IOException;

import bmbremaster.client.Client;
import bmbremaster.graphics.Assets;
import bmbremaster.graphics.GameInfo;
import bmbremaster.server.Msg;
import bmbremaster.tiles.Tiles;
import bmbremaster.tiles.blocks.Bomb;
import bmbremaster.tiles.blocks.Bricks;
import bmbremaster.tiles.blocks.Concrete;
import bmbremaster.tiles.blocks.Fire;
import bmbremaster.tiles.players.Player;

public class Game implements Runnable {
	
	private ClientWindow window;
	private Thread thread;
	private boolean running = false;
	private Client client;
	private KeyInput keyboard;
	private GameInfo gameInfo;
	private boolean connected = false, errorMsg = false;
	
	private BufferStrategy bs;
	private Graphics g;
	
	private int width, height;
	private String title;


	public Game(String title, int width, int height) {
		this.width = width;
		this.height = height;
		this.title = title;
		client = new Client();
		window = new ClientWindow(title, width, height, client);
		keyboard = new KeyInput();
		window.getCanvas().addKeyListener(keyboard);
		gameInfo = new GameInfo();
	}
	
	private void tick() {
		Msg coords = new Msg();
		//Load coordinates
		try {
			coords = client.getMessage();
			if(this.connected == false) {
				this.connected = true;
				errorMsg = true;
				window.getChat().connected = true;
			}
			window.getChat().printToConsole(coords.getText());
			gameInfo.getPlayer(0).setX(coords.p1x);
			gameInfo.getPlayer(0).setY(coords.p1y);		
			gameInfo.getPlayer(0).setHealth(coords.p1h);
			gameInfo.getPlayer(1).setX(coords.p2x);
			gameInfo.getPlayer(1).setY(coords.p2y);
			gameInfo.getPlayer(1).setHealth(coords.p2h);
			
			gameInfo.resetConcretes();
			for( Dimension cord : coords.getConcretes() ) {
				gameInfo.addConcrete(new Concrete( cord.width, cord.height, Tiles.TILE_SIZE, Tiles.TILE_SIZE ));
			}
			
			gameInfo.resetBombs();
			for( Dimension cord : coords.getBombs() )
				gameInfo.addBomb(new Bomb( cord.width, cord.height, Tiles.TILE_SIZE, Tiles.TILE_SIZE ));
			
			gameInfo.resetFire();
			for( Fire.FireCoords cord : coords.getFireCoords() )
				gameInfo.addFire(new Fire( cord.x, cord.y, cord.width, cord.height, cord.direction ));
			
			gameInfo.resetBricks();
			for( Dimension cord : coords.getBricks() ) {
				gameInfo.addBrick(new Bricks(cord.width, cord.height, Tiles.TILE_SIZE, Tiles.TILE_SIZE));
			}
			
		} catch (Exception e) {
			this.connected = false;
			window.getChat().connected = false;
			if(errorMsg)
				window.getChat().printToConsole("One of players has disconnected.\nTry to connect again.");
			errorMsg = false;
		}
		
	}
	
	private void render() {
		bs = window.getCanvas().getBufferStrategy();
		if(bs == null ) {
			window.getCanvas().createBufferStrategy(3);
			return;
		}
		
		g = bs.getDrawGraphics();
		if(connected) {
			g.clearRect(0, 0, width, height);
			drawGame(g);
			
			bs.show();
			g.dispose();
		} else {
			g.clearRect(0, 0, width, height);
			bs.show();
			g.dispose();
		}
		
	}
	
	private void send() { 
		try {
			client.sendMessage(new Msg(keyboard.left, keyboard.right, keyboard.up, keyboard.down, keyboard.space));	
		} catch (IOException e) {
			this.connected = false;
			window.getChat().connected = false;
		}
	}
	
	private void init() {
		Assets.init();
		gameInfo.addPlayer(new Player(0,0,0));
		gameInfo.addPlayer(new Player(0,0,1));
	}
	
	private void drawGame(Graphics g) {
		
		//draw grass background
		g.setColor(new Color(0x54DC35));
		g.fillRect(0, 0, 800, 800);
		
		 for(int j = 0; j < gameInfo.getFireSize(); j++)
				gameInfo.getFire(j).render(g);

		//steel edges
		g.drawImage( Assets.steelVertical, 0, 0, 10, Assets.HEIGHT, null );
		g.drawImage( Assets.steelVertical, Assets.WIDTH - 10, 0, 10, Assets.HEIGHT, null );
		g.drawImage( Assets.steelHorizontal, 10, 0, Assets.WIDTH - 20, 10, null );
		g.drawImage( Assets.steelHorizontal, 10, Assets.HEIGHT - 10, Assets.WIDTH - 20, 10, null );
		
		//wallz
		int y = Tiles.TILE_SIZE;
        for( int i = 0; i < 13; i++ ) {
            int j = 0;
            while( j < 13 ) {
                if( i == 0  || i == 12 )
                    g.drawImage( Assets.concrete, i * y + 10 , j * y + 10, null );
                if( (j == 0 || j == 12) && i != 12 ) 
                    g.drawImage( Assets.concrete, (i+1) * y + 10 , j * y + 10, null );
                if( i == 0 || i == 12 )
                    j++;
                else 
                    j+=2;
            }
        }
        
		for( int j = 0; j < gameInfo.getConcreteSize(); j ++ )
			gameInfo.getConcrete(j).render(g);
		for( int j = 0; j < gameInfo.getBricksSize(); j++ )
			gameInfo.getBrick(j).render(g);
		for( int j = 0; j < gameInfo.getBombsSize(); j ++ )
			gameInfo.getBomb(j).render(g);	
		
		gameInfo.getPlayer(0).render(g);
		gameInfo.getPlayer(1).render(g);
			
		drawHealthBars(gameInfo.getPlayer(0).getHealth(), gameInfo.getPlayer(1).getHealth(), g);
		
	}
	
	public void drawHealthBars( int health1, int health2, Graphics g ) {
		int greenValue1 = health1*2;
		greenValue1 = Player.clamp(greenValue1, 0, 255);
		int greenValue2 = health2*2;
		greenValue2 = Player.clamp(greenValue2, 0, 255);
		int localHealth1 = Player.clamp(health1, 0, Player.DEFAULT_HEALTH);
		int localHealth2 = 100 - Player.clamp(health2, 0, Player.DEFAULT_HEALTH);
		
		g.setColor( Color.gray );
		g.fillRect( 16, 16, 200, 16 );
		g.setColor( new Color(100, greenValue1, 0 ) );
		g.fillRect( 16, 16, localHealth1 * 2, 16 );
		g.setColor( Color.white );
		g.drawRect( 16, 16, 200, 16 );
		
		g.setColor( Color.gray );
		g.fillRect( Assets.WIDTH - 16 - 200, 16, 200, 16 );
		g.setColor( new Color(100, greenValue2, 0 ) );
		g.fillRect( Assets.WIDTH - 16 - 200 + localHealth2*2, 16, 200 - localHealth2*2, 16 );
		g.setColor( Color.white );
		g.drawRect( Assets.WIDTH - 16 - 200, 16, 200, 16 );
	}
	
	public void run() {
		init();
		
		int fps = 60;
		double timePerTick = 1000000000 / fps;	//nanoseconds
		double delta = 0;
		long now;
		long lastTime = System.nanoTime();
		//long timer = 0;
		//long ticks = 0;
		
		while(running) {
			now = System.nanoTime();
			delta += (now - lastTime) / timePerTick;
			//timer += now - lastTime;
			lastTime = now;
			
			if(delta >= 1) {
				tick();
				render();
				if(connected) {
				keyboard.update();
				send();
				}
				//ticks++;
				delta--;
			}
			//if(timer >= 1000000000) {
			//	System.out.println("Ticks and frames: " + ticks);
			//	ticks = 0;
			//	timer = 0;
			//}
		}
		
		stop();
	}
	
	public synchronized void start() {
		if(running)
			return;
		running = true;
		thread = new Thread(this);
		thread.start();
	}
	
	public synchronized void stop() {
		if(!running)
			return;
		running = false;
		try {
			thread.join();
		} catch(InterruptedException e ) {
			e.printStackTrace();
		}
	}
	
	public String getTitle() {
		return title;
	}
}
