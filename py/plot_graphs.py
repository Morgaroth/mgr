#!/usr/bin/env python3

from os import listdir, stat, chmod, getcwd, chdir, remove
from stat import S_IEXEC
from os import system as cmd

plotTemplate = '''#!/usr/bin/env gnuplot

set terminal svg fname 'Verdana' fsize 12
set output '{fileName}.svg'

# set terminal png size 960,720 font Verdana 10
# set output '{fileName}.png'

set datafile separator ","
set title "{plotName}"
set xlabel "{xLabel}"
set ylabel "{yLabel}"
set logscale y 2
set key left top

set xtics 1

set xrange [3:16]

plot '<grep RegisterSync {dataFile}' using 2:3 with {linesFormat} linecolor rgb 'red' title "Sync" , \\
        "" using 2:3:5 with yerrorbars linecolor rgb 'red' pointtype 7 pointsize 0.1 notitle, \\
     '<grep RegisterOwnTerminated {dataFile}' using 2:3 with {linesFormat} linecolor rgb 'blue' title "OwnTerminated", \\
        "" using 2:3:5 with yerrorbars linecolor rgb 'blue' pointtype 7 pointsize 0.1 notitle, \\
     '<grep RegisterCustomMap {dataFile}' using 2:3 with {linesFormat} linecolor rgb 'green'  title "OwnMap", \\
        "" using 2:3:5 with yerrorbars linecolor rgb 'green' pointtype 7 pointsize 0.1 notitle
'''

origin = getcwd()
cmd('sbt "run-main io.github.morgaroth.quide.utils.RefactorData"')

xLabel = 'Rozmiar rejestru w kubitach [n]'
yLabel = 'Czas [ms]'

linesFormat = 'linespoints pointtype 7 pointsize 0.5 lw 1'

plots = [
    {
        'fileName': 'RoundTime',
        'dataFile': 'round-time.csv',
        'xLabel': xLabel,
        'yLabel': yLabel,
        'linesFormat': linesFormat,
        'plotName': 'Wykres czasu wykonania jednej rundy',
    },
    {
        'fileName': 'ExecutionTime',
        'dataFile': 'execution-time.csv',
        'xLabel': xLabel,
        'yLabel': yLabel,
        'linesFormat': linesFormat,
        'plotName': 'Wykres czasu symulacji algorytmu Grover\'a',
    },
    {
        'fileName': 'MemoryUsage',
        'dataFile': 'round-memory-usage.csv',
        'xLabel': xLabel,
        'yLabel': yLabel,
        'linesFormat': linesFormat,
        'plotName': 'Wykres użycia pamięci',
    },
]

chdir('data')

for plot in plots:
    result = plotTemplate.format(**plot)
    name = plot['fileName']
    scriptFile = name + '.gpt'
    with open(scriptFile, 'w') as f:
        f.write(result)
    chmod(scriptFile, stat(scriptFile).st_mode | S_IEXEC)
    cmd('./{}'.format(scriptFile))
    cmd('inkscape -z -e {0}.png -h 2000 {0}.svg'.format(name))

clean = True
# clean = False

if clean:
    for file in [f for f in listdir('.') if f.endswith('.gpt') or f.endswith('.csv') or f.endswith('.svg')]:
        remove(file)

chdir(origin)
