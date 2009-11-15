all: compiler vm
	true

compiler:
	cd adela/src && javac `find . -name '*.java'` && jar cvf -m META-INF/MANIFEST.MF ../../govmc.jar `find . -name '*.class'`

vm:
	cd pygovm && make
