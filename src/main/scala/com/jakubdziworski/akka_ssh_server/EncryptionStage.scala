package com.jakubdziworski.akka_ssh_server

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by 'Jakub Dziworski' on 25.10.16
  */
class EncryptionStage extends GraphStage[FlowShape[SSHMessage, SSHMessage]] with LazyLogging {
  val in = Inlet[SSHMessage]("Encryption.in")
  val out = Outlet[SSHMessage]("Encryption.out")
  private var encryptionKey: String = ""

  override def shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val res = grab(in) match {
            case InitConnection(e, y) =>
              val f = SshConstants.G.pow(y).mod(SshConstants.P) //RFC: Server generates a random number y (0 < y < q) and computes f = g^y mod p
              encryptionKey = BigInt(e).pow(y).mod(SshConstants.P).toString() //RFC: It computes K = e^y mod p,
              logger.debug(s"generated new SECRET key = $encryptionKey")
              InitConnectionResponse(f.toString())
            case Command(cmd) =>
              logger.debug(s"received command = $cmd")
              val decrypted = Encryption.decrypt(encryptionKey, cmd)
              logger.debug(s"decrypting command $cmd with key $encryptionKey = $decrypted")
              Command(decrypted)
            case CommandResponse(cmdResponse) =>
              val encrypted = Encryption.encrypt(encryptionKey, cmdResponse)
              logger.debug(s"encrypting response $cmdResponse with key $encryptionKey = $encrypted")
              CommandResponse(encrypted)
            case unencrypted => unencrypted
          }
          push(out, res)
        }
      })
      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }
  }


}
