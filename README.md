
# synqpay-sdk-demo

Demo code that uses Synqpay SDK. Includes examples of

- Use Synqpay API to invoke Synqpay JSON-RPC API over IPC
- Manage Synqpay Service
- Synqpay PAL (Print Abstract Layer) to use integrated printer

Add Synqpay to your project
--------------------------------  

The Synqpay SDK is publicly available and hosted on the Maven Central repository. 
This ensures that developers can easily access and integrate the SDK into their applications, facilitating streamlined development processes.

Android projects:
```groovy  
implementation("com.synqpay:synqpay-sdk:1.0")  
```

Synqpay in 3 steps
------------------  

1. Init from your Activity/Application creation

    ```java
    SynqpaySDK.get().init(this);
    ```

2. Bind

   Register and unregister binding listeners

    ```java
    @Override  
     public void onStart() {  
        super.onStart(); 
        SynqpaySDK.get().setListener(this); 
        SynqpaySDK.get().bindService(); 
     }

     @Override  
     public void onStop() {  
        super.onStop(); 
        SynqpaySDK.get().unbindService(); 
        SynqpaySDK.get().setListener(null); 
     } 
    ```


3. Use the desired module

    ```java
    @Override
        public void onSynqpayConnected() {  
        this.api = SynqpaySDK.get().getSynqpayApi(); 
        this.manager = SynqpaySDK.get().getSynqpayManager(); 
        this.printer = SynqpaySDK.get().getSynqpayPrinter();
    } 
    ```
   
    ```java
    public void sendRequest() {  
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.
                    put("jsonrpc","2.0").
                    put("id","1234").
                    put("method","getTerminalStatus").
                    put("params",null);
        } catch (JSONException e) {}
        try {
            api.sendRequest(jsonObject.toString(),responseCallback);
        } catch (RemoteException ignored) {}
    } 
    ```
