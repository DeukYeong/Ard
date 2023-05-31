package kr.co.iksung.hce_example;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.cardemulation.HostApduService;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;




public class Hce extends HostApduService {
    private static final String TAG = "IS HostApduService";

    public int Token = 0;

    private static final byte[] APDU_SELECT = {
            (byte)0x00, // CLA	- Class - Class of instruction
            (byte)0xA4, // INS	- Instruction - Instruction code
            (byte)0x04, // P1	- Parameter 1 - Instruction parameter 1
            (byte)0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte)0x07, // Lc field	- Number of bytes present in the data field of the command
            (byte)0xFF, (byte)0x69, (byte)0x6B, (byte)0x73, (byte)0x75, (byte)0x6E, (byte)0x67, // NDEF Tag Application name

    };

    private static final byte[] A_OKAY = {
            (byte)0x90,  // SW1	Status byte 1 - Command processing status
            (byte)0x00   // SW2	Status byte 2 - Command processing qualifier
    };
    private static final byte[] A_ERROR = {
            (byte)0x6A,  // SW1	Status byte 1 - Command processing status
            (byte)0x82   // SW2	Status byte 2 - Command processing qualifier
    };
    private byte[] gTagValueBytes;
    private byte[] gAidNameBytes;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.hasExtra("tagValue")) {
            gTagValueBytes = utils.stringToBytes(intent.getStringExtra("tagValue"));
            //gTagValueBytes = utils.decimalStringToByteArray(intent.getStringExtra("tagValue"));
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras)  {



        if (commandApdu == null) {
            return A_ERROR;
        }

        if (gTagValueBytes != null)
        {
            if (utils.isEqual(APDU_SELECT, commandApdu)) {
                byte array[];
                if (gTagValueBytes != null)
                    array = new byte[gTagValueBytes.length + 2];
                else
                    array = new byte[2];
                if (array.length > 2) {
                    System.arraycopy(gTagValueBytes, 0, array, 0, gTagValueBytes.length);
                    System.arraycopy(A_OKAY, 0, array, gTagValueBytes.length, 2);
                } else {
                    array[0] = A_OKAY[0];
                    array[1] = A_OKAY[1];
                }

                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                MediaPlayer player = MediaPlayer.create(getApplicationContext(), R.raw.buzz);



                // 1초 진동
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    Toast.makeText(getApplicationContext(),

                                    "승인요청 되었습니다.", Toast.LENGTH_SHORT)
                            .show();
                    player.start();//buzz

                    Token = 1;
                } else {
                    vibrator.vibrate(500);
                }
                getToken();
                Intent intent = new Intent("hce_service_stopped");
                sendBroadcast(intent);
                return array;
            }
        }

        return A_ERROR;
    }



    @Override
    public void onDeactivated(int reason) {
        gTagValueBytes = null;

    }

    public int getToken() {
        Log.d("Token", "onGetTokenCompleted: " + Token);
        return Token;
    }

    public void setToken(int token) {
        Token = token;
    }
}














































