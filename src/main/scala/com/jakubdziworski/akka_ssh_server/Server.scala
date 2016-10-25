package com.jakubdziworski.akka_ssh_server

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by 'Jakub Dziworski' on 25.10.16
  */
class Server(randomValueProvider: () => Int) extends Directives with LazyLogging {

  private[this] val websocketMessagetoDomainFlow = Flow[Message].map(m => logStage(s"converting raw message $m", m)) map {
    case TextMessage.Strict(msg) if msg.startsWith(SshConstants.SSH_MSG_KEXDH_INIT) => InitConnection(msg.substring(1), randomValueProvider())
    case TextMessage.Strict(msg) if msg.startsWith(SshConstants.SSH_MSG_COMMAND) => Command(msg.substring(1))
  }
  private[this] val domainToWebSocketMessageFlow = Flow[SSHMessage].map(m => logStage(s"converting domain object $m", m)).map {
    case InitConnectionResponse(secret) => TextMessage(SshConstants.SSH_MSG_KEXDH_REPLY + secret)
    case CommandResponse(msg) => TextMessage(SshConstants.SSH_MSG_COMMAND_REPLY + msg)
  }

  private[this] val commandInterpreterFlow = Flow[SSHMessage].map(m => logStage(s"interpreting command $m", m)).map {
    case Command(msg) => CommandResponse(s"I've received '$msg'")
    case plainTextMessage => plainTextMessage
  }

  private[this] val encryptionStage = new EncryptionStage()


  private[this] def logStage[T](msg: String, t: T) = {
    logger.debug(msg)
    t
  }

  val flow = websocketMessagetoDomainFlow
    .via(encryptionStage)
    .via(commandInterpreterFlow)
    .via(encryptionStage)
    .via(domainToWebSocketMessageFlow)

  val route = get {
    handleWebSocketMessages(flow)
  }
}
