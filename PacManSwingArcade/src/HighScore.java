import javax.swing.JLabel;

public class HighScore implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int score;
	public String name;
	public HighScore(String name, int score){
		this.name = name;
		this.score = score;
	}

	public String asString(){
		String str = name + "..............." + String.valueOf(score);
		return str;
	}
}
