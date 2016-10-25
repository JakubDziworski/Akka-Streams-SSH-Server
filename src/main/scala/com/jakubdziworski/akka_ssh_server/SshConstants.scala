package com.jakubdziworski.akka_ssh_server

/**
  * Created by 'Jakub Dziworski' on 25.10.16
  */
object SshConstants {
  //RFC: p is a large safe prime; g is a generator for a subgroup of GF(p)
  //Me: Client and server agree on G and P before connection (that is why it can be constant)
  val G = BigInt(3)
  val P = BigInt(17)
  val SSH_MSG_KEXDH_INIT = "1"
  val SSH_MSG_KEXDH_REPLY = "2"
  val SSH_MSG_COMMAND = "3"
  val SSH_MSG_COMMAND_REPLY = "4"
}
