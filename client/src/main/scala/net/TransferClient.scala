package se.nullable.kth.id1212.fileserver.client.net

import java.io.PrintStream
import java.net.SocketAddress
import java.nio.channels.{Channels, FileChannel, SocketChannel}
import java.nio.file.{Path, StandardOpenOption}

import resource._

import se.nullable.kth.id1212.fileserver.common.controller.TicketID

class TransferClient {
  def upload(addr: SocketAddress, ticket: TicketID, path: Path): Unit =
    for {
      socketChan <- managed(SocketChannel.open(addr))
      outStream <- managed(Channels.newOutputStream(socketChan))
      printer <- managed(new PrintStream(outStream, true, "UTF-8"))
      file <- managed(FileChannel.open(path, StandardOpenOption.READ))
    } {
      println(s"Connected to transfer server $addr")
      printer.println(ticket.id)
      printer.flush()
      file.transferTo(0, file.size(), socketChan)
      println("Done uploading")
    }
}
