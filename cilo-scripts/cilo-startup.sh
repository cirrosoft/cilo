#!/usr/bin/env sh
if [ -e "${CILO_PIPELINE}.cilo" ]; then
    PIPELINE_FILE="/home/groovy/cilo/tmp/${CILO_PIPELINE}.cilo"
    touch "$PIPELINE_FILE"
    BASE_IMPORT=`cat <<EOF
import groovy.transform.BaseScript
@BaseScript CiloBaseScript base

EOF`
    echo "$BASE_IMPORT" >> "$PIPELINE_FILE"    
    cat "${CILO_PIPELINE}.cilo" >> "$PIPELINE_FILE"
    groovy -cp /home/groovy/cilo/bin/ "${PIPELINE_FILE}"
else
    echo "COULD NOT FIND PIPELINE FILE (${CILO_PIPELINE}.cilo)."
fi
