
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author dgorrie
 *
 * Testing class for our HEXGraphImplementation
 */
public class HEXGraphTest {
	private static HEXGraphMethods mHexGraphMethods;
	
	
	@BeforeClass
	public static void setup() throws IOException, IllegalStateException {
		HEXGraphFactory factory = new HEXGraphFactory();
		String directory = "src/";
		String sample = directory + "sample.hxg";
		String unconnected = directory + "no_connections.hxg";
		factory.buildHEXGraph(sample);
		factory.buildHEXGraph(unconnected);
		mHexGraphMethods = new HEXGraphMethods(factory, unconnected);
	}
	
	@Test
	public void testCreation() {
	    HEXGraph<String> newGraph = new HEXGraph<String>();
	    newGraph.addNode("Person");
	    newGraph.addNode("Dog");
	    newGraph.addNode("Actor");
	    newGraph.addNode("Politician");
	    newGraph.addExclusion("Person", "Dog");
	    newGraph.addHierarchy("Person", "Actor");
	    newGraph.addHierarchy("Person", "Politician");
	    assertEquals(4, newGraph.size());
	}
	
	@Test
	public void testListStateSpace() {
		Set<Configuration<String>> configs = mHexGraphMethods.listStateSpace();
		assertEquals(1024, configs.size());
	}
	 
	 
	 
	 
	
	
	
}
