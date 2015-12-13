def sonaranalysis(){
	def sonar_jdbc_uri = 'jdbc:mysql://sonarqube.db:3306/sonar?autoReconnect=true&useUnicode=true&characterEncoding=utf8'
	def sonar_web_url = 'https://sonarqube.company.com/'
    def sonar_jdbc_credentialsID = '********************'
    echo '[WORKFLOW] :: Starting Sonar analysis.'
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: sonar_jdbc_credentialsID, usernameVariable: 'sonar_jdbc_user', passwordVariable: 'sonar_jdbc_password']]) {
    	sh 'gradle sonarRunner -Dsonar.host.url=' + sonar_web_url + ' -Dsonar.jdbc.password=' + sonar_jdbc_password + ' -Dsonar.verbose=true -Dsonar.jdbc.username=' + sonar_jdbc_user + ' -Dsonar.jdbc.url=' + sonar_jdbc_uri + ' -Dsonar.jdbc.driverClassName=' + 'com.mysql.jdbc.Driver'
    }
}

// set jenkins build number variable if run on jenkins or set it to 'dev'

def version() {
  def buildNumber = env.BUILD_NUMBER  ?: "dev"
  def version = "1.0.${buildNumber}"
  return version
}


