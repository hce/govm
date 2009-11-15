all: compiler vm
	true

compiler:
	cd adela/src && javac `find . -name '*.java'` && jar cvf ../../govmc.jar -M META-INF/MANIFEST.MF `find . -name '*.class'`

vm:
	cd pygovm && make
