#!/usr/bin/env python3

from os import system as cmd
from sys import argv

if len(argv) < 3:
    print('illegal format', argv)
    exit(-1)

typeName = argv[1]
sizes = [str(int(s)) for s in argv[2:]]

types = {
    'own': 'io.github.morgaroth.quide.core.register.own.RegisterOwn',
    'sync': 'io.github.morgaroth.quide.core.register.sync.RegisterSync',
    'ownterm': 'io.github.morgaroth.quide.core.register.own_terminated.RegisterOwnTerminated',
    'nodeath': 'io.github.morgaroth.quide.core.register.nodeath.RegisterNoDeaths',
    'customap': 'io.github.morgaroth.quide.core.register.custom_map.RegisterCustomMap',
}

type = types.get(typeName)

if type is None:
    print('type {} not recognized'.format(typeName))
    exit(-2)

tester = 'io.github.morgaroth.quide.tests.TimeTest'

print('run {} with size {}'.format(typeName, sizes))

run__command = 'sbt "run-main {0} {1} {2}"'.format(tester, type, ' '.join(sizes))
print(run__command)
cmd(run__command)
