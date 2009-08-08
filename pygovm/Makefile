PYTHON=/usr/bin/python2.5
SSCRIPT=setup.py

all: govm.so

govm.so: pygovm.c pygovm.h
	$(PYTHON) $(SSCRIPT) build && mv -vf build/lib*/govm.so .
