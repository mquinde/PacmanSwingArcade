import java.util.Comparator;

public class HighScoreComparer implements Comparator<HighScore> {
	@Override
	public int compare(HighScore a, HighScore b) {
		return a.score < b.score ? -1
				: a.score > b.score ? 1
						: 0;
	}
}