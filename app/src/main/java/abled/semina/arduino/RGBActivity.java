package abled.semina.arduino;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import java.io.IOException;
import java.util.List;

public class RGBActivity extends AppCompatActivity {

    private final String TAG = "시리얼 통신";
    private UsbSerialPort serialPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rgbactivity);

        EditText redInput = findViewById(R.id.redInput);
        EditText greenInput = findViewById(R.id.greenInput);
        EditText blueInput = findViewById(R.id.blueInput);
        Button sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String redText = redInput.getText().toString();
                String greenText = greenInput.getText().toString();
                String blueText = blueInput.getText().toString();

                if (!redText.isEmpty() && !greenText.isEmpty() && !blueText.isEmpty()) {
                    sendCommand(redText, greenText, blueText);
                } else {
                    Toast.makeText(RGBActivity.this, "모든 값이 입력되어야 합니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // USB 포트 연결 시도
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendCommand(String red, String green, String blue) {
        if (serialPort != null) {
            try {
                String command = red + " " + green + " " + blue + "\n";
                serialPort.write(command.getBytes(), 1000);
                Toast.makeText(RGBActivity.this, "RGB 값 전송", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(RGBActivity.this, "명령 전송 실패", Toast.LENGTH_SHORT).show();
            }
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
