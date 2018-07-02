#!/usr/bin/env sh
if [ -e "${CILO_PIPELINE}.cilo" ]; then
    # BASE IMPORT
    PIPELINE_FILE="/home/groovy/cilo/tmp/${CILO_PIPELINE}.cilo"
    touch "$PIPELINE_FILE"
    BASE_IMPORT=`cat <<EOF
import groovy.transform.BaseScript
@BaseScript CiloBaseScript base

EOF`
    echo "$BASE_IMPORT" >> "$PIPELINE_FILE"    
    cat "${CILO_PIPELINE}.cilo" >> "$PIPELINE_FILE"
    # PREPROCESSOR
    cat "${PIPELINE_FILE}" | awk -f /home/groovy/cilo/bin/shell-substitution.awk > "${PIPELINE_FILE}.sub"
    rm "${PIPELINE_FILE}"
    mv "${PIPELINE_FILE}.sub" "${PIPELINE_FILE}"
    # RUN
    groovy -cp /home/groovy/cilo/bin/ "${PIPELINE_FILE}"
else
    echo "COULD NOT FIND PIPELINE FILE (${CILO_PIPELINE}.cilo)."
fi
