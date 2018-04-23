package models.daos

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Random

import org.mindrot.jbcrypt.BCrypt

import controllers.AppConstants
import javax.inject.Inject
import models.AccountStatus
import models.Roles
import models.Account
import models.UserStatus
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import controllers.AppContext
import sun.net.ftp.FtpClient.TransferType
import models.AccountLevel

/**
 *
 * Queries with SlickBUG should be replace leftJoin with for comprehesive. Bug:
 * "Unreachable reference to after resolving monadic joins"
 *
 */

// inject this
// conf: play.api.Configuration,
// and then get conf value
// conf.underlying.getString(Utils.meidaPath)
class DAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends DBTableDefinitions with HasDatabaseConfigProvider[slick.jdbc.JdbcProfile] with TraitDTOToModel {

  import profile.api._

  val pageSize = 17

  val maxLikesView = 10

  def getAccountsPage(pageId: Long): Future[Seq[models.Account]] =
    db.run(accounts.sortBy(_.id.desc).drop(if (pageId > 0) pageSize * (pageId - 1) else 0).take(pageSize).result) map (_ map accountFrom)

  def getAccountsPages(): Future[Int] =
    db.run(accounts.size.result) map (_ / pageSize)

  def findAccountById(id: Long) =
    getAccountFromQuery(accounts.filter(_.id === id))

  def findAccountByEmail(email: String): Future[Option[Account]] =
    getAccountFromQuery(accounts.filter(_.email === email))

  def findAccountByLoginOrEmail(loginOrElamil: String): Future[Option[Account]] =
    getAccountFromQuery(accounts.filter(u => u.login === loginOrElamil || u.email === loginOrElamil))

  def findAccountIdByLoginOrEmail(loginOrElamil: String): Future[Option[Long]] =
    getAccountFromQuery(accounts.filter(u => u.login === loginOrElamil || u.email === loginOrElamil)).map(_.map(_.id))

  def findAccountByLogin(login: String): Future[Option[Account]] =
    getAccountFromQuery(accounts.filter(_.login === login))

  def isLoginExists(login: String): Future[Boolean] =
    db.run(accounts.filter(t => t.login === login.trim.toLowerCase || t.email === login).exists.result)

  def isEmailExists(email: String): Future[Boolean] =
    db.run(accounts.filter(_.email === email.trim.toLowerCase).exists.result)

  def findSessionByAccountIdSessionKeyAndIP(userId: Long, ip: String, sessionKey: String): Future[Option[models.Session]] =
    getSessionFromQuery(sessions.filter(s => s.userId === userId && s.ip === ip && s.sessionKey === sessionKey))

  def findAccountBySUIDAndSessionId(sessionId: Long, sessionKey: String): Future[Option[Account]] = {
    val query = for {
      dbSession <- sessions.filter(t => t.id === sessionId && t.sessionKey === sessionKey)
      dbAccount <- accounts.filter(_.id === dbSession.userId)
    } yield (dbAccount, dbSession)
    db.run(query.result.headOption).map(_.map {
      case (dbAccount, dbSession) =>
        val user = accountFrom(dbAccount)
        user.sessionOpt = Some(sessionFrom(dbSession))
        user
    })
  }

  def getAccountFromQuery(query: Query[(Accounts), (DBAccount), Seq]): Future[Option[models.Account]] =
    db.run(query.result.headOption).map(_.map(accountFrom))

  def getSessionFromQuery(query: Query[(Sessions), (DBSession), Seq]): Future[Option[models.Session]] =
    db.run(query.result.headOption).map(_.map(sessionFrom))

  def findAccountWithRolesById(userId: Long): Future[Option[Account]] =
    updateAccountWithRoles(findAccountById(userId))

  def updateAccountWithRoles(futureOptAccount: Future[Option[Account]]): Future[Option[Account]] =
    futureOptAccount flatMap {
      case Some(u) =>
        findRolesByAccountId(u.id).map { r =>
          Some {
            u.roles = r
            u
          }
        }
      case None => Future(None)
    }

  def findRolesByAccountId(userId: Long) =
    db.run(roles.filter(_.userId === userId).result).map(_.map(_.role))

  def invalidateSessionBySessionKeyAndIP(sessionKey: String, ip: String): Future[Boolean] =
    db.run(sessions.filter(t => t.sessionKey === sessionKey && t.ip === ip).map(_.expire).update(System.currentTimeMillis).transactionally) map (r => if (r == 1) true else false)

  def createSession(
    userId:     Long,
    ip:         String,
    sessionKey: String,
    created:    Long,
    expire:     Long): Future[Option[models.Session]] = {
    val query = for {
      dbSession <- (sessions returning sessions.map(_.id) into ((v, id) => v.copy(id = id))) += new models.daos.DBSession(
        0,
        userId,
        ip,
        sessionKey,
        created,
        expire)
    } yield dbSession
    db.run(query.transactionally) map { dbSession =>
      Some(sessionFrom(dbSession))
    }
  }

  def getAccountsPages(pageSize: Long) =
    db.run(accounts.length.result).map { r =>
      if (r > 0) r / pageSize + 1 else 0
    }

  def findAccountByConfirmCodeAndLogin(login: String, code: String): Future[Option[models.Account]] =
    getAccountFromQuery(accounts.filter(t => t.login === login && t.confirmCode === code))

  def emailVerified(login: String, code: String, password: String): Future[Option[Account]] =
    db.run(accounts.filter(t => t.login === login && t.confirmCode === code)
      .map(t => (t.confirmCode, t.accountStatus, t.hash))
      .update(None, AccountStatus.CONFIRMED, Some(BCrypt.hashpw(password, BCrypt.gensalt())))).flatMap { raws =>
      if (raws == 1) findAccountByLogin(login) else Future.successful(None)
    }

  def findAccountBySessionKeyAndIP(sessionKey: String, ip: String): Future[Option[models.Account]] = {
    val query = for {
      dbSession <- sessions.filter(t => t.sessionKey === sessionKey && t.ip === ip)
      dbAccount <- accounts.filter(_.id === dbSession.userId)
    } yield (dbAccount, dbSession)
    db.run(query.result.headOption).map(_.map {
      case (dbAccount, dbSession) =>
        val user = accountFrom(dbAccount)
        user.sessionOpt = Some(sessionFrom(dbSession))
        user
    })
  }

  def createAccount(
    login: String,
    email: String): Future[Option[models.Account]] = {
    val timestamp = System.currentTimeMillis()
    val query = for {
      dbAccount <- (accounts returning accounts.map(_.id) into ((v, id) => v.copy(id = id))) += new models.daos.DBAccount(
        0,
        login,
        email,
        None /*Some(BCrypt.hashpw(password, BCrypt.gensalt()))*/ ,
        UserStatus.NORMAL,
        AccountStatus.WAITE_CONFIRMATION,
        System.currentTimeMillis,
        Some(BCrypt.hashpw(Random.nextString(5) + login + System.currentTimeMillis.toString, BCrypt.gensalt()).replaceAll("\\.", "s")))
    } yield (dbAccount)
    db.run(query.transactionally) flatMap {
      case dbAccount =>
        addRolesToAccount(dbAccount.id, Roles.CLIENT) map (t => Some(accountFrom(dbAccount, models.Roles.CLIENT)))
    }
  }

  def addRolesToAccount(userId: Long, rolesIn: Int*): Future[Unit] =
    db.run(DBIO.seq(roles ++= rolesIn.map(r => DBRole(userId, r))).transactionally)

  ////////////// HELPERS ////////////////

  @inline final def someToSomeFlatMap[T1, T2](f1: Future[Option[T1]], f2: T1 => Future[Option[T2]]): Future[Option[T2]] =
    f1 flatMap (_ match {
      case Some(r) => f2(r)
      case None    => Future.successful(None)
    })

  @inline final def someToSomeFlatMapElse[T](f1: Future[Option[_]], f2: Future[Option[T]]): Future[Option[T]] =
    f1 flatMap (_ match {
      case Some(r) => Future.successful(None)
      case None    => f2
    })

  @inline final def someToBooleanFlatMap[T](f1: Future[Option[T]], f2: T => Future[Boolean]): Future[Boolean] =
    f1 flatMap (_ match {
      case Some(r) => f2(r)
      case None    => Future.successful(false)
    })

  @inline final def someToSeqFlatMap[T1, T2](f1: Future[Option[T1]], f2: T1 => Future[Seq[T2]]): Future[Seq[T2]] =
    f1 flatMap (_ match {
      case Some(r) => f2(r)
      case None    => Future.successful(Seq.empty[T2])
    })

  @inline final def seqToSeqFlatMap[T1, T2](f1: Future[Seq[T1]], f2: T1 => Future[T2]): Future[Seq[T2]] =
    f1 flatMap { rs =>
      Future.sequence {
        rs map { r =>
          f2(r)
        }
      }
    }

}


