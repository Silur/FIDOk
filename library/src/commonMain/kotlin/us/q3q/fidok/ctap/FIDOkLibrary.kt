package us.q3q.fidok.ctap

import co.touchlab.kermit.Logger
import us.q3q.fidok.cable.CaBLESupport
import us.q3q.fidok.crypto.CryptoProvider
import us.q3q.fidok.webauthn.WebauthnClient
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

class FIDOkLibrary private constructor(
    val cryptoProvider: CryptoProvider,
    private val authenticatorAccessors: List<AuthenticatorListing>,
    private val defaultPinCollectionFromUser: suspend(client: CTAPClient) -> String? = { null },
) {

    companion object {
        @JvmStatic
        @JvmOverloads
        fun init(
            cryptoProvider: CryptoProvider,
            authenticatorAccessors: List<AuthenticatorListing> = listOf(),
            pinCollection: suspend (client: CTAPClient) -> String? = { null },
        ): FIDOkLibrary {
            Logger.d { "Initializing FIDOk library using ${authenticatorAccessors.size} types of Authenticator accessor" }
            return FIDOkLibrary(
                cryptoProvider,
                authenticatorAccessors,
                defaultPinCollectionFromUser = pinCollection,
            )
        }
    }

    fun listDevices(allowedTransports: List<AuthenticatorTransport>? = null): Array<AuthenticatorDevice> {
        val devices = arrayListOf<AuthenticatorDevice>()
        for (accessor in authenticatorAccessors) {
            devices.addAll(
                accessor.listDevices().filter {
                    if (allowedTransports == null) {
                        true
                    } else {
                        var match = false
                        for (transport in it.getTransports()) {
                            if (allowedTransports.contains(transport)) {
                                match = true
                                break
                            }
                        }
                        match
                    }
                },
            )
        }
        return devices.toTypedArray()
    }

    fun caBLESupport(): CaBLESupport {
        return CaBLESupport(this)
    }

    fun ctapClient(device: AuthenticatorDevice, collectPinFromUser: suspend (device: CTAPClient) -> String? = defaultPinCollectionFromUser): CTAPClient {
        return CTAPClient(this, device, collectPinFromUser)
    }

    fun webauthn(): WebauthnClient {
        return WebauthnClient(this)
    }
}
