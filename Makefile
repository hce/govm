all: compiler vm examples
	true

compiler:
	cd adela/src && javac `find . -name '*.java'` && jar cvfm ../../govmc.jar META-INF/MANIFEST.MF `find . -name '*.class'`

vm:
	cd pygovm && make

examples: govmc.jar
	cd examples && make
