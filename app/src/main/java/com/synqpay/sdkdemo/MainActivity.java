package com.synqpay.sdkdemo;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.synqpay.sdk.CommandListener;
import com.synqpay.sdk.SynqpayCommander;
import com.synqpay.sdk.SynqpayManager;
import com.synqpay.sdk.SynqpayPAL;
import com.synqpay.sdk.SynqpayPrinter;
import com.synqpay.sdk.SynqpaySDK;
import com.synqpay.sdk.SynqpayStartupNotifier;
import com.synqpay.sdk.pal.IDocument;
import com.synqpay.sdk.pal.ILine;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements SynqpaySDK.ConnectionListener {

    private SynqpayCommander commander;
    private SynqpayManager manager;
    private SynqpayPrinter printer;
    private SynqpayStartupNotifier startupNotifier;

    private TextView tvBindStatus;
    private TextView tvCommanderEnabled;
    private CheckBox cbNotifyUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startupNotifier = new SynqpayStartupNotifier();
        SynqpaySDK.get().init(this);
        SynqpaySDK.get().setListener(this);


        tvBindStatus = findViewById(R.id.text_synqpay_status);
        tvCommanderEnabled = findViewById(R.id.text_commander_enabled);
        cbNotifyUpdate = findViewById(R.id.checkbox_notify_update);

        Button btnGetAshraitStatus = findViewById(R.id.button_getAshraitStatus);
        btnGetAshraitStatus.setOnClickListener(v ->
                sendCommand(getAshraitStatusCommand(),getStatusListener));

        Button btnSettlement = findViewById(R.id.button_settlement);
        btnSettlement.setOnClickListener(v ->
                sendCommand(settlementCommand(),getStatusListener));

        Button btnStartTransaction = findViewById(R.id.button_startTransaction);
        btnStartTransaction.setOnClickListener(v ->
                sendCommand(getStartTransactionCommand(),startTransactionListener));

        Button btnContinueTransaction = findViewById(R.id.button_continueTransaction);
        btnContinueTransaction.setOnClickListener(v ->
                sendCommand(getContinueTransactionCommand(),startTransactionListener));


        Button btnRestart = findViewById(R.id.button_restart);
        btnRestart.setOnClickListener(v -> restartSynqpay());

        Button btnPrint = findViewById(R.id.button_print);
        btnPrint.setOnClickListener(v -> print());
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

    private void sendCommand(String command, CommandListener commandListener) {
        if (this.commander != null) {
            try {
                Log.i("DEMO"," => "+command);
                commander.sendCommand(command,commandListener);
            } catch (RemoteException ignored) {}
        }
    }

    private void restartSynqpay() {
        try {
            manager.restartSynqpay();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }


    private String getAshraitStatusCommand() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.
                    put("jsonrpc","2.0").
                    put("id","1234").
                    put("method","getAshraitStatus").
                    put("params",null);
        } catch (JSONException e) {
            return "";
        }
        return jsonObject.toString();
    }

    private String settlementCommand() {
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



    private String getStartTransactionCommand() {
        JSONObject jsonObject = new JSONObject();
        JSONObject params = new JSONObject();
        try {
            params
                    .put("paymentMethod","CREDIT_CARD")
                    .put("tranType","SALE")
                    .put("referenceId","referenceId")
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


    private String getContinueTransactionCommand() {
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

    private final CommandListener.Stub getStatusListener = new CommandListener.Stub() {
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

    private final CommandListener.Stub startTransactionListener = new CommandListener.Stub() {
        @Override
        public void onResponse(String response) {
            Log.i("DEMO"," <= "+response);
            String terminalId,result;
            try {
                JSONObject jsonResponse = new JSONObject(response);
                JSONObject jsonResult = jsonResponse.optJSONObject("result");
                if (jsonResult == null)
                    return;
                terminalId = jsonResult.optString("terminalId","");
                result = jsonResult.optString("transactionStatus","");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            MainActivity.this.runOnUiThread(()
                    -> Toast.makeText(
                    MainActivity.this,terminalId+" :"+result,Toast.LENGTH_LONG).show());
        }
    };


    private void print() {

        IDocument document = SynqpayPAL.newDocument();
        ILine line1 = document.addLine();
        line1.fillLast(true);
        line1.addText().text("T1").bold(true);
        line1.addText().text("T2").bold(true).align(SynqpayPAL.Align.END);
        line1.spaceTop(10);
        document.addDivider(4);
        ILine line2 = document.addLine();
        line2.addText()
                .text("HELLO")
                .weight(1);
        line2.addText()
                .text("WORLD")
                .weight(2);

        document.addSpace(20);
        document.addLine().align(SynqpayPAL.Align.CENTER).
                fillLast(true).addText().text("CENTER").bold(true);
        try {
            printer.print(document.bundle());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSynqpayConnected() {
        this.commander = SynqpaySDK.get().getSynqpayCommander();
        this.manager = SynqpaySDK.get().getSynqpayManager();
        this.printer = SynqpaySDK.get().getSynqpayPrinter();

        this.tvBindStatus.setText("Synqpay Bounded");
        try {
            this.tvCommanderEnabled.setText(manager.isCommanderEnabled()?"Commander Enabled":"Commander Disabled");
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public void onSynqpayDisconnected() {
        this.commander = null;
        this.tvBindStatus.setText("Synqpay Unbounded");
        this.tvCommanderEnabled.setText("");
    }
}