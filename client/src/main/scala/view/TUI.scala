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

      case Array("upload" | "ul") =>
        println("Usage: upload <file>")
      case Array("upload" | "ul", pathStr @ _*) =>
        (for {
          fs <- fileServerEither
          path <- Some(Paths.get(pathStr.mkString(" ")))
            .filter(Files.exists(_))
            .toRight("No such file")
          ticket <- fs.uploadFile(path.getFileName.toString())
        } yield
          transferClient.upload(manager.transferServerAddr, ticket, path)).left
          .foreach(println)

      case Array("download" | "dl") =>
        println("Usage: download <file>")
      case Array("download" | "dl", pathStr @ _*) =>
        (for {
          fs <- fileServerEither
          path = Paths.get(pathStr.mkString(" "))
          ticket <- fs.downloadFile(path.getFileName.toString())
        } yield
          transferClient.download(manager.transferServerAddr, ticket, path)).left
          .foreach(println)

      case Array("rm") =>
        println("Usage: rm <file>")
      case Array("rm", pathStr @ _*) =>
        (for {
          fs <- fileServerEither
          _ <- fs.deleteFile(pathStr.mkString(" "))
        } yield ()).left.foreach(println)

      case Array("ls") =>
        (for (fs <- fileServerEither)
          yield fs.listFiles().foreach(println)).left.foreach(println)

      case Array("chmod", perms, pathStr @ _*) =>
        (for {
          fs <- fileServerEither
          _ <- fs.setPermissions(pathStr.mkString(" "),
                                 publicRead = perms.contains('r'),
                                 publicWrite = perms.contains('w'))
        } yield ()).left.foreach(println)
      case Array("chmod", _*) =>
        println("Usage: chmod [r][w] <file>")

      case Array("listen") =>
        println("Usage: listen <file>")
      case Array("listen", pathStr @ _*) =>
        val path = pathStr.mkString(" ")
        (for {
          fs <- fileServerEither
          _ <- fs.addEventListener(path, new TUIEventListener(path))
        } yield ()).left.foreach(println)

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
