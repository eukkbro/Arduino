package abled.semina.arduino;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class StepperActivity extends AppCompatActivity {

    private final String TAG = "시리얼 통신";
    private UsbSerialPort serialPort;
    private Spinner spinnerDirection, spinnerSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stepper);

        spinnerDirection = findViewById(R.id.spinnerDirection);
        spinnerSpeed = findViewById(R.id.spinnerSpeed);
        Button btnSendCommand = findViewById(R.id.btnSendCommand);

        // 방향 선택 Spinner 설정
        ArrayAdapter<CharSequence> directionAdapter = ArrayAdapter.createFromResource(this,
                R.array.directions, android.R.layout.simple_spinner_item);
        directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDirection.setAdapter(directionAdapter);

        // 속도 선택 Spinner 설정
        ArrayAdapter<CharSequence> speedAdapter = ArrayAdapter.createFromResource(this,
                R.array.speeds, android.R.layout.simple_spinner_item);
        speedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeed.setAdapter(speedAdapter);

        // 시리얼 포트 연결
        connectToUsbDevice();

        // Send Command 버튼 클릭 시 명령 전송
        btnSendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String direction = spinnerDirection.getSelectedItem().toString();
                String speed = spinnerSpeed.getSelectedItem().toString();

                // 시리얼 포트로 데이터 전송
                sendCommand(direction, speed);
            }
        });
    }

    private void connectToUsbDevice() {
        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        if (!availableDrivers.isEmpty()) {
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());

            if (connection != null) {
                serialPort = driver.getPorts().get(0);
                try {
                    serialPort.open(connection);
                    serialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    Log.d(TAG, "USB device connected.");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Error opening serial port.");
                }
            }
        } else {
            Log.d(TAG, "No USB device found.");
        }
    }

    private void sendCommand(String direction, String speed) {
        if (serialPort != null) {

            String speedCommand = speedToNumber(speed);  // 속도: 0 = 느림, 1 = 중간, 2 = 빠름
            String directionCommand = direction.equals("Clockwise") ? "D0" : "D1";  // 방향: D0는 시계방향, D1은 반시계방향


            String command = speedCommand + "," + directionCommand;

            try {
                serialPort.write(command.getBytes(), 1000);  // 아두이노로 데이터 전송
                Toast.makeText(StepperActivity.this, "Command sent: " + command, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(StepperActivity.this, "Failed to send command", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String speedToNumber(String speed) {
        switch (speed) {
            case "Slow":
                return "0";
            case "Medium":
                return "1";
            case "Fast":
                return "2";
            default:
                return "1";  // 기본값: Medium
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
    }
}
