#!/bin/bash

# Baseline

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors baseline \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors baseline \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors baseline \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors baseline \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors baseline \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors baseline \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

# Changepoint

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors changepoint \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors changepoint \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors changepoint \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors changepoint \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors changepoint \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors changepoint \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

# Distdiff

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors distdiff \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors distdiff \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors distdiff \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors distdiff \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors distdiff \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors distdiff \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

# Loss

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors loss \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors loss \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors loss \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors loss \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors loss \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors loss \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

# Mode

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors mode \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors mode \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors mode \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors mode \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors mode \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors mode \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

# Spike

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors spike \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors spike \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors spike \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors spike \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors spike \
  --scoreTargets standard \
  --nabScoreScalingMode continuous \
  --randomise-defaults true \
  --wallclock-limit 30000

java -jar parameterTuner-assembly-0.1-SNAPSHOT.jar \
  --detectors spike \
  --scoreTargets reward_low_FN_rate \
  --nabScoreScalingMode binary \
  --randomise-defaults true \
  --wallclock-limit 30000
