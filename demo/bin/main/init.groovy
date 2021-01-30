import hudson.plugins.groovy.Groovy
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration
import javaposse.jobdsl.plugin.JenkinsJobManagement
import jenkins.model.GlobalConfiguration
import jenkins.model.Jenkins
import net.sf.json.JSONObject

println "--> disabling scripts security for job dsl scripts"
GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).useScriptSecurity = false

def jobScript = new File('/usr/share/jenkins/jenkins_pipeline.groovy')
def jobManagement = new JenkinsJobManagement(System.out, [:], new File('.'))

println "Marking allow macro token"
Groovy.DescriptorImpl descriptor =
		(Groovy.DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(Groovy)
descriptor.configure(null, JSONObject.fromObject('''{"allowMacro":"true"}'''))

println "Creating the seed job"
new DslScriptLoader(jobManagement).with {
	runScript(jobScript.text)
}

println "Creating the settings.xml file"
final String m2Home = jenkinsHome + '/.m2'
final String settingsPath = '/usr/share/jenkins/settings.xml'
File m2HomeFile = new File(m2Home)
m2HomeFile.mkdirs()
File mavenSettings = new File("${m2Home}/settings.xml")
if (m2HomeFile.exists()) {
	boolean settingsCreated = mavenSettings.createNewFile()
	if (settingsCreated) {
		mavenSettings.text = new File(settingsPath).text
	} else if (mavenSettings.exists()) {
		println "Overridden existing maven settings"
		mavenSettings.text = new File(settingsPath).text
	} else {
		println "Failed to create settings.xml!"
	}
} else {
	println "Failed to create .m2 folder!"
}