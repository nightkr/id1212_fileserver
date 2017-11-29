package se.nullable.kth.id1212.fileserver.client.view

import java.nio.file.{Files, Paths}
import scala.io.StdIn
import se.nullable.kth.id1212.fileserver.client.net.TransferClient
import se.nullable.kth.id1212.fileserver.common.controller.{
  FileServer,
  FileServerManager
}

class TUI(manager: FileServerManager, transferClient: TransferClient) {
  private var fileServer: Option[FileServer] = None

  private def fileServerEither = fileServer.toRight("Not logged in")

  def mainLoop(): Unit = {
    var continue = true
    while (continue) {
      val cmdLine = Option(StdIn.readLine()).getOrElse("quit").split(" ")
      handleCmd(cmdLine, () => continue = false)
    }
  }

  private def handleCmd(cmd: Array[String], stop: () => Unit): Unit =
    cmd match {
      case Array("") =>
      case Array("quit") =>
        stop()
      case Array("help") =>
        printHelp()

      case Array("login", username, password) =>
        manager.login(username, password) match {
          case None =>
            println("Failed to log in")
          case Some(fs) =>
            fileServer = Some(fs)
            println("Successfully logged in")
        }
      case Array("login", _*) =>
        println("Usage: login <username> <password>")
      case Array("logout") =>
        fileServer = None
        println("Successfully logged out")

      case Array("register", username, password) =>
        manager.register(username, password) match {
          case Left(msg) =>
            println(s"Failed to register: $msg")
          case Right(()) =>
            println("Sucessfully registered, you can now log in")
        }
      case Array("register", _*) =>
        println("Usage: register <username> <password>")

      case Array("upload") =>
        println("Usage: upload <file>")
      case Array("upload", pathStr @ _*) =>
        (for {
          fs <- fileServerEither
          path <- Some(Paths.get(pathStr.mkString(" ")))
            .filter(Files.exists(_))
            .toRight("No such file")
          ticket <- fs.uploadFile(path.getFileName.toString())
        } yield
          transferClient.upload(manager.transferServerAddr, ticket, path)).left
          .foreach(println)

      case _ =>
        println("Invalid command -- run help for some advice")
    }

  private def printHelp(): Unit =
    println("""
Available commands:
quit -- Quit
help -- Print this message
login <username> <password> -- Log in
logout -- Log out
register <username> <password> -- Register
""".trim())
}
