#!/usr/bin/env python3

from os import listdir, stat, chmod, getcwd, chdir
from stat import S_IEXEC
from os import system as cmd

plotTemplate = '''#!/usr/bin/env gnuplot

set terminal svg fname 'Verdana' fsize 10
set output '{fileName}.svg'

set datafile separator ","
set title "{plotName}"
set xlabel "{xLabel}"
set ylabel "{yLabel}"
set logscale y 2
set key left top

set xtics 1

set xrange [3:15]

plot '<grep RegisterSync {dataFile}' using 2:3 with lines title "Sync" linetype 1, \\
         "" using 2:3:5 with yerrorbars linetype 1 notitle, \\
     '<grep RegisterOwn {dataFile}' using 2:3 with lines title "Own" linetype 2, \\
         "" using 2:3:5 with yerrorbars linetype 2 notitle, \\
     '<grep RegisterCustomMap {dataFile}' using 2:3 with lines title "Map" linetype 3, \\
         "" using 2:3:5 with yerrorbars linetype 3 notitle
'''

origin = getcwd()
cmd('sbt "run-main io.github.morgaroth.quide.utils.RefactorData"')

scripts = [f for f in listdir('data/') if f.endswith('.gpt')]
chdir('data')

xLabel = 'Rozmiar rejestru w kubitach [n]'
yLabel = 'Czas [ms]'

plots = [
    {
        'fileName': 'RoundTime',
        'dataFile': 'round-time.csv',
        'xLabel': xLabel,
        'yLabel': yLabel,
        'plotName': 'Wykres czasu wykonania jednej rundy',
    },
    {
        'fileName': 'ExecutionTime',
        'dataFile': 'execution-time.csv',
        'xLabel': xLabel,
        'yLabel': yLabel,
        'plotName': 'Wykres czasu symulacji algorytmu Grover\'a',
    },
    {
        'fileName': 'MemoryUsage',
        'dataFile': 'round-memory-usage.csv',
        'xLabel': xLabel,
        'yLabel': yLabel,
        'plotName': 'Wykres użycia pamięci',
    },
]

for plot in plots:
    result = plotTemplate.format(**plot)
    scriptFile = plot['fileName'] + '.gpt'
    with open(scriptFile, 'w') as f:
        f.write(result)
    chmod(scriptFile, stat(scriptFile).st_mode | S_IEXEC)

for script in scripts:
    cmd('./{}'.format(script.replace(' ', "\ ")))
