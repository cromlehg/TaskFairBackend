package models.daos

case class DBRole(
  userId: Long,
  role:   Int)

case class DBScheduledTask(
  val id:       Long,
  val executed: Option[Long])

case class DBSession(
  val id:         Long,
  val userId:     Long,
  val ip:         String,
  val sessionKey: String,
  val created:    Long,
  val expire:     Long)

case class DBAccount(
  val id:            Long,
  val login:         String,
  val email:         String,
  val hash:          Option[String],
  val userStatus:    Int,
  val accountStatus: Int,
  val registered:    Long,
  val confirmCode:   Option[String])

