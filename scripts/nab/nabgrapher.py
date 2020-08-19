import json
import os
from datetime import datetime as dt
from pathlib import Path

import matplotlib.pyplot as plot
import pandas as pd
from dateutil import parser


def do_the_thing(loc):
    loc = "/".join(loc.split('/')[-2:])
    print(loc)
    datafile = f"../../data/NAB/data/{loc}.csv"
    oureventsfile = f"../../out/nab-allDetectors/{loc}.csv"
    theirlabelsfile = f"../../data/NAB/labels/combined_labels.json"
    theirwindowsfile = f"../../data/NAB/labels/combined_windows.json"

    data = pd.read_csv(datafile)
    ourevents = pd.read_csv(oureventsfile, names=["title", "severity", "time", "description"])
    with open(theirlabelsfile) as f:
        theirlabels = json.load(f)
    with open(theirwindowsfile) as f:
        theirwindows = json.load(f)

    theirlabels = theirlabels[f"{'/'.join(loc.split('/')[-2:])}.csv"]
    theirwindows = theirwindows[f"{'/'.join(loc.split('/')[-2:])}.csv"]

    plot.figure(figsize=(15, 10))
    plot.style.use('dark_background')

    data['timestamp'] = data['timestamp'].map(parser.parse)

    for e in ourevents["time"]:
        et = dt.fromtimestamp(e / 1000)
        plot.axvline(x=et, color='g')

    for l in theirlabels:
        plot.axvline(x=parser.parse(l), color='r')
    for w in theirwindows:
        for i in w:
            plot.axvline(x=parser.parse(i), color='b')

    plot.plot(data['timestamp'], data['value'], color='w', linewidth=0.5)

    # plot.show()

    plot.savefig(f"../../out/nab-graphs/{loc}.svg")
    plot.close()


print(f"Running from {Path.cwd()}")

files = Path.cwd().parent.parent.glob('data/NAB/data/**/*.csv')
for f in files:
    do_the_thing(os.path.splitext(f)[0])
