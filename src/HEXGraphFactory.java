import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author dgorrie
 *
 * Factory class that returns a new HEX graph object based on the .hexg file passed in
 */
public class HEXGraphFactory {
	private BufferedReader mBufferedReader;
	
	
	public HEXGraphFactory (String filepath) throws IOException {
		mBufferedReader = new BufferedReader(new FileReader(filepath));
	}
	
	public HEXGraph getHEXGraph() {
		return new HEXGraph();
	}
	
}
