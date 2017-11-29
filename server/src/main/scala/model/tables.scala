package se.nullable.kth.id1212.fileserver.server.model

import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.duration._

import javax.inject.Inject
import slick.jdbc.H2Profile

class FSProfile extends H2Profile
object FSProfile extends FSProfile

import FSProfile.api._

class DBManager @Inject() (db: Database) {
  def setUp(): Unit = {
    Await.result(db.run(Users.schema.create), 2.seconds)
  }
}

case class User(id: Long,
                username: String,
                password: String)

private class Users(tag: Tag) extends Table[User](tag, "USERS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def username = column[String]("USERNAME", O.Unique)
  def password = column[String]("PASSWORD")

  override def * = (id, username, password) <> (User.tupled, User.unapply)
}

private object Users extends TableQuery[Users](new Users(_))
