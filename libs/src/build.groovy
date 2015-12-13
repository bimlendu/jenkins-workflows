// Ensure maven and java is in path.
def ensureMaven() {
  env.PATH = "${tool 'M3'}/bin:${env.PATH}"
}

def ensureJDK8() {
  env.PATH = "${tool 'jdk8'}/bin:${env.PATH}"
}

def ensureGradle() {
  env.PATH = "${tool 'gradle_26'}/bin:${env.PATH}"
}

def svncheckout(scmUrl) {
  def scmcredentialsId = '***********************'
  checkout poll: false, scm: [$class: 'SubversionSCM', locations: [[credentialsId:scmcredentialsId, depthOption: 'infinity', ignoreExternalsOption: true, local: '.', remote: scmUrl]], workspaceUpdater: [$class: 'UpdateUpdater'], extensions: [$class: 'CleanCheckout']]
}
