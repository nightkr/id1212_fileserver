package se.nullable.kth.id1212.fileserver.server.model

import scala.concurrent.ExecutionContext

import FSProfile.api._

class UserManager {
  def find(username: String): DBIO[Option[User]] =
    Users
      .filter(_.username === username)
      .result
      .headOption

  def create(username: String, password: String)(
      implicit ec: ExecutionContext): DBIO[Unit] =
    (Users += User(0, username, hashPassword(password))).map(_ => ())

  def verifyPassword(user: User, password: String): Boolean =
    comparePassword(user.password, password)

  // FIXME replace with actual pwd hash
  private def comparePassword(hash: String, password: String): Boolean =
    hash == password
  private def hashPassword(password: String): String = password
}
