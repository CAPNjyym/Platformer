import java.awt.*;
import java.applet.Applet;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import javax.swing.Timer;
import java.applet.AudioClip;

public class Platformer extends Applet implements MouseListener,ActionListener,KeyListener{
	// Sprite constants
	public final int LEFT=0;
	public final int RIGHT=1;
	public final int WALK=0;
	public final int STAND=-1;
	public final int ATTACK=4;
	public final int JUMP=6;
	
	// SOUNDS
	public AudioClip JumpSound=null;
	public AudioClip BGMusic=null;
	
	// sumthin
	public long LastKeyCheck;
	public long LastCheck;
	
	// Images
	public Image[] PlayerSprite;
	public Image spritesheet;
	public Image[] numbers;
	public Image numbersheet;
	public Image background;
	public Image TileLayer;
	public Image MainMenu;
	public Image AddonLayer;
	public Image PL;
	public BufferedImage PixLayer;
	public Image prescrn;
	public Graphics preScrn;
	public Graphics MakeSprite;
	public Graphics m;
	public Image loadScr;
	public Image shadow;
	public Image panel;
	public Image heart;
	public Image bullet;
	
	// Timer
	public int MinLeft;
	public int SecsTens;
	public int SecsOnes;
	public int TimeCounter=0;
	
	// Level & scrolling
	public int CurrentLevel=0;
	public int BGOffset;
	public int TLayOff;
	public int BordLayOff;
	public int TileOffset;
	public boolean LvlLoaded=false;
	
	// Player
	public int Lives;
	public int Health;
	public double PlayerX;
	public double PlayerY;
	public int PlayerPose=STAND;
	public int PlayerPoseFrame=1;
	public int PlayerDirection=1;
	public double Velocity=0;
	public int attackDuration=0;
	public int stunTime=0;
	public int align;
	public double FrameCount=0;
	public double JumpLoop=-15;
	
	// Player Bullet
	public double PBX=0, PBY=0, PBV=0;
	public boolean PB=false, PBF=false;
	
	// Others: gravity, shadow, gameRunning, mapBlocks, debugMode, NPCs, timer, etc.
	public double Gravity=.8;
	public int ShadowX=0;
	public double ShadowPer=1;
	public boolean gameRunning;
	public boolean MediaLoaded;
	public boolean debugMode = false;
	public ArrayList others;
	public ArrayList bullets;
	public Timer UCtime;
	public Timer jumpTimer;
	public MediaTracker ImageLoader;
	public int ImageCount=1;
	ReadKeys RK;
	
	public void init(){
		gameRunning=false;
		MediaLoaded=false;
		setLayout(null);
		addMouseListener(this);
		addKeyListener(this);
		RK=new ReadKeys();
		new Thread(RK).start();
		
		ImageLoader=new MediaTracker(this);
		//spritesheet=new Image;
		PlayerSprite=new Image[8];
		numbers=new Image[11];
		
		m=getGraphics();
		spritesheet=getImage(getDocumentBase(),"platformer/Sprite/left.png");
		//spritesheet[1]=getImage(getDocumentBase(),"platformer/Sprite/right.png");
		shadow=getImage(getDocumentBase(),"platformer/Sprite/shadow.png");
		loadScr=getImage(getDocumentBase(),"platformer/GUI/loading.png");
		MainMenu=getImage(getDocumentBase(),"platformer/GUI/MainMenu.png");
		panel=getImage(getDocumentBase(),"platformer/GUI/panel.png");
		heart=getImage(getDocumentBase(),"platformer/GUI/hearts.png");
		numbersheet=getImage(getDocumentBase(),"platformer/GUI/numbers.png");
		bullet=getImage(getDocumentBase(),"platformer/sprite/bullet.png");
		LoadIMG(bullet);
		
		//INITIALIZE SOUNDS
		JumpSound=getAudioClip(getDocumentBase(),"platformer/Sounds/jump.wav");
		BGMusic=getAudioClip(getDocumentBase(),"platformer/Sounds/Music/music.wav");
		
		
		MainMenu();
	}
	
	public void paint(Graphics G){
		if(gameRunning){
			m=getGraphics();
			BufferedImage spr;
			
			// Pix Layer
			spr=new BufferedImage(PL.getWidth(null),800,BufferedImage.TYPE_INT_ARGB);
			MakeSprite=spr.getGraphics();
			MakeSprite.drawImage(PL,0,0,PL.getWidth(null),800,this);
			PixLayer=spr;
			
			// main char
			//for(int d=0;d<2;d++){
				for(int i=0;i<8;i++){
					spr=new BufferedImage(96,85,BufferedImage.TYPE_INT_ARGB);
					MakeSprite=spr.getGraphics();
					MakeSprite.drawImage(spritesheet,(96*i*-1),0,768,85,this);
					PlayerSprite[i]=spr;
				}
			// }
			
			// GUI
			BufferedImage num;
			for(int d=0;d<11;d++){
				num=new BufferedImage(23,40,BufferedImage.TYPE_INT_ARGB);
				MakeSprite=num.getGraphics();
				MakeSprite.drawImage(numbersheet,(23*d*-1),0,253,40,this);
				numbers[d]=num;
			}
			
			// NPCs
			NPC temp;
			for (int i=0;i<others.size();i++){
				
				temp = (NPC) others.get(i);
				if(!temp.getName().equals("bullet")){
					temp.setSheet(getImage(getDocumentBase(),"platformer/Sprite/" + temp.getName() + ".png"));
					
					for(int j=0;j<7;j++){
						spr=new BufferedImage(temp.getWidth(),temp.getHeight(),BufferedImage.TYPE_INT_ARGB);
						MakeSprite=spr.getGraphics();
						MakeSprite.drawImage(temp.getSheet(),(temp.getWidth()*j*-1),0,7*temp.getWidth(),temp.getHeight(),this);
						temp.setSprite(j,spr);
					}
				}
			}
		}
		
		UpdateScreen();
	}
	
	public void MainMenu(){
		m.drawImage(MainMenu,0,0,800,600,this);
		PlayMusic pm=new PlayMusic(BGMusic);
		new Thread(pm).start();
		Lives = 5;
	}
	
	public void StartGame(){
		UCtime=new Timer(1,this);
		UCtime.start();
	}
	
	public void UpdateChar(){
		if (attackDuration>0){
			PlayerPose=ATTACK;
			PlayerPoseFrame=2;
			
			attackDuration--;
			if (attackDuration==0){
				if(!RK.jump()){
					PlayerPose=STAND;
				}
				else
					PlayerPose=JUMP;
				PlayerPoseFrame=1;
			}
		}
		
		else{
			if(RK.attack()){
				PlayerPose=ATTACK;
				PlayerPoseFrame=2;
				RK.attack(false);
				attackDuration = 50;
			}
			
			else if((RK.ML())&&(!RK.MR())){  //  if moving left
				PlayerDirection=0;
				RK.ML(true);
				if(RK.jump())
					PlayerPose=JUMP;
				if(!RK.jump()){
					PlayerPose=WALK;
					FrameCount+=.5;
					if(FrameCount>3){
						FrameCount=0;
						PlayerPoseFrame++;
						checkFrame();
					}
					if (TLayOff == -200)
						PlayerX-=1;
				}
				
				if (TLayOff < -200){ // if able to scroll screen right
					PlayerX = 350;
					BGOffset+=1;
					TLayOff+=3;
					
					NPC temp;
					for(int i=0;i<others.size();i++){
						temp = (NPC) others.get(i);
						temp.setCoordinates(temp.getX()+3, temp.getY());
					}
				}
				else if (PlayerX <= -10)
					PlayerX = -10;
				else
					PlayerX-=2;
			}
			
			else if((RK.MR())&&(!RK.ML())){ // if moving right
				PlayerDirection=1;
				RK.MR(true);
				if(RK.jump())
					PlayerPose=JUMP;
				if(!RK.jump()){
					PlayerPose=WALK;
					FrameCount+=.5;
					if(FrameCount>3){
						FrameCount=0;
						PlayerPoseFrame++;
						checkFrame();
					}
					if(PlayerX<=350)
						PlayerX+=1;
				}
				
				if (PlayerX >= 350){
					PlayerX = 350;
					BGOffset-=1;
					TLayOff-=3;
					
					NPC temp;
					for(int i=0;i<others.size();i++){
						temp = (NPC) others.get(i);
						temp.setCoordinates(temp.getX()-3, temp.getY());
					}
				}
				else
					PlayerX+=2;
			}
			
			if(RK.jump()&&RK.ground()){ // if trying to jump and on the ground then allows jump
				PlayerPoseFrame=1;
				PlayerPose=JUMP;
				Velocity=15;  // jumps up
				RK.jump(true);
				RK.ground(false);
				RK.GoinUp(true);
			}
		}
		
		gravity();
		
		if (stunTime > 0)
			stunTime--;
		if (PlayerY > 600 || Health <=0)
			die();
		else if (1000-TLayOff > PL.getWidth(null)){
			endLevel();
			nextLevel();
		}
	}
	
	public void gravity(){ // this is gravity constantly acting on the character
		double prevY = PlayerY;
		
		if(Velocity<0){
			RK.GoinUp(false);
			RK.ground(false);
			if (attackDuration==0){
				PlayerPoseFrame=1;
				PlayerPose=JUMP;
			}
		}
		PlayerY-=Velocity*Gravity;
		Velocity-=.4;
		if(PlayerY<0){
			PlayerY=0;
			Velocity=0;
		}
		
		ShadowX=FindNextPlat((int)PlayerY+80);
		ShadowPer=1.3-CalcDist();
		
		if (!RK.GoinUp()){
			int i, pix = -1;
			for(i=(int)PlayerY+2;i>=prevY;i--){
				if (PixLayer.getRGB((int)PlayerX+48-TLayOff,i+85) < -8388608)
					pix = i;
			}
			
			if (pix != -1){
				PlayerY = pix-1;
				Velocity = 0;
				RK.ground(true);
				RK.jump(false);
				if((!RK.ML())&&(!RK.MR())&&(attackDuration<=0)){
					PlayerPose=STAND;
					PlayerPoseFrame=1;
				}
			}
		}
	}
	
	public void UpdateNPCs(){
		boolean damage = false;
		NPC temp;
		
		for(int i=0;i<others.size();i++){
			temp = (NPC) others.get(i);
			if (temp instanceof Enemy1)
				temp = (Enemy1) temp;
			else if (temp instanceof Enemy2)
				temp = (Enemy2) temp;
			else if (temp instanceof Bullet)
				temp = (Bullet) temp;
			
			if (temp.getY() > 600 || temp.fadeTimer() > 50){
				others.remove(i);
				i--;
			}
			else if ((temp instanceof Bullet)&&(temp.damage() || temp.getX() > 800 || temp.getX() + temp.getWidth() < 0)){
				if (temp.damage())
					damage = true;
				others.remove(i);
				i--;
			}
			
			else if (temp.fadeTimer() == 0){
				temp.act();
				if (temp instanceof Enemy2){
					Enemy2 temp2 = (Enemy2) temp;
					if (temp2.firing()){
						Bullet temp3 = temp2.fireBullet(10.2);
						temp3.setSheet(bullet);
						temp3.setSprite(0, bullet);
						others.add(0, temp3);
					}
				}
				
				if (!damage)
					damage = temp.damage();
			}
			//if (i==0)
				//showStatus("Player X: "+(int)PlayerX+"     x:"+temp.getX()+"     "+temp.killed());
		}
		
		if (damage&&stunTime==0){
			Health--;
			stunTime = 50;
		}
	}
	
	public void UpdateScreen(){
		if(preScrn==null){
			prescrn=createImage(800,600);
			preScrn=prescrn.getGraphics();
		}
		if(!gameRunning){
			m.drawImage(MainMenu,0,0,800,600,this);
		}
		
		/*if(gameRunning && !LvlLoaded){
			m.drawImage(loadScr,0,0,800,600,this);
			Pause(2000);
			LvlLoaded=true;
		}*/
		
		else if(gameRunning){
			if(ImageLoader.checkAll()||MediaLoaded){
				MediaLoaded=true;
				preScrn.drawImage(background,BGOffset,0,background.getWidth(null),600,this);
				preScrn.drawImage(TileLayer,TLayOff,0,TileLayer.getWidth(null),600,this);
				
				NPC temp;
				if (debugMode){					
					// debug PixLayer
					preScrn.drawImage(PixLayer,TLayOff,0,PL.getWidth(null),800,this);
					
					// debug PlayerBullet
					if (PB){
						preScrn.setColor(Color.green);
						if (PBF)
							preScrn.drawRect((int)PBX-26,(int)PBY,-26,10);
						else
							preScrn.drawRect((int)PBX,(int)PBY,26,10);
					}
					
					// debug NPCs
					preScrn.setColor(Color.red);
					for(int i=0;i<others.size();i++){
						temp = (NPC) others.get(i);
						if ((temp.getX() > -140)&&(temp.getX() < 840))
							preScrn.drawRect(temp.getX(), temp.getY(), temp.getWidth(), temp.getHeight());
					}
					
					// debug character
					preScrn.setColor(Color.blue);
					preScrn.drawRect((int)PlayerX+21,(int)PlayerY,55,85);
				}
				
				// draw PlayerBullet
				if (PB){
					if (PBF)
						preScrn.drawImage(bullet,(int)PBX-26,(int)PBY,-26,10,this);
					else
						preScrn.drawImage(bullet,(int)PBX,(int)PBY,26,10,this);
				}
				
				// draw NPCs
				for(int i=0;i<others.size();i++){
					temp = (NPC) others.get(i);
					if ((temp.getX() > -1*temp.getWidth())&&(temp.getX() < 800)){
						if (!temp.killed()||temp.fadeTimer()%2==0){
							if ((temp.shadowSize() > 0)&&((int)temp.shadowY() > temp.getY()))
								preScrn.drawImage(shadow, temp.shadowX(), (int)temp.shadowY()-5, (int)(54*temp.shadowSize()), (int)(9*temp.shadowSize()), this);
							
							if (temp.facingRight())
								preScrn.drawImage(temp.getSprite(), temp.getX() + temp.getWidth(), temp.getY(), -1*temp.getWidth(), temp.getHeight(), this);
							else
								preScrn.drawImage(temp.getSprite(), temp.getX(), temp.getY(), temp.getWidth(), temp.getHeight(), this);
						}
					}
				}
				
				// draw character
				if (stunTime % 2 == 0){
					if ((ShadowPer > 0)&&(ShadowX > (int)PlayerY))
						preScrn.drawImage(shadow,(int)(26+PlayerX+18*CalcDist()-3.9*ShadowPer),ShadowX-(int)(5*ShadowPer),(int)(42.5*ShadowPer),(int)(7*ShadowPer),this);
					if (PlayerDirection == 0)
						preScrn.drawImage(PlayerSprite[PlayerPose+PlayerPoseFrame],(int)PlayerX,(int)PlayerY,96,85,this);
					else if (PlayerDirection == 1)
						preScrn.drawImage(PlayerSprite[PlayerPose+PlayerPoseFrame],(int)PlayerX+96,(int)PlayerY,-96,85,this);
				}
				if (!debugMode)
					preScrn.drawImage(AddonLayer,TLayOff,0,AddonLayer.getWidth(null),600,this);
					DrawGUI();
				m.drawImage(prescrn,0,0,null);
			}
		}
	}
	
	public double CalcDist(){
		return (ShadowX-PlayerY)/300.0;
	}
	
	public int FindNextPlat(int Y){
		for(int i=Y;i<600;i++)
			if (PixLayer.getRGB((int)PlayerX+48-TLayOff,i) < -8388608)
				return i-1;
		return -1;
	}
	
	public void die(){
		// death animation: simply falls over and then returns to begin of level or game over
		
		Lives--;
		if (Lives > 0){
			CurrentLevel--;
			nextLevel();
		}
		else{
			// game over animation: single screen + se
			
			gameRunning = false;
			Lives++;
			MainMenu();
		}
	}
	
	public void DrawGUI(){
		preScrn.drawImage(panel,800-261,0,261,118,this);
		preScrn.drawImage(numbers[Lives],800-110,60,23,40,this);
		preScrn.drawImage(numbers[MinLeft],800-230,7,23,40,this);
		preScrn.drawImage(numbers[10],800-215,5,23,40,this);
		preScrn.drawImage(numbers[SecsTens],800-200,5,23,40,this);
		preScrn.drawImage(numbers[SecsOnes],800-185,7,23,40,this);
		//preScrn.drawImage(heart, 800-60,15,45,45,this);
		for(int i=0;i<Health;i++)
			preScrn.drawImage(heart,800-(i*45)-48,10,45,45,this);
	}
	
	public void endLevel(){
		while (PlayerY < 500){
			UpdateNPCs();
			UpdateScreen();
			PlayerX+=3;
			gravity();
		}
		PlayerY=500;
		
		while (PlayerX < 800){
			PlayerX+=3;
			PlayerPose=WALK;
			FrameCount+=.5;
			if(FrameCount>3){
				FrameCount=0;
				PlayerPoseFrame++;
				checkFrame();
			}
			UpdateNPCs();
			UpdateScreen();
		}
		PlayerX=-100;
		
		// player walks to the center of the screen, where his heart is then weighed
		if (align >= 0){
			// animation:
			//		scale tips in favor of the weight
			//		char judged as good
			//		char walks right off screen
			while (PlayerX < 800){
				PlayerX++;
				// print shit to screen here
			}
		}
		else{
		  // animation:
			//		scale tips in favor of heart
			//		char judged as evil
			//		floor falls out
			while (PlayerY < 600){
				gravity();
				// print shit to screen here
			}
			CurrentLevel--;
		}
	}
	
	public void nextLevel(){
		CurrentLevel=1;
		BGOffset = 0;
		TLayOff = -200;
		BordLayOff=0;
		TileOffset = 0;
		Health = 3;
		PlayerX = 100;
		PlayerY = 100;
		stunTime = 0;
		align = 5;
		
		MinLeft = 2;
		SecsTens = 0;
		SecsOnes = 0;
		
		others = new ArrayList();
		others.add(new Enemy2(450, 210, 100, false));
		others.add(new Enemy1(3400, 10, 100, true));
		
		background=getImage(getDocumentBase(),"platformer/Level"+CurrentLevel+"/background.png");
		TileLayer=getImage(getDocumentBase(),"platformer/Level"+CurrentLevel+"/TileLayer.png");
		AddonLayer=getImage(getDocumentBase(),"platformer/Level"+CurrentLevel+"/AddonLayer.png");
		PL=getImage(getDocumentBase(),"platformer/Level"+CurrentLevel+"/PixLayer.png");
		LoadIMG(background);
		LoadIMG(TileLayer);
		LoadIMG(AddonLayer);
		LoadIMG(PL);
		try{ImageLoader.waitForAll();}
		catch(Exception e){}
		m.drawImage(loadScr,0,0,800,600,this);
		
		paint(m);
	}
	
	public void LoadIMG(Image a){
		ImageLoader.addImage(a,ImageCount);
		ImageCount++;
	}
	
	public void checkFrame(){
		int f=PlayerPoseFrame;
		if(PlayerPose==JUMP){
			if(f>1)
				PlayerPoseFrame=1;
		}
		else if(PlayerPose==STAND){
			if(f>1)
				PlayerPoseFrame=1;
		}
		else if(PlayerPose==WALK){
			if(f>4)
				PlayerPoseFrame=1;
		}
		else if(PlayerPose==ATTACK){
			if(f>2){
				PlayerPoseFrame=1;
				PlayerPose=STAND;
				RK.attack(false);
			}
		}
	}
	
	public void actionPerformed(ActionEvent e){
		TimeCounter++;
		if(TimeCounter%95==0)
			UpdateTimer();
		UpdateNPCs();
		UpdateChar();
		UpdateScreen();
	}
	
	public void UpdateTimer(){
		SecsOnes--;
		if(SecsOnes==-1){
			SecsOnes=9;
			SecsTens--;
			if(SecsTens==-1){
				SecsTens=5;
				MinLeft--;
				if(MinLeft<0)
					die();
			}
		}
	}
	
	public boolean ClickedPlay(int x, int y){
		if((!gameRunning)&&(x>333)&&(x<474)&&(y>255)&&(y<303))
			return true;
		return false;
	}
	
	public boolean ClickedInstructions(int x,int y){
		if((!gameRunning)&&(x>333)&&(x<474)&&(y>332)&&(y<381))
			return true;
		return false;
	}
	
	public void Pause(int dur){
		try{Thread.currentThread().sleep(dur);}
		catch(InterruptedException ie){}
	}
	
	public void keyPressed(KeyEvent e){
		int keypress=e.getKeyCode();
		if(keypress==37){ // left
			if (attackDuration==0)
				PlayerDirection=0;
			RK.ML(true);
		}
		else if(keypress==39){ // right
			if (attackDuration==0)
				PlayerDirection=1;
			RK.MR(true);
		}
		else if(keypress==32){ // space
			if(!RK.jump()&&(!RK.GoinUp())&&(Velocity>=0)){
				PlaySound pn=new PlaySound(JumpSound);
				new Thread(pn).start();
			}
			RK.jump(true);
		}
		else if(keypress==112){ // F1
			debugMode=!debugMode;
		}
		else if(keypress==90){ // Z
			FrameCount=0;
			RK.attack(true);
		}
		else if(keypress==88){ // X
			FrameCount=0;
			RK.attack(true);
		}
		
		// no-gravity haxx
		else if(keypress==38) // up
			PlayerY--;
		else if(keypress==40) // down
			PlayerY++;
	}
	
	public void keyReleased(KeyEvent e){
		if(e.getKeyCode()==37){
			RK.ML(false);
			
			if((!RK.MR())&&(!RK.jump())&&!RK.attack()){			
				PlayerPose=STAND;
				PlayerPoseFrame=1;
			}
			
			else{
				FrameCount=0;
				PlayerPoseFrame=1;
			}
		}
		
		else if(e.getKeyCode()==39){
			RK.MR(false);
			
			if((!RK.ML())&&(!RK.jump())&&!RK.attack()){
				PlayerPose=STAND;
				PlayerPoseFrame=1;
			}
			
			else{
				FrameCount=0;
				PlayerPoseFrame=1;
			}
		}
		
		UpdateScreen();	
	}
	
	public void mouseReleased(MouseEvent e){
		int x = e.getX(), y = e.getY();
		showStatus(""+ x + " " + y);
		if (ClickedPlay(x, y)){
			showStatus("Clicked Play");
			gameRunning=true;
			nextLevel();
			StartGame();
		}
		else if (ClickedInstructions(x, y))
			showStatus("Clicked Instructions");
	}
	
	class NPC{
		protected int width, height, range, walked = 0, timer = 0;
		protected double x, y, velocity = 0.0, step = 0.0;
		protected boolean facing;
		protected String name;
		protected Image spriteSheet, sprites[];
		
		NPC(){
			x = 400;
			y = 300;
			width = 100;
			height = 100;
			range = 100;
			facing = false;
			name = "";
			sprites = new Image[7];
		}
		
		NPC(double xpos, double ypos, int w, int h, int r, boolean f){
			x = xpos;
			y = ypos;
			width = w;
			height = h;
			range = r;
			facing = f;
			name = "";
			sprites = new Image[7];
		}
		
		public void act(){
			if (facing){
				x++;
				walked++;
				if ((walked >= range)){
					facing = false;
					x-=25;
				}
				step+=.07;
			}
			
			else{
				x--;
				walked--;
				if ((-1*walked >= range)){
					facing = true;
					x+=25;
				}
				step+=.07;
			}
			
			if (step >= 4)
				step = 0.0;
			double prevY = y;
			
			// gravity
			y-=velocity*Gravity;
			velocity-=.4;
			if(y<0){
				y=0;
				velocity=0;
			}
			
			if(ground(prevY) != -1){
				y=ground(prevY) - 1;
				velocity = 0;
			}
		}
		
		public void setCoordinates(double newX, double newY){
			x = newX;
			y = newY;
		}
		
		public void setDimensions(int w, int h){
			width = w;
			height = h;
		}
		
		public void setRange(int r){
			range = r;
		}
		
		public void setFacing(boolean f){
			facing = f;
		}
		
		public void aboutFace(){
			facing = !facing;
		}
		
		public void setName(String n){
			name = n;
		}
		
		public void setSheet(Image S){
			spriteSheet = S;
		}
		
		public void setSprite(int index, Image S){
			sprites[index] = S;
		}
		
		public int getX(){
			return (int) x;
		}
		
		public int getY(){
			return (int) y;
		}
		
		public int getWidth(){
			return width;
		}
		
		public int getHeight(){
			return height;
		}
		
		public int getRange(){
			return range;
		}
		
		public boolean facingRight(){
			return facing;
		}
		
		public String getName(){
			return name;
		}
		
		public Image getSheet(){
			return spriteSheet;
		}
		
		public Image getSprite(int index){
			return sprites[index];
		}
		
		public Image getSprite(){
			return sprites[(int)step];
		}
		
		public boolean killed(){
			if (timer > 0){
				timer++;
				return true;
			}
			else if (attackDuration>0){
				if (PlayerY>y-67&&PlayerY-height<y-65)
				  if ((PlayerDirection==1&&PlayerX>x-100&&PlayerX<x)||(PlayerDirection==0&&PlayerX>x&&PlayerX-width-10<x)){
						timer=1;
						return true;
					}
			}
			if (PB&&PBX>x&&PBX<x&&PBY>y&&PBY<y){
				align--;
				return true;
			}
			return false;
		}
		
		public int ground(double prevY){
			int i, pix = -1;
			for(i=(int)y+2;i>=prevY;i--)
				if (PixLayer.getRGB((int)x+width/2-TLayOff,i+height) < -8388608)
					pix = i;
			return pix;
		}
		
		protected boolean contact(){
			return (contactX() && contactY());
		}
		
		protected boolean contactX(){
			return (PlayerX > x - 52 && PlayerX - width < x - 41);
		}
		
		protected boolean contactY(){
			return (PlayerY > y - 78 && PlayerY - height < y - 8);
		}
		
		public boolean damage(){
			return contact();
		}
		
		public int shadowX(){
			return (int) (x + width/2 - 25.5*shadowSize());
		}
		
		public int shadowY(){
			for (int i=(int)y+height;i<600;i++)
				if (PixLayer.getRGB((int)x+width/2-TLayOff,i) < -8388608)
					return i-1;
			return -1;
		}
		
		public double shadowSize(){
			if (timer == 0)
				return 1.3 - (shadowY()-y)/(3*height);
			return 0;
		}
		
		public int fadeTimer(){
			return timer;
		}
	}
	
	class Enemy1 extends NPC{  // walks around and attacks you
		
		Enemy1(){
			super();
			width = 82;
			height = 95;
			name = "Enemy1";
		}
		
		Enemy1(double xpos, double ypos, int r, boolean f){
			super(xpos, ypos, 82, 95, r, f);
			name = "Enemy1";
		}
		
		public void act(){
			if ((!contact())||(ground(y)==-1))
				super.act();
			else{
				if (PlayerX < x)
					facing = false;
				else
					facing = true;
				
				step+=.13;
				if ((step < 4.0)||(step > 7.0))
					step = 4.0;
				else if (step >= 6.0)
					step -= .10;
			}	
		}
		
		protected boolean contactX(){
			return (PlayerX > x - 62 && PlayerX - width < x - 34);
		}
		
		protected boolean contactY(){
			return (PlayerY > y - 30 && PlayerY - height < y - 20);
		}
		
		public boolean damage(){
			return ((super.contact())||(contactX() && contactY() && step >=6.0));
		}
		
		public int shadowX(){
			if (facing)
				return super.shadowX() - 5;
			return super.shadowX() + 5;
		}
	}
	
	class Enemy2 extends NPC{  // throws objects at you
		
		Enemy2(){
			super();
			width = 95;
			height = 98;
			name = "Enemy2";
		}
		
		Enemy2(double xpos, double ypos, int r, boolean f){
			super(xpos, ypos, 95, 98, r, f);
			name = "Enemy2";
		}
		
		public void act(){
			if ((!contact())||(ground(y)==-1))
				super.act();
			else{
				if (PlayerX < x)
					facing = false;
				else
					facing = true;
				
				step+=.01;
				if (step >= 6.0){
					step += .08;
					if (step >= 7.0)
						step = 5.0;
				}
				else if (step < 4.83)
					step = 4.83;
			}
		}
		
		protected boolean contactX(){
			return ((facing && PlayerX > x)||(!facing && PlayerX < x));
		}
		
		protected boolean contactY(){
			return (PlayerY > y - 32 && PlayerY - height < y - 32);
		}
		
		public boolean damage(){
			return (PlayerX > x - 36 && PlayerX - width < x - 26 && super.contactY());
		}
		
		public int shadowX(){
			if (facing)
				return super.shadowX() - 18;
			return super.shadowX() + 18;
		}
		
		public boolean firing(){
			return (step >= 6.0 && step < 6.09);
		}
		
		public Bullet fireBullet(double v){
			if (facing)
				return new Bullet(v, x+width-14, y+59, facing);
			return new Bullet(v, x-9, y+59, facing);
		}
	}
	
	class Bullet extends NPC{  // bullets shot by Enemy2
		
		Bullet(){
			velocity = 1;
			x = 400;
			y = 300;
			width = 26;
			height = 10;
			facing = true;
			name = "Bullet";
			sprites = new Image[1];
		}
		
		Bullet(double v, double xpos, double ypos, boolean f){
			velocity = v;
			x = xpos;
			y = ypos;
			width = 26;
			height = 10;
			facing = f;
			name = "Bullet";
			sprites = new Image[1];
		}
		
		public void act(){
			if (facing)
				x += velocity;
			else
				x -= velocity;
		}
		
		protected boolean contactX(){
			return (PlayerX > x - 71 && PlayerX - width < x - 27);
		}
		
		protected boolean contactY(){
			return (PlayerY > y - 82 && PlayerY - height < y - 18);
		}
		
		public double shadowSize(){
			return 0;
		}
	}
	
	class Item{  // should be complete for now
		
		protected int x, y, width, height, effect, magnitude;
		protected Image sprite;
		
		Item(){
			x = 400;
			y = 300;
			width = 100;
			height = 100;
			effect = 0;
			magnitude = 0;
		}
		
		Item(int xpos, int ypos, int w, int h, int e, int m){
			x = xpos;
			y = ypos;
			width = w;
			height = h;
			effect = e;
			magnitude = m;
		}
		
		public void setEffect(int e, int m){
			effect = e;
			magnitude = m;
			//  Effects List
			// --------------
			// 0. No effect
			// 1. Health?
			// 2. Invincibility?
			// 3. Cancel Gravity?
			// 4. more?
		}
		
		public int getEffect(){
			return effect;
		}
		
		public int getMagnitude(){
			return magnitude;
		}
		
		public void setCoordinates(int newX, int newY){
			x = newX;
			y = newY;
		}
		
		public void setDimensions(int w, int h){
			width = w;
			height = h;
		}
		
		public void setSprite(Image S){
			sprite = S;
		}
		
		public int getX(){
			return x;
		}
		
		public int getY(){
			return y;
		}
		
		public int getWidth(){
			return width;
		}
		
		public int getHeight(){
			return height;
		}
		
		public Image getSprite(){
			return sprite;
		}
	}
	
	public static void main(String[] args)throws IOException{}
	public static void Update(Graphics G){}
	public void mouseClicked(MouseEvent e){}
  public void mouseEntered(MouseEvent e){}
  public void mouseExited(MouseEvent e){}
  public void mousePressed(MouseEvent e){}
	public void keyTyped(KeyEvent e){}
} // END OF CLASS

class ReadKeys implements Runnable{
	public boolean moving;
	public boolean jump;
	public boolean attack;
	public boolean falling;
	public boolean ground; // whether or not char is on ground - for jump purposes
	public boolean ML;
	public boolean MR;
	public boolean GoinUp;
	
	ReadKeys(){}
	
	public void run(){
		moving=false;
		jump=false;
		attack=false;
		falling=false;
		ML=false;
		MR=false;
		GoinUp=false;
	}
	
	public boolean GoinUp(){
		return GoinUp;
	}
	
	public void GoinUp(boolean a){
		GoinUp=a;
	}
	
	public boolean ML(){
		return ML;
	}
	
	public void ML(boolean a){
		ML=a;
	}
	
	public boolean MR(){
		return MR;
	}
	
	public void MR(boolean a){
		MR=a;
	}
	
	public boolean jump(){
		return jump;
	}
	
	public boolean moving(){
		return moving;
	}
	
	public boolean attack(){
		return attack;
	}
	
	public boolean falling(){
		return falling;
	}
	
	public boolean ground(){
		return ground;
	}
	
	public void jump(boolean a){
		jump=a;
	}
	
	public void moving(boolean a){
		moving=a;
	}
	
	public void attack(boolean a){
		attack=a;
	}
	
	public void falling(boolean a){
		falling=a;
	}
	
	public void ground(boolean a){
		ground=a;
	}
}

class PlaySound implements Runnable{
	AudioClip noise;
	PlaySound(AudioClip t){
		noise=t;
	}
	public void run(){
		noise.play();
	}
}

class PlayMusic implements Runnable{
	AudioClip music;
	PlayMusic(AudioClip s){
		music=s;
	}
	public void run(){
		music.loop();
	}
	public void stopMusic(){
		music.stop();
	}
}
