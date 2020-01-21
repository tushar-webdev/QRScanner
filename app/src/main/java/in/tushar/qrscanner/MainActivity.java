package in.tushar.qrscanner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.blikoon.qrcodescanner.QrCodeActivity;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_QR_SCAN = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String LOGTAG = "-----LOG-------";
    ConstraintLayout layout;
    private RequestQueue queue;
    StringRequest stringRequest;
    ImageButton setting;
    String url, address;
    LayoutInflater layoutInflater;
    final Context context = this;
    View promptView;
    AlertDialog.Builder alert;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    public static final String MyPREFERENCES = "MyPrefs";
    public static final String addressKey = "addressKey";
    DialogInterface dia;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.layout);
        queue = Volley.newRequestQueue(this);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        setting = findViewById(R.id.setting);
        address = sharedpreferences.getString(addressKey, "");
        Log.e(LOGTAG, "shared data " + address);
        if (address.isEmpty()) {
            setIpAddress();
        } else {
           openCamera();
        }
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setIpAddress();
            }
        });
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (address.isEmpty()) {
                    setIpAddress();
                } else {
                    openCamera();
                }
            }
        });
        if (checkPermission()) {
            //main logic or main code

            // . write your main code to execute, It will execute if the permission is already given.
            if (address.isEmpty()) {
                setIpAddress();
            } else {
               openCamera();
            }
        } else {
            requestPermission();
        }

    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            Log.d(LOGTAG, "COULD NOT GET A GOOD RESULT.");
            if (data == null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if (result != null) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Scan Error");
                alertDialog.setMessage("QR Code could not be scanned");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            return;

        }
        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (data == null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            Log.d(LOGTAG, "Have scan result in your app activity :" + result);
            sendData(result.toString().trim());
            final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Scan result");
            alertDialog.setMessage(result);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dia=dialog;
                            dialog.dismiss();
                        }
                    });
            if (result != null) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openCamera();
                    }
                }, 2000);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        alertDialog.dismiss();
                    }
                }, 1000);
            }
            alertDialog.show();

        }
    }

    public void sendData(final String QrResponse) {
        url = "http://" + address + "/api/qrdata";
        try {
            stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.e(LOGTAG, "Post Response : " + response);
                    Toast.makeText(MainActivity.this, "Response : " + response, Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(LOGTAG, "Volley Error : " + error);
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> MyData = new HashMap<String, String>();
                    MyData.put("qrData", QrResponse);
                    Log.i("sending ", MyData.toString());
                    return MyData;
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Content-Type", "application/x-www-form-urlencoded");
                    return params;
                }
            };
            // Add the realibility on the connection.
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
            queue.add(stringRequest);
            Log.e(LOGTAG, "In Try request");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setIpAddress() {
//        if(address.isEmpty()){

        Log.e(LOGTAG, "Empty Dailog box");
//            handler.removeCallbacksAndMessages(null);
        layoutInflater = LayoutInflater.from(context);
        promptView = layoutInflater.inflate(R.layout.prompt, null);
        alert = new AlertDialog.Builder(context);

        alert.setView(promptView);
        final EditText userInput = (EditText) promptView.findViewById(R.id.editTextDialogUserInput);
        if (address.isEmpty()) {
            Log.e(LOGTAG,"SET IP ADDRESS.....");
            userInput.setText("192.168.0.157:8125");
        } else {
            userInput.setText(address);
        }
        alert.setCancelable(false).setPositiveButton("save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                address = userInput.getText().toString();
                Log.e(LOGTAG, "First user Input" + userInput.getText());
                Toast.makeText(context, "Address : " + address, Toast.LENGTH_SHORT).show();
                editor = sharedpreferences.edit();
                editor.putString(addressKey, address);
                editor.commit();
            }
        }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        // create alert dialog
        AlertDialog alertDialog = alert.create();

        // show it
        alertDialog.show();
//        }else{
//            Log.e(LOGTAG,"address=> "+address);
//        }
    }
    public void openCamera(){
        Intent i = new Intent(MainActivity.this, QrCodeActivity.class);
        startActivityForResult(i, REQUEST_CODE_QR_SCAN);
    }
}

