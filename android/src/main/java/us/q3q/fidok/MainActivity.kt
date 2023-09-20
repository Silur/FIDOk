package us.q3q.fidok

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import us.q3q.fidok.ctap.CTAPClient
import us.q3q.fidok.ctap.Device
import us.q3q.fidok.ctap.commands.GetInfoResponse
import us.q3q.fidok.ui.InfoDisplay
import us.q3q.fidok.ui.theme.FidoKTheme
import us.q3q.fidok.usb.ACTION_USB_PERMISSION
import us.q3q.fidok.usb.AndroidUSBHIDListing
import us.q3q.fidok.usb.usbPermissionIntentReceiver

class MainActivity : ComponentActivity() {
    var nfcAdapter: NfcAdapter? = null

    val techDiscoveredIntentFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
    val supportedTechClassNames = arrayOf(
        IsoDep::class.java.name,
    )

    var nfcPendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        Logger.setMinSeverity(Severity.Verbose)

        val usbPermissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbPermissionIntentReceiver, filter)

        val nfcIntent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        nfcPendingIntent = PendingIntent.getActivity(
            this, 0, nfcIntent,
            PendingIntent.FLAG_MUTABLE,
        )

        setContent {
            FidoKTheme {
                var deviceList by remember { mutableStateOf<List<Device>?>(null) }
                var info by remember { mutableStateOf<GetInfoResponse?>(null) }

                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column {
                        DeviceDisplayAndManip(
                            deviceList = deviceList,
                            onListUSBReq = {
                                deviceList = AndroidUSBHIDListing.listDevices(applicationContext, usbPermissionIntent)
                            },
                            getInfoReq = {
                                info = CTAPClient(it).getInfo()
                            },
                        )
                        InfoDisplay(info = info)
                    }
                }
            }
        }
    }

    private fun foregroundNfcDispatch() {
        val adapter = nfcAdapter
        if (nfcPendingIntent != null && adapter != null) {
            adapter.enableForegroundDispatch(
                this,
                nfcPendingIntent,
                arrayOf(techDiscoveredIntentFilter),
                arrayOf(supportedTechClassNames),
            )
        }
    }

    override fun onDestroy() {
        unregisterReceiver(usbPermissionIntentReceiver)
        super.onDestroy()
    }

    public override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    public override fun onResume() {
        super.onResume()
        foregroundNfcDispatch()
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        Logger.i { "Detected tag $tag" }
    }
}

@Composable
fun DeviceDisplayAndManip(
    deviceList: List<Device>?,
    onListUSBReq: () -> Unit = {},
    getInfoReq: (d: Device) -> Unit = {},
) {
    Column {
        Button(onClick = onListUSBReq) {
            Text("List USB Devices")
        }
        if (deviceList != null) {
            DevicesDisplay(deviceList, getInfoReq = getInfoReq)
        }
    }
}

@Composable
fun DevicesDisplay(devices: List<Device>, getInfoReq: (d: Device) -> Unit) {
    Column {
        Text("Found ${devices.size} devices")
        devices.map {
            DeviceDisplay(it, getInfoReq = {
                getInfoReq(it)
            })
        }
    }
}

@Composable
fun DeviceDisplay(device: Device, getInfoReq: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(device.toString(), modifier = Modifier.padding(3.dp))
        Button(onClick = getInfoReq) {
            Text("Get Info")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DevicesDisplayPreviewEmpty() {
    DeviceDisplayAndManip(null)
}

@Preview(showBackground = true)
@Composable
fun DevicesDisplayPreview() {
    DeviceDisplayAndManip(
        deviceList = listOf(
            object : Device {
                override fun sendBytes(bytes: ByteArray) = byteArrayOf()
                override fun toString(): String = "FirstDevice"
            },
            object : Device {
                override fun sendBytes(bytes: ByteArray) = byteArrayOf()
                override fun toString(): String = "SecondDevice"
            },
        ),
    )
}
