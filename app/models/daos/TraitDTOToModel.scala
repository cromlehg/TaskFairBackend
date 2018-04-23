package models.daos

trait TraitDTOToModel {

  def sessionFrom(dto: DBSession) =
    new models.Session(
      dto.id,
      dto.userId,
      dto.ip,
      dto.sessionKey,
      dto.created,
      dto.expire)

  def accountFrom(dto: DBAccount): models.Account =
    new models.Account(
      dto.id,
      dto.login,
      dto.email,
      dto.hash,
      dto.userStatus,
      dto.accountStatus,
      dto.registered,
      dto.confirmCode)

  def accountFrom(dto: DBAccount, roles: Int*): models.Account = {
    val user = accountFrom(dto)
    user.roles = roles
    user
  }

}