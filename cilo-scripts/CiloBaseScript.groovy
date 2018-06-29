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

    def shell(command) {
        // TODO: write shell command
    }

    def step(name, closure) {
        steps[name] = closure
    }
        
}
