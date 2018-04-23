package models

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import controllers.AppConstants
import controllers.AppContext
import java.util.Date
import org.ocpsoft.prettytime.PrettyTime

object PrettyTimeHelper {

  val prettyTime = new PrettyTime()

}

class Account(
  val id:            Long,
  val login:         String,
  val email:         String,
  val hash:          Option[String],
  val userStatus:    Int,
  val accountStatus: Int,
  val registered:    Long,
  val confirmCode:   Option[String]) {

  var sessionOpt: Option[Session] = None

  var roles: Seq[Int] = Seq()

  val ldt = new LocalDateTime(registered, DateTimeZone.UTC)

  lazy val createdPrettyTime = PrettyTimeHelper.prettyTime.format(new Date(registered))

  override def equals(obj: Any) = obj match {
    case user: Account => user.email == email
    case _             => false
  }

  override def toString = email

  def getRegistered(zone: String): DateTime = getRegistered.toDateTime(DateTimeZone forID zone)

  def getRegistered: LocalDateTime = ldt

  def toJsonAuth(inJsObj: JsObject)(implicit ac: AppContext): JsObject = {
    var jsObj = inJsObj ++ Json.obj("email" -> email)
    jsObj = confirmCode.fold(jsObj) { t => jsObj ++ Json.obj("confirm_code" -> t) }
    jsObj
  }

  lazy val displayName = login

  def toJson(implicit ac: AppContext): JsObject = {
    var jsObj = Json.obj(
      "id" -> id,
      "login" -> login,
      "user_status" -> UserStatus.strById(userStatus),
      "account_status" -> AccountStatus.strById(accountStatus),
      "registered" -> registered,
      "display_name" -> displayName)

    ac.authorizedOpt.fold(jsObj)(_ => toJsonAuth(jsObj))
  }

}

