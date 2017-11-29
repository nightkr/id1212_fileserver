package se.nullable.kth.id1212.fileserver.server.model

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Files => FileOps, Path, Paths}
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

class FileStore {
  private val base = Paths.get("files")

  private def fileHash(path: Path): String = {
    val buffer = ByteBuffer.allocate(10 * 1024)
    val file = FileChannel.open(path)
    val hash = MessageDigest.getInstance("SHA-256")
    while (file.position() < file.size()) {
      file.read(buffer)
      buffer.flip()
      hash.update(buffer)
      buffer.compact()
    }
    Hex.encodeHexString(hash.digest())
  }

  def moveIntoStore(path: Path): String = {
    if (!FileOps.exists(base)) {
      FileOps.createDirectory(base)
    }
    val hash = fileHash(path)
    val storePath = base.resolve(hash)
    if (FileOps.exists(storePath)) {
      FileOps.delete(path)
    } else {
      FileOps.move(path, storePath)
    }
    hash
  }

  def get(hash: String): Option[Path] = {
    // Only allow canonical hexadecimal strings, to avoid path injection
    if (hash != Hex.encodeHexString(Hex.decodeHex(hash))) {
      return None
    }

    Some(base.resolve(hash)).filter(FileOps.exists(_))
  }
}
