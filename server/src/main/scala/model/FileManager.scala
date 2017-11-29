package se.nullable.kth.id1212.fileserver.server.model

import java.util.UUID

import scala.concurrent.ExecutionContext

import FSProfile.api._
import javax.inject.Inject
import se.nullable.kth.id1212.fileserver.common.model.{FileInfo, TicketID}

class FileManager @Inject()(fileStore: FileStore) {
  private def visibleFor(user: User) =
    Files
      .filter(f => f.owner === user.id || f.publicRead || f.publicWrite)

  def find(user: User, name: String): DBIO[Option[File]] =
    visibleFor(user)
      .filter(_.name === name)
      .result
      .headOption

  def findByTicket(ticket: Ticket): DBIO[File] =
    Files.filter(_.id === ticket.file).result.head

  def all(user: User)(implicit ec: ExecutionContext): DBIO[Seq[FileInfo]] =
    (for {
      file <- visibleFor(user)
      owner <- file.ownerFk
    } yield (file, owner)).result.map(_.map {
      case ((file, owner)) =>
        FileInfo(file.name, owner.username, file.publicRead, file.publicWrite)
    })

  def create(user: User, name: String)(
      implicit ec: ExecutionContext): DBIO[File] =
    (for {
      id <- Files.returning(Files.map(_.id)) += File(id = 0,
                                                     name = name,
                                                     hash = None,
                                                     owner = user.id,
                                                     publicRead = false,
                                                     publicWrite = false)
      file <- Files.filter(_.id === id).result.head
    } yield file).transactionally

  def findOrCreate(user: User, name: String)(
      implicit ec: ExecutionContext): DBIO[File] =
    find(user, name)
      .flatMap(_.fold(create(user, name))(DBIO.successful))
      .transactionally

  def delete(user: User, name: String)(
      implicit ec: ExecutionContext): DBIO[Unit] =
    Files
      .filter(_.owner === user.id)
      .filter(_.name === name)
      .delete
      .filter(_ == 1)
      .map(_ => ())

  def setFileHash(ticket: Ticket, hash: String)(
      implicit ec: ExecutionContext): DBIO[Unit] =
    Files
      .filter(_.id === ticket.file)
      .map(_.hash)
      .update(Some(hash))
      .map(_ => ())

  def setFileAccess(
      user: User,
      name: String,
      publicRead: Boolean,
      publicWrite: Boolean)(implicit ec: ExecutionContext): DBIO[Unit] =
    Files
      .filter(_.owner === user.id)
      .filter(_.name === name)
      .map(f => (f.publicRead, f.publicWrite))
      .update((publicRead, publicWrite))
      .filter(_ == 1)
      .map(_ => ())

  def createTicket(user: User, file: File, upload: Boolean)(
      implicit ec: ExecutionContext): DBIO[Option[TicketID]] =
    if (user.id == file.owner || (!upload && file.publicRead) || (upload && file.publicWrite)) {
      val id = UUID.randomUUID()
      (Tickets += Ticket(id = id, file = file.id, upload = upload))
        .map(_ => Some(TicketID(id)))
    } else
      DBIO.successful(None)

  def consumeTicket(id: TicketID)(
      implicit ec: ExecutionContext): DBIO[Option[Ticket]] = {
    val query = Tickets.filter(_.id === id.id)
    for {
      ticket <- query.forUpdate.result.headOption
      _ <- query.delete
    } yield ticket
  }.transactionally
}
