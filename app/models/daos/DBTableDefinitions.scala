package models.daos

import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape.proveShapeOf

trait DBTableDefinitions {

  protected val driver: JdbcProfile
  import driver.api._

  class Roles(tag: Tag) extends Table[DBRole](tag, "roles") {
    def userId = column[Long]("user_id")
    def role = column[Int]("role")
    def * = (userId, role) <> (DBRole.tupled, DBRole.unapply)
  }

  val roles = TableQuery[Roles]

  class ScheduledTasks(tag: Tag) extends Table[DBScheduledTask](tag, "scheduled_tasks") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def executed = column[Option[Long]]("executed")
    def * = (
      id,
      executed) <> (DBScheduledTask.tupled, DBScheduledTask.unapply)
  }

  val scheduledTasks = TableQuery[ScheduledTasks]

  class Sessions(tag: Tag) extends Table[DBSession](tag, "sessions") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("user_id")
    def ip = column[String]("ip")
    def sessionKey = column[String]("session_key")
    def created = column[Long]("created")
    def expire = column[Long]("expire")
    def * = (id, userId, ip, sessionKey, created, expire) <> (DBSession.tupled, DBSession.unapply)
  }

  val sessions = TableQuery[Sessions]

  class Accounts(tag: Tag) extends Table[DBAccount](tag, "accounts") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def login = column[String]("login")
    def email = column[String]("email")
    def hash = column[Option[String]]("hash")
    def userStatus = column[Int]("user_status")
    def accountStatus = column[Int]("account_status")
    def registered = column[Long]("registered")
    def confirmCode = column[Option[String]]("confirm_code")
    def postsCount = column[Long]("posts_count")
    def * = (
      id,
      login,
      email,
      hash,
      userStatus,
      accountStatus,
      registered,
      confirmCode) <> (DBAccount.tupled, DBAccount.unapply)

  }

  val accounts = TableQuery[Accounts]

}

