abstract class CiloBaseScript extends Script {
    abstract def runCode()

    def steps = [:]
    
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

    def ciloShellScript(filename) {
        // file = new File(filename)
        // linumLine = file.readLines().get(2)
        // print linumLine
        shell("chmod 777 $filename")
        shell("$filename")
    }
    
    def shell(command) {
        // TODO: write shell command
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = command.execute()
        proc.consumeProcessOutput(sout, serr)
        proc.waitForOrKill(1000)
        print "$sout"
    }

    def step(name, closure) {
        steps[name] = closure
    }
        
}
