import org.codehaus.groovy.control.CompilerConfiguration

abstract class CiloBaseScript extends Script {
    abstract def runCode()

    private static Boolean firstRun = true;
    public static def steps = [:]
    public static def secretsMap = [:]
    public static def envMap = [:]

    public static def stdOutInterceptor
    public static def stdErrInterceptor
    
    def run() {
        if (firstRun) {
            firstRun = false
        } else {
            return
        }
        def allScripts = []
        def dir = new File("/home/cilo/tmp/")
        dir.eachFileRecurse(groovy.io.FileType.FILES) { file ->
            def matcher = (file =~ /(.*)\.(.*)$/)
            if (matcher.matches()) {
                def name = matcher[0][1]
                def extension = matcher[0][2]
                if (extension in ['groovy', 'cilo']) {
                    allScripts << file
                }
            }
        }
        for (sourceFile in allScripts) {
            def matcher = (sourceFile =~ /(.*)\.(.*)$/)
            def name=""
            def extension=""
            if (matcher.matches()) {
                name = matcher[0][1]
                extension = matcher[0][2]
            }
            GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader())
            Class groovyClass = loader.parseClass(sourceFile);
            GroovyObject myObject = (GroovyObject) groovyClass.newInstance();
            // Add methods
            myObject.metaClass.getMethods().stream()
                .filter({
                    it ->
                    methodName = it.getName()
                    className = it.getDeclaringClass().getName()
                    def matches = !(className in ["java.lang.Object", "groovy.lang.GroovyObjectSupport", "groovy.lang.Script", "CiloBaseScript"]) && !methodName.equals("runCode") && !methodName.equals("main")
                    matches
                }).each({
                    it ->
                    methodName = it.getName()
                    this.metaClass."${methodName}" = { Object... args ->
                        it.invoke(myObject, args)
                    }
                })
        }
        // Secret Interception
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
                println "----------------------------STEP (${step.key})-------------------------------------------"
                beforeEachStep()
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

    public def step(name, closure) {
        closure.delegate = this;
        steps[name] = closure
    }
    
    public def ciloShellScript(filename) {
        shell("chmod 777 $filename")
        return shell("$filename")
    }
    
    public def shell(command) {
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

    public def env(map, closure) {
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
            envMap -= ["${key}":"${value}"]
        }
    }
    
    public def secret(name, closure) {
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
        closure.delegate = this;
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
