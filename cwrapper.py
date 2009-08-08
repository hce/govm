import struct
import socket
import sys

class GoVMException(Exception): pass

def recvall(s, l):
    buf = []
    while l:
        x = s.recv(l)
        buf.append(x)
        l = l - len(x)
    return ''.join(buf)

def readstr(s):
    l = struct.unpack(">i", recvall(s, 4))[0]
    return recvall(s, l)

def compile(source, host, port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((host, port))
    x = recvall(s, 4)
    if x != 'GoVM': raise GoVMException("Illegal header")
    codelen = struct.pack(">i", len(source))
    s.sendall(codelen)
    s.sendall(source)
    res = struct.unpack(">i", recvall(s, 4))[0]
    if res != 1: raise GoVMException("Unexpected result")
    res = struct.unpack(">i", recvall(s, 4))[0]
    if res == 2: bytecode = readstr(s)
    else: bytecode = None
    messages = readstr(s)
    return bytecode, messages

if __name__ == '__main__':
    try: [fin, fout, host] = sys.argv[1:]
    except:
        print "USAGE: %s INPUT OUTPUT HOST" % sys.argv[0]
        sys.exit(1)
    f = open(fin, 'r')
    source = f.read()
    f.close()
    port = 2318
    bytecode, messages = compile(source, host, port)
    if bytecode:
        f = open(fout, 'w')
        f.write(bytecode)
        f.close()
    print messages
