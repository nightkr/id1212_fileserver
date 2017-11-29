package se.nullable.kth.id1212.fileserver.server.model

import java.util.UUID

import scala.concurrent.ExecutionContext

import FSProfile.api._
import javax.inject.Inject
import se.nullable.kth.id1212.fileserver.common.controller.TicketID

class FileManager @Inject()(fileStore: FileStore) {
  def find(user: User, name: String): DBIO[Option[File]] =
    Files
      .filter(_.name === name)
      .filter(f => f.owner === user.id || f.publicRead || f.publicWrite)
      .result
      .headOption

  def findByTicket(ticket: Ticket): DBIO[File] =
    Files.filter(_.id === ticket.file).result.head

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

  def setFileHash(ticket: Ticket, hash: String)(
      implicit ec: ExecutionContext): DBIO[Unit] =
    Files
      .filter(_.id === ticket.file)
      .map(_.hash)
      .update(Some(hash))
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
