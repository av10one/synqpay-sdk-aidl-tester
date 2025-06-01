package com.synqpay.sdkdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.synqpay.sdk.ResponseCallback;
import com.synqpay.sdk.SynqpayAPI;
import com.synqpay.sdk.SynqpayManager;
import com.synqpay.sdk.SynqpayPAL;
import com.synqpay.sdk.SynqpayPrinter;
import com.synqpay.sdk.SynqpaySDK;
import com.synqpay.sdk.SynqpayStartupNotifier;
import com.synqpay.sdk.pal.IDocument;
import com.synqpay.sdk.pal.ILine;
import com.synqpay.sdk.pal.ImageFrame;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SynqpaySDK.ConnectionListener {

    private SynqpayAPI api;
    private SynqpayManager manager;
    private SynqpayPrinter printer;
    private SynqpayStartupNotifier startupNotifier;

    private TextView tvBindStatus;
    private TextView tvApiEnabled;
    private CheckBox cbNotifyUpdate;
    private Button btnDeposit; // Field for the Deposit button
    private Button btnGetBatchFileStatus; // Field for the Get Batch File Status button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startupNotifier = new SynqpayStartupNotifier();
        SynqpaySDK.get().init(this);
        SynqpaySDK.get().setListener(this);


        tvBindStatus = findViewById(R.id.text_synqpay_status);
        tvApiEnabled = findViewById(R.id.text_api_enabled);
        cbNotifyUpdate = findViewById(R.id.checkbox_notify_update);

        Button btnGetTerminalStatus = findViewById(R.id.button_getTerminalStatus);
        btnGetTerminalStatus.setOnClickListener(v ->
                sendRequest(getTerminalStatusRequest(),getStatusListener));

        Button btnSettlement = findViewById(R.id.button_settlement);
        btnSettlement.setOnClickListener(v ->
                sendRequest(settlementRequest(),getStatusListener));

        Button btnStartTransaction = findViewById(R.id.button_startTransaction);
        // Modified to call the dialog first
        btnStartTransaction.setOnClickListener(v -> showReferenceIdInputDialog());

        Button btnContinueTransaction = findViewById(R.id.button_continueTransaction);
        btnContinueTransaction.setOnClickListener(v ->
                sendRequest(getContinueTransactionRequest(),startTransactionListener));


        Button btnRestart = findViewById(R.id.button_restart);
        btnRestart.setOnClickListener(v -> restartSynqpay());

        Button btnPrint = findViewById(R.id.button_print);
        btnPrint.setOnClickListener(v -> print());

        // Find the Deposit button and set its OnClickListener
        btnDeposit = findViewById(R.id.button_deposit);
        btnDeposit.setOnClickListener(v ->
                sendRequest(depositRequest(), depositListener));

        // Find the Get Batch File Status button and set its OnClickListener
        btnGetBatchFileStatus = findViewById(R.id.button_get_batch_file_status);
        btnGetBatchFileStatus.setOnClickListener(v ->
                sendRequest(getBatchFileStatusRequest(), getBatchFileStatusListener));
    }

    @Override
    protected void onStart() {
        super.onStart();
        SynqpaySDK.get().bindService();
        startupNotifier.start(this, () ->
                Toast.makeText(this,"Synqpay Started",Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.manager != null)
            SynqpaySDK.get().unbindService();
        startupNotifier.stop(this);
    }

    private void showReferenceIdInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reference_id, null);
        builder.setView(dialogView);

        RadioGroup rgReferenceIdOptions = dialogView.findViewById(R.id.rg_reference_id_options);
        RadioButton rbGenerateUuid = dialogView.findViewById(R.id.rb_generate_uuid);
        RadioButton rbEnterManually = dialogView.findViewById(R.id.rb_enter_manually);
        EditText etManualReferenceId = dialogView.findViewById(R.id.et_manual_reference_id);

        rgReferenceIdOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_enter_manually) {
                etManualReferenceId.setVisibility(View.VISIBLE);
            } else {
                etManualReferenceId.setVisibility(View.GONE);
            }
        });

        builder.setTitle("Set Transaction Reference ID");
        builder.setPositiveButton("OK", null); // Will be overridden
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button listener
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String referenceId = "";
            // Re-find views from dialogView to ensure they are correctly scoped for the listener
            RadioButton rbGenerate = dialogView.findViewById(R.id.rb_generate_uuid);
            EditText etManual = dialogView.findViewById(R.id.et_manual_reference_id);

            if (rbGenerate.isChecked()) {
                referenceId = UUID.randomUUID().toString();
                // Toast.makeText(MainActivity.this, "Using UUID: " + referenceId, Toast.LENGTH_SHORT).show(); // Removed
            } else {
                referenceId = etManual.getText().toString().trim();
                if (referenceId.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Manual Reference ID cannot be empty", Toast.LENGTH_SHORT).show();
                    return; // Don't dismiss if manual is chosen but empty
                }
                // Toast.makeText(MainActivity.this, "Using Manual ID: " + referenceId, Toast.LENGTH_SHORT).show(); // Removed
            }

            String transactionRequest = getStartTransactionRequest(referenceId);
            sendRequest(transactionRequest, startTransactionListener);
            dialog.dismiss();
        });
    }

    private void sendRequest(String request, ResponseCallback responseCallback) {
        if (this.api != null) {
            try {
                Log.i("DEMO"," => "+request);
                api.sendRequest(request,responseCallback);
            } catch (RemoteException ignored) {

            }
        }
    }

    private void restartSynqpay() {
        try {
            manager.restartSynqpay();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }


    private String getTerminalStatusRequest() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.
                    put("jsonrpc","2.0").
                    put("id","1234").
                    put("method","getTerminalStatus").
                    put("params",null);
        } catch (JSONException e) {
            return "";
        }
        return jsonObject.toString();
    }

    // Method to create the JSON request for getBatchFileStatus
    private String getBatchFileStatusRequest() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.
                    put("jsonrpc", "2.0").
                    put("id", "1234"). // Using a static ID as in other requests
                    put("method", "getBatchFileStatus");
            // No "params" needed for this request
        } catch (JSONException e) {
            Log.e("DEMO", "Error creating getBatchFileStatus request JSON", e);
            return ""; // Return empty string on error
        }
        return jsonObject.toString();
    }

    private String settlementRequest() {
        JSONObject jsonObject = new JSONObject();
        JSONObject params = new JSONObject();
        try {
            params.put("host","SHVA");
            jsonObject.
                    put("jsonrpc","2.0").
                    put("id","1234").
                    put("method","settlement").
                    put("params",params);
        } catch (JSONException e) {
            return "";
        }
        return jsonObject.toString();
    }

    // Method to create the JSON request for deposit
    // TODO: Modify getStartTransactionRequest to accept a referenceId parameter
    // For now, the existing getStartTransactionRequest will be used by the dialog's OK button placeholder.
    private String depositRequest() {
        JSONObject jsonObject = new JSONObject();
        JSONObject params = new JSONObject();
        try {
            params.put("host", "SHVA");
            jsonObject.
                    put("jsonrpc", "2.0").
                    put("id", "1234"). // Using a static ID as in other requests
                    put("method", "deposit").
                    put("params", params);
        } catch (JSONException e) {
            Log.e("DEMO", "Error creating deposit request JSON", e);
            return ""; // Return empty string on error, consistent with other methods
        }
        return jsonObject.toString();
    }

    private String getStartTransactionRequest(String referenceId) { // Signature changed
        JSONObject jsonObject = new JSONObject();
        JSONObject params = new JSONObject();
        try {
            params
                    .put("paymentMethod","CREDIT_CARD")
                    .put("transactionType","SALE")
                    .put("referenceId", referenceId) // Use the parameter here
                    .put("amount",1000)
                    .put("currency",376)
                    .put("notifyUpdate",cbNotifyUpdate.isChecked())
            ;

            jsonObject.
                    put("jsonrpc","2.0").
                    put("id","1234").
                    put("method","startTransaction").
                    put("params",params);
        } catch (JSONException e) {
            return "";
        }
        return jsonObject.toString();
    }


    private String getContinueTransactionRequest() {
        JSONObject jsonObject = new JSONObject();
        JSONObject params = new JSONObject();
        try {
            params
                    .put("creditTerms","REGULAR")
            ;

            jsonObject.
                    put("jsonrpc","2.0").
                    put("id","1234").
                    put("method","continueTransaction").
                    put("params",params);
        } catch (JSONException e) {
            return "";
        }
        return jsonObject.toString();
    }

    private final ResponseCallback.Stub getStatusListener = new ResponseCallback.Stub() {
        @Override
        public void onResponse(String response) {
            Log.i("DEMO"," <= "+response);
            String terminalId,status;
            try {
                JSONObject jsonResponse = new JSONObject(response);
                JSONObject jsonResult = jsonResponse.optJSONObject("result");
                if (jsonResult == null)
                    return;
                terminalId = jsonResult.optString("terminalId","");
                status = jsonResult.optString("status","");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            MainActivity.this.runOnUiThread(()
                    -> Toast.makeText(
                    MainActivity.this,terminalId+" :"+status,Toast.LENGTH_LONG).show());
        }
    };

    private final ResponseCallback.Stub startTransactionListener = new ResponseCallback.Stub() {
        @Override
        public void onResponse(String response) {
            Log.i("DEMO"," <= "+response);
            String terminalId, resultText; // Renamed 'result' to 'resultText' to avoid confusion
            try {
                JSONObject jsonResponse = new JSONObject(response);

                // Check for error first
                JSONObject errorObject = jsonResponse.optJSONObject("error");
                if (errorObject != null) {
                    int errorCode = errorObject.optInt("code");
                    String errorMessage = errorObject.optString("message");
                    if (errorCode == 303 && "Transaction Already Exist".equals(errorMessage)) {
                        MainActivity.this.runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Use a unique ID", Toast.LENGTH_LONG).show());
                        return; // Stop further processing
                    }
                }

                JSONObject jsonResult = jsonResponse.optJSONObject("result");
                if (jsonResult == null) { // If no result and no specific error handled above, show generic message or log
                    MainActivity.this.runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Transaction Status: No result data", Toast.LENGTH_LONG).show());
                    return;
                }
                terminalId = jsonResult.optString("terminalId","");
                resultText = jsonResult.optString("transactionStatus","");
            } catch (JSONException e) {
                Log.e("DEMO", "Error parsing startTransaction response JSON", e);
                MainActivity.this.runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error parsing transaction response.", Toast.LENGTH_LONG).show());
                return; // Stop on parsing error
            }
            // Ensure resultText is not null or empty before showing to avoid "Terminal ID: " with no status
            final String finalResultText = resultText == null || resultText.isEmpty() ? "Unknown status" : resultText;
            final String finalTerminalId = terminalId == null || terminalId.isEmpty() ? "N/A" : terminalId;

            MainActivity.this.runOnUiThread(()
                    -> Toast.makeText(
                    MainActivity.this,finalTerminalId+" :"+finalResultText,Toast.LENGTH_LONG).show());
        }
    };

    // ResponseCallback for the deposit request
    private final ResponseCallback.Stub depositListener = new ResponseCallback.Stub() {
        @Override
        public void onResponse(String response) {
            Log.i("DEMO", " <= " + response); // Log the raw response
            String depositStatus = "Error"; // Default status in case of parsing failure
            try {
                JSONObject jsonResponse = new JSONObject(response);
                JSONObject jsonResult = jsonResponse.optJSONObject("result");
                if (jsonResult != null) {
                    depositStatus = jsonResult.optString("result", "Unknown Status");
                    // Optionally, parse result.deposit for more details if result.result is "OK"
                    // For now, just displaying the overall status.
                }
            } catch (JSONException e) {
                Log.e("DEMO", "Error parsing deposit response JSON", e);
                // depositStatus remains "Error" or "Unknown Status"
            }
            final String finalDepositStatus = depositStatus;
            MainActivity.this.runOnUiThread(() ->
                    Toast.makeText(
                            MainActivity.this, "Deposit Result: " + finalDepositStatus, Toast.LENGTH_LONG).show());
        }
    };

    // ResponseCallback for the getBatchFileStatus request
    private final ResponseCallback.Stub getBatchFileStatusListener = new ResponseCallback.Stub() {
        @Override
        public void onResponse(String response) {
            Log.i("DEMO", " <= " + response); // Log the raw response
            String terminalId = "N/A";
            int batchFileCount = 0;
            String message;

            try {
                JSONObject jsonResponse = new JSONObject(response);
                JSONObject jsonResult = jsonResponse.optJSONObject("result");
                if (jsonResult != null) {
                    JSONObject mainTerminal = jsonResult.optJSONObject("mainTerminal");
                    if (mainTerminal != null) {
                        terminalId = mainTerminal.optString("terminalId", "N/A");
                        org.json.JSONArray batchFile = mainTerminal.optJSONArray("batchFile");
                        if (batchFile != null) {
                            batchFileCount = batchFile.length();
                        }
                    }
                    message = "Terminal ID: " + terminalId + ", Batch Transactions: " + batchFileCount;
                } else {
                    // Check for error object if result is null
                    JSONObject error = jsonResponse.optJSONObject("error");
                    if (error != null) {
                        String errorMessage = error.optString("message", "Unknown error");
                        message = "Error: " + errorMessage;
                    } else {
                        message = "Error: Invalid response structure";
                    }
                }
            } catch (JSONException e) {
                Log.e("DEMO", "Error parsing getBatchFileStatus response JSON", e);
                message = "Error parsing response.";
            }

            final String finalMessage = message;
            MainActivity.this.runOnUiThread(() ->
                    Toast.makeText(
                            MainActivity.this, finalMessage, Toast.LENGTH_LONG).show());
        }
    };

    private void print() {

        IDocument document = SynqpayPAL.newDocument();
        document.addImage()
                .align(SynqpayPAL.Align.CENTER)
                .image(new ImageFrame(
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.mcdonalds), 200,200,false)))
                .spaceBottom(20);

        ILine line1 = document.addLine();
        line1.addText()
                .text("Hello");
        line1.addText().text("World!");

        document.addSpace(30);

        document.addLine().addText().text("Fill Last Mode").bold(true);
        document.addDivider(4);
        ILine line2 = document.addLine();
        line2.fillLast(true);
        line2.addText().text("Hello");
        line2.addText().text("World").align(SynqpayPAL.Align.END);

        document.addSpace(30);

        document.addLine().addText().text("Weight Mode").bold(true);
        document.addDivider(4);
        ILine line3 = document.addLine();
        line3.addText().text("Hello").weight(1);
        line3.addText().text("World").weight(1);
        ILine line4 = document.addLine();
        line4.addText().text("Another").weight(1);
        line4.addText().text("Text of line").weight(1);

        document.addSpace(10);

        ILine line5 = document.addLine();
        line5.addText().text("ITEM").weight(6);
        line5.addText().text("QTY").weight(2);
        line5.addText().text("PRICE").weight(3).align(SynqpayPAL.Align.END);

        ILine line6 = document.addLine();
        line6.addText().text("Apples").weight(6);
        line6.addText().text("1kg").weight(2);
        line6.addText().text("$1.00").weight(3).align(SynqpayPAL.Align.END);

        ILine line7 = document.addLine();
        line7.addText().text("Coca Cola 1,5l").weight(6);
        line7.addText().text("3 pcs").weight(2);
        line7.addText().text("$10.00").weight(3).align(SynqpayPAL.Align.END);

        ILine line8 = document.addLine();
        line8.addText().text("Bread 1,5l").weight(6);
        line8.addText().text("3 pcs").weight(2);
        line8.addText().text("$10.00").weight(3).align(SynqpayPAL.Align.END);

        document.addBarcode()
                .size(250)
                .type(SynqpayPAL.BarcodeType.QR_CODE)
                .content("https://google.com")
                .spaceBottom(10)
                .spaceTop(10)
                .align(SynqpayPAL.Align.CENTER);

        document.addBarcode()
                .size(40,400)
                .type(SynqpayPAL.BarcodeType.CODE_128)
                .content("12345678901234567890")
                .spaceBottom(10)
                .spaceTop(10)
                .align(SynqpayPAL.Align.CENTER);


        try {
            printer.print(document.bundle());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSynqpayConnected() {
        this.api = SynqpaySDK.get().getSynqpayAPI();
        this.manager = SynqpaySDK.get().getSynqpayManager();
        this.printer = SynqpaySDK.get().getSynqpayPrinter();

        this.tvBindStatus.setText("Synqpay Bounded");
        try {
            this.tvApiEnabled.setText(manager.isApiEnabled()?"API Enabled":"API Disabled");
            manager.setRedirectActivity(getPackageName(), getClass().getName());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public void onSynqpayDisconnected() {
        this.api = null;
        this.tvBindStatus.setText("Synqpay Unbounded");
        this.tvApiEnabled.setText("");
    }
}
