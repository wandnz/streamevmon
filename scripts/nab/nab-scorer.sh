#!/bin/bash

source venv/bin/activate

cd data/NAB/ || (echo "Failed to cd" && exit)

python run.py \
  -d "${1-baseline,changepoint,distdiff,mode,spike}" \
  --resultsDir "${2-results}" \
  --skipConfirmation \
  --optimize \
  --score \
  --normalize
