package org.justcards.client

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.TestProbe
import org.justcards.client.Server.ServerReady
import org.justcards.client.connection_manager.ConnectionManager.InitializeConnection
import org.justcards.client.connection_manager.TcpConnectionManager
import org.justcards.commons.AppError._
import org.justcards.commons._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.justcards.client.Utils._

class ConnectionManagerTest extends WordSpecLike with Matchers with BeforeAndAfterAll {

  private implicit val system: ActorSystem = ActorSystem("ConnectionManagerTest")
  private var nextAvailableServerPort = 6700

  override def afterAll: Unit = {
    system.terminate()
  }

  "The connection manager" should {

    "send a LogIn message to the server correctly when received from the application controller" in {
      sendMessageToConnectionManagerAndCheckIfItIsCorrectlyRedirectedToTheServer(LogIn(username))
    }

    "send a Logged message to the application controller when received from the server" in {
      receiveMessageFromServerAndCheckItIsCorrectlyRedirectedToTheApplicationManager(Logged())
    }

    "send an ErrorOccurred message to the application controller when received from the server" in {
      receiveMessageFromServerAndCheckItIsCorrectlyRedirectedToTheApplicationManager(ErrorOccurred(errorMessage))
    }

    "send a RetrieveAvailableGames message to the server correctly when received from the application controller" in {
      sendMessageToConnectionManagerAndCheckIfItIsCorrectlyRedirectedToTheServer(RetrieveAvailableGames())
    }

    "send an AvailableGames message to the application controller when received from the server" in {
      receiveMessageFromServerAndCheckItIsCorrectlyRedirectedToTheApplicationManager(AvailableGames(Set(game)))
    }

    "send a CreateLobby message to the server correctly when received from the application controller" in {
      sendMessageToConnectionManagerAndCheckIfItIsCorrectlyRedirectedToTheServer(CreateLobby(game))
    }

    "send an LobbyCreated message to the application controller when received from the server" in {
      receiveMessageFromServerAndCheckItIsCorrectlyRedirectedToTheApplicationManager(LobbyCreated(lobby))
    }

    "send a RetrieveAvailableLobbies message to the server correctly when received from the application controller" in {
      sendMessageToConnectionManagerAndCheckIfItIsCorrectlyRedirectedToTheServer(RetrieveAvailableLobbies())
    }

    "send an AvailableLobbies message to the application controller when received from the server" in {
      receiveMessageFromServerAndCheckItIsCorrectlyRedirectedToTheApplicationManager(AvailableLobbies(Set((lobby,Set(user)))))
    }

    "send a JoinLobby message to the server correctly when received from the application controller" in {
      sendMessageToConnectionManagerAndCheckIfItIsCorrectlyRedirectedToTheServer(JoinLobby(lobby))
    }

    "send an LobbyJoined message to the application controller when received from the server" in {
      receiveMessageFromServerAndCheckItIsCorrectlyRedirectedToTheApplicationManager(LobbyJoined(lobby,Set(user)))
    }

    "send an LobbyUpdate message to the application controller when received from the server" in {
      receiveMessageFromServerAndCheckItIsCorrectlyRedirectedToTheApplicationManager(LobbyUpdate(lobby,Set(user)))
    }

    "inform the application controller that the connection to the server cannot be established" in {
      val testProbe = TestProbe()
      val testActor: ActorRef = testProbe.ref
      val appController = system.actorOf(TestAppController(testActor))
      val connectionManager = system.actorOf(TcpConnectionManager(getNewServerAddress)(appController))
      connectionManager ! InitializeConnection
      testProbe.expectMsg(ErrorOccurred(CANNOT_CONNECT))
    }

    "inform the application controller that the connection was lost" in {
      val (connectionManager,testProbe) = initComponents
      val server = connectToServer(connectionManager, testProbe)
      server ! PoisonPill
      testProbe.expectMsg(ErrorOccurred(CONNECTION_LOST))
    }

  }

  private def receiveMessageFromServerAndCheckItIsCorrectlyRedirectedToTheApplicationManager(message: AppMessage): Unit = {
    val (connectionManager,testProbe) = initComponents
    val server = connectToServer(connectionManager, testProbe)
    server ! message
    testProbe expectMsg message
  }

  private def sendMessageToConnectionManagerAndCheckIfItIsCorrectlyRedirectedToTheServer(message: AppMessage): Unit = {
    val (connectionManager,testProbe) = initComponents
    connectToServer(connectionManager, testProbe)
    connectionManager ! message
    testProbe expectMsg message
  }

  private def initComponents: (ActorRef, /*ActorRef,*/ TestProbe) = {
    val testProbe = TestProbe()
    val testActor: ActorRef = testProbe.ref
    val serverAddress = getNewServerAddress
    /*system.actorOf(Server(serverAddress, SimpleConnectionHandler(testActor), testActor))
    testProbe expectMsg ServerReady*/
    startServer(serverAddress,testActor,testProbe)
    val appController = system.actorOf(TestAppController(testActor))
    val connectionManager = system.actorOf(TcpConnectionManager(serverAddress)(appController))
    /*connectionManager ! InitializeConnection
    val server = waitToBeConnectedAndGetSenderServer(testProbe)
    println("Test (testProbe = " + testProbe + ", testActor = " + testProbe.ref + ", connectionManager = " + connectionManager + ") : ready.")
    (connectionManager, server, testProbe)*/
    (connectionManager, testProbe)
  }

  private def startServer(serverAddress: InetSocketAddress, testActor: ActorRef, testProbe: TestProbe): Unit = {
    system.actorOf(Server(serverAddress, SimpleConnectionHandler(testActor), testActor))
    testProbe expectMsg ServerReady
  }

  private def connectToServer(connectionManager: ActorRef, testProbe: TestProbe): ActorRef = {
    connectionManager ! InitializeConnection
    waitToBeConnectedAndGetSenderServer(testProbe)
  }

  private def waitToBeConnectedAndGetSenderServer(testProbe: TestProbe): ActorRef = {
    println("Test (testProbe = " + testProbe + ", testActor = " + testProbe.ref + ") : wait to receive server ref and connected.")
    testProbe.receiveN(2).find(_.isInstanceOf[ActorRef]).get.asInstanceOf[ActorRef]
  }

  private def getNewServerAddress: InetSocketAddress = synchronized {
    val serverAddress = new InetSocketAddress(serverHost,nextAvailableServerPort)
    nextAvailableServerPort += 1
    serverAddress
  }
}
