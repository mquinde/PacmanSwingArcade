import java.util.ArrayList;
import java.util.Collections;

public class ArcadeMemory implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7569896020729384045L;
	private ArrayList<HighScore> highScores;
	private int highScoresCount;
	
	public ArcadeMemory (){
		highScores = new ArrayList<HighScore>();
		highScoresCount = 0;
	}
	
	public ArrayList<HighScore> getHighScores(){
		return highScores;
	}
	
	public int getHighScore(){
		if(highScoresCount==0){
			return 0;
		} 
		return highScores.get(0).score;
		
	}
	
	public HighScore getHighScore(int i){
		if(i<highScoresCount){
			return highScores.get(i);
		}
		return null;
	}
	
	public int getCount(){
		return highScoresCount;
	}
	
	public void addHighScore(String name, Integer hs){
		if(highScoresCount < 10){
			highScores.add(new HighScore(name, hs));
			highScoresCount++;
			Collections.sort(highScores, new HighScoreComparer());
			Collections.reverse(highScores);
		} else if(hs >= highScores.get(9).score){
			highScores.remove(9);
			highScores.add(new HighScore(name, hs));
			Collections.sort(highScores, new HighScoreComparer());
			Collections.reverse(highScores);
		}
	}
	
	public boolean isHighScore(int score){
		return(score >= highScores.get(highScoresCount-1).score);
	}
	
}
