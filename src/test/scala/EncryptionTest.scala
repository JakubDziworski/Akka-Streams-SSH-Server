import org.scalatest.{FunSuite, Matchers}

/**
  * Created by 'Jakub Dziworski' on 23.10.16
  */
class EncryptionTest extends FunSuite with Matchers{

  test("should encrypt string") {
    val key = "4"
    val value = "what's up dude"
    val encrypted = Encryption.encrypt(key,value)
    encrypted should be("01OSkj7YsdVrH3RgnzHwDg==")
    val decrypted = Encryption.decrypt(key,encrypted)
    value shouldBe decrypted
  }

}
