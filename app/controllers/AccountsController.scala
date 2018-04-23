package controllers

import scala.concurrent.ExecutionContext

import com.typesafe.config.Config

import javax.inject.Inject
import javax.inject.Singleton
import models.daos.DAO
import play.api.mvc.ControllerComponents
import scala.util.Random

import play.api.data.Forms.email
import play.api.data.Forms.text
import play.api.data.Forms.boolean
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.mvc.Flash

import play.api.data.Form

import java.io.IOException

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.mindrot.jbcrypt.BCrypt

import com.sendgrid.Content
import com.sendgrid.Email
import com.sendgrid.Mail
import com.sendgrid.Method
import com.sendgrid.SendGrid
import com.typesafe.config.Config

import javax.inject.Inject
import javax.inject.Singleton
import models.AccountStatus
import models.ErrCodes
import models.daos.DAO
import play.Logger
import play.api.libs.json.JsArray
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result
import play.twirl.api.Html
import play.api.mvc.Action

@Singleton
class AccountsController @Inject() (cc: ControllerComponents, dao: DAO, config: Config)(implicit ec: ExecutionContext)
  extends RegisterCommonAuthorizable(cc, dao, config) {

  import scala.concurrent.Future.{ successful => future }

  case class ApproveData(
    val login:                String,
    val pwd:                  String,
    val repwd:                String,
    val understandNotRecover: Boolean,
    val haveSecured:          Boolean,
    val code:                 String)

  val loginVerifying = nonEmptyText(3, 20).verifying("Must contain lowercase letters and digits only.", name => name.matches("[a-z0-9]{3,20}"))

  val approveForm = Form(
    mapping(
      "login" -> loginVerifying,
      "pwd" -> nonEmptyText(8, 80),
      "repwd" -> nonEmptyText(8, 80),
      "understandNotRecover" -> boolean.verifying(_ == true),
      "haveSecured" -> boolean.verifying(_ == true),
      "code" -> nonEmptyText)(ApproveData.apply)(ApproveData.unapply))

  case class AuthData(val email: String, val pass: String)

  sealed trait RegData {
    def email: String
    def login: String
  }

  case class RegDataUser(
    override val email: String,
    override val login: String) extends RegData

  val authForm = Form(
    mapping(
      "email" -> nonEmptyText(3, 50),
      "pass" -> nonEmptyText(8, 80))(AuthData.apply)(AuthData.unapply))

  val regFormUser = Form(
    mapping(
      "email" -> email,
      "login" -> loginVerifying)(RegDataUser.apply)(RegDataUser.unapply))

  def login() = Action.async { implicit request =>
    implicit val ac = new AppContext()
    notAuthorized(future(Ok(views.html.app.login(authForm))))
  }

  def processLogin() = Action.async { implicit request =>
    implicit val ac = new AppContext()
    notAuthorized {
      authForm.bindFromRequest.fold(formWithErrors => future(BadRequest(views.html.app.login(formWithErrors))), { authData =>
        authCheckBlock(authData.email, authData.pass) { msg =>
          val formWithErrors = authForm.fill(authData)
          future(BadRequest(views.html.app.login(formWithErrors)(Flash(formWithErrors.data) + ("error" -> msg), implicitly, implicitly)))
        } { case (account, session) => future(Redirect(routes.AppController.index())) }
      })
    }
  }

  def processApproveRegister() = Action.async { implicit request =>
    implicit val ac = new AppContext()
    notAuthorized {
      approveForm.bindFromRequest.fold(
        formWithErrors => future(BadRequest(views.html.app.approveRegister(formWithErrors))), {
          approveData =>
            if (approveData.pwd == approveData.repwd)
              dao.findAccountByConfirmCodeAndLogin(approveData.login, approveData.code) flatMap (_.fold(future(BadRequest("Login or confirm code not found"))) { account =>
                dao.emailVerified(approveData.login, approveData.code, approveData.pwd) map (_.fold(BadRequest("Can't verify email")) { accountVerified =>
                  Ok(views.html.app.registerFinished())
                })
              })
            else {
              val formWithErrors = approveForm.fill(approveData)
              future(Ok(views.html.app.approveRegister(formWithErrors)(Flash(formWithErrors.data) + ("error" -> "Passwords should be equals"), implicitly, implicitly)))
            }
        })
    }
  }

  def approveRegister(login: String, code: String) = Action.async { implicit request =>
    implicit val ac = new AppContext()
    notAuthorized {
      dao.findAccountByConfirmCodeAndLogin(login, code) map (_.fold(BadRequest("Login or confirm code not found")) { account =>
        Ok(views.html.app.approveRegister(approveForm.fill(ApproveData(
          login,
          BCrypt.hashpw(login.toString + code + System.currentTimeMillis() + Random.nextDouble(), BCrypt.gensalt()),
          null,
          false,
          false,
          code))))
      })
    }
  }

  private def baseRegisterChecks[T <: RegData](
    regForm: Form[T])(f1: (String, Form[_]) => Future[Result])(f2: Form[_] => Html)(f3: (T, String, String) => Future[Result])(implicit request: Request[_], ac: AppContext) = {
    regForm.bindFromRequest.fold(
      formWithErrors => future(BadRequest(f2(formWithErrors))), {
        userInRegister =>
          dao.isLoginExists(userInRegister.login) flatMap { isLoginExists =>
            if (isLoginExists)
              f1("Login already exists!", regForm.fill(userInRegister))
            else
              dao.isEmailExists(userInRegister.email) flatMap { isEmailExists =>
                if (isEmailExists)
                  f1("Email already exists!", regForm.fill(userInRegister))
                else
                  f3(userInRegister, userInRegister.login, userInRegister.email)
              }
          }
      })

  }

  def registerProcessUser() = Action.async { implicit request =>
    implicit val ac = new AppContext()
    notAuthorized {

      def redirectWithError(msg: String, form: Form[_]) =
        future(Ok(views.html.app.registerUser(form)(Flash(form.data) + ("error" -> msg), implicitly, implicitly)))

      baseRegisterChecks(regFormUser)(redirectWithError)(t => views.html.app.registerUser(t)) { (target, login, email) =>
        createAccount("sendgrid.letter", login, email) { account =>
          Ok(views.html.app.registerProcess())
        }
      }

    }
  }

  def registerUser() = Action.async { implicit request =>
    implicit val ac = new AppContext()
    notAuthorized {
      future(Ok(views.html.app.registerUser(regFormUser)))
    }
  }

}

