package models

class AccountStatus(val id: Int, val status: String) {

  override def hashCode = 17 * status.hashCode

  override def equals(obj: Any) = obj match {
    case s: AccountStatus => s.status == status
    case _                => false
  }

  override def toString = status

}

object AccountStatus {

  val ACTIVE = "active"

  val LOCKED = "locked"
  
  val NEEDS_APPROVE = "needs approve"

  def apply(id: Int, status: String): AccountStatus =
    new AccountStatus(id, status)

}

object AccountStatusApprove {

  def apply(): AccountStatus =
    new AccountStatus(3, AccountStatus.NEEDS_APPROVE)

}

object AccountStatusActive {

  def apply(): AccountStatus =
    new AccountStatus(1, AccountStatus.ACTIVE)

}

object AccountStatusLocked {

  def apply(): AccountStatus =
    new AccountStatus(2, AccountStatus.LOCKED)

}



