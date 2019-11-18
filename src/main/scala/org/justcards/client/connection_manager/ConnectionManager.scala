package org.justcards.client.connection_manager

import akka.actor.{Actor, ActorRef, Props, Stash}
import org.justcards.client.connection_manager.ConnectionManager.{Connected, InitializeConnection}
import org.justcards.commons.AppError.{CANNOT_CONNECT, CONNECTION_LOST, MESSAGE_SENDING_FAILED}
import org.justcards.commons.{AppError, AppMessage, ErrorOccurred}
import org.justcards.commons.actor_connection.{ActorWithConnection, Outer}

trait ConnectionManager extends (ActorRef => Props)

object ConnectionManager {
  case object InitializeConnection
  case object Connected
}

abstract class AbstractConnectionManager(appController: ActorRef) extends ActorWithConnection with Actor with Stash {

  override def receive: Receive = parse orElse waitForInit

  protected def init: Receive

  protected def initializeConnection(): Unit

  protected def connected(server: ActorRef): Unit = {
    unstashAll()
    this become (work(server) orElse connectionErrorHandling(server) orElse stashUnhandled)
    appController ! Connected
  }

  protected def connectionErrorHandling(server: ActorRef): Receive

  private def waitForInit: Receive = {
    case InitializeConnection =>
      println("Test (testActor = " + appController + ", CM = " + self + ") : cm received InitializeConnection ")
      initializeConnection()
      this become (init orElse stashUnhandled)
  }

  private def work(server: ActorRef): Receive = {
    case m: AppMessage =>
      println("Test (testActor = " + appController + ", CM = " + self + ") : cm received inner message " + m)
      server ==> m
    case Outer(m: AppMessage) =>
      println("Test (testActor = " + appController + ", CM = " + self + ") : cm received outer message " + m)
      appController ! m
  }

  private def stashUnhandled: Receive = {
    case m =>
      println("Test (testActor = " + appController + ", CM = " + self + ") : cm received unhandled message " + m)
      stash()
  }

  protected def error(message: AppError.Value): Unit = message match {
    case CONNECTION_LOST =>
      unstashAll()
      this become (init orElse stashUnhandled)
      appController ! ErrorOccurred(CONNECTION_LOST)
    case CANNOT_CONNECT => appController ! ErrorOccurred(CANNOT_CONNECT)
    case MESSAGE_SENDING_FAILED => appController ! ErrorOccurred(MESSAGE_SENDING_FAILED)
  }
}
