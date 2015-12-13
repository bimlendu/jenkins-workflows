def sonarqube_analysis(add_opts, sonar_branch, build_file, appVersion) {
  def sonar_jdbc_uri = 'jdbc:mysql://sonarqube.db:3306/sonar?autoReconnect=true&useUnicode=true&characterEncoding=utf8'
  def sonar_web_url = 'https://sonarqube.company.com/'
  def sonar_jdbc_credentialsID = '**************************'
  def opts
  if(add_opts == '') {
   opts = ' '
 } else {
   opts = add_opts + appVersion
 }
  new build().ensureMaven()
  new build().ensureJDK8()
  echo '[WORKFLOW] :: Starting Sonar analysis.'
  withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: sonar_jdbc_credentialsID, usernameVariable: 'sonar_jdbc_user', passwordVariable: 'sonar_jdbc_password']]) {
  sh 'mvn -U -e -B -f ' + build_file + ' ' + opts + ' sonar:sonar ' + sonar_branch + ' -Dsonar.jdbc.username=$sonar_jdbc_user -Dsonar.jdbc.password=$sonar_jdbc_password -Dsonar.jdbc.url="' + sonar_jdbc_uri + '" -Dsonar.host.url="' + sonar_web_url + '"'
  echo '[WORKFLOW] :: SONAR ANALYSIS COMPLETE.'
  }
}
