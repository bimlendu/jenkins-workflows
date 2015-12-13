import java.net.URL  // Needed for Slack notification

def slack(webhook_url, String channel, String text,String color,String attachment_fields_title,String attachment_fields_value) {
  try {
  def username = 'builds-jenkins'
  def payload = "payload={\"channel\": \"${channel}\", \"username\": \"${username}\", \"text\": \"${text}\", \"attachments\":[ { \"color\": \"${color}\", \"fields\": [ { \"title\": \"${attachment_fields_title}\", \"value\": \"${attachment_fields_value}\", \"short\": false } ] } ] }"
  def url = new URL(webhook_url)
  def connection = url.openConnection()
  connection.setRequestMethod("POST")
  connection.doOutput = true
  def writer = new OutputStreamWriter(connection.outputStream)
  writer.write(payload)
  writer.flush()
  writer.close()
  connection.connect()
  def slackResponse = connection.content
  echo '[WORKFLOW] :: Slack response ' + slackResponse

  }
  catch(Exception e) {
    println 'SLACK returned error'
  }
}

def throwAndNotify(slack_webhook_url, String slack_channel, Exception e, String stage) {
  def console_output = env.BUILD_URL + 'console'
  StringWriter w = new StringWriter()
  PrintWriter p = new PrintWriter(w)
  e.printStackTrace(p)
  p.flush()
  // String stackTrace = w.toString() ## Error Stacktrace
  slack(slack_webhook_url, slack_channel, "Job ${env.JOB_NAME}, STAGE :: " + stage + " , failed.", 'danger', "ERROR : ${e.message}", "Please go to <$console_output>")
  throw e
}
