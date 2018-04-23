package models

class ScheduledTask(
  val id:       Long,
  val executed: Option[Long]) {

}

object ScheduledTasks {

  val REWARDER = 1

  val SCHEDULED_TXS_PROCESSOR = 2

  def idByStr(str: String): Option[Int] =
    str match {
      case "rewarder"                => Some(REWARDER)
      case "scheduled txs processor" => Some(SCHEDULED_TXS_PROCESSOR)
      case _                         => None
    }

  def strById(id: Int): Option[String] =
    id match {
      case 1 => Some("rewarder")
      case 2 => Some("scheduled txs processor")
      case _ => None
    }

}