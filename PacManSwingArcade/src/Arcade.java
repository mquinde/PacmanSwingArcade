import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.peer.KeyboardFocusManagerPeer;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Arcade extends JFrame implements KeyEventDispatcher{
	private final String playerUp = "1UP";
	private final String highScore = "HIGH SCORE";
	private final String pressStart = "PRESS START!"; 
	private final String controls = "CONTROLS"; 
	private final String highScores = "HIGH SCORES";
	private final String exit = "EXIT"; 
	private final Font sysFont12 = new Font("Sanserif", Font.BOLD, 12);
	private final Font sysFont14 = new Font("Sanserif", Font.BOLD, 14);
	private final Font sysFont18 = new Font("Sanserif", Font.BOLD, 18);
	private final Font sysFont26 = new Font("Sanserif", Font.BOLD, 26);
	private ResourceHandler resHandler = new ResourceHandler();
	private final static String memoryPath = "./src/Resources/memory.ser";
	private static ArcadeMemory arcadeMemory;
	private static JPanel screen;
	private static int currentScore = 0;
	private static int currentHScore;
	private static String previous;
	private JPanel [][] mazePanels = new JPanel[21][21];
	private int [][] mazeValues = new int[21][21];
	private GhostWorker ghostWorker = new GhostWorker();
	private PacmanWorker pacmanWorker = new PacmanWorker();
	private PacmanMover pacmanMover = new PacmanMover();
	private PinkGhostMover pinkMover = new PinkGhostMover();
	private RedGhostMover redMover = new RedGhostMover();
	private BlueGhostMover blueMover = new BlueGhostMover();
	private OrangeGhostMover orangeMover = new OrangeGhostMover();
	private JLabel highScoreDisplay = new JLabel("");
	private JLabel highScoreNumber = new JLabel("");
	private JLabel scoreDisplay = new JLabel(String.valueOf(currentScore));
	private volatile static boolean paused = false; 
	private int level = 0;
	private boolean passedHighScore = false;
	private int pacmanDirection = 0;
	private int pinkDirection = 0;
	private int redDirection = 1;
	private int orangeDirection = 2;
	private int blueDirection = 2;
	private int rx = 10;
	private int ry = 12;
	private int px = 10;
	private int py = 8;
	private int ox = 10;
	private int oy = 11;
	private int bx = 10; 
	private int by = 9; 

	private Random rand = new Random();

	public Arcade() throws ClassNotFoundException, IOException {
		addComponents();
	}

	private void addComponents() throws IOException, ClassNotFoundException {

		// load memory
		File memory = new File(memoryPath);
		if(memory.exists()){
			InputStream file = new FileInputStream(memoryPath);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer);
			arcadeMemory = (ArcadeMemory)input.readObject();
		} else {
			arcadeMemory = new ArcadeMemory(); 
		}

		// create components and add them to frame
		screen = new JPanel(new CardLayout());
		screen.add(new HomePanel(), "homeScreen");
		screen.add(new HighScorePanel(), "highScoreScreen");
		screen.add(new ControlsPanel(), "controlsScreen");
		screen.add(new GamePanel(), "gameScreen");
		previous = "homeScreen";
		getContentPane().add(screen, BorderLayout.NORTH);
		setBackground(Color.BLACK);
		setFocusable(true);
		requestFocusInWindow();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		pack();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(new ArcadeListener());
		setVisible(true);	      
	}

	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				Arcade arcade = null;
				try {
					arcade = new Arcade();
				} catch (ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				arcade.setVisible(true);
			}
		});
	}

	// ------- Game Helpers --------
	// Starts a new game and background threads
	public void startGame(){
		previous = "gameScreen";
		CardLayout cl = (CardLayout)(screen.getLayout());
		cl.show(screen, "gameScreen");
		currentScore = 0;
		ghostWorker.execute();
		pacmanWorker.execute();
	}

	// Quites Game, saves data if necessary
	public void quitGame(){
		previous = "homeScreen";
		// Prompt user for name
		if(arcadeMemory.isHighScore(currentScore)){

			// prompt the user to enter their name
			String name = JOptionPane.showInputDialog(this, "New HighScore! Enter your name:","MQG");
			arcadeMemory.addHighScore(name, currentScore);
			highScoreNumber.setText(String.valueOf(currentHScore));
		};
		// Go to home screen
		highScoreNumber.setText(String.valueOf(currentHScore));
		CardLayout cl = (CardLayout)(screen.getLayout());
		cl.show(screen, "homeScreen");
		
	}

	public void unpauseGame(){
		paused = false;
		ghostWorker = new GhostWorker();
		ghostWorker.execute();
		pacmanWorker = new PacmanWorker();
		pacmanWorker.execute();
	}
	
	// Exits Game
	public void exitArcade(){
		paused = true;
		Object[] options = {"Exit","Cancel"};
		int n = JOptionPane.showOptionDialog(this,"Are you sure you want to exit?",
				"Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if(n==1){
			return;
		}
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	public void loadMaze(){
		mazeValues = MazeData.getMaze(level);
		for(int x = 0; x< 21; x++){
			for(int y = 0; y<21; y++){
				JPanel panel = new JPanel();
				panel = new JPanel(new CardLayout());
				panel.add(getMazeImage(0), "0");
				panel.add(getMazeImage(1), "1");
				panel.add(getMazeImage(2), "2");
				panel.add(getMazeImage(3), "3");
				panel.add(getMazeImage(4), "4");
				panel.add(getMazeImage(5), "5");
				panel.add(getMazeImage(6), "6");
				panel.add(getMazeImage(7), "7");
				panel.add(getMazeImage(8), "8");
				panel.add(getMazeImage(9), "9");
				panel.add(getMazeImage(10), "10");
				panel.setBackground(Color.black);
				panel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
				CardLayout cl = (CardLayout)(panel.getLayout());
				cl.show(panel, Integer.toString(mazeValues[x][y]));
				mazePanels[x][y] = panel;
			}
		}
	}

	public void updateMaze(){
		mazeValues = MazeData.getMaze(level);
		for(int x = 0; x< 21; x++){
			for(int y = 0; y<21; y++){
				CardLayout cl = (CardLayout)(mazePanels[x][y].getLayout());
				cl.show(mazePanels[x][y], String.valueOf(mazeValues[x][y]));
			}
		}
	}

	public JPanel getMaze(){

		JPanel maze = new JPanel();
		maze.setBackground(Color.black);
		maze.setLayout(new GridBagLayout());
		maze.setBorder(BorderFactory.createEmptyBorder(50,5,20,5));
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.insets = new Insets(0,0,0,0);
		constraints.gridwidth = 1;
		for(int x = 0; x< 21; x++){
			for(int y = 0; y<21; y++){
				constraints.gridx = x+1;
				constraints.gridy = y+1;
				maze.add(mazePanels[x][y], constraints);
			}
		}

		return maze;
	}

	public void gameOver(){
		paused = true;
		quitGame();
	}

	public void checkDead(int x, int y){
		if(x==px){
			if(y==py){
				gameOver();
			}
		}
		if(x==rx){
			if(y==ry){
				gameOver();
			}
		}
		if(x==ox){
			if(y==oy){
				gameOver();
			}
		}
		if(x==bx){
			if(y==by){
				gameOver();
			}
		}
	}

	public boolean movePacman(int x, int y){
		if(mazeValues[x][y]==2){
			return false;
		}
		if(mazeValues[x][y]>6){
			//kill
			return false;
		}
		if(mazeValues[x][y]==1){
			//kill
			mazeValues[x][y]=0;
			currentScore++;
			if(passedHighScore || currentScore>currentHScore){
				passedHighScore = true;
				currentHScore = currentScore;
				highScoreDisplay.setText(String.valueOf(currentScore));	
				highScoreNumber.setText(String.valueOf(currentScore));	
			}
			scoreDisplay.setText(String.valueOf(currentScore));

			return true;
		}

		return true;
	}

	public boolean moveGhost(int x, int y){
		if(mazeValues[x][y]==2){
			return false;
		}
		if(mazeValues[x][y]==4){
			//kill
			return false;
		}
		if(mazeValues[x][y]==1){
			return true;
		}

		return true;
	}

	// -------- UI Helpers -----------
	public static void showControls(){
		paused = true;
		CardLayout cl = (CardLayout)(screen.getLayout());
		cl.show(screen, "controlsScreen");
	}

	public void showHighScores(){
		paused = true;
		CardLayout cl = (CardLayout)(screen.getLayout());
		cl.show(screen, "highScoreScreen");
	}

	public void showBack(){
		CardLayout cl = (CardLayout)(screen.getLayout());
		cl.show(screen, previous);
	}

	public JLabel getPacmanLogo(){
		JLabel pacmanLogo = resHandler.getImageAsLabel("/Resources/pacmanLogo.png");
		return pacmanLogo;
	}

	public JLabel getMazeImage(int i){
		String path = "/Resources/empty.png";
		switch (i) {
		case 0:
			path = "/Resources/empty.png";
			break;
		case 1:
			path = "/Resources/dot.png";
			break;
		case 2:
			path = "/Resources/wall.png";
			break;
		case 3:
			path = "/Resources/pacmanLeft.png";
			break;
		case 4:
			path = "/Resources/pacmanRight.png";
			break;
		case 5:
			path = "/Resources/pacmanUp.png";
			break;
		case 6:
			path = "/Resources/pacmanDown.png";
			break;
		case 7:
			path = "/Resources/red.png";
			break;
		case 8:
			path = "/Resources/blue.png";
			break;
		case 9:
			path = "/Resources/orange.png";
			break;
		case 10:
			path = "/Resources/pink.png";
			break;
		default:
			break;
		}
		JLabel label = resHandler.getImageAsLabel(path);
		return label;
	}

	public JButton getControlsBtn(){
		JButton button = new JButton("CONTROLS");
		try {
			Image img = ImageIO.read(getClass().getResource("Resources/controlsAdj.png"));
			button.setIcon(new ImageIcon(img));
		} catch (IOException ex) {
		}
		button.setBackground(Color.BLACK);
		button.setForeground(Color.WHITE);
		button.setFont(sysFont12);
		button.setContentAreaFilled(false);
		button.setOpaque(true);
		button.setBorderPainted(false);
		button.setActionCommand("controls");
		button.addActionListener(new ButtonActionListener());
		return button;
	}

	public JButton getHighScoreBtn(){
		JButton button = new JButton("HIGH SCORES");
		try {
			Image img = ImageIO.read(getClass().getResource("Resources/highScoresAdj.png"));
			button.setIcon(new ImageIcon(img));
		} catch (IOException ex) {
		}
		button.setBackground(Color.BLACK);
		button.setForeground(Color.WHITE);
		button.setFont(sysFont12);
		button.setContentAreaFilled(false);
		button.setOpaque(true);
		button.setBorderPainted(false);
		button.setActionCommand("highscores");
		button.addActionListener(new ButtonActionListener());
		return button;
	}

	public JButton getExitBtn(){
		JButton button = new JButton("EXIT");
		try {
			Image img = ImageIO.read(getClass().getResource("Resources/exitAdj.png"));
			button.setIcon(new ImageIcon(img));
		} catch (IOException ex) {
		}
		button.setBackground(Color.BLACK);
		button.setForeground(Color.WHITE);
		button.setFont(sysFont12);
		button.setContentAreaFilled(false);
		button.setOpaque(true);
		button.setBorderPainted(false);
		button.setActionCommand("exit");
		button.addActionListener(new ButtonActionListener());
		return button;
	}

	public JButton getQuitBtn(){
		JButton button = new JButton("Quit");
		try {
			Image img = ImageIO.read(getClass().getResource("Resources/exitAdj.png"));
			button.setIcon(new ImageIcon(img));
		} catch (IOException ex) {
		}
		button.setBackground(Color.BLACK);
		button.setForeground(Color.WHITE);
		button.setFont(sysFont12);
		button.setContentAreaFilled(false);
		button.setOpaque(true);
		button.setBorderPainted(false);
		button.setActionCommand("quit");
		button.addActionListener(new ButtonActionListener());
		return button;
	}

	public JButton getBackBtn(){
		JButton button = new JButton();
		try {
			Image img = ImageIO.read(getClass().getResource("Resources/backAdj.png"));
			button.setIcon(new ImageIcon(img));
		} catch (IOException ex) {
		}
		button.setBackground(Color.BLACK);
		button.setForeground(Color.WHITE);
		button.setFont(sysFont12);
		button.setContentAreaFilled(false);
		button.setOpaque(true);
		button.setBorderPainted(false);
		button.setActionCommand("back");
		button.addActionListener(new ButtonActionListener());
		return button;
	}

	public JButton getSoundBtn(){
		JButton button = new JButton("Sound");
		try {
			Image img = ImageIO.read(getClass().getResource("Resources/soundAdj.png"));
			button.setIcon(new ImageIcon(img));
		} catch (IOException ex) {
		}
		button.setBackground(Color.BLACK);
		button.setForeground(Color.WHITE);
		button.setFont(sysFont12);
		button.setContentAreaFilled(false);
		button.setOpaque(true);
		button.setBorderPainted(false);
		button.setActionCommand("sound");
		button.addActionListener(new ButtonActionListener());
		return button;
	}

	public JButton getPauseBtn(){
		JButton button = new JButton("Pause");
		try {
			Image img = ImageIO.read(getClass().getResource("Resources/pauseAdj.png"));
			button.setIcon(new ImageIcon(img));
		} catch (IOException ex) {
		}
		button.setBackground(Color.BLACK);
		button.setForeground(Color.WHITE);
		button.setFont(sysFont12);
		button.setContentAreaFilled(false);
		button.setOpaque(true);
		button.setBorderPainted(false);
		button.setActionCommand("pause");
		button.addActionListener(new ButtonActionListener());
		return button;
	}

	public JButton getStartBtn(){
		JButton button = new JButton(pressStart);
		button.setBackground(Color.BLACK);
		button.setForeground(Color.WHITE);
		button.setFont(sysFont18);
		button.setContentAreaFilled(false);
		button.setOpaque(true);
		button.setBorderPainted(false);
		button.setActionCommand("game");
		button.addActionListener(new ButtonActionListener());
		return button;
	}

	public JButton getStartLogoBtn(){
		JButton button = new JButton();
		try {
			Image img = ImageIO.read(getClass().getResource("/Resources/PacmanChasingGhostsAdj.png"));
			button.setIcon(new ImageIcon(img));
		} catch (IOException ex) {
		}
		button.setBackground(Color.BLACK);
		button.setForeground(Color.WHITE);
		button.setFont(sysFont18);
		button.setContentAreaFilled(false);
		button.setOpaque(true);
		button.setBorderPainted(false);
		button.setActionCommand("game");
		button.addActionListener(new ButtonActionListener());
		return button;
	}

	public JPanel getHomeTopPanel(){
		JLabel upLabel = new JLabel(playerUp);
		upLabel.setForeground(Color.white);
		upLabel.setFont(sysFont12);
		JLabel highScoreLabel = new JLabel(highScore);
		highScoreLabel.setForeground(Color.white);
		highScoreLabel.setFont(sysFont12);
		JLabel upNumber = new JLabel("1");
		upNumber.setForeground(Color.white);
		upNumber.setFont(sysFont12);
		currentHScore = arcadeMemory.getHighScore();
		highScoreNumber .setText(String.valueOf(currentHScore));
		highScoreNumber.setForeground(Color.white);
		highScoreNumber.setFont(sysFont12);

		JPanel panel = new JPanel();
		panel.setBackground(Color.black);
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5,5,10,5));
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.CENTER;
		constraints.insets = new Insets(0,10,0,10);
		constraints.gridx = 3;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		panel.add(upLabel, constraints);
		constraints.gridx = 4;
		panel.add(highScoreLabel, constraints);
		constraints.gridy = 2;
		constraints.gridx = 3;
		panel.add(upNumber, constraints);
		constraints.gridx = 4;
		panel.add(highScoreNumber, constraints);

		return panel;
	}

	public JPanel getGameTopPanel(){
		JLabel upLabel = new JLabel(playerUp);
		upLabel.setForeground(Color.white);
		upLabel.setFont(sysFont12);
		JLabel highScoreLabel = new JLabel(highScore);
		highScoreLabel.setForeground(Color.white);
		highScoreLabel.setFont(sysFont12);
		JLabel upNumber = new JLabel("1");
		upNumber.setForeground(Color.white);
		upNumber.setFont(sysFont12);
		currentHScore = arcadeMemory.getHighScore();
		highScoreDisplay.setText(String.valueOf(currentHScore));
		highScoreDisplay.setForeground(Color.white);
		highScoreDisplay.setFont(sysFont12);

		JPanel panel = new JPanel();
		panel.setBackground(Color.black);
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5,5,10,5));
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.CENTER;
		constraints.insets = new Insets(0,10,0,10);
		constraints.gridx = 3;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		panel.add(upLabel, constraints);
		constraints.gridx = 4;
		panel.add(highScoreLabel, constraints);
		constraints.gridy = 2;
		constraints.gridx = 3;
		panel.add(upNumber, constraints);
		constraints.gridx = 4;
		panel.add(highScoreDisplay, constraints);

		return panel;
	}

	public JPanel getLogoPanel(){
		JPanel panel = new JPanel();
		panel.setBackground(Color.black);
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5,50,5,50));
		panel.add(getPacmanLogo(), BorderLayout.CENTER);

		return panel;
	}

	public JPanel getMiddlePanel(){
		JPanel panel = new JPanel();
		panel.setBackground(Color.black);
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(30,5,20,5));
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.CENTER;
		constraints.insets = new Insets(10,10,10,10);
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		panel.add(getStartBtn(), constraints);
		constraints.gridy = 2;
		panel.add(getStartLogoBtn(), constraints);

		return panel;
	}

	public JPanel getHomeFooterPanel(){ 
		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BorderLayout());
		controlsPanel.setBackground(Color.BLACK);
		controlsPanel.add(getControlsBtn(), BorderLayout.WEST);

		JPanel highScoresPanel = new JPanel();
		highScoresPanel.setLayout(new BorderLayout());
		highScoresPanel.setBackground(Color.BLACK);
		highScoresPanel.add(getHighScoreBtn(), BorderLayout.WEST);

		JPanel exitPanel = new JPanel();
		exitPanel.setLayout(new BorderLayout());
		exitPanel.setBackground(Color.BLACK);
		exitPanel.add(getExitBtn(), BorderLayout.WEST);

		JPanel panel = new JPanel();
		panel.setBackground(Color.black);
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(50,5,20,5));
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.insets = new Insets(0,10,0,10);
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		panel.add(controlsPanel, constraints);
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.gridx = 2;
		panel.add(highScoresPanel, constraints);
		constraints.anchor = GridBagConstraints.LINE_END;
		constraints.gridx = 3;
		panel.add(exitPanel, constraints);

		return panel;
	}

	public JPanel getGameFooterPanel(){ 
		JPanel scorePanel = new JPanel();
		scorePanel.setLayout(new BorderLayout());
		scorePanel.setBackground(Color.BLACK);
		JLabel scoreLabel = new JLabel("Score: ");
		scoreLabel.setForeground(Color.WHITE);
		scoreLabel.setFont(sysFont14);
		scorePanel.add(scoreLabel, BorderLayout.WEST);
		scoreDisplay.setForeground(Color.WHITE);
		scoreDisplay.setFont(sysFont14);
		scorePanel.add(scoreDisplay, BorderLayout.EAST);

		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BorderLayout());
		controlsPanel.setBackground(Color.BLACK);
		controlsPanel.add(getControlsBtn(), BorderLayout.WEST);

		JPanel soundPanel = new JPanel();
		soundPanel.setLayout(new BorderLayout());
		soundPanel.setBackground(Color.BLACK);
		soundPanel.add(getSoundBtn(), BorderLayout.WEST);

		JPanel pausePanel = new JPanel();
		pausePanel.setLayout(new BorderLayout());
		pausePanel.setBackground(Color.BLACK);
		pausePanel.add(getPauseBtn(), BorderLayout.WEST);

		JPanel quitPanel = new JPanel();
		quitPanel.setLayout(new BorderLayout());
		quitPanel.setBackground(Color.BLACK);
		quitPanel.add(getQuitBtn(), BorderLayout.WEST);

		JPanel panel = new JPanel();
		panel.setBackground(Color.black);
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(20,5,20,5));
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.insets = new Insets(0,10,0,10);
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		panel.add(scorePanel, constraints);
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.gridx = 2;
		panel.add(controlsPanel, constraints);
		constraints.gridx = 3;
		panel.add(soundPanel, constraints);
		constraints.gridx = 4;
		panel.add(pausePanel, constraints);
		constraints.anchor = GridBagConstraints.LINE_END;
		constraints.gridx = 5;
		panel.add(quitPanel, constraints);

		return panel;
	}

	public JPanel getBackFooterPanel(){ 
		JPanel backPanel = new JPanel();
		backPanel.setLayout(new BorderLayout());
		backPanel.setBackground(Color.BLACK);
		backPanel.add(getBackBtn(), BorderLayout.WEST);

		JPanel panel = new JPanel();
		panel.setBackground(Color.black);
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10,5,10,5));

		panel.add(backPanel, BorderLayout.WEST);
		return panel;
	}

	// -------- Panel Classes ---------
	public class HomePanel extends JPanel {
		public HomePanel(){
			setBackground(Color.black);
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			add(getHomeTopPanel(), BorderLayout.NORTH);

			JPanel middlePanel = new JPanel();
			middlePanel.setBackground(Color.BLACK);
			middlePanel.setLayout(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.anchor = GridBagConstraints.CENTER;
			constraints.gridx = 1;
			constraints.gridy = 1;
			constraints.gridwidth = 1;
			middlePanel.add(getLogoPanel(), constraints);
			constraints.gridy = 2;
			middlePanel.add(getMiddlePanel(), constraints);
			add(middlePanel, BorderLayout.CENTER);
			add(getHomeFooterPanel(), BorderLayout.SOUTH);

		}
	}

	public class HighScorePanel extends JPanel {

		public HighScorePanel(){
			JLabel label = new JLabel("HIGH SCORES");
			label.setForeground(Color.white);
			label.setFont(sysFont26);
			JLabel label2 = new JLabel("NAME         SCORE");
			label2.setForeground(Color.white);
			label2.setFont(sysFont18);


			JPanel scores = new JPanel();
			scores.setBackground(Color.black);
			scores.setLayout(new GridBagLayout());
			scores.setBorder(BorderFactory.createEmptyBorder(5,5,20,5));
			GridBagConstraints constraints = new GridBagConstraints();

			constraints.anchor = GridBagConstraints.CENTER;
			constraints.insets = new Insets(50,200,50,200);
			constraints.gridx = 1;
			constraints.gridwidth = 1;
			constraints.gridy = 1;
			scores.add(label, constraints);
			constraints.gridy = 2;
			constraints.insets = new Insets(10,200,10,200);
			scores.add(label2, constraints);
			ArrayList<HighScore> highScoreList = arcadeMemory.getHighScores();
			for(int i = 0; i<arcadeMemory.getCount(); i++){
				constraints.gridy = i+3;
				JLabel score = new JLabel(highScoreList.get(i).asString());
				score.setForeground(Color.white);
				score.setFont(sysFont14);
				scores.add(score, constraints);
			}

			setBackground(Color.black);
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(5,5,20,5));
			add(scores, BorderLayout.NORTH);
			add(getBackFooterPanel(), BorderLayout.SOUTH);
		}
	}

	public class ControlsPanel extends JPanel {

		public ControlsPanel(){
			JLabel label = new JLabel("Controls");
			label.setForeground(Color.white);
			label.setFont(sysFont26);
			JPanel cpan = new JPanel();
			cpan.setBackground(Color.black);
			cpan.setLayout(new GridBagLayout());
			cpan.setBorder(BorderFactory.createEmptyBorder(5,5,20,5));
			GridBagConstraints constraints = new GridBagConstraints();

			constraints.anchor = GridBagConstraints.CENTER;
			constraints.insets = new Insets(50,200,50,200);
			constraints.gridx = 1;
			constraints.gridwidth = 1;
			constraints.gridy = 1;
			cpan.add(label, constraints);
			constraints.insets = new Insets(10,200,10,200);

			String[] controls = {"↑ MOVE UP",
					"↓ MOVE DOWN",
					"→ MOVE RIGHT",
					"← MOVE LEFT",
					"P PAUSE GAME",
					"Q QUIT GAME",
					"M MUTE GAME",
					"C CONTROLS"
			};

			for(int i = 0; i<controls.length; i++){
				constraints.gridy = i+2;
				JLabel control = new JLabel(controls[i]);
				control.setForeground(Color.white);
				control.setFont(sysFont14);
				cpan.add(control, constraints);
			}

			setBackground(Color.black);
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(5,5,20,5));
			add(cpan, BorderLayout.NORTH);
			add(getBackFooterPanel(), BorderLayout.SOUTH);
		}
	}

	public class GamePanel extends JPanel {

		public GamePanel(){
			paused = false;
			level = 0;
			passedHighScore = false;
			pacmanDirection = 0;
			pinkDirection = 0;
			redDirection = 1;
			orangeDirection = 2;
			blueDirection = 2;
			rx = 10;
			ry = 12;
			px = 10;
			py = 8;
			ox = 10;
			oy = 11;
			bx = 10; 
			by = 9; 
			setBackground(Color.black);
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

			loadMaze();
			add(getGameTopPanel(), BorderLayout.NORTH);
			add(getMaze(), BorderLayout.CENTER);
			add(getGameFooterPanel(), BorderLayout.SOUTH);
		}
	}

	// ---------Workers --------------
	public class GhostWorker extends SwingWorker<Void, Void> {
		@Override
		protected Void doInBackground() throws Exception {
			while(!paused){
				Thread.sleep(170);
				pinkMover.move();
				redMover.move();
				orangeMover.move();
				blueMover.move();
			}

			return null;
		}
	};
	
	// worker to move pacman
	public class PacmanWorker extends SwingWorker<Void, Void>{
		@Override
		protected Void doInBackground() throws Exception {
			while(!paused){
				Thread.sleep(150);
				pacmanMover.move();
			}

			return null;
		}
	};
	
	// ---------Object Movers ----------
	public class PacmanMover{
		int x = 10;
		int y = 12;
		public void move() {
			switch (pacmanDirection) {
			case 0:
				if(x<19 && movePacman(x+1, y)){
					CardLayout cl = (CardLayout)(mazePanels[x][y].getLayout());
					cl.show(mazePanels[x][y], "0");
					cl = (CardLayout)(mazePanels[x+1][y].getLayout());
					cl.show(mazePanels[x+1][y], "4");
					x++;
				}
				break;
			case 1:
				if(x>0 && movePacman(x-1, y)){
					CardLayout cl = (CardLayout)(mazePanels[x][y].getLayout());
					cl.show(mazePanels[x][y], "0");
					cl = (CardLayout)(mazePanels[x-1][y].getLayout());
					cl.show(mazePanels[x-1][y], "3");
					x--;
				}
				break;
			case 2:
				if(y>0 && movePacman(x, y-1)){
					CardLayout cl = (CardLayout)(mazePanels[x][y].getLayout());
					cl.show(mazePanels[x][y], "0");
					cl = (CardLayout)(mazePanels[x][y-1].getLayout());
					cl.show(mazePanels[x][y-1], "5");
					y--;
				}
				break;
			case 3:
				if(y<19 && movePacman(x, y+1)){
					CardLayout cl = (CardLayout)(mazePanels[x][y].getLayout());
					cl.show(mazePanels[x][y], "0");
					cl = (CardLayout)(mazePanels[x][y+1].getLayout());
					cl.show(mazePanels[x][y+1], "6");
					y++;
				}
				break;

			default:
				break;
			}

			if(currentScore==MazeData.maxScore[level]){
				level++;
				updateMaze();
				x = 10;
				y = 12;
			}
			checkDead(x, y);
		}
	}

	public class PinkGhostMover {
		public void move() {
			switch (pinkDirection) {
			case 0:
				if(px<19 && moveGhost(px+1,py)){
					CardLayout cl = (CardLayout)(mazePanels[px][py].getLayout());
					cl.show(mazePanels[px][py], String.valueOf(mazeValues[px][py]));
					cl = (CardLayout)(mazePanels[px+1][py].getLayout());
					cl.show(mazePanels[px+1][py], "10");
					px++;
				} else {
					pinkDirection = rand.nextInt(4);
				}
				break;
			case 1:
				if(px>0 && moveGhost(px-1, py)){
					CardLayout cl = (CardLayout)(mazePanels[px][py].getLayout());
					cl.show(mazePanels[px][py], String.valueOf(mazeValues[px][py]));
					cl = (CardLayout)(mazePanels[px-1][py].getLayout());
					cl.show(mazePanels[px-1][py], "10");
					px--;
				} else {
					pinkDirection = rand.nextInt(4);
				}
				break;
			case 2:
				if(py>0 && moveGhost(px, py-1)){
					CardLayout cl = (CardLayout)(mazePanels[px][py].getLayout());
					cl.show(mazePanels[px][py], String.valueOf(mazeValues[px][py]));
					cl = (CardLayout)(mazePanels[px][py-1].getLayout());
					cl.show(mazePanels[px][py-1], "10");
					py--;
				} else {
					pinkDirection = rand.nextInt(4);
				}
				break;
			case 3:
				if(py<19 && moveGhost(px, py+1)){
					CardLayout cl = (CardLayout)(mazePanels[px][py].getLayout());
					cl.show(mazePanels[px][py], String.valueOf(mazeValues[px][py]));
					cl = (CardLayout)(mazePanels[px][py+1].getLayout());
					cl.show(mazePanels[px][py+1], "10");
					py++;
				} else {
					pinkDirection = rand.nextInt(4);
				}
				break;

			default:
				break;
			}

		}
	}

	public class RedGhostMover {
		public void move() {
			switch (redDirection) {
			case 0:
				if(rx<19 && moveGhost(rx+1,ry)){
					CardLayout cl = (CardLayout)(mazePanels[rx][ry].getLayout());
					cl.show(mazePanels[rx][ry], String.valueOf(mazeValues[rx][ry]));
					cl = (CardLayout)(mazePanels[rx+1][ry].getLayout());
					cl.show(mazePanels[rx+1][ry], "7");
					rx++;
				} else {
					redDirection = rand.nextInt(4);
				}
				break;
			case 1:
				if(rx>0 && moveGhost(rx-1, ry)){
					CardLayout cl = (CardLayout)(mazePanels[rx][ry].getLayout());
					cl.show(mazePanels[rx][ry], String.valueOf(mazeValues[rx][ry]));
					cl = (CardLayout)(mazePanels[rx-1][ry].getLayout());
					cl.show(mazePanels[rx-1][ry], "7");
					rx--;
				} else {
					redDirection = rand.nextInt(4);
				}
				break;
			case 2:
				if(ry>0 && moveGhost(rx, ry-1)){
					CardLayout cl = (CardLayout)(mazePanels[rx][ry].getLayout());
					cl.show(mazePanels[rx][ry], String.valueOf(mazeValues[rx][ry]));
					cl = (CardLayout)(mazePanels[rx][ry-1].getLayout());
					cl.show(mazePanels[rx][ry-1], "7");
					ry--;
				} else {
					redDirection = rand.nextInt(4);
				}
				break;
			case 3:
				if(ry<19 && moveGhost(rx, ry+1)){
					CardLayout cl = (CardLayout)(mazePanels[rx][ry].getLayout());
					cl.show(mazePanels[rx][ry], String.valueOf(mazeValues[rx][ry]));
					cl = (CardLayout)(mazePanels[rx][ry+1].getLayout());
					cl.show(mazePanels[rx][ry+1], "7");
					ry++;
				} else {
					redDirection = rand.nextInt(4);
				}
				break;

			default:
				break;
			}

		}
	}

	public class BlueGhostMover {
		public void move() {
			switch (blueDirection) {
			case 0:
				if(bx<19 && moveGhost(bx+1,by)){
					CardLayout cl = (CardLayout)(mazePanels[bx][by].getLayout());
					cl.show(mazePanels[bx][by], String.valueOf(mazeValues[bx][by]));
					cl = (CardLayout)(mazePanels[bx+1][by].getLayout());
					cl.show(mazePanels[bx+1][by], "8");
					bx++;
				} else {
					blueDirection = rand.nextInt(4);
				}
				break;
			case 1:
				if(bx>0 && moveGhost(bx-1, by)){
					CardLayout cl = (CardLayout)(mazePanels[bx][by].getLayout());
					cl.show(mazePanels[bx][by], String.valueOf(mazeValues[bx][by]));
					cl = (CardLayout)(mazePanels[bx-1][by].getLayout());
					cl.show(mazePanels[bx-1][by], "8");
					bx--;
				} else {
					blueDirection = rand.nextInt(4);
				}
				break;
			case 2:
				if(by>0 && moveGhost(bx, by-1)){
					CardLayout cl = (CardLayout)(mazePanels[bx][by].getLayout());
					cl.show(mazePanels[bx][by], String.valueOf(mazeValues[bx][by]));
					cl = (CardLayout)(mazePanels[bx][by-1].getLayout());
					cl.show(mazePanels[bx][by-1], "8");
					by--;
				} else {
					blueDirection = rand.nextInt(4);
				}
				break;
			case 3:
				if(by<19 && moveGhost(bx, by+1)){
					CardLayout cl = (CardLayout)(mazePanels[bx][by].getLayout());
					cl.show(mazePanels[bx][by], String.valueOf(mazeValues[bx][by]));
					cl = (CardLayout)(mazePanels[bx][by+1].getLayout());
					cl.show(mazePanels[bx][by+1], "8");
					by++;
				} else {
					blueDirection = rand.nextInt(4);
				}
				break;

			default:
				break;
			}

		}
	}

	public class OrangeGhostMover {
		public void move() {
			switch (orangeDirection) {
			case 0:
				if(ox<19 && moveGhost(ox+1,oy)){
					CardLayout cl = (CardLayout)(mazePanels[ox][oy].getLayout());
					cl.show(mazePanels[ox][oy], String.valueOf(mazeValues[ox][oy]));
					cl = (CardLayout)(mazePanels[ox+1][oy].getLayout());
					cl.show(mazePanels[ox+1][oy], "9");
					ox++;
				} else {
					orangeDirection = rand.nextInt(4);
				}
				break;
			case 1:
				if(ox>0 && moveGhost(ox-1, oy)){
					CardLayout cl = (CardLayout)(mazePanels[ox][oy].getLayout());
					cl.show(mazePanels[ox][oy], String.valueOf(mazeValues[ox][oy]));
					cl = (CardLayout)(mazePanels[ox-1][oy].getLayout());
					cl.show(mazePanels[ox-1][oy], "9");
					ox--;
				} else {
					orangeDirection = rand.nextInt(4);
				}
				break;
			case 2:
				if(oy>0 && moveGhost(ox, oy-1)){
					CardLayout cl = (CardLayout)(mazePanels[ox][oy].getLayout());
					cl.show(mazePanels[ox][oy], String.valueOf(mazeValues[ox][oy]));
					cl = (CardLayout)(mazePanels[ox][oy-1].getLayout());
					cl.show(mazePanels[ox][oy-1], "9");
					oy--;
				} else {
					orangeDirection = rand.nextInt(4);
				}
				break;
			case 3:
				if(oy<19 && moveGhost(ox, oy+1)){
					CardLayout cl = (CardLayout)(mazePanels[ox][oy].getLayout());
					cl.show(mazePanels[ox][oy], String.valueOf(mazeValues[ox][oy]));
					cl = (CardLayout)(mazePanels[ox][oy+1].getLayout());
					cl.show(mazePanels[ox][oy+1], "9");
					oy++;
				} else {
					orangeDirection = rand.nextInt(4);
				}
				break;

			default:
				break;
			}

		}
	}

	// ----------Dispatcher and Listeners-------
	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		assert EventQueue.isDispatchThread();

		int keyCode = e.getKeyCode();

		if (e.getID() == KeyEvent.KEY_PRESSED) {
			if(previous=="gameScreen"){
				switch (keyCode) {
				case 38:
					//Up
					pacmanDirection = 2;
					break;
				case 40:
					//Down
					pacmanDirection = 3;
					break;
				case 37:
					//Left
					pacmanDirection = 1;
					break;
				case 39:
					//Right
					pacmanDirection = 0;
					break;
				default:
					break;
				}
			}
		}
		if (e.getID() == KeyEvent.KEY_RELEASED) {
			if(keyCode==67){
				paused = true;
				CardLayout cl = (CardLayout)(screen.getLayout());
				cl.show(screen, "controlsScreen");
			} else if(previous=="gameScreen"){
				switch (keyCode) {
				case 80:
					//Pause
					if(paused){
						unpauseGame();
					} else {
						paused = true;
					}
					break;
				case 81:
					//Quit
					paused = true;
					Object[] options = {"Exit","Cancel"};
					int n = JOptionPane.showOptionDialog(new JFrame(),"Are you sure you want to quit?",
							"Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					if(n==1){
						unpauseGame();
					} else {
						quitGame();
					}
					break;
				case 77:
					//Mute
					//paused = true;
					break;
				default:
					break;
				}
			}
		}
		return false;
	}

	public class ArcadeListener implements WindowListener {
		public void windowClosing(WindowEvent e){
			arcadeMemory.addHighScore("MQG", currentScore);
			File f = new File(memoryPath);
			try {
				f.createNewFile();
				// Serialize memory object into file
				FileOutputStream memory = new FileOutputStream(memoryPath);
				ObjectOutputStream out = new ObjectOutputStream(memory);
				out.writeObject(arcadeMemory);
				out.close();
				memory.close();
				System.exit(0);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

		@Override
		public void windowOpened(WindowEvent e) {
		}

		@Override
		public void windowClosed(WindowEvent e) {
		}

		@Override
		public void windowIconified(WindowEvent e) {
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
		}

		@Override
		public void windowActivated(WindowEvent e) {
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
		}
	}

	public class ButtonActionListener implements ActionListener {
		public void actionPerformed (ActionEvent e){
			// check which button was triggered
			if (e.getActionCommand().equals("highscores")) {
				showHighScores();
			} else if(e.getActionCommand().equals("controls")) {
				showControls();
			}else if(e.getActionCommand().equals("exit")) {
				exitArcade();
			}else if(e.getActionCommand().equals("back")) {
				showBack();
			}else if(e.getActionCommand().equals("game")) {
				startGame();
			}else if(e.getActionCommand().equals("quit")) {
				paused = true;
				Object[] options = {"Exit","Cancel"};
				int n = JOptionPane.showOptionDialog(new JFrame(),"Are you sure you want to quit?",
						"Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				if(n==1){
					unpauseGame();
				} else {
					quitGame();
				}
			}else if(e.getActionCommand().equals("pause")) {
				if(paused){
					unpauseGame();
				} else {
					paused = true;
				}	
			}else if(e.getActionCommand().equals("sound")) {
				paused = false;


			}
		}
	}

}
