package models

class Param(val id: Long, val name: String, val value: String) {

  override def hashCode = 17 * name.hashCode

  override def equals(obj: Any) = obj match {
    case param: Param => param.name == name
    case _            => false
  }

  override def toString = name

}