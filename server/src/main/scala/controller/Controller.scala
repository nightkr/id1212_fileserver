package se.nullable.kth.id1212.fileserver.server.controller

import java.net.SocketAddress
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import javax.inject.{Inject, Named}
import se.nullable.kth.id1212.fileserver.common.controller.{
  FileEventListener,
  FileServer,
  FileServerManager
}
import se.nullable.kth.id1212.fileserver.common.model.FileInfo
import se.nullable.kth.id1212.fileserver.common.model.TicketID
import se.nullable.kth.id1212.fileserver.server.model.{User, UserManager}
import se.nullable.kth.id1212.fileserver.server.model.FSProfile.api._
import se.nullable.kth.id1212.fileserver.server.model.FileManager

object Utils {
  def logErrors[A](f: => A): A =
    try {
      f
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        throw new RemoteException("Internal server error")
    }
}

class ManagerController @Inject()(
    userManager: UserManager,
    db: Database,
    fileManager: FileManager,
    notifier: Notifier,
    @Named("transferServer")
    override val transferServerAddr: SocketAddress)
    extends UnicastRemoteObject
    with FileServerManager {
  override def login(username: String, password: String): Option[FileServer] =
    Utils.logErrors {
      val user = for {
        user <- userManager.find(username)
        success = user
          .map(userManager.verifyPassword(_, password))
          .getOrElse(false)
      } yield user.filter(_ => success)
      val ctrl =
        user.map(_.map(new LoggedInController(_, fileManager, db, notifier)))
      Await.result(db.run(ctrl), 2.seconds)
    }

  override def register(username: String,
                        password: String): Either[String, Unit] =
    Utils.logErrors {
      val create = userManager
        .create(username, password)
        .asTry
        .map(_.toEither.left.map(_ => "That username is taken"))
      Await.result(db.run(create), 2.seconds)
    }
}

class LoggedInController(user: User,
                         fileManager: FileManager,
                         db: Database,
                         notifier: Notifier)
    extends UnicastRemoteObject
    with FileServer {
  override def uploadFile(name: String): Either[String, TicketID] =
    Utils.logErrors {
      val ticket = (for {
        file <- fileManager.findOrCreate(user, name)
        ticket <- fileManager.createTicket(user, file, upload = true).map(_.get)
      } yield ticket).transactionally.asTry.map(_.toEither.left.map(_ =>
        "You do not have access to that file"))
      Await.result(db.run(ticket), 2.seconds)
    }

  override def downloadFile(name: String): Either[String, TicketID] =
    Utils.logErrors {
      val ticket = (for {
        file <- fileManager.find(user, name).map(_.get)
        ticket <- fileManager
          .createTicket(user, file, upload = false)
          .map(_.get)
      } yield ticket).transactionally.asTry.map(_.toEither.left.map(_ =>
        "You do not have access to that file"))
      Await.result(db.run(ticket), 2.seconds)
    }

  override def deleteFile(name: String): Either[String, Unit] =
    Utils.logErrors {
      val delete = fileManager
        .delete(user, name)
        .asTry
        .map(_.toEither.left.map(_ => "You do not have access to that file"))
      Await.result(db.run(delete), 2.seconds)
    }

  override def listFiles(): Seq[FileInfo] = Utils.logErrors {
    val files = fileManager.all(user)
    Await.result(db.run(files), 2.seconds)
  }

  override def setPermissions(name: String,
                              publicRead: Boolean,
                              publicWrite: Boolean): Either[String, Unit] =
    Utils.logErrors {
      val update =
        fileManager
          .setFileAccess(user, name, publicRead, publicWrite)
          .asTry
          .map(_.toEither.left.map(_ => "You do not have access to that file"))
      Await.result(db.run(update), 2.seconds)
    }

  override def addEventListener(
      name: String,
      listener: FileEventListener): Either[String, Unit] = Utils.logErrors {
    val addListener = (for {
      file <- fileManager.find(user, name).map(_.get)
      if file.owner == user.id
    } yield notifier.addListener(file, listener)).asTry
      .map(_.toEither.left.map(_ => "You do not own that file"))
    Await.result(db.run(addListener), 2.seconds)
  }
  override def removeEventListener(
      name: String,
      listener: FileEventListener): Either[String, Unit] = Utils.logErrors {
    val removeListener = (for {
      file <- fileManager.find(user, name).map(_.get)
      if file.owner == user.id
    } yield notifier.removeListener(file, listener)).asTry
      .map(_.toEither.left.map(_ => "You do not own that file"))
    Await.result(db.run(removeListener), 2.seconds)
  }
}
