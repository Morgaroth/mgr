#!/usr/bin/env python3

from os import system as cmd
from sys import argv

if len(argv) < 3:
    print('illegal format', argv)
    exit(-1)

kindsDict = {
    'own': 'io.github.morgaroth.quide.core.register.own.RegisterOwn',
    'sync': 'io.github.morgaroth.quide.core.register.sync.RegisterSync',
    'ownterm': 'io.github.morgaroth.quide.core.register.own_terminated.RegisterOwnTerminated',
    'nodeath': 'io.github.morgaroth.quide.core.register.nodeath.RegisterNoDeaths',
    'customap': 'io.github.morgaroth.quide.core.register.custom_map.RegisterCustomMap',
}

kinds = []
for arg in argv[1:]:
    if arg in kindsDict:
        kinds.append(kindsDict[arg])
sizes = []
for arg in argv[1:]:
    try:
        sizes.append(str(int(arg)))
    except ValueError:
        pass

if len(kinds) == 0:
    print('no kinds recognized')
    exit(-1)

if len(sizes) == 0:
    print('no size recognized')
    exit(-1)

tester = 'io.github.morgaroth.quide.tests.TimeTest'

repeats = 5

for kind in kinds:
    for _ in range(0, repeats):
        for size in sizes:
            print('run {} with size {}'.format(kind, size))
            run__command = 'sbt "run-main {0} {1} {2}"'.format(tester, kind, size)
            print(run__command)
            cmd(run__command)
