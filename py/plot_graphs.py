#!/usr/bin/env python3

from os import listdir, stat, chmod, getcwd, chdir, remove
from stat import S_IEXEC
from sys import argv
from os import system as cmd

datafile = 'data.data'
if len(argv) > 1:
    datafile = argv[1]

plotTemplateBase = '''#!/usr/bin/env gnuplot

set terminal svg fname 'Verdana' fsize 12

# set terminal png size 960,720 font Verdana 10
# set output '{fileName}.png'

set datafile separator ","
set title "{plotName}"
set xlabel "{xLabel}"
set ylabel "{yLabel}"
set logscale y 2
set key left top

set xtics 1

set xrange [4:16] '''

plotTemplate = plotTemplateBase + '''
set output '{fileName}.svg'

plot '<grep RegisterSync {dataFile}' using 2:3 with {linesFormat} linecolor rgb 'red' title "Sync", \\
        "" using 2:3:5 with yerrorbars linecolor rgb 'red' pointtype 7 pointsize 0.1 notitle, \\
        "" using 2:6 with points lc rgb 'red' pointtype 7 pointsize 0.5 title "Sync min", \\
     '<grep RegisterOwnTerminated {dataFile}' using 2:3 with {linesFormat} linecolor rgb 'blue' title "OwnTerminated", \\
        "" using 2:3:5 with yerrorbars linecolor rgb 'blue' pointtype 7 pointsize 0.1 notitle, \\
        "" using 2:6 with points linecolor rgb 'blue' pointtype 7 pointsize 0.5 title "OwnTerminated min", \\
     '<grep RegisterCustomMap {dataFile}' using 2:3 with {linesFormat} linecolor rgb 'green' title "OwnMap", \\
        "" using 2:3:5 with yerrorbars linecolor rgb 'green' pointtype 7 pointsize 0.1 notitle, \\
        "" using 2:6 with points linecolor rgb 'green' pointtype 7 pointsize 0.5 title "OwnMap min"
'''

comparePlotTemplate = plotTemplateBase + '''

set output '{fileName}-cmp.svg'
plot '<grep {register} execution-time.csv' using 2:3 with {linesFormat} linecolor rgb 'red' title "Breaks", \\
        "" using 2:3:5 with yerrorbars linecolor rgb 'red' pointtype 7 pointsize 0.1 notitle, \\
        "" using 2:6 with points lc rgb 'red' pointtype 7 pointsize 0.5 title "Breaks min", \\
     '<grep {register} execution-time-total.csv' using 2:3 with {linesFormat} linecolor rgb 'blue' title "NoBreaks", \\
        "" using 2:3:5 with yerrorbars linecolor rgb 'blue' pointtype 7 pointsize 0.1 notitle, \\
        "" using 2:6 with points linecolor rgb 'blue' pointtype 7 pointsize 0.5 title "NoBreaks min"
'''

origin = getcwd()
cmd('sbt "run-main io.github.morgaroth.quide.utils.RefactorData {}"'.format(datafile))

xLabel = 'Rozmiar rejestru w kubitach [n]'
yLabel = 'Czas [ms]'

linesFormat = 'linespoints pointtype 7 pointsize 0.5 lw 1'

comparePlot = {'xLabel': xLabel, 'yLabel': yLabel, 'linesFormat': linesFormat, 'plotName': 'Wykres porównania'}

roundTimePlot = {'fileName': 'RoundTime', 'dataFile': 'round-time.csv', 'xLabel': xLabel, 'yLabel': yLabel,
                 'linesFormat': linesFormat, 'plotName': 'Wykres czasu wykonania jednej rundy'}

execTimePlot = {'fileName': 'ExecutionTime', 'dataFile': 'execution-time.csv', 'xLabel': xLabel, 'yLabel': yLabel,
                'linesFormat': linesFormat, 'plotName': 'Wykres czasu symulacji algorytmu Grover\'a', }

memoryUsagePlot = {'fileName': 'MemoryUsage', 'dataFile': 'round-memory-usage.csv', 'xLabel': xLabel,
                   'yLabel': 'Użycie pamięci [kB]', 'linesFormat': linesFormat, 'plotName': 'Wykres użycia pamięci'}

execTimePlotFull = execTimePlot.copy()
execTimePlotFull.update({'fileName': execTimePlot['fileName'] + '-NoBreaks', 'dataFile': 'execution-time-total.csv'})

plots = [
    roundTimePlot,
    execTimePlot,
    memoryUsagePlot,
    execTimePlotFull,
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

for reg in ['RegisterSync', 'RegisterOwnTerminated', 'RegisterCustomMap']:
    result = comparePlotTemplate.format(**comparePlot, fileName=reg, register=reg)
    scriptFile = '%s-cmp.gpt' % reg
    with open(scriptFile, 'w') as f:
        f.write(result)
    chmod(scriptFile, stat(scriptFile).st_mode | S_IEXEC)
    cmd('./{}'.format(scriptFile))
    cmd('inkscape -z -e {0}-cmp.png -h 2000 {0}-cmp.svg'.format(reg))

clean = True
# clean = False

if clean:
    for file in [f for f in listdir('.') if f.endswith('.gpt') or f.endswith('.svg')]:
        remove(file)

chdir(origin)
