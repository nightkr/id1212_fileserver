package se.nullable.kth.id1212.fileserver.server.model

import java.util.UUID
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

import javax.inject.Inject
import slick.jdbc.H2Profile

class FSProfile extends H2Profile
object FSProfile extends FSProfile

import FSProfile.api._

class DBManager @Inject()(db: Database) {
  private val log = LoggerFactory.getLogger(getClass)
  def setUp(): Unit =
    try {
      Await.result(
        db.run((Users.schema ++ Files.schema ++ Tickets.schema).create),
        2.seconds)
    } catch {
      case _: Exception =>
        log.info("Failed to create schema, assuming it already exists")
    }
}

case class User(id: Long, username: String, password: String)

case class File(id: Long,
                name: String,
                hash: Option[String],
                owner: Long,
                publicRead: Boolean,
                publicWrite: Boolean)

case class Ticket(id: UUID, file: Long, upload: Boolean)

private class Users(tag: Tag) extends Table[User](tag, "USERS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def username = column[String]("USERNAME", O.Unique)
  def password = column[String]("PASSWORD")

  override def * = (id, username, password) <> (User.tupled, User.unapply)
}

private class Files(tag: Tag) extends Table[File](tag, "FILES") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME", O.Unique)
  def hash = column[Option[String]]("HASH")
  def owner = column[Long]("OWNER")
  def publicRead = column[Boolean]("PUBLIC_READ")
  def publicWrite = column[Boolean]("PUBLIC_WRITE")

  def ownerFk = foreignKey("OWNER_FK", owner, Users)(_.id)

  override def * =
    (id, name, hash, owner, publicRead, publicWrite) <> (File.tupled, File.unapply)
}

private class Tickets(tag: Tag) extends Table[Ticket](tag, "TICKETS") {
  def id = column[UUID]("ID", O.PrimaryKey)
  def file = column[Long]("FILE")
  def upload = column[Boolean]("UPLOAD")

  def fileFk = foreignKey("FILE_FK", file, Files)(_.id)

  override def * = (id, file, upload) <> (Ticket.tupled, Ticket.unapply)
}

private object Users extends TableQuery[Users](new Users(_))

private object Files extends TableQuery[Files](new Files(_))

private object Tickets extends TableQuery[Tickets](new Tickets(_))
