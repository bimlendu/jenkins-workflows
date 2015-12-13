/**
* @author Bimlendu Mishra
*/


// ---------- VARIABLES DECLARATIONS START. ---------- //

// ---------- PROJECT VARIABLES ---------- //
def app_context = ''     // context path for the application
def scmUrl = ''     // source code
def deployment_repo = ''     // deployment repository name in artifactory
def build_file = 'pom.xml'
def build_opts = 'clean deploy -Dmaven.test.skip=true'
def role = ''     // chef role for the project instances
def slack_channel = '' // slack channel to post updates to
def slack_webhook_url = ''  // slack webhook url
def envs = [['dev','deploy'], ['qe','deploy'], ['uat','codedeploy']]
def deployers = [ 'dev_deployers', 'qe_deployers', 'uat_deployers' ]
def build_settings = '~/.m2/project-settings.xml'
def sonar_branch = '-Dsonar.branch=PROJECT_BRANCH'
def add_opts = ''
def service_name = ''
def application_name = ''
def deployment_config_name = 'CodeDeployDefault.OneAtATime'
def deployment_group_name = 'project-uat'
def s3_bucket = 'codedeploy'
def bundle_type = 'zip'
def key = 'project-uat.zip'
def region = 'us-east-1'
// ---------- SYSTEM VARIABLES ---------- //

def artifactory_uri = ''
def deployment_repo_uri = artifactory_uri + deployment_repo
def deploy_opts = ' -DaltDeploymentRepository="' + deployment_repo_uri + '" -DdeployAtEnd=true -DretryFailedDeploymentCount=10 -DupdateReleaseInfo=true'
def input_url = env.BUILD_URL + 'input/'
// ---------- VARIABLES DECLARATIONS DONE. ---------- //


// Gets version string from parent pom.xml.
def version(String build_file) {
  def matcher = readFile(build_file) =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}


// ---------- STAGES ---------- //

stage 'BUILD'
node { // Compile (and Junit).
  try {
    echo '[WORKFLOW] :: Cleaning up workspace before starting.'
    sh 'rm -rf *'
    
    echo '[WORKFLOW] :: Getting source code from SCM.'
    new build().svncheckout(scmUrl)
    new build().ensureJDK7()
    new build().ensureMaven()
    def appVersion = version(build_file) + '-' +  env.BUILD_NUMBER

    echo '[WORKFLOW] :: Setting updated version in maven.'
    sh 'mvn -s ' + build_settings + ' -B -f ' + build_file + ' versions:set -DnewVersion=' + appVersion
    
    echo '[WORKFLOW] :: Building  version ' + appVersion + ' and deploying to artifactory'
    sh 'mvn -s ' + build_settings + ' -f ' +  build_file +  ' -B ' + build_opts + deploy_opts
    step([$class: 'ArtifactArchiver', artifacts: '**/target/*', fingerprint: true])
  } catch (e) {
    new notification().throwAndNotify(slack_webhook_url, slack_channel, e, 'BUILD')
  }
}
stage name: 'QUALITY AND PERFORMANCE ANALYSIS', concurrency: 1

parallel(qualityAnalysis: {
  node { // Sonar analysis. TODO: jmeter tests config.
    try {
      def appVersion = version(build_file)
      new codeanalysis().sonarqube_analysis(add_opts, sonar_branch, build_file, appVersion)
     } catch (e) {
      new notification().throwAndNotify(slack_webhook_url, slack_channel, e, 'SONAR')
    }
  }
})

stage 'DEPLOY'
node { // Deploy artifacts to various environments.
  try {
    def appVersion = version(build_file)
    echo '[WORKFLOW] :: ' + appVersion
    def slack_subject = 'Job ' + env.JOB_NAME +', ( ' + env.BUILD_NUMBER + ' ) is waiting for input.'
    def slack_description = "Please go to <${input_url}>"

    for ( i = 0; i < envs.size(); i++ ) {
      new notification().slack(slack_webhook_url, slack_channel, slack_subject, 'warning', '@' + deployers[i], slack_description)
      if (envs[i][1] == 'codedeploy'){
        new deploy().codedeploy (envs[i][0], deployers[i], appVersion, service_name, application_name, deployment_config_name, deployment_group_name, s3_bucket, bundle_type, key, region)
        }else if (envs[i][1] == 'deploy'){ 
          new deploy().deploy (envs[i][0], role, appVersion, deployers[i], app_context, service_name)
        }
    }

    // TODO: optional release email
    echo '[WORKFLOW] :: FINISHED.'
    } catch (hudson.AbortException e) {
    println "WORKFLOW was aborted."
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
    println "WORKFLOW was aborted."
    } catch (e) {
    new notification().throwAndNotify(slack_webhook_url, slack_channel, e, 'DEPLOY')
  }
}