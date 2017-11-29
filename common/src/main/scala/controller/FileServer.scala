package se.nullable.kth.id1212.fileserver.common.controller

import java.net.SocketAddress
import java.rmi.{Remote, RemoteException}

trait FileEventListener extends Remote

trait FileServer extends Remote {
  @throws[RemoteException]
  def uploadFile(name: String): Either[String, TicketID]

  @throws[RemoteException]
  def addEventListener(listener: FileEventListener): Unit

  @throws[RemoteException]
  def removeEventListener(listener: FileEventListener): Unit
}

trait FileServerManager extends Remote {
  @throws[RemoteException]
  def transferServerAddr: SocketAddress

  @throws[RemoteException]
  def login(username: String, password: String): Option[FileServer]

  @throws[RemoteException]
  def register(username: String, password: String): Either[String, Unit]
}

object FileServerCompanion {
  final val REGISTRY_NAME = "FILE_SERVER"
}
