package org.justcards.client.view

import java.util.concurrent.Executors

import org.justcards.client.controller.AppController
import org.justcards.commons.{AppError, GameId, LobbyId, UserId}

import scala.concurrent.{ExecutionContextExecutor, Future}


case class ConsoleManagerImpl(controller: AppController) extends View {
  import ConsoleManager._
  import ConsoleManagerImpl._

  implicit val executor: ExecutionContextExecutor =  scala.concurrent.ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  override def chooseNickname(): Unit = createTaskAskNickname

  override def error(error: AppError.Value): Unit = createTaskError(error)

  override def showMenu(): Unit = createTaskMenuChoice

  override def showLobbyCreation(games: Set[GameId]): Unit = createTaskLobbyCreation(games)

  override def showLobbyJoin(lobbies: Set[(LobbyId, Set[UserId])]): Unit = createTaskLobbyJoining(lobbies)

  override def lobbyCreated(lobby: LobbyId): Unit = ???

  override def lobbyJoined(lobby: LobbyId, members: Set[UserId]): Unit = ???

  override def lobbyUpdate(lobby: LobbyId, members: Set[UserId]): Unit = ???

  private def createTaskAskNickname = Future {controller login ask(CHOOSE_NICKNAME)}

  private def createTaskError(error: AppError.Value) = Future {
    error match {
      case AppError.USER_ALREADY_PRESENT =>
        println(NICKNAME_ERROR)
        controller login ask(CHOOSE_NICKNAME)
      case AppError.MESSAGE_SENDING_FAILED =>
        println(LAST_MESSAGE_LOST)
      case AppError.CANNOT_CONNECT =>
        println(CANNOT_CONNECT)
      case AppError.CONNECTION_LOST =>
        println(CONNECTION_LOST)
    }
  }

  private def createTaskMenuChoice = Future {
    println(MENU_TITLE)
    for (choice <- MenuChoice.values) println(choice.id + ")" + choice)
    controller menuSelection choiceSelection(MenuChoice.maxId - 1) //maxId = 4
  }

  private def createTaskLobbyCreation(games: Set[GameId]) = Future {
    println(LOBBY_CREATION_TITLE)
    val gamesList = games.toList
    for (index <- 1 to gamesList.size) println(index + ")" + gamesList(index-1).name)
    controller createLobby gamesList(choiceSelection(games.size) - 1)
  }

  private def createTaskLobbyJoining(lobbies: Set[(LobbyId, Set[UserId])]) = Future {
    println(LOBBY_LIST_TITLE)
    val lobbiesList = lobbies.toList
    for (index <- 1 to lobbiesList.size) println(index + ")" + lobbiesList(index-1))
    controller joinLobby lobbiesList(choiceSelection(lobbies.size) - 1)._1
  }
}

object ConsoleManagerImpl {
  val NUMBER_CHOICE = "Insert number of your choice:"
  val WRONG_VALUE = "Error: unacceptable value"
  val EMPTY_RESPONSE = "Empty answer isn't allowed"

  @scala.annotation.tailrec
  private def choiceSelection(maxValue: Int): Int = {
    try {
      ask(NUMBER_CHOICE) toInt match {
        case a if 0 < a && a <= maxValue => a
      }
    } catch {
      case e: Exception =>
        println(WRONG_VALUE)
        choiceSelection(maxValue)
    }
  }

  @scala.annotation.tailrec
  private def ask(question: String): String = {
    import scala.io.StdIn._
    println(question)
    readLine match {
      case a if a.isBlank || a.isEmpty =>
        println(EMPTY_RESPONSE)
        ask(question)
      case a => a
    }
  }

  @throws(classOf[Exception])
  implicit private def IntToMenuChoice(choice: Int): MenuChoice.Value = MenuChoice(choice)

}

object TestConsole extends App{
  //val console = ConsoleManagerImpl(AppController)

  //console chooseNickname()
  //console errorLogin()
  //console showMenu()
  //console showLobbyCreation (Set( GameId(10, "Beccaccino"), GameId(20, "Briscola")))
  //console showLobbyJoining(List( Lobby(LobbyId(100), "prima", 1), Lobby(LobbyId(200), "seconda", 4) ))
}
