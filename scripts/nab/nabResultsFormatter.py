from datetime import datetime as dt
from pathlib import Path

import pandas as pd
from dateutil import parser


def do_the_thing(loc):
    print(f"Processing {loc}!")
    # We'll read in the events we outputted
    ourevents = pd.read_csv(f"../../out/nab-allDetectors/{loc}", names=["detector", "severity", "time", "description"])

    # And the baseline results, so we can steal their labels column
    exampleresults = pd.read_csv(f"../../data/NAB/results/null/{loc.split('/')[0]}/null_{loc.split('/')[1]}")

    # We'll format our timestamps correctly
    ourevents['datetime'] = ourevents['time'].map(lambda x: dt.fromtimestamp(x / 1000))
    ourevents['formatted_time'] = ourevents['datetime'].map(lambda x: x.strftime("%Y-%m-%d %H:%M:%S"))

    # For each detector that might provide an output, we'll format it according to Appendix C.
    for det in ['baseline_events', 'changepoint_events', 'distdiff_events', 'mode_events', 'spike_events']:
        detname = det.split('_')[0]

        # Figure out what file to put it in
        output_loc = f"../../data/NAB/results/{detname}/{loc.split('/')[0]}"
        Path(output_loc).mkdir(parents=True, exist_ok=True)
        output_file = f"{output_loc}/{detname}_{loc.split('/')[1]}"

        # Construct the correct format
        out = pd.DataFrame(columns=["timestamp", "value", "anomaly_score", "label"])
        out['timestamp'] = exampleresults['timestamp']
        out['value'] = exampleresults['value']
        out['label'] = exampleresults['label']

        # Iterate over all the timestamps in order to populate the anomaly_score with our severity.
        relevant_events = ourevents[ourevents['detector'] == det]
        if det not in ourevents["detector"].unique():
            print(f"No scores for {det}...")
            out['anomaly_score'] = 0
        else:
            print(f"Processing {det}!")

            # This is really slow, but it works.
            scores = []
            for x in exampleresults['timestamp']:
                x_as_dt = parser.parse(x)

                rows = relevant_events[relevant_events['datetime'].map(lambda n: n.to_pydatetime()) == x_as_dt]
                if len(rows) > 0:
                    scores.append(rows['severity'].iloc[0] / 100)
                else:
                    scores.append(0)

            out['anomaly_score'] = scores

        out.to_csv(output_file, index=False)


print(f"Running from {Path.cwd()}")

files = Path.cwd().parent.parent.glob('out/nab-allDetectors/**/*.csv')
for f in sorted(files):
    print(f)
    do_the_thing("/".join(str(f).split('/')[-2:]))
