package models

class UserStatus(val id: Int, val status: String) {

  override def hashCode = 17 * status.hashCode

  override def equals(obj: Any) = obj match {
    case s: UserStatus => s.status == status
    case _             => false
  }

  override def toString = status

}

object UserStatus {

  val OFFLINE = "offline"

  val ONLINE = "online"

  def apply(id: Int, status: String): UserStatus =
    new UserStatus(id, status)

}

object UserStatusOnline {

  def apply(): UserStatus =
    new UserStatus(2, UserStatus.ONLINE)

}