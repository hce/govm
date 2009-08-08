from glob import glob
import cgi
import subprocess

for ifile, ofile, lfile in [(f, "bytecode/%s.govm" % f[8:-4], "bytecode/%s.log" % f[8:-4]) for f in glob("scripts/*.adl")]:
    cmd = ['java', '-jar', 'govmc.jar', ifile, ofile]
    print "Executing " + " ".join(cmd)
    sp = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    so, se = sp.communicate()
    f = open(lfile, 'w')
    f.write(so)
    f.close()
    if len(se):
        print "Errors:"
        print "=" * 70
        print se
