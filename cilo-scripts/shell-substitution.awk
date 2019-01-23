BEGIN {
    i=0
    linum=0
    script="";
    scriptLinum=0
}

{
    linum++
    if ( match($0, /^\s*\$(.*)/) ) { # if matches prompt with leading $
        sub(/^\s*\$/, "", $0)   # strip leading prompt
        sub(/#.*$/, "", $0)   # strip comments
        commandPart=$0
        script=script commandPart "\n";
        scriptLinum += 1
    } else {
        if ( length(script) > 0 ) {
            i++
            sheBang="#!/usr/bin/env bash"
            funcs="docker() { DOCKER_LOCATION=`which docker`; if ! [ -z ${CILO_DOCKER_ADDRESS+x} ]; then HSTRING=\"-H $CILO_DOCKER_ADDRESS \"; fi; if type unbuffer &> /dev/null; then UNBUF='unbuffer -p '; else UNBUF=''; fi; ${UNBUF}\"${DOCKER_LOCATION}\" ${HSTRING}$@ ; }"
            lineNumberString="# script from line number " linum " of original file."
            filename="/home/cilo/.cilo/tmp/script" i ".sh"
            print sheBang "\n" lineNumberString "\n" funcs "\n" script >> filename
            print "stdOut=\"\"; stdErr=\"\"; stdMap = ciloShellScript('''" filename "'''); stdOut=stdMap['stdOut']; stdErr=stdMap['stdErr']; exitCode=stdMap['exitCode']; stdMap=[:];";
        }
        print $0
        for (j = 0; j < scriptLinum-1; j++) {
            print "// cilo script line removed"
        }
        script="";
        scriptLinum=0
    }
}
                    
