package org.justcards.server

import akka.actor.ActorSystem
import org.justcards.server.connection_manager.ServerConnectionManager
import org.justcards.server.knowledge_engine.{GameKnowledge, KnowledgeEngine}
import org.justcards.server.user_manager.UserManager

/**
  * Server App entry point.
  */
object ServerApp extends App {

  val port = 6789

  val system = ActorSystem("justCards-server")
  val knowledgeEngine = system.actorOf(KnowledgeEngine(GameKnowledge()))
  val userManager = system.actorOf(UserManager(knowledgeEngine))
  system.actorOf(ServerConnectionManager(port, userManager))

}
