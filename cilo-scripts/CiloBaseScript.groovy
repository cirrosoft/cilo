abstract class CiloBaseScript extends Script {
    abstract def runCode()

    def steps = [:]
    def secretsMap = [:]
    
    def run() {
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
        println "CILOSHELLSCRIPT SECRETS DELEGATE: "
        println secretsMap
        for (secret in secretsMap) {
            println secret
        }
        println "CILOSHELLSCRIPT SECRETS DELEGATE END"
        shell("chmod 777 $filename")
        shell("$filename")
    }
    
    def shell(command) {
        def sout = new StringBuilder()
        def serr = new StringBuilder()
        // env=[]
        // System.getenv().each{ k, v -> env<<"$k=$v" }
        // secretsMap.each{ k, v -> env<<"$k=$v" }
        // def proc = command.execute(env, new File("/home/cilo/workspace/"))
        def proc = command.execute()
        proc.consumeProcessOutput(sout, serr)
        def minutes = 3
        proc.waitForOrKill(minutes*60*1000)
        // println env
        // println "COMMAND: \n${command}"
        // println "SHELLOUT: \n$sout"
        // println "SHELLERR: \n$serr"
        println "$sout"
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
        secretsMap << [name:secretText]
        secretsMap << [nameText:secretText]
        secretsMap << [nameBytes:secretBytes]
        secretsMap << [nameFile:"/home/cilo/secret/${name}"]
        binding.setVariable(name, secretText)
        binding.setVariable(nameText, secretText)
        binding.setVariable(nameBytes, secretBytes)
        binding.setVariable(nameFile, "/home/cilo/secret/${name}")
        closure.setBinding(binding)
        closure.call()
        secretsMap -= [name:secretText]
        secretsMap -= [nameText:secretText]
        secretsMap -= [nameBytes:secretBytes]
        secretsMap -= [nameFile:"/home/cilo/secret/${name}"]
        shell("rm /home/cilo/secret/${name}")
    }
        
}
