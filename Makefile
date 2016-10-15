all: compiler vm examples
	true

compiler:
	cd adela/src && javac `find . -name '*.java'` && jar cvfm ../../govmc.jar META-INF/MANIFEST.MF `find . -name '*.class'`

vm:
	cd pygovm && make

examples: govmc.jar
	cd examples && make

install: govmc.jar govmc
	mkdir $(out)/bin
	mkdir $(out)/lib
	mkdir $(out)/man
	cp govmc $(out)/bin
	cp govmc.jar $(out)/lib
	cp govmc.1 $(out)/man
