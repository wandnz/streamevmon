#!/bin/bash

cd ../../ || (echo "Failed to cd" && exit)
source venv/bin/activate

cd data/NAB/ || (echo "Failed to cd" && exit)

python run.py -d baseline,changepoint,distdiff,mode,spike --skipConfirmation --optimize --score --normalize
