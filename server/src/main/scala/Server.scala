package se.nullable.kth.id1212.fileserver.server

import java.rmi.{ConnectException, NotBoundException}
import java.rmi.registry.{LocateRegistry, Registry}

import com.google.inject.{Binder, Guice, Module, Provides}
import javax.inject.Singleton
import se.nullable.kth.id1212.fileserver.common.controller.FileServerCompanion
import se.nullable.kth.id1212.fileserver.server.controller.ManagerController
import se.nullable.kth.id1212.fileserver.server.model.{DBManager, FSProfile}
import slick.basic.DatabaseConfig

import org.h2.tools.{Server => H2Server}

object Server {
  val injector = Guice.createInjector(new ServerModule())

  def createDatabase(): Unit = {
    val dbManager = injector.getInstance(classOf[DBManager])
    dbManager.setUp()

    // Start management UI
    H2Server.createWebServer().start()
  }

  def findRegistry(): Registry =
    try {
      val reg = LocateRegistry.getRegistry
      reg.list() // Will fail if not running
      reg
    } catch {
      case _: ConnectException =>
        LocateRegistry.createRegistry(Registry.REGISTRY_PORT)
    }

  def registerService(): Unit = {
    val registry = findRegistry()
    val manager = injector.getInstance(classOf[ManagerController])
    try {
      registry.unbind(FileServerCompanion.REGISTRY_NAME)
    } catch {
      case _: NotBoundException =>
    }
    registry.bind(FileServerCompanion.REGISTRY_NAME, manager)
  }

  def main(args: Array[String]): Unit = {
    createDatabase()
    registerService()
  }
}

class ServerModule extends Module {
  @Provides
  @Singleton
  def dbConfig: DatabaseConfig[FSProfile] = DatabaseConfig.forConfig("slick")

  @Provides
  @Singleton
  def db(config: DatabaseConfig[FSProfile]): FSProfile.api.Database = config.db

  def configure(binder: Binder): Unit = {
  }
}
