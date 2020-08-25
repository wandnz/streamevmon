#!/bin/bash

source venv/bin/activate

cd data/NAB/ || (echo "Failed to cd" && exit)

python run.py \
  -d baseline,changepoint,distdiff,mode,spike \
  --resultsDir "${1-results}" \
  --skipConfirmation \
  --optimize \
  --score \
  --normalize
