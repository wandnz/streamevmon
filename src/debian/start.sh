#!/bin/sh

echo ${FLINK_DIR}/bin/taskmanager.sh start
${FLINK_DIR}/bin/taskmanager.sh start
echo ${FLINK_DIR}/bin/standalone-job.sh start-foreground --job-classname ${STREAMEVMON_ENTRYPOINT}
${FLINK_DIR}/bin/standalone-job.sh start-foreground --job-classname ${STREAMEVMON_ENTRYPOINT}
