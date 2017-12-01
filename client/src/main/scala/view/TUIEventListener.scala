package se.nullable.kth.id1212.fileserver.client.view

import java.rmi.server.UnicastRemoteObject
import se.nullable.kth.id1212.fileserver.common.controller.FileEventListener


class TUIEventListener(file: String) extends UnicastRemoteObject with FileEventListener {
  override def fileRead(username: String): Unit =
    println(s"File $file read by $username")

  override def fileModified(username: String): Unit =
    println(s"File $file changed by $username")
}
