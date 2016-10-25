import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream._
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FunSuite, GivenWhenThen, Matchers}

/**
  * Created by 'Jakub Dziworski' on 22.10.16
  */
class SshConnectionTest extends FunSuite with Matchers with ScalatestRouteTest with GivenWhenThen {
  test("should send command and receive result securely") {
    Given("Alice and Bob")
    val x = 3 //RFC: C (Client) generates a random number x
    val y = 4 //RFC: S (Server) generates a random number y
    val e = SshConstants.G.pow(x).mod(SshConstants.P) //RFC: e = g^x mod p
    val f = SshConstants.G.pow(y).mod(SshConstants.P) //RFC: f = g^y mod p
    val key = f.pow(x).mod(SshConstants.P).toString() //RFC: C then computes K = f^x
    val alice = WSProbe()
    val bob = new Server(randomValueProvider = () => y)
    And("simple command")
    val command = "whats up dude"
    val expectedResponse = "I've received 'whats up dude'"
    val encodedCommand = Encryption.encrypt(key, command)
    val expectedEncryptedResponse = Encryption.encrypt(key, expectedResponse)

    When("Alice sends it's E value and Encrypted Command to Bob")
    WS("/", alice.flow) ~> bob.route
    alice.sendMessage(SshConstants.SSH_MSG_KEXDH_INIT + e)
    alice.sendMessage(SshConstants.SSH_MSG_COMMAND + encodedCommand)

    Then("Bob should calculate and send it's F value back to Alice")
    alice.expectMessage(SshConstants.SSH_MSG_KEXDH_REPLY + f)
    And("be able to decrypt command interpret it and send back encrypted result")
    alice.expectMessage(SshConstants.SSH_MSG_COMMAND_REPLY + expectedEncryptedResponse)
    And("The decrypted response should be as expected")
    Encryption.decrypt(key, expectedEncryptedResponse) shouldBe expectedResponse
  }

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

  sealed trait SSHMessage
  case class InitConnection(e: String, x: Int) extends SSHMessage
  case class InitConnectionResponse(y: String) extends SSHMessage
  case class Command(msg: String) extends SSHMessage
  case class CommandResponse(msg: String) extends SSHMessage

}

