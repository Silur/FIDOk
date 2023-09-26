package us.q3q.fidok.ctap.commands

import us.q3q.fidok.crypto.KeyAgreementPlatformKey
import us.q3q.fidok.crypto.PinUVProtocol

/**
 * An extension to store arbitrary data alongside a credential.
 *
 * After creating a credential, check [wasCreated] - if it returns true, the blob was stored.
 * After getting an assertion, call [getBlob] to retrieve a previously-stored blob.
 *
 * @param blobToStore Blob to associated with newly created credentials
 */
class CredBlobExtension(private val blobToStore: ByteArray? = null) : Extension {

    private val NAME = "credBlob"

    init {
        ExtensionSetup.register(
            NAME,
            creationParameterDeserializer = BooleanExtensionParameter.serializer(),
            assertionParameterDeserializer = ByteArrayExtensionParameter.serializer(),
        )
    }

    private var created: Boolean = false
    private var credBlob: ByteArray? = null

    fun wasCreated(): Boolean = created

    fun getBlob(): ByteArray? = credBlob

    override fun getName(): ExtensionName {
        return NAME
    }

    override fun makeCredential(keyAgreement: KeyAgreementPlatformKey?, pinUVProtocol: PinUVProtocol?): ExtensionParameters? {
        if (blobToStore != null) {
            return ByteArrayExtensionParameter(blobToStore)
        }
        return null
    }

    override fun makeCredentialResponse(response: MakeCredentialResponse) {
        val gotten = response.authData.extensions?.get(getName())
        created = (gotten as BooleanExtensionParameter).v
    }

    override fun getAssertion(keyAgreement: KeyAgreementPlatformKey?, pinUVProtocol: PinUVProtocol?): ExtensionParameters {
        return BooleanExtensionParameter(true)
    }

    override fun getAssertionResponse(response: GetAssertionResponse) {
        val gotten = response.authData.extensions?.get(getName())
        credBlob = (gotten as ByteArrayExtensionParameter).v
    }

    override fun checkSupport(info: GetInfoResponse): Boolean {
        if (!super.checkSupport(info)) {
            return false
        }
        if (info.extensions?.contains("credProtect") != true) {
            // credBlob requires credProtect
            return false
        }
        if (blobToStore != null && info.maxCredBlobLength != null &&
            info.maxCredBlobLength < blobToStore.size.toUInt()
        ) {
            // Too long
            return false
        }
        return true
    }
}
