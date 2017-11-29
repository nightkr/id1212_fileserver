package se.nullable.kth.id1212.fileserver.server.controller

import javax.inject.Singleton
import org.slf4j.LoggerFactory
import se.nullable.kth.id1212.fileserver.common.controller.FileEventListener
import se.nullable.kth.id1212.fileserver.server.model.{ File, User }

import scala.collection.mutable

@Singleton
class Notifier {
  private val listeners: mutable.Map[Long, mutable.Set[FileEventListener]] = mutable.Map.empty.withDefault(_ => mutable.Set.empty)
  private val log = LoggerFactory.getLogger(getClass)

  def addListener(file: File, listener: FileEventListener): Unit = {
    listeners(file.id) += listener
  }

  def removeListener(file: File, listener: FileEventListener): Unit = {
    listeners(file.id) -= listener
  }

  def sendNotification(file: File, user: User, modified: Boolean): Unit = {
    val fileListeners = listeners(file.id)
    for (listener <- fileListeners.toSeq) {
      try {
        if (modified) {
          listener.fileModified(user.username)
        } else {
          listener.fileRead(user.username)
        }
      } catch {
        case ex: Exception =>
          log.error("Access notification error", ex)
          fileListeners -= listener
      }
    }
  }
}
