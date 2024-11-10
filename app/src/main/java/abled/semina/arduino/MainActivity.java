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

        // 켜짐 버튼
        onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //아두이노에 on 명롱 전송
                sendCommand("on");
            }
        });


        //꺼짐 버튼
        offButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 아두이노에 off 명령 전송
                sendCommand("off");
            }
        });

        // USB 권한 요청 리시버 등록
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        //보안정책 강화로 인한 export 속성 지정
        registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);

        // USB 포트 연결 시도
        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        if (!availableDrivers.isEmpty()) { //연결가능한 usb 디바이스가 있으면
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDevice device = driver.getDevice();
            if (!usbManager.hasPermission(device)) {
                //usb 권한이 없으면 권한 요청
                PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(device, permissionIntent);
            } else {
                //권한이 있으면 시리얼포트 연결
                connectSerialPort(usbManager, driver);
            }
        } else {
            Log.d(TAG, "onCreate: 연결 가능한 USB 디바이스 없음");
        }
    }

    //USB 권한을 요청하고, 해당 장치와 시리얼 포트를 연결하는 브로드캐스트리시버
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        //권한이 요청되었을 때, 시리얼 포트 연결 시도
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


    //시리얼포트 연결 메서드
    private void connectSerialPort(UsbManager usbManager, UsbSerialDriver driver) {
        UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
        if (connection != null) {
            //연결에 성공하면 시리얼포트에 첫번째 포트를 저장
            serialPort = driver.getPorts().get(0);
            try {
                //시리얼 포트 열기
                serialPort.open(connection);
                //아두이노는 보통 9600보드레이트 전송속도
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


    //아두이노로 명령을 보내는 메서드
    private void sendCommand(String command) {
        if (serialPort != null) {
            try {
                //String을 받아서 포트에 write로 보내기.
                //getBytes를 통해 바이트형태로 변환해서 전송하는 이유?
                //시리얼통신은 문자열을 그대로 전송할 수 없고 반드시 바이트 형식으로 전송해야 되게 때문
                serialPort.write((command + "\n").getBytes(), 1000);
                Toast.makeText(MainActivity.this, command + " 명령 전송", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "명령 전송 실패", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "sendCommand: 시리얼 포트가 연결되지 않음");
        }
    } //sendCommand()

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //액티비티 파괴되는 시점에서 시리얼포트와 리시버 정리
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
