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
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = command.execute()
        proc.consumeProcessOutput(sout, serr)
        def minutes = 3
        proc.waitForOrKill(minutes*60*1000)
        print "$sout"
    }
    
    def secret(name, closure) {
        def secret = "xnandor"
        def binding = new Binding()
        secretsMap << [name:secret]
        binding.setVariable(name, secret)
        closure.setBinding(binding)
        closure.call()
        secretsMap -= [name:secret]
    }
        
}
