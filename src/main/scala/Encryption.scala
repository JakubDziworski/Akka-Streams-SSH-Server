/**
  * Created by 'Jakub Dziworski' on 23.10.16
  */

import java.security.MessageDigest
import java.util
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64

/**
  * Sample:
  * {{{
  *   scala> val key = "My very own, very private key here!"
  *
  *   scala> Encryption.encrypt(key, "pula, pizda, coaiele!")
  *   res0: String = 9R2vVgkqEioSHyhvx5P05wpTiyha1MCI97gcq52GCn4=
  *
  *   scala> Encryption.decrypt(key", res0)
  *   res1: String = pula, pizda, coaiele!
  * }}}
  */
import java.security.MessageDigest
import java.util
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64

/**
  * Sample:
  * {{{
  *   scala> val key = "My very own, very private key here!"
  *
  *   scala> Encryption.encrypt(key, "pula, pizda, coaiele!")
  *   res0: String = 9R2vVgkqEioSHyhvx5P05wpTiyha1MCI97gcq52GCn4=
  *
  *   scala> Encryption.decrypt(key", res0)
  *   res1: String = pula, pizda, coaiele!
  * }}}
  */
object Encryption {
  def encrypt(key: String, value: String): String = {
    val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, keyToSpec(key))
    Base64.encodeBase64String(cipher.doFinal(value.getBytes("UTF-8")))
  }

  def decrypt(key: String, encryptedValue: String): String = {
    val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
    cipher.init(Cipher.DECRYPT_MODE, keyToSpec(key))
    new String(cipher.doFinal(Base64.decodeBase64(encryptedValue)))
  }

  def keyToSpec(key: String): SecretKeySpec = {
    var keyBytes: Array[Byte] = (SALT + key).getBytes("UTF-8")
    val sha: MessageDigest = MessageDigest.getInstance("SHA-1")
    keyBytes = sha.digest(keyBytes)
    keyBytes = util.Arrays.copyOf(keyBytes, 16)
    new SecretKeySpec(keyBytes, "AES")
  }

  private val SALT: String =
    "gfdgfdsgdfk5695i09jgfkln^72k"
}
