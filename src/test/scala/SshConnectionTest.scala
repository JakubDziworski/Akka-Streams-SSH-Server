import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import com.jakubdziworski.akka_ssh_server.{Encryption, SshConstants,Server}
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
}








