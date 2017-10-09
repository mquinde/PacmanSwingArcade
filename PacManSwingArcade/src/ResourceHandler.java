import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class ResourceHandler {
	public JLabel getImageAsLabel(String path){
		BufferedImage img;
		JLabel image = new JLabel();
		try {
			img = ImageIO.read(new File(getClass().getResource(path).toURI()));
			ImageIcon icon = new ImageIcon(img);
	        image = new JLabel(icon);
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return image;
	}
}
