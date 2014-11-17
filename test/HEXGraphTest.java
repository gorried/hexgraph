
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author dgorrie
 *         <p/>
 *         Testing class for our HEXGraphImplementation
 */
public class HEXGraphTest {
    private static HEXGraph<String> graph;
    private static final String[] testNames = {"PERSON", "MOUNTAIN", "ANIMAL"};
    private static final String[] exclusions = {"CAT, DOG", "ANIMAL, LAKE", "LAKE, MOUNTAIN"};
    private static final String[] subsets = {"ARTIST, POLITICIAN", "", "PUPPY, PERSON, KITTEN, ARTIST, CAT, POLITICIAN, DOG"};
    private static final String[] supersets = {"ANIMAL", "", ""};
    private static HEXGraphMethods mHexGraphMethods;

    @BeforeClass
    public static void setup() throws IOException, IllegalStateException {
        HEXGraphFactory factory = new HEXGraphFactory();
        String directory = "/Users/dgorrie/Documents/workspace/hexgraph/src/";
        String sample = directory + "sample.hxg";
        String unconnected = directory + "no_connections.hxg";
        factory.buildHEXGraph(sample);
        factory.buildHEXGraph(unconnected);
        mHexGraphMethods = new HEXGraphMethods(factory, unconnected);
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
            assertEquals(graph.getDescendants(testNames[i]).toString(), "[" + subsets[i] + "]");
        }
    }

    @Test
    public void testHierarchySuperset() {
        for (int i = 0; i < testNames.length; i++) {
            // System.out.println(graph.getHierarchySuperset(testNames[i]).toString());
            assertEquals(graph.getDescendants(testNames[i]).toString(), "[" + supersets[i] + "]");
        }
    }

    private HEXGraph<String> createSimpleGraph() {
        HEXGraph<String> graph = new HEXGraph<String>();
        graph.addNode("Person");
        graph.addNode("Dog");
        graph.addNode("Actor");
        graph.addNode("Politician");
        graph.addExclusion("Person", "Dog");
        graph.addHierarchy("Person", "Actor");
        graph.addHierarchy("Person", "Politician");
        return graph;
    }

    @Test
    public void testCreation() {
        HEXGraph<String> graph = createSimpleGraph();
        assertEquals(4, graph.size());
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Dog", "Person"));
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Person", "Dog"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUPER, graph.getRelationship("Actor", "Person"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUPER, graph.getRelationship("Politician", "Person"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUB, graph.getRelationship("Person", "Actor"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUB, graph.getRelationship("Person", "Politician"));
        assertEquals(HEXGraph.Relationship.OVERLAPPING, graph.getRelationship("Actor", "Politician"));
        assertEquals(HEXGraph.Relationship.OVERLAPPING, graph.getRelationship("Politician", "Actor"));
    }


    @Test
    public void testDensify() {
        HEXGraph<String> graph = createSimpleGraph();
        graph.densify();
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Dog", "Actor"));
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Dog", "Person"));
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Dog", "Politician"));
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Actor", "Dog"));
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Person", "Dog"));
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Politician", "Dog"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUPER, graph.getRelationship("Actor", "Person"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUPER, graph.getRelationship("Politician", "Person"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUB, graph.getRelationship("Person", "Actor"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUB, graph.getRelationship("Person", "Politician"));
        assertEquals(HEXGraph.Relationship.OVERLAPPING, graph.getRelationship("Actor", "Politician"));
        assertEquals(HEXGraph.Relationship.OVERLAPPING, graph.getRelationship("Politician", "Actor"));
    }

    @Test
    public void testSparisfy() {
        HEXGraph<String> graph = createSimpleGraph();
        graph.sparsify();
        // graph stays the same
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Dog", "Person"));
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Person", "Dog"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUPER, graph.getRelationship("Actor", "Person"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUPER, graph.getRelationship("Politician", "Person"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUB, graph.getRelationship("Person", "Actor"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUB, graph.getRelationship("Person", "Politician"));
        assertEquals(HEXGraph.Relationship.OVERLAPPING, graph.getRelationship("Actor", "Politician"));
        assertEquals(HEXGraph.Relationship.OVERLAPPING, graph.getRelationship("Politician", "Actor"));
    }

    @Test
    public void testSparisfyAfterDensify() {
        HEXGraph<String> graph = createSimpleGraph();
        graph.densify();
        graph.sparsify();
        // graph stays the same
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Dog", "Person"));
        assertEquals(HEXGraph.Relationship.EXCLUSION, graph.getRelationship("Person", "Dog"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUPER, graph.getRelationship("Actor", "Person"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUPER, graph.getRelationship("Politician", "Person"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUB, graph.getRelationship("Person", "Actor"));
        assertEquals(HEXGraph.Relationship.HIERARCHY_SUB, graph.getRelationship("Person", "Politician"));
        assertEquals(HEXGraph.Relationship.OVERLAPPING, graph.getRelationship("Actor", "Politician"));
        assertEquals(HEXGraph.Relationship.OVERLAPPING, graph.getRelationship("Politician", "Actor"));
    }

    @Test
    public void testListStateSpace() {
        Set<Configuration<String>> configs = mHexGraphMethods.ListStateSpace();
        assertEquals(1024, configs.size());
    }

   @Test
    public void testListStateSpace() {
        System.out.println("testing list state space");
	HEXGraph<String> graph = createSimpleGraph();
	HEXGraphMethods method = new HEXGraphMethods(graph);
        Configuration<String> conf = method.ListStateSpace();
System.out.println("printing");
       System.out.println(conf.toString());
    }

}
