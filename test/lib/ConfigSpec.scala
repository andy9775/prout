package lib

import com.netaporter.uri.Uri
import lib.Config.{CheckpointDetails, CheckpointMessages}
import lib.labels.{Overdue, Seen}
import org.joda.time.Period.minutes
import org.scalatest.{Inside, OptionValues}
import org.scalatestplus.play._
import play.api.libs.json._

import scalax.io.JavaConverters._

class ConfigSpec extends PlaySpec with OptionValues with Inside {

   "Config json parsing" must {
     "parse normal Checkpoint config" in {
       val details = checkpointDetailsFrom("/sample.checkpoint.json")

       details mustEqual JsSuccess(CheckpointDetails(Uri.parse("https://membership.theguardian.com/"), minutes(14)))
       details.get.sslVerification mustBe true
      }

     "parse Checkpoint config with custom messages" in {
       val details = checkpointDetailsFrom("/sample.messages.checkpoint.json")

       details mustEqual JsSuccess(
         CheckpointDetails(
           url = Uri.parse("https://www.theguardian.com"),
           overdue = minutes(20),
           messages = Some(CheckpointMessages(Seen -> "prout/seen.md", Overdue -> "prout/overdue.md"))
         )
       )
     }

     "parse Checkpoint config with one custom message" in {
       val details = checkpointDetailsFrom("/sample.one.message.checkpoint.json")

       details mustEqual JsSuccess(
         CheckpointDetails(
           url = Uri.parse("https://www.theguardian.com"),
           overdue = minutes(15),
           messages = Some(CheckpointMessages(Seen -> "prout/seen.md"))
         )
       )
     }

     "parse insecure config" in {
       checkpointDetailsFrom("/sample.insecure.checkpoint.json").get.sslVerification mustBe false
     }

     "parse afterSeen (post-deploy) config" in {
       inside (checkpointDetailsFrom("/sample.travis.checkpoint.json")) {
         case JsSuccess(checkpoint, _) =>
           val afterSeen = checkpoint.afterSeen.value

           val travis = afterSeen.travis.value

           inside (travis.config \ "script") { case JsDefined(script) =>
             script mustEqual JsString("sbt ++$TRAVIS_SCALA_VERSION acceptance-test")
           }
       }
     }
   }

  def checkpointDetailsFrom(resourcePath: String): JsResult[CheckpointDetails] = {
    Json.parse(getClass.getResource(resourcePath).asInput.byteArray).validate[CheckpointDetails]
  }
}