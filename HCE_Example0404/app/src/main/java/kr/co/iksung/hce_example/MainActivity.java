package kr.co.iksung.hce_example;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {


    private BroadcastReceiver mReceiver;


    private static final int PERMISSIONS_REQUEST_CODE = 22;
    private Context mContext = null;
    private ImageButton buttonNfcPlaintextScan = null;

    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 10;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private ProgressDialog customProgressDialog;

    int page;


    private String PhoneNumber;
    private byte[] bTrnValue = null;
    private String sTagValue;
    private NfcAdapter mNfcAdapter;



    @Override
    public void onResume(){
        super.onResume();
        //TODO [NFC 지원 여부 및 활성 상태 확인 >> 서비스 호출]
        try {
            // [0 = NFC 지원안하는 기기 / 1 = NFC 지원 및 기능 비활성 상태 / 2 = NFC 지원 및 기능 활성 상태]
            if(getNfcEnable() == 2){
                //TODO [NFC HCE 통신 지원 서비스 호출]
                Intent A_Nfc_Service = new Intent(getApplication(), Hce.class);
                A_Nfc_Service.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startService(A_Nfc_Service);
                overridePendingTransition(0,0);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    /** [NFC 활성 상태 확인 코드] **/
    public int getNfcEnable(){


        int result = 0;
        // NFC 활성 상태 확인 실시
        try {
            NfcAdapter nfcAdapter = null;
            nfcAdapter = NfcAdapter.getDefaultAdapter(getApplication());
            if(nfcAdapter == null){ //TODO NFC를 지원하지 않는 기기인지 확인
                result = 0;
                Toast.makeText(getApplicationContext(), "NFC를 지원하지 않는 단말기입니다.", Toast.LENGTH_SHORT).show();
                //TODO [Alert 팝업창 알림 실시]
                //getAlertDialog("[알림]",
                  //      "NFC 기능을 지원하지 않는 단말기입니다.",
                    //    "확인", "취소", "");
            }
            else { //TODO NFC가 켜져있는지 확인 [NFC 지원 기기]
                if(nfcAdapter.isEnabled() == true){
                    Log.d("","\n"+"[A_Nfc > NFC 기능 활성 확인 : 활성 상태]");
                    result = 2;
                }
                else {
                    Log.d("","\n"+"[A_Nfc > NFC 기능 활성 확인 : 비활성 상태]");
                    result = 1;
                    //TODO [Alert 팝업창 알림 실시]
                    //getAlertDialog("[알림]",
                      //      "NFC 기능이 비활성 상태입니다.\nNFC 기본 모드를 활성화해주세요.",
                        //    "설정", "취소", "");
                    Toast.makeText(getApplicationContext(), "NFC를 기본모드로 활성화해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        // 결과 리턴 실시
        return result;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mContext = this;

        mNfcAdapter =  NfcAdapter.getDefaultAdapter(this) ;

        TelephonyManager tm = (TelephonyManager)  getSystemService(Context.TELEPHONY_SERVICE);

        if (chkPermission())    {
            //  휴대폰 정보는 TelephonyManager    를 이용

            //  READ_PHONE_NUMBERS 또는 READ_PHONE_STATE 권한을 허가 받았는지 확인
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        PhoneNumber = tm.getLine1Number();
        Log.i("DCCSDK", "PHONENUM : " + PhoneNumber);
        sTagValue =  PhoneNumber.replace("+82", "0");
        String uuid = UUID.randomUUID().toString();
        Log.d("TAG", "UUID >> " + uuid);


        //로딩창 객체 생성
        customProgressDialog = new kr.co.iksung.hce_example.ProgressDialog(this);



        buttonNfcPlaintextScan = (ImageButton) findViewById(R.id.buttonNfcPlaintextScan);
        buttonNfcPlaintextScan.setOnClickListener(mClickListener);

        page=0;
        /*
         * TMobilePass BLE Crypto Service Implementation END
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Marshmallow+ Permission APIs
            MarshMallow();
        }


        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // HCE service 종료 시 Toast 메시지 띄우기
                dismissProgressDialog();
            }
        };
        IntentFilter filter = new IntentFilter("hce_service_stopped");
        registerReceiver(mReceiver, filter);




    }


    Button.OnClickListener mClickListener = new View.OnClickListener() {



        public void onClick(View v) {

            //Hce hceInstance = new Hce();

            switch (v.getId()) {

                case R.id.buttonNfcPlaintextScan:

                    Intent intent = new Intent(MainActivity.this, Hce.class);
                    intent.putExtra("tagValue", sTagValue);
                    startService(intent);
                    //Log.d("Data", sTagValue);

                    buttonNfcPlaintextScan.setEnabled(false);
                    Log.d("MainActivity", "===> buttonNfcPlaintextScan!");
                    Toast.makeText(getApplicationContext(),

                                    "NFC 태그 스캔이 준비되었습니다.", Toast.LENGTH_SHORT)
                            .show();
                    // 로딩창 보여주기
                    customProgressDialog = new ProgressDialog(MainActivity.this);
                    customProgressDialog.setCancelable(false);
                    customProgressDialog.show();



//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            customProgressDialog.setCancelable(true);
//                            buttonNfcPlaintextScan.setEnabled(true);
//                            customProgressDialog.dismiss();
//                        }
//                    }, 5000);

            }


        }
    };

    public void dismissProgressDialog() {
        customProgressDialog.setCancelable(true);
        buttonNfcPlaintextScan.setEnabled(true);
        customProgressDialog.dismiss();

    }






    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void MarshMallow() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();

        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("ACCESS_FINE_LOCATION");
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION))
            permissionsNeeded.add("ACCESS_COARSE_LOCATION");
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            permissionsNeeded.add("ACCESS_BACKGROUND_LOCATION");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    @TargetApi(Build.VERSION_CODES.M)
    private boolean addPermission(List<String> permissionsList, String permission) {

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }

    public boolean chkPermission() {
        // 위험 권한을 모두 승인했는지 여부
        boolean mPermissionsGranted = false;
        String[] mRequiredPermissions = new String[1];
        // 승인 받기 위한 권한 목록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mRequiredPermissions[0] = Manifest.permission.READ_PHONE_NUMBERS;

        }else{
            mRequiredPermissions[0] = Manifest.permission.READ_PRECISE_PHONE_STATE;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 필수 권한을 가지고 있는지 확인한다.
            mPermissionsGranted = hasPermissions(mRequiredPermissions);

            // 필수 권한 중에 한 개라도 없는 경우
            if (!mPermissionsGranted) {
                // 권한을 요청한다.
                ActivityCompat.requestPermissions(MainActivity.this, mRequiredPermissions, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            mPermissionsGranted = true;
        }

        return mPermissionsGranted;
    }


    public boolean hasPermissions(String[] permissions) {
        // 필수 권한을 가지고 있는지 확인한다.
        for (String permission : permissions) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            // 권한을 모두 승인했는지 여부
            boolean chkFlag = false;
            // 승인한 권한은 0 값, 승인 안한 권한은 -1을 값으로 가진다.
            for (int g : grantResults) {
                if (g == -1) {
                    chkFlag = true;
                    break;
                }
            }

            // 권한 중 한 개라도 승인 안 한 경우
            if (chkFlag){
                chkPermission();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // BroadcastReceiver 등록 해제
        unregisterReceiver(mReceiver);
    }


    @Override
    public void onBackPressed() {
        //biometricPrompt.cancelAuthentication();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("종료하시겠습니까?");
        builder.setCancelable(false);   // 다이얼로그 화면 밖 터치 방지
        builder.setPositiveButton("종료", new AlertDialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                exit();
            }
        });

        builder.setNegativeButton("아니요", new AlertDialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //biometricPrompt.authenticate(promptInfo);
            }
        });
        builder.show();
    }

    public void exit() { // 종료
        super.onBackPressed();
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }



}