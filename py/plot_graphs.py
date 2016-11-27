#!/usr/bin/env python3

from os import listdir, getcwd, chdir
from os import system as cmd

origin = getcwd()
cmd('sbt "run-main io.github.morgaroth.quide.utils.RefactorData"')

scripts = [f for f in listdir('data/') if f.endswith('.gpt')]
chdir('data')
for script in scripts:
    cmd('./{}'.format(script.replace(' ', "\ ")))
