// Gets nodes associated with the current environment and role.
def getnodes(String env, String role, String app_context) {

  sh 'knife search node \'chef_environment:' + env + ' AND role:' + role + '\' -i | grep -v \"items found\" | sed \'/^\\s*$/d\' > ' + role + '.' + env + '.nodes'

  echo '[WORKFLOW] :: Deployed on these instances.'

  def nodes = readFile("${role}.${env}.nodes").split("\r?\n")
  for (def node: nodes) {
    echo 'http://' + node + ':8080/' + app_context
  }
}

// Submits version changes to the chef environment, in other words, deploy.
def deploy(String env, String role,String version,String submitter, String app_context, String service_name) {
  input message: 'Deploy to ' + env, submitter: submitter

  echo '[WORKFLOW] :: Deploying to ' + env + ' environment.'

  sh 'knife environment show -Fj ' + env + ' > ' + env + '.json'
  sh 'jq \'.override_attributes.versions.' + service_name + '=\"' + version + '\"\' ' + env + '.json > tmp.' + env + '.json && mv -f tmp.' + env + '.json ' + env + '.json'
  sh 'knife environment from file ' + env + '.json'

  echo '[WORKFLOW] :: Updated chef environment ' + env  + '.'
  echo '[WORKFLOW] :: Version ' + version + ' will be deployed in environment ' + env + ' after the next chef-client run.'

  getnodes(env, role, app_context)

  echo '[WORKFLOW] :: Deploy to environment ' + env + ' completed.'
  echo '[WORKFLOW] :: Waiting for ' + env + ' signoff.'

  input message: 'Proceed to next stage?', submitter: submitter
}

//using aws code deploy to deploy.
def codedeploy(String env, String submitter, String version, String service_name, String application_name, String deployment_config_name, String deployment_group_name, String s3_bucket, String bundle_type, String key, String region) {
input message: 'Deploy to ' + env, submitter: submitter
echo '[WORKFLOW] :: Deploying to ' + env + ' environment.'

sh 'knife environment show -Fj ' + env + ' > ' + env + '.json'
sh 'jq \'.override_attributes.versions.' + service_name + '=\"' + version + '\"\' ' + env + '.json > tmp.' + env + '.json && mv -f tmp.' + env + '.json ' + env + '.json'
sh 'knife environment from file ' + env + '.json'
echo '[WORKFLOW] :: Updated chef environment ' + env  + '.'

sh 'aws deploy create-deployment  --application-name ' + application_name + ' --deployment-config-name ' + deployment_config_name + ' --deployment-group-name ' + deployment_group_name + ' --s3-location bucket=' + s3_bucket + ',bundleType=' + bundle_type + ',key=' + key + ' --region ' + region  

echo '[WORKFLOW] :: Deploy to environment ' + env + ' completed.'
echo '[WORKFLOW] :: Waiting for ' + env + ' signoff.'
input message: 'Proceed to next stage?', submitter: submitter
}

