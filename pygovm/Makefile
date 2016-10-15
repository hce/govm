PYTHON=python
SSCRIPT=setup.py

all: govm.so

govm.so: pygovm.c pygovm.h
	$(PYTHON) $(SSCRIPT) build && mv -vf build/lib*/govm.so .

install: govm.so
	mkdir $(out)/bin $(out)/lib
	cp govm.so $(out)/lib
	cp rungovm.py $(out)/lib
	cp govm $(out)/bin
