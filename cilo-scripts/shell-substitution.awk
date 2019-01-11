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
            filename="/home/cilo/tmp/script" i ".sh"
            print "#!/usr/bin/env sh" "\n" "# script from line number " linum " of original file. \n" script >> filename
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
                    
