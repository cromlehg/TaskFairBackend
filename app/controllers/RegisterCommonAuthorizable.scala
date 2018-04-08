package controllers

import java.io.IOException

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.sendgrid.Content
import com.sendgrid.Email
import com.sendgrid.Mail
import com.sendgrid.Method
import com.sendgrid.SendGrid
import com.typesafe.config.Config

import javax.inject.Inject
import javax.inject.Singleton
import models.daos.DAO
import play.Logger
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import java.util.regex.Pattern

class RegisterCommonAuthorizable @Inject() (cc: ControllerComponents, dao: DAO, config: Config)(implicit ec: ExecutionContext)
  extends Authorizable(cc, dao, config) {

  protected def createAccount(
    emailPatternName: String,
    accountType:      Int,
    login:            String,
    email:            String,
    companyNameOpt:   Option[String],
    ITNOpt:           Option[String],
    IECOpt:           Option[String])(
    f: models.Account => Result): Future[Result] = {
    dao.createAccount(
      login,
      email,
      AppConstants.INITIAL_DP,
      accountType,
      companyNameOpt,
      ITNOpt,
      IECOpt) map (_.fold(BadRequest("Can't create account")) { account =>
        val from = new Email(config getString "sendgrid.from")
        val subject = config getString "sendgrid.subject"
        val to = new Email(account.email)
        val content = new Content(
          "text/plain",
          (config getString emailPatternName)
            .replace("%account.login%", account.login)
            .replace("%account.confirmCode%", account.confirmCode.get))
        val mail = new Mail(from, subject, to, content)
        val sg = new SendGrid(config.getString("sendgrid.apikey"));
        val request = new com.sendgrid.Request()
        try {
          request.setMethod(Method.POST)
          request.setEndpoint("mail/send")
          request.setBody(mail.build())
          val response = sg.api(request)
          if (response.getStatusCode() == 202)
            f(account)
          else {
            Logger.error("can't send email")
            Logger.error("status code: " + response.getStatusCode())
            Logger.error("body: " + response.getBody())
            Logger.error("headers: " + response.getHeaders())
            BadRequest("Some problems")
          }
        } catch {
          case e: IOException =>
            Logger.error(e.toString())
            BadRequest("Some problems")
        }
      })
  }

}

