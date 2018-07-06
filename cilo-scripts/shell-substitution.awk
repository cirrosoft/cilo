BEGIN {
    i=0
    linum=0
    script="";
}

{
    linum++
    if ( match($0, /^\s*\$(.*)/) ) {
        sub(/^\s*\$/, "", $0)   # strip leading prompt
        sub(/#.*$/, "", $0)   # strip comments
        commandPart=$0
        script=script commandPart "\n";
    } else {
        if ( length(script) > 0 ) {
            i++
            filename="/home/cilo/tmp/script" i ".sh"
            print "#!/usr/bin/env sh" "\n" "# script from line number " linum " of original file. \n" script >> filename
            print "stdMap = ciloShellScript('''" filename "'''); stdOut=stdMap['stdOut']; stdErr=stdMap['stdErr']; exitCode=stdMap['exitCode'];";
        }
        print $0
        script="";
    }
}
                    
