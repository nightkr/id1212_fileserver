package se.nullable.kth.id1212.fileserver.server.net

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.{ByteBuffer, CharBuffer}
import java.nio.channels.{FileChannel, ServerSocketChannel, SocketChannel}
import java.nio.charset.Charset
import java.nio.file.{Files, StandardOpenOption}
import java.util.UUID

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

import javax.inject.{Inject, Singleton}
import resource._
import se.nullable.kth.id1212.fileserver.common.controller.TicketID
import se.nullable.kth.id1212.fileserver.server.model.{
  FSProfile,
  FileManager,
  FileStore,
  Ticket
}
import FSProfile.api._

@Singleton
class TransferServer @Inject()(implicit fileManager: FileManager,
                               fileStore: FileStore,
                               executionContext: ExecutionContext,
                               db: Database) {
  private val serverSocket = ServerSocketChannel.open()

  private def awaitDB[A](dbio: DBIO[A]): A =
    Await.result(db.run(dbio), 2.seconds)

  private object ListenerThread extends Thread {
    setDaemon(true)

    override def run(): Unit =
      while (serverSocket.isOpen()) {
        val sock = serverSocket.accept()
        new TransferThread(sock).start()
      }
  }

  private class TransferThread(sock: SocketChannel) extends Thread {
    def readLine(): String = {
      // Can't use Scanner since it buffers too eagerly
      // which corrupts further input
      val outBuf = new StringBuilder()
      val charBuf = CharBuffer.allocate(1)
      val byteBuf = ByteBuffer.allocate(1)
      val decoder = Charset.forName("UTF-8").newDecoder()
      def hasEol = outBuf.length > 0 && outBuf.charAt(outBuf.length - 1) == '\n'
      while (!hasEol && sock.read(byteBuf) != -1) {
        byteBuf.flip()
        decoder.decode(byteBuf, charBuf, false)
        byteBuf.compact()
        charBuf.flip()
        while (charBuf.hasRemaining()) {
          outBuf.append(charBuf.get)
        }
        charBuf.compact()
      }
      outBuf.toString.trim
    }

    def handleUpload(ticket: Ticket): Unit = {
      val buf = ByteBuffer.allocate(10 * 1024)
      val target = Files.createTempFile("fileserver-upload-", ".tmp")
      val hash = try {
        for (targetChan <- managed(
               FileChannel.open(target, StandardOpenOption.WRITE))) {
          println((sock, buf))
          while (sock.read(buf) != -1) {
            println(buf)
            buf.flip()
            targetChan.write(buf)
            buf.compact()
          }
        }
        fileStore.moveIntoStore(target)
      } finally {
        Files.deleteIfExists(target)
      }
      awaitDB(fileManager.setFileHash(ticket, hash))
    }

    def handleDownload(ticket: Ticket): Unit = {
      val file = awaitDB(fileManager.findByTicket(ticket))
      val path = file.hash.flatMap(fileStore.get)
      for (srcChan <- managed(FileChannel.open(path.get))) {
        srcChan.transferTo(0, srcChan.size(), sock)
      }
    }

    override def run(): Unit =
      for (_ <- managed(sock)) {
        val ticketId = readLine()
        val ticket = awaitDB(
          fileManager.consumeTicket(TicketID(UUID.fromString(ticketId)))).get

        if (ticket.upload) {
          handleUpload(ticket)
        } else {
          handleDownload(ticket)
        }
      }
  }

  lazy val address: SocketAddress = {
    serverSocket.bind(new InetSocketAddress(0))
    ListenerThread.start()
    serverSocket.getLocalAddress
  }
}
