package org.justcards.client.connection_manager

import java.net.InetSocketAddress

import akka.actor.{ActorRef, Props, Stash}
import akka.io.{IO, Tcp}
import akka.io.Tcp.{CommandFailed, Connect, Connected, ConnectionClosed, Register, Write}
import org.justcards.commons.ErrorOccurred
import org.justcards.commons.AppError._
import org.justcards.commons.actor_connection.ActorWithTcp

object TcpConnectionManager {

  def apply(address: InetSocketAddress): ConnectionManager =
    (appController: ActorRef) => Props(classOf[TcpConnectionManager], address, appController)

  def apply(host: String, port: Int): ConnectionManager = TcpConnectionManager(new InetSocketAddress(host, port))

  private[this] class TcpConnectionManager(address: InetSocketAddress, appController: ActorRef)
    extends AbstractConnectionManager(appController) with ActorWithTcp {

    import context.system

    override protected def initializeConnection(): Unit = IO(Tcp) ! Connect(address)

    override protected def init: Receive = {
      case CommandFailed(m: Connect) =>
        println("Test (testActor = " + appController + ", CM = " + self + ") : cm received command failed connect " + m)
        error(CANNOT_CONNECT)
      case m @ Connected(_, _) =>
        println("Test (testActor = " + appController + ", CM = " + self + ") : cm connected " + m)
        val server = sender()
        server ! Register(self)
        connected(server)
    }

    override protected def connectionErrorHandling(server: ActorRef): Receive = {
      case CommandFailed(w: Write) =>
        println("Test (testActor = " + appController + ", CM = " + self + ") : cm received command failed write " + w)
        error(MESSAGE_SENDING_FAILED)
      case m: ConnectionClosed =>
        println("Test (testActor = " + appController + ", CM = " + self + ") : cm received connection closed " + m)
        error(CONNECTION_LOST)
    }
  }
}

