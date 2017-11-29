package se.nullable.kth.id1212.fileserver.common.controller

import java.net.SocketAddress
import java.rmi.{Remote, RemoteException}
import se.nullable.kth.id1212.fileserver.common.model.{FileInfo, TicketID}

trait FileEventListener extends Remote

trait FileServer extends Remote {
  @throws[RemoteException]
  def uploadFile(name: String): Either[String, TicketID]

  @throws[RemoteException]
  def downloadFile(name: String): Either[String, TicketID]

  @throws[RemoteException]
  def deleteFile(name: String): Either[String, Unit]

  @throws[RemoteException]
  def listFiles(): Seq[FileInfo]

  @throws[RemoteException]
  def setPermissions(name: String,
                     publicRead: Boolean,
                     publicWrite: Boolean): Either[String, Unit]

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
