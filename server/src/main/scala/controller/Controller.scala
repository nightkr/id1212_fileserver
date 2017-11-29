package se.nullable.kth.id1212.fileserver.server.controller

import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import resource._

import javax.inject.Inject
import se.nullable.kth.id1212.fileserver.common.controller.{FileServer, FileServerManager}
import se.nullable.kth.id1212.fileserver.server.model.{ FSProfile, User, UserManager }
import se.nullable.kth.id1212.fileserver.server.model.FSProfile.api._
import slick.basic.DatabaseConfig

object Utils {
  def logErrors[A](f: => A): A = try {
    f
  } catch {
    case ex: Exception =>
      ex.printStackTrace()
      throw new RemoteException("Internal server error")
  }
}

class ManagerController @Inject() (userManager: UserManager,
                                   db: Database) extends UnicastRemoteObject with FileServerManager {
  override def login(username: String, password: String): Option[FileServer] = Utils.logErrors {
    val user = for {
      user <- userManager.find(username)
      success = user.map(userManager.verifyPassword(_, password)).getOrElse(false)
    } yield user.filter(_ => success)
    val ctrl = user.map(_.map(new LoggedInController(_)))
    Await.result(db.run(ctrl), 2.seconds)
  }

  override def register(username: String, password: String): Either[String, Unit] = Utils.logErrors {
    val create = userManager.create(username, password).asTry.map(_.toEither.left.map(_ => "That username is taken"))
    Await.result(db.run(create), 2.seconds)
  }
}

class LoggedInController(user: User) extends UnicastRemoteObject with FileServer {
  override def getFile(name: String) = ???
}
