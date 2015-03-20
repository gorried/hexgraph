hexgraph
========
HEX Graph implementation for fine grained entity recognition

To use HEXGRAPH:

Specify your graph in the form of a .hxg file. The .hxg format is as follows:

#E#
enumerate all the classes in your graph (1 per line)

#X#
Put exclusion relationships in this section by listing any number of classes separated by spaces. For example, if a line in this section is "A B C", exclusions [A,B], [A,C], and [B,C] will be added.

#H#
Put hierarchy relationships in this section using the format [Superclass]: [space separated list of subclasses]

Specify your class scores. This is done by simply creating a text file with the class name and corresponding score value all on one line.

Specify the paths to the score files, graph files, and an output destination in ResultRunner.java.

Run your files
