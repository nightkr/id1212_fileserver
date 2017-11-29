package se.nullable.kth.id1212.fileserver.client

import java.rmi.registry.LocateRegistry
import org.slf4j.LoggerFactory
import se.nullable.kth.id1212.fileserver.client.net.TransferClient
import se.nullable.kth.id1212.fileserver.client.view.TUI
import se.nullable.kth.id1212.fileserver.common.controller.{
  FileServer,
  FileServerCompanion,
  FileServerManager
}

object Client extends App {
  private val log = LoggerFactory.getLogger(getClass)

  println("Connecting to file server")
  val registry = LocateRegistry.getRegistry
  val manager = registry
    .lookup(FileServerCompanion.REGISTRY_NAME)
    .asInstanceOf[FileServerManager]
  println(s"Connected to file server $manager")

  val tui = new TUI(manager, new TransferClient)
  tui.mainLoop()
}
