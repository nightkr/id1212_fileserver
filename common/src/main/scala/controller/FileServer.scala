package se.nullable.kth.id1212.fileserver.common.controller

import java.rmi.{ Remote, RemoteException }

trait FileServer extends Remote {
  @throws[RemoteException]
  def getFile(name: String): Unit
}

trait FileServerManager extends Remote {
  @throws[RemoteException]
  def login(username: String, password: String): Option[FileServer]

  @throws[RemoteException]
  def register(username: String, password: String): Either[String, Unit]
}

object FileServerCompanion {
  final val REGISTRY_NAME = "FILE_SERVER"
}
