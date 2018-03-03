package models

import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.joda.time.DateTimeZone

class Timezone(val id: Int, val timezone: String) {

  override def hashCode = 17 * timezone.hashCode

  override def equals(obj: Any) = obj match {
    case s: Timezone => s.timezone == timezone
    case _           => false
  }

  override def toString = timezone

}

object TimezoneMoscow {

  def apply(): Timezone =
    new models.Timezone(1, "Europe/Moscow")

}
