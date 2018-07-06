abstract class CiloBaseScript extends Script {
    abstract def runCode()

    def steps = [:]
    def secretsMap = [:]
    def envMap = [:]

    def stdOutInterceptor
    def stdErrInterceptor
    
    def run() {
        def interceptorClosure = { secretsMap, str ->
            def newString=str
            for (pair in secretsMap) {
                def key = pair.key;
                def value = pair.value;
                def valueArray = value.split('\n')
                for (valueLine in valueArray) {
                    newString = newString.replace(valueLine.trim(), "********")
                }
            }
            return newString;
        }
        stdOutInterceptor = new SecretInterceptor(secretsMap, interceptorClosure, true)
        stdErrInterceptor = new SecretInterceptor(secretsMap, interceptorClosure, false)
        stdOutInterceptor.start()
        stdErrInterceptor.start()
        
        beforePipeline()
        try {
            final result = runCode()
            for (step in steps) {
                beforeEachStep()
                println "----------------------------STEP (${step.key})-------------------------------------------"
                step.value() // run closure
                afterEachStep()
            }
        } catch (e) {
            e.printStackTrace()
        } finally {
            afterPipeline()
        }
    }

    def beforeEachStep() {

    }

    def afterEachStep() {

    }
    
    def beforePipeline() {
        
    }
    
    def afterPipeline() {
        
    }

    def step(name, closure) {
        steps[name] = closure
    }
    
    def ciloShellScript(filename) {
        shell("chmod 777 $filename")
        return shell("$filename")
    }
    
    def shell(command) {
        StringBuilder stdOut = new StringBuilder()
        StringBuilder stdErr = new StringBuilder()
        int exitCode = 1
        env=[]
        System.getenv().each{ k, v -> env<<"$k=$v" }
        secretsMap.each{ k, v -> env<<"$k=$v" }
        envMap.each{ k, v -> env<<"$k=$v" }
        def proc = command.execute(env, new File("/home/cilo/workspace/"))
        proc.in.eachLine { line ->
            def newString=line
            for (pair in secretsMap) {
                def key = pair.key;
                def value = pair.value;
                def valueArray = value.split('\n')
                for (valueLine in valueArray) {
                    newString = newString.replace(valueLine.trim(), "********")
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

    def env(map, closure) {
        for (pair in map) {
            def key=pair.key
            def value=pair.value
            envMap << ["${key}":"${value}"]
        }
        closure.call()
        for (pair in map) {
            def key=pair.key
            def value=pair.value
            envMap -= ["${key}":"${value}"]
        }
    }
    
    def secret(name, closure) {
        shell("cilo-decrypt-secret ${name}")
        def nameText = "${name}Text"
        def nameBytes = "${name}Bytes"
        def nameFile = "${name}File"
        def secretFile = new File("/home/cilo/secret/${name}")
        def secretBytes = secretFile.getBytes()
        def secretText = secretFile.getText()
        def binding = new Binding()
        secretsMap << ["${name}":"${secretText}"]
        secretsMap << ["${nameText}":"${secretText}"]
        secretsMap << ["${nameBytes}":"${secretBytes}"]
        secretsMap << ["${nameFile}":"/home/cilo/secret/${name}"]
        for (secretPair in secretsMap) {
            binding.setVariable(secretPair.key, secretPair.value)
        }
        closure.setBinding(binding)
        closure.call()
        secretsMap -= ["${name}":"${secretText}"]
        secretsMap -= ["${nameText}":"${secretText}"]
        secretsMap -= ["${nameBytes}":"${secretBytes}"]
        secretsMap -= ["${nameFile}":"/home/cilo/secret/${name}"]
        shell("rm /home/cilo/secret/${name}")
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
