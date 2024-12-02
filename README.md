
# synqpay-sdk-demo

Demo code that uses Synqpay SDK. Includes examples of

- Use Synqpay Commander to invoke Synqpay JSON-RPC API over IPC
- Manage Synqpay Service
- Synqpay PAL (Print Abstract Layer) to use integrated printer


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
        this.commander = SynqpaySDK.get().getSynqpayCommander(); 
        this.manager = SynqpaySDK.get().getSynqpayManager(); 
        this.printer = SynqpaySDK.get().getSynqpayPrinter();  
        try { 
            this.commander.sendCommand(); 
        } catch (RemoteException e) {  
            throw new RuntimeException(e);  
        }
    } 
    ```

Add Synqpay to your project
--------------------------------  

Android projects:
```groovy  
implementation("com.synqpay:synqpay-sdk:0.9")  
```