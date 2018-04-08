package controllers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.mindrot.jbcrypt.BCrypt

import com.typesafe.config.Config

import javax.inject.Inject
import models.daos.DAO
import play.api.i18n.I18nSupport
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result

class Authorizable @Inject() (cc: ControllerComponents, dao: DAO, config: Config)(implicit ec: ExecutionContext)
  extends AbstractController(cc)
  with I18nSupport {

  import scala.concurrent.Future.{ successful => future }

  val SESSION_KEY = "session_key"

  protected def notAuthorized[T](f: Future[Result])(implicit request: Request[T], ac: AppContext): Future[Result] =
    request.session.get(SESSION_KEY).fold(f)(curSessionKey =>
      future(BadRequest("You should logout before")))

  protected def sessionNotExpired(account: models.Account)(f: models.Account => Future[Result])(implicit ac: AppContext): Future[Result] =
    account.sessionOpt.fold(Future.successful(BadRequest("Empty user session"))) { session =>
      ac.authorizedOpt = Some(account)
      f(account)
    }

  protected def logout[T](f: Result)(implicit request: Request[T], ac: AppContext): Future[Result] =
    request.session.get(SESSION_KEY).fold(future(BadRequest("You shuld authorize before")))(curSessionKey =>
      dao.invalidateSessionBySessionKeyAndIP(curSessionKey, request.remoteAddress) map (t => f.withNewSession))

  // access only if user authorized ???
  protected def onlyAuthorizedOwnerUserOrSelf[T](userId: Option[Long])(f: models.Account => Future[Result])(implicit request: Request[T], ac: AppContext): Future[Result] =
    userId.fold(onlyAuthorized(f))(userId => onlyAuthorizedOwnerUser(userId)(f))

  protected def optionalAuthorized[T](f: Option[models.Account] => Future[Result])(implicit request: Request[T], ac: AppContext): Future[Result] =
    request.session.get(SESSION_KEY).fold(f(None))(curSessionKey =>
      dao.findAccountBySessionKeyAndIPWithBalances(curSessionKey, request.remoteAddress)
        flatMap (_.fold(f(None))(user => user.sessionOpt.fold(f(None)) { session =>
          ac.authorizedOpt = Some(user)
          f(Some(user))
        })))

  protected def onlyAuthorized[T](f: models.Account => Future[Result])(implicit request: Request[T], ac: AppContext): Future[Result] =
    request.session.get(SESSION_KEY).fold(future(BadRequest("Session not found. You shoud authorize before")))(curSessionKey =>
      dao.findAccountBySessionKeyAndIPWithBalances(curSessionKey, request.remoteAddress)
        flatMap (_.fold(future(BadRequest("Can't find session. You should authorize before")))(user =>
          sessionNotExpired(user)(f))))

  protected def onlyAuthorizedOwnerUser[T](userId: Long)(f: models.Account => Future[Result])(implicit request: Request[T], ac: AppContext): Future[Result] =
    request.session.get(SESSION_KEY).fold(future(BadRequest("Session not found. You shoud authorize before")))(curSessionKey =>
      dao.findAccountBySessionKeyAndIPWithBalances(curSessionKey, request.remoteAddress)
        flatMap (_.fold(future(BadRequest("Can't find session. You should authorize before")))(user =>
          sessionNotExpired(user)(if (user.id == userId) f else { account => future(BadRequest("Access forbidden")) } ))))

  protected def sessionKeyStr(userId: Long)(implicit request: Request[_]) =
    BCrypt.hashpw(userId + System.currentTimeMillis + request.remoteAddress, BCrypt.gensalt())

}
