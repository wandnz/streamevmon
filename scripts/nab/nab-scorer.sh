#!/bin/bash

echo "Activating virtualenv..."
source venv/bin/activate
echo "Using python at $(which python)"

echo "Moving to NAB directory..."
cd data/NAB/ || (echo "Failed to cd" && exit)

if [ "${3-noProfile}" == "profile" ]; then
  echo "Running NAB scorer with profiler..."
  py-spy record --format speedscope \
    -o "${2-results}/scorer-speedscope.json" \
    --subprocesses \
    --threads \
    --native \
    -- \
    ../../venv/bin/python run.py \
    -d "${1-baseline,changepoint,distdiff,mode,spike}" \
    --resultsDir "${2-results}" \
    --skipConfirmation \
    --optimize \
    --score \
    --normalize
else
  echo "Running NAB scorer..."
  ../../venv/bin/python run.py \
    -d "${1-baseline,changepoint,distdiff,mode,spike}" \
    --resultsDir "${2-results}" \
    --skipConfirmation \
    --optimize \
    --score \
    --normalize
fi
