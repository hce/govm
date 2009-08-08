import govm
import sys

if len(sys.argv) < 2:
    print "USAGE: %s $BYTECODE" % sys.argv[0]
    sys.exit(1)

asm = open(sys.argv[1], 'r').read()

def getc():
    while True:
        s = sys.stdin.read(1)
        if s == -1:
            break
        yield s
def putc(char):
    sys.stdout.write(char)
    sys.stdout.flush()
res = govm.run(asm, "", "", putc, getc().next)
