import co.touchlab.kermit.Logger
import us.q3q.fidok.BotanCryptoProvider
import us.q3q.fidok.LibHIDDevice
import us.q3q.fidok.LibPCSCLiteDevice
import us.q3q.fidok.ctap.CTAPClient
import us.q3q.fidok.ctap.Library
import us.q3q.fidok.ctap.commands.CredProtectExtension
import us.q3q.fidok.ctap.commands.ExtensionSetup
import us.q3q.fidok.ctap.commands.HMACSecretExtension
import us.q3q.fidok.ctap.commands.PublicKeyCredentialDescriptor
import us.q3q.fidok.ctap.commands.UVMExtension
import kotlin.random.Random

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val devices = LibHIDDevice.list() + LibPCSCLiteDevice.list()
    if (devices.isEmpty()) {
        Logger.e("No devices found!")
        return
    }

    Library.init(BotanCryptoProvider())

    val device = devices[0]
    val client = CTAPClient(device)

    val credProtect = CredProtectExtension(2u)
    val hmacSecret = HMACSecretExtension(Random.Default.nextBytes(32))
    val uvm = UVMExtension()
    val extensions = ExtensionSetup(
        listOf(
            credProtect,
            hmacSecret,
            uvm,
        ),
    )
    val RPID = "something.cool.example"
    val res = client.makeCredential(
        rpId = RPID,
        userDisplayName = "Bob",
        extensions = extensions,
    )
    println("RESULT: $res")
    println("credRes: ${credProtect.getLevel()}")
    println("hmacSecret: ${hmacSecret.wasCreated()}")
    println("uvm: ${uvm.getUVMEntries()}")

    val assertions = client.getAssertions(
        Random.nextBytes(32),
        rpId = RPID,
        allowList = listOf(
            PublicKeyCredentialDescriptor(id = res.authData.attestedCredentialData!!.credentialId),
        ),
    )
    println("Got assertions: $assertions")
}
