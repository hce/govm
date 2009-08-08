from distutils.core import setup, Extension

module1 = Extension('govm',
                    sources = ['pygovm.c'])

setup (name = 'govm',
	   author = 'hc',
       version = '0.1',
       description = 'Python GoVM interpreter',
       ext_modules = [module1])
