# NAB scripts

These scripts operate on the output of `NabAllDetectors`.

- `nabgrapher.py` shows the events detected (in green) on the input data, along
  with NAB's labels of anomalies (in red), and their acceptable windows (in 
  blue). 
- `nabResultsFormatter.py` will place the results into the correct location and format for scoring. This is a very slow
  operation which could be optimised, but hasn't :)
- `nab-scorer.sh` should be run after `nabResultsFormatter.py`. It will display the scores in the terminal.
- `run-parameter-tuner.sh` was used to get the scores obtained in the final results for the "Parameter Tuning Results"
  wiki page, discussed in issue #31.
