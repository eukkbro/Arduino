package abled.semina.arduino;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "시리얼 통신";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private UsbSerialPort serialPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button onButton = findViewById(R.id.onButton);
        Button offButton = findViewById(R.id.offButton);

        onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand("on");
            }
        });

        offButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand("off");
            }
        });

        // USB 권한 요청 리시버 등록
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);

        // USB 포트 연결 시도
        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (!availableDrivers.isEmpty()) {
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDevice device = driver.getDevice();
            if (!usbManager.hasPermission(device)) {
                PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(device, permissionIntent);
            } else {
                connectSerialPort(usbManager, driver);
            }
        } else {
            Log.d(TAG, "onCreate: 연결 가능한 USB 디바이스 없음");
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
                            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
                            if (!availableDrivers.isEmpty()) {
                                connectSerialPort(usbManager, availableDrivers.get(0));
                            }
                        }
                    } else {
                        Log.d(TAG, "USB 권한이 거부되었습니다.");
                    }
                }
            }
        }
    };

    private void connectSerialPort(UsbManager usbManager, UsbSerialDriver driver) {
        UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
        if (connection != null) {
            serialPort = driver.getPorts().get(0);
            try {
                serialPort.open(connection);
                serialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                Log.d(TAG, "시리얼 포트 연결 완료");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "시리얼 포트 연결 실패");
            }
        } else {
            Log.d(TAG, "시리얼 포트를 열 수 없습니다.");
        }
    }

    private void sendCommand(String command) {
        if (serialPort != null) {
            try {
                serialPort.write((command + "\n").getBytes(), 1000);
                Toast.makeText(MainActivity.this, command + " 명령 전송", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "명령 전송 실패", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "sendCommand: 시리얼 포트가 연결되지 않음");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (serialPort != null) {
                serialPort.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        unregisterReceiver(usbReceiver); // USB 권한 리시버 해제
    }
}
