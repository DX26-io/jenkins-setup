package io.cloudpipelines.spinnaker.pipeline.steps

import groovy.transform.CompileStatic
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.helpers.ScmContext
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext
import javaposse.jobdsl.dsl.helpers.step.StepContext
import javaposse.jobdsl.dsl.helpers.wrapper.WrapperContext
import javaposse.jobdsl.dsl.jobs.FreeStyleJob

import io.cloudpipelines.common.BashFunctions
import io.cloudpipelines.common.Coordinates
import io.cloudpipelines.common.EnvironmentVariables
import io.cloudpipelines.common.PipelineDefaults
import io.cloudpipelines.common.PipelineDescriptor
import io.cloudpipelines.common.StepEnabledChecker
import io.cloudpipelines.steps.CommonSteps
import io.cloudpipelines.steps.CreatedJob
import io.cloudpipelines.steps.Step

/**
 * Sets up services for stage
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@CompileStatic
class TestPrepare implements Step<FreeStyleJob> {
	private final DslFactory dsl
	private final io.cloudpipelines.common.PipelineDefaults pipelineDefaults
	private final BashFunctions bashFunctions
	private final CommonSteps commonSteps

	TestPrepare(DslFactory dsl, io.cloudpipelines.common.PipelineDefaults pipelineDefaults) {
		this.dsl = dsl
		this.pipelineDefaults = pipelineDefaults
		this.bashFunctions = pipelineDefaults.bashFunctions()
		this.commonSteps = new CommonSteps(this.pipelineDefaults, this.bashFunctions)
	}

	@Override
	CreatedJob step(String projectName, Coordinates coordinates, PipelineDescriptor descriptor) {
		StepEnabledChecker checker = new StepEnabledChecker(descriptor, pipelineDefaults)
		if (checker.testStepMissing()) {
			return null
		}
		String gitRepoName = coordinates.gitRepoName
		String fullGitRepo = coordinates.fullGitRepo
		Job job = dsl.job("${projectName}-test-prepare") {
			environmentVariables(pipelineDefaults.defaultEnvVars as Map<Object, Object>)
			parameters {
				stringParam(EnvironmentVariables.PIPELINE_VERSION_ENV_VAR, "", "Version of the project to run the tests against")
			}
			wrappers {
				commonSteps.defaultWrappers(delegate as WrapperContext)
				credentialsBinding {
					if (!pipelineDefaults.gitUseSshKey()) usernamePassword(EnvironmentVariables.GIT_USERNAME_ENV_VAR,
						EnvironmentVariables.GIT_PASSWORD_ENV_VAR,
						pipelineDefaults.gitCredentials())
					if (!pipelineDefaults.gitUseSshKey()) usernamePassword(EnvironmentVariables.GIT_USERNAME_ENV_VAR,
						EnvironmentVariables.GIT_PASSWORD_ENV_VAR,
						pipelineDefaults.gitCredentials())
					// remove::start[CF]
					if (pipelineDefaults.cfTestCredentialId()) usernamePassword(
						EnvironmentVariables.PAAS_TEST_USERNAME_ENV_VAR,
						EnvironmentVariables.PAAS_TEST_PASSWORD_ENV_VAR,
						pipelineDefaults.cfTestCredentialId())
					// remove::end[CF]
					// remove::start[K8S]
					if (pipelineDefaults.k8sTestTokenCredentialId()) string(EnvironmentVariables.TOKEN_ENV_VAR,
						pipelineDefaults.k8sTestTokenCredentialId())
					// remove::end[K8S]
				}
			}
			scm {
				commonSteps.configureScm(delegate as ScmContext, fullGitRepo,
					"dev/${gitRepoName}/\${${EnvironmentVariables.PIPELINE_VERSION_ENV_VAR}}")
			}
			steps {
				commonSteps.downloadTools(delegate as StepContext, fullGitRepo)
				shell(commonSteps.readScript("test_prepare.sh"))
			}
			publishers {
				commonSteps.defaultPublishers(delegate as PublisherContext)
				commonSteps.deployPublishers(delegate as PublisherContext)
			}
		}
		customize(job)
		return new CreatedJob(job, false)
	}

	@Override
	void customize(FreeStyleJob step) {
		commonSteps.customizers().each {
			it.customizeAll(step)
			it.customizeStage(step)
		}
	}
}
