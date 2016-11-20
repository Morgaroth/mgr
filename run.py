#!/usr/bin/env python3

from sys import argv
from os import system as cmd

if len(argv) != 3:
    print('illegal format', argv)
    exit(-1)

typeName = argv[1]
size = int(argv[2])

types = {
    'own': 'io.github.morgaroth.quide.core.register.own.RegisterOwn',
    'sync': 'io.github.morgaroth.quide.core.register.sync.RegisterSync',
    'ownterm': 'io.github.morgaroth.quide.core.register.own_terminated.sync.RegisterOwnTerminated',
    'nodeath': 'io.github.morgaroth.quide.core.register.nodeath.RegisterNoDeaths',
}

type = types.get(typeName)

if type is None:
    print('type {} not recognized'.format(typeName))
    exit(-2)

tester = 'io.github.morgaroth.quide.tests.TimeTest'

print('run {} with size {}'.format(typeName, size))

run__command = 'sbt "run-main {} {} {}"'.format(tester, type, size)
print(run__command)
cmd(run__command)
