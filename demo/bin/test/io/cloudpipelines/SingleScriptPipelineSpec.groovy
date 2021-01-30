package io.cloudpipelines

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.MemoryJobManagement
import javaposse.jobdsl.dsl.ScriptRequest
import spock.lang.Specification

/**
 * Tests that all dsl scripts in the jobs directory will compile.
 */
class SingleScriptPipelineSpec extends Specification {


	def 'should create seed jobs'() {
		given:
		MemoryJobManagement jm = new MemoryJobManagement()
		DslScriptLoader loader = new DslScriptLoader(jm)

		when:
		GeneratedItems scripts = loader.runScripts([new ScriptRequest(
				new File("seed/jenkins_pipeline.groovy").text)])

		then:
		noExceptionThrown()

		and:
		scripts.jobs.collect { it.jobName }.contains("jenkins")

		and:
		scripts.jobs.collect { it.jobName }.contains("jenkins-pipeline-seed")

		and:
		scripts.jobs.collect { it.jobName }.contains("meta-seed")
		
	}

}

