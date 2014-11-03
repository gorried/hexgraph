import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Created by xiaoling on 11/3/14.
 */
public class HEXGraphTest {
    @Test
    public void testCreation() {
        HEXGraph graph = new HEXGraph();
        graph.addNode("Person");
        graph.addNode("Dog");
        graph.addNode("Actor");
        graph.addNode("Politician");
        graph.addExclusion("Person", "Dog");
        graph.addHierarchy("Person", "Actor");
        graph.addHierarchy("Person", "Politician");
        assertEquals(4, graph.getNodes().size());
    }

}
