
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author dgorrie
 *
 * Testing class for our HEXGraphImplementation
 */
public class HEXGraphTest {
	private static HEXGraph<String> graph;
	private static final String[] testNames = {"PERSON", "MOUNTAIN", "ANIMAL"};
	private static final String[] exclusions = {"CAT, DOG", "ANIMAL, LAKE", "LAKE, MOUNTAIN"};
	private static final String[] subsets = {"ARTIST, POLITICIAN", "", "PUPPY, PERSON, KITTEN, ARTIST, CAT, POLITICIAN, DOG"};
	private static final String[] supersets = {"ANIMAL", "", ""};
	
	
	@BeforeClass
	public static void setup() throws IOException, IllegalStateException {
		HEXGraphFactory factory = new HEXGraphFactory();
		String filepath = "/Users/dgorrie/Documents/workspace/hexgraph/src/sample.hxg";
		factory.buildHEXGraph(filepath);
		graph = factory.getLiteralGraph(filepath);
	}
	
	@Test
	public void testExclusions() {
		for (int i = 0; i < testNames.length; i++) {			
			// System.out.println(graph.getExcluded(testNames[i]).toString());
			assertEquals(graph.getExcluded(testNames[i]).toString(), "[" + exclusions[i] + "]");
		}
	}
	
	@Test
	public void testHierarchySubset() {
		for (int i = 0; i < testNames.length; i++) {			
			// System.out.println(graph.getHierarchySubset(testNames[i]).toString());
			assertEquals(graph.getHierarchySubset(testNames[i]).toString(), "[" + subsets[i] + "]");
		}
	}
	
	@Test
	public void testHierarchySuperset() {
		for (int i = 0; i < testNames.length; i++) {			
			// System.out.println(graph.getHierarchySuperset(testNames[i]).toString());
			assertEquals(graph.getHierarchySuperset(testNames[i]).toString(), "[" + supersets[i] + "]");
		}
	}
	
	
}
