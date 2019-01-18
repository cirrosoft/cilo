import groovy.json.*

abstract class CiloBaseScript extends Script {
    abstract def runCode()

    private static def dockerEnv = System.getenv()

    public static def PROJECT_NAME
    public static def RUN_NAME
    public static def RUN_NUMBER
    public static def logFile

    public static Boolean firstRun = true;
    public static Boolean firstPrint = true;
    public static Boolean lastPrint = false;
    public static Boolean interceptorOverride = false;
    
    
    private static def steps = [:]
    private static def secretsMap = [:]
    private static def envMap = [:]

    public static def git = [:]
    
    private static def isInsideSshClosure = false
    private static def sshBoundAddress
    private static def sshBoundIdentityFile

    private static def stdOutInterceptor
    private static def stdErrInterceptor
    
    // used by macro processor
    protected static def stdMap = [:]
    protected static def stdOut = ""
    protected static def stdErr = ""
    protected static def exitCode = 0

    // used for json status object
    protected static def stepStatus = [:]
    
    def run() {
        PROJECT_NAME = dockerEnv["PROJECT_NAME"]
        RUN_NAME = dockerEnv["RUN_NAME"]
        RUN_NUMBER = dockerEnv["RUN_NUMBER"].toInteger()
        logFile = new File("/home/cilo/workspace/.cilo/log/${PROJECT_NAME}-${RUN_NAME}-${RUN_NUMBER}-log.txt")
        if (firstRun) {
            firstRun = false
        } else {
            return
        }
        // Secret Interception
        def interceptorClosure = { secretsMap, str ->
            def newString = str
            def timestamp = (new Date().format("MM-dd-yyyy-HH-mm-ss", Calendar.getInstance().getTimeZone())) + " RUN:\t"
            if (CiloBaseScript.interceptorOverride) {
                return newString;
            }
            for (pair in secretsMap) {
                def key = pair.key;
                def value = pair.value.trim();
                def valueArray = value.split('\n')
                for (valueLine in valueArray) {
                    def trimmed = valueLine.trim()
                    newString = newString.replace(trimmed, "********")
                }
            }
            if (CiloBaseScript.firstPrint) {
                CiloBaseScript.firstPrint = false
                newString = timestamp + newString
            }
            if (!CiloBaseScript.lastPrint) {
                newString = newString.replace("\n", "\n"+timestamp)
            }
            logFile.append(newString)
            return newString;
        }
        stdOutInterceptor = new SecretInterceptor(secretsMap, interceptorClosure, true)
        stdErrInterceptor = new SecretInterceptor(secretsMap, interceptorClosure, false)
        stdOutInterceptor.start()
        stdErrInterceptor.start()

        collectGitInformation()

        beforeRun()
        writeStatus()
        def currentStep=null
        def currentStepName=null
        long startTime = 0
        long endTime = 0
        long difference = 0
        try {
            final result = runCode()
            // Add Steps' Status
            def number = 0
            for (step in steps) {
                number++
                def stepString = "${step.key}"
                def status = [:]
                status.name = stepString
                status.elapsedTimeMS = 0
                status.status = "pending"
                status.number = number
                stepStatus[stepString] = status
            }
            // Execute Steps
            for (step in steps) {
                def stepString = "${step.key}"
                currentStep = step
                currentStepName = stepString
                stepStatus[stepString].status = "in-progress"
                def lineCount = 80-stepString.length()
                def line = "-".multiply(lineCount)
                println "-".multiply(80)
                println "-".multiply(76)+"STEP"
                println "${line}${stepString}"
                println "-".multiply(80)
                //before
                beforeEachStep()
                writeStatus()
                // run closure
                startTime = System.nanoTime();
                step.value()
                endTime = System.nanoTime();
                // elapsed time
                difference = (endTime - startTime) / 1e6;
                stepStatus[stepString].status = "completed"
                stepStatus[stepString].elapsedTimeMS = difference
                // after
                afterEachStep()
                writeStatus()
            }
        } catch (e) {
            if (currentStepName != null) {
                difference = (endTime - startTime) / 1e6;
                stepStatus[currentStepName].status = "failed"
                stepStatus[currentStepName].elapsedTimeMS = difference
            }
            CiloBaseScript.lastPrint = true
            println "Exception: " + e
        } finally {
            afterRun()
            writeStatus()
        }
    }

    private writeStatus() {
        def json = getStatus()
        CiloBaseScript.interceptorOverride = true
        statusFile = new File("/home/cilo/workspace/.cilo/log/${PROJECT_NAME}-${RUN_NAME}-${RUN_NUMBER}-status.txt")
        statusFile.bytes = []
        statusFile << json
        CiloBaseScript.interceptorOverride = false
    }

    private getStatus() {
        def status = [:]
        def summary = [:]
        def steps = []
        def sorted = []
        stepStatus.each { key, value -> sorted << value }
        sorted = sorted.sort { a, b ->
            a.number <=> b.number
        }
        // Gather Status
        def failedStepNumber = sorted.findIndexOf { step -> step.status == "failed" } + 1
        def stepCount = sorted.size()
        def pendingCount = sorted.count { step -> step.status == "pending" }
        def lastStepNumber = stepCount - pendingCount
        def lastStatus = "unknown"
        if (failedStepNumber > 0) {
            lastStatus = "failure"
        } else if (lastStepNumber == stepCount) {
            lastStatus = "success"
        } else {
            lastStatus = "running"
        }
        def elapsedTimeMS = 0
        for (step in sorted) {
            steps << step
            elapsedTimeMS += step.elapsedTimeMS
        }
        // Add to Summary and Status
        summary.project = PROJECT_NAME
        summary.runName = RUN_NAME
        summary.runNumber = RUN_NUMBER
        summary.branchName = git.branchName
        summary.commitHash = git.commitHash
        summary.status = lastStatus
        summary.totalStepCount = stepCount
        summary.lastStepNumber = lastStepNumber
        summary.totalTimeMS = elapsedTimeMS
        summary.userName = git.userName
        summary.userEmail = git.userEmail
        status.steps = steps
        status.summary = summary
        def json = JsonOutput.toJson(status)
        return json
    }

    def beforeEachStep() {

    }

    def afterEachStep() {

    }
    
    def beforeRun() {
        
    }
    
    def afterRun() {
        
    }

    public static def step(name, closure) {
        closure.delegate = this;
        steps[name] = closure
    }
                    
    public static def ciloShellScript(filename) {
        shell("chmod 700 $filename")
        if (isInsideSshClosure) {
            shell("touch ${filename}.ssh", false)
            shell("chmod 700 ${filename}.ssh", false)
            shell("touch ${filename}.bash", false)
            shell("chmod 700 ${filename}.bash", false)
            def file = new File("${filename}")
            def sshFile = new File("${filename}.ssh")
            def bashWithEnvFile = new File("${filename}.bash")
            // Environmental variables are written to the script executed through ssh.
            // Before thinking of another way to do this look at stack overflow article below:
            //  StackOverflow: https://stackoverflow.com/questions/4409951/can-i-forward-env-variables-over-ssh
            def environment=[]
            def allowedEnvs = ["PROJECT_NAME", "RUN_NUMBER", "RUN_NAME", "CILOFILE"]
            System.getenv().each{ k, v ->
                allowedEnvs.each { a ->
                    if (a.equals(k)) {
                        environment<<"""read -r -d '' $k <<'EOM'
$v
EOM"""
                    }
                }
            }
            secretsMap.each{ 
                k, v -> environment<<"""read -r -d '' $k <<'EOM'
$v
EOM"""}
            envMap.each{ k, v -> environment<<"""read -r -d '' $k <<'EOM'
$v
EOM"""}
            List<String> lines = file.readLines();
            // copy lines from file inserting into line 2 just after the shebang statement.
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                def line = lines[lineIndex]
                if (lineIndex == 1) {
                    environment.each{
                        s ->
                            bashWithEnvFile << "$s\n"
                    }
                }
                bashWithEnvFile << "$line\n"
            }
            sshFile << "#!/usr/bin/env bash\n"
            sshFile << "ssh -o \"StrictHostKeyChecking no\" -i ${sshBoundIdentityFile} ${sshBoundAddress} 'bash -s' < ${filename}.bash\n"
            println "Attempting to run ssh script at \"${sshBoundAddress}\" using identity \"${sshBoundIdentityFile}\""
            def shReturn = shell("${filename}.ssh")
            sshFile.delete()
            bashWithEnvFile.delete()
            file.delete()
            return shReturn
        } else {
            return shell("${filename}")
        }
    }

    private static def collectGitInformation() {
      def files = new File(".cilo").listFiles()
      for (file in files) {
        if (file.getName().endsWith(".git")) {
          git.put(file.getName().substring(0, file.getName().lastIndexOf(".")), file.getText().trim())
        }
      }
    }

    public static def ssh(sshAddressString, identityFile, closure) {
        def prevSsh = isInsideSshClosure
        def prevBoundAddress = sshBoundAddress
        def prevBoundIdentityFile = sshBoundIdentityFile
        isInsideSshClosure = true
        sshBoundAddress = sshAddressString
        sshBoundIdentityFile = identityFile
        closure()
        isInsideSshClosure = prevSsh
        sshBoundAddress = prevBoundAddress
        sshBoundIdentityFile = prevBoundIdentityFile
    }
    
    public static def shell(command, shouldPrint = true) {
        StringBuilder stdOut = new StringBuilder()
        StringBuilder stdErr = new StringBuilder()
        int exitCode = 1
        def environment=[]
        System.getenv().each{ k, v -> environment<<"$k=$v" }
        secretsMap.each{ k, v -> environment<<"$k=$v" }
        envMap.each{ k, v -> environment<<"$k=$v" }
        def proc = command.execute(environment, new File("/home/cilo/workspace/"))
        proc.in.eachLine { line ->
            def newString=line
            def timestamp = (new Date().format("MM-dd-yyyy-HH-mm-ss")) + " RUN:\t"
            for (pair in secretsMap) {
                def key = pair.key;
                def value = pair.value.trim();
                def valueArray = value.split('\n')
                for (valueLine in valueArray) {
                    def trimmed = valueLine.trim()
                    newString = newString.replace(trimmed, "********")
                }
            }
            stdOut.append(line)
            println newString;
        }
        def minutes = 60
        proc.waitForOrKill(minutes*60*1000)
        stdErr.append(proc.err.text)
        exitCode = proc.exitValue()
        return ["stdOut":stdOut, "stdErr":stdErr, "exitCode":exitCode]
    }

    public static def env(map, closure) {
        for (pair in map) {
            def key=pair.key
            def value=pair.value
            envMap << ["${key}":"${value}"]
        }
        closure.delegate = this;
        closure.call()
        for (pair in map) {
            def key=pair.key
            def value=pair.value
            envMap.remove("${key}")
        }
    }

    public static def secrets(namesMap, closure) {
        def binding = new Binding()
        for (name in namesMap) {
            shell("cilo-decrypt-secret ${name}")
            def nameText = "${name}Text"
            def nameBytes = "${name}Bytes"
            def nameFile = "${name}File"
            def secretFile = new File("/home/cilo/secret/local/${name}")
            if (secretFile == null || secretFile.length() <= 0) {
                throw new SecretNotFoundException("Secret '${name}' is either empty or does not exist.")
            }
            def secretBytes = secretFile.getBytes()
            def secretText = secretFile.getText()
            secretsMap << ["${name}":"${secretText}"]
            secretsMap << ["${nameText}":"${secretText}"]
            secretsMap << ["${nameBytes}":"${secretBytes}"]
            secretsMap << ["${nameFile}":"/home/cilo/secret/local/${name}"]
            for (secretPair in secretsMap) {
                binding.setVariable(secretPair.key, secretPair.value)
            }
        }
        closure.delegate = this;
        closure.setBinding(binding)
        closure.call()
        for (name in namesMap) {
            def nameText = "${name}Text"
            def nameBytes = "${name}Bytes"
            def nameFile = "${name}File"
            def secretFile = new File("/home/cilo/secret/local/${name}")
            if (secretFile == null || secretFile.length() <= 0) {
                throw new IllegalArgumentException("Secret '${name}' is either empty or does not exist.")
            }
            def secretBytes = secretFile.getBytes()
            def secretText = secretFile.getText()
            secretsMap.remove("${name}")
            secretsMap.remove("${nameText}")
            secretsMap.remove("${nameBytes}")
            secretsMap.remove("${nameFile}")
            shell("rm /home/cilo/secret/local/${name}")
        }
    }
    
    public static def secret(name, closure) {
        secrets([name], closure)
    }
}

class SecretNotFoundException extends Exception {
    public SecretNotFoundException(String message) {
        super(message);
    }
}

class SecretInterceptor extends java.io.FilterOutputStream {
    public  Closure callback;
    public boolean output;
    public LinkedHashMap secretsMap
    public PrintStream outStream
    SecretInterceptor(LinkedHashMap secretsMap, final Closure callback, Boolean output) {
        super(output ? System.out : System.err);
        assert secretsMap != null;
        this.secretsMap = secretsMap;
        assert callback != null;
        this.callback = callback;
        this.output = true;
    }
    public void start() {
        outStream = new PrintStream(this)
        if (output) {
            System.setOut(outStream);
        } else {
            System.setErr(outStream);
        }
    }
    public void stop() {
        if (output) {
            System.setOut(System.out);
        } else {
            System.setErr(System.err);
        }
    }
    public void write(byte[] b) throws IOException {
        String newString = (String) callback.call(secretsMap, new String(b));
        def newBytes = newString.getBytes();
        out.write(newBytes);
    }
    public void write(byte[] b, int off, int len) throws IOException {
        String newString = (String) callback.call(secretsMap, new String(b, off, len));
        def newBytes = newString.getBytes();
        out.write(newBytes, 0, newBytes.length);
    }
    public void write(int b) throws IOException {
        String newString = (String) callback.call(secretsMap, String.valueOf((char) b));
        out.write(b);
    }
}
