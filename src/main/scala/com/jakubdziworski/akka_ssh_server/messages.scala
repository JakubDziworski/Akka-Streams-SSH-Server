package com.jakubdziworski.akka_ssh_server

/**
  * Created by 'Jakub Dziworski' on 25.10.16
  */
sealed trait SSHMessage
case class InitConnection(e: String, x: Int) extends SSHMessage
case class InitConnectionResponse(y: String) extends SSHMessage
case class Command(msg: String) extends SSHMessage
case class CommandResponse(msg: String) extends SSHMessage

