import javaposse.jobdsl.dsl.DslFactory

/**
 *  This script contains logic that
 *
 *  - for each project from the REPOS env variable generates a Jenkinsfile deployment pipeline
 */


DslFactory dsl = this

// Git credentials to use
String gitCredentials = binding.variables["GIT_CREDENTIAL_ID"] ?: "git"
String gitSshCredentials = binding.variables["GIT_SSH_CREDENTIAL_ID"] ?: "git-ssh"
Boolean gitUseSshKey= Boolean.parseBoolean(binding.variables["GIT_USE_SSH_KEY"] ?: "false")
// we're parsing the REPOS parameter to retrieve list of repos to build
String repos = binding.variables["REPOS"] ?: [
		"https://github.com/viz-centric/flair-commons"
	].join(",")
List<String> parsedRepos = repos.split(",")
String jenkinsfileDir = binding.variables["JENKINSFILE_DIR"] ?: "${WORKSPACE}/declarative-pipeline/src/main/resources"

Map<String, Object> envs = [:]
envs['PIPELINE_VERSION_FORMAT'] = binding.variables["PIPELINE_VERSION_FORMAT"] ?: '''${BUILD_DATE_FORMATTED, \"yyMMdd_HHmmss\"}-VERSION'''
envs['PIPELINE_VERSION_PREFIX'] = binding.variables["PIPELINE_VERSION_PREFIX"] ?: '''1.0.0.M1'''
envs['PIPELINE_VERSION'] = binding.variables["PIPELINE_VERSION"] ?: ""
envs['GIT_CREDENTIAL_ID'] = gitCredentials
envs['GIT_SSH_CREDENTIAL_ID'] = gitSshCredentials
envs['GIT_USE_SSH_KEY'] = gitUseSshKey
envs['JDK_VERSION'] = binding.variables["JDK_VERSION"] ?: "jdk11"
envs['GIT_EMAIL'] = binding.variables["GIT_EMAIL"] ?: "admin@vizcentric.com"
envs['GIT_NAME'] = binding.variables["GIT_NAME"] ?: "admin-vizcentric"
// envs["PAAS_TYPE"] = binding.variables["PAAS_TYPE"] ?: "cf"
envs['SCRIPTS_URL'] = binding.variables["SCRIPTS_URL"] ?: 'https://github.com/CloudPipelines/scripts'
envs["SCRIPTS_BRANCH"] = binding.variables["SCRIPTS_BRANCH"] ?: "master"
envs["M2_SETTINGS_REPO_ID"] = binding.variables["M2_SETTINGS_REPO_ID"] ?: "dev-azure-com-vizcentric-flair-commons"
envs["REPO_WITH_BINARIES"] = binding.variables["REPO_WITH_BINARIES"] ?: "https://pkgs.dev.azure.com/VizCentric/_packaging/flair-commons/maven/v1"
envs["REPO_WITH_BINARIES_FOR_UPLOAD"] = binding.variables["REPO_WITH_BINARIES_FOR_UPLOAD"] ?: "https://pkgs.dev.azure.com/VizCentric/_packaging/flair-commons/maven/v1"
envs['REPO_WITH_BINARIES_CREDENTIAL_ID'] = binding.variables['REPO_WITH_BINARIES_CREDENTIAL_ID'] ?: 'dev-azure-com-vizcentric-flair-commons'
envs["API_COMPATIBILITY_STEP_REQUIRED"] = binding.variables["API_COMPATIBILITY_STEP_REQUIRED"] ?: true

parsedRepos.each {
	String gitRepoName = it.split('/').last() - '.git'
	String fullGitRepo = it
	String branchName = "master"
	int customNameIndex = it.indexOf('$')
	int customBranchIndex = it.indexOf('#')
	if (customNameIndex == -1 && customBranchIndex == -1) {
		// url
		fullGitRepo = it
		branchName = "master"
	} else if (customNameIndex > -1 && (customNameIndex < customBranchIndex || customBranchIndex == -1)) {
		fullGitRepo = it.substring(0, customNameIndex)
		if (customNameIndex < customBranchIndex) {
			// url$newName#someBranch
			gitRepoName = it.substring(customNameIndex + 1, customBranchIndex)
			branchName = it.substring(customBranchIndex + 1)
		} else if (customBranchIndex == -1) {
			// url$newName
			gitRepoName = it.substring(customNameIndex + 1)
		}
	} else if (customBranchIndex > -1) {
		fullGitRepo = it.substring(0, customBranchIndex)
		if (customBranchIndex < customNameIndex) {
			// url#someBranch$newName
			gitRepoName = it.substring(customNameIndex + 1)
			branchName = it.substring(customBranchIndex + 1, customNameIndex)
		} else if (customNameIndex == -1) {
			// url#someBranch
			gitRepoName = it.substring(it.lastIndexOf("/") + 1, customBranchIndex)
			branchName = it.substring(customBranchIndex + 1)
		}
	}
	String projectName = "${gitRepoName}-declarative-pipeline"
	
	envs['GIT_REPOSITORY'] = fullGitRepo
	envs['GIT_BRANCH_NAME'] = branchName

	println "For project [${projectName}] setting repo [${fullGitRepo}] and branch [${branchName}]"

	dsl.pipelineJob(projectName) {
		environmentVariables(envs)
		definition {
			cps {
				script("""${dsl.readFileFromWorkspace(jenkinsfileDir + '/Jenkinsfile-flair')}""")
			}
		}
	}
}
