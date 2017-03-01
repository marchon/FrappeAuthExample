package me.revant.frappeauthexample;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private String authToken;
    private ListView lv,tokenResult;
    AccountManager mAccountManager;
    Map<String, String> idpSettings;
    Account mAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tokenResult = (ListView) findViewById(R.id.tokenResult);
        mAccountManager = AccountManager.get(this);
        loadButtons();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            Bundle bundle = data.getExtras();
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                Log.d("callback", String.format("%s %s", key, value.toString()));
            }
            Account account = new Account(
                    data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),
                    data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
            );
            getAuthToken(account, idpSettings.get(account.type));
        }
    }

    private void loadButtons() {
        loadButtonFrappe();
    }

    private void loadButtonFrappe() {
        Button button = (Button) findViewById(R.id.button_frappe);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAuthToken("io.frappe.frappeauthenticator", "Read only");
                if (mAccount!=null && authToken!=null) {

                }
            }
        });
    }

    private void getAuthToken(String accountType, String authTokenType) {
        Account[] accounts = mAccountManager.getAccountsByType(accountType);
        rememberIdpSettings(accountType, authTokenType);
        if (!appInstalledOrNot("io.frappe.frappeauthenticator")){
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("Install App")
                .setMessage("Please Install Frappe Authenticator")
                .show();
        }
        else if (accounts.length == 1) {
            Log.d("account", accounts[0].name);
            mAccount = accounts[0];
            getAuthToken(accounts[0], authTokenType);
        }
        else {
            Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{accountType}, null, null, null, null);
            startActivityForResult(intent, 1);
        }
    }

    private void getAuthToken(final Account account, String authTokenType) {
        mAccountManager.getAuthToken(account, authTokenType, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bundle = future.getResult();
                    for (String key : bundle.keySet()) {
                        Object value = bundle.get(key);
                        Log.d("callback", String.format("%s %s", key, value.toString()));
                    }
                    authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);

                    tokenResult = (ListView) findViewById(R.id.tokenResult);

                    // Instanciating an array list (you don't need to do this,
                    // you already have yours).
                    List<String> token = new ArrayList<String>();
                    token.add(authToken);
                    // This is the array adapter, it takes the context of the activity as a
                    // first parameter, the type of list view as a second parameter and your
                    // array as a third parameter.
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                            MainActivity.this,
                            android.R.layout.simple_expandable_list_item_1,
                            token);

                    tokenResult.setAdapter(arrayAdapter);

                    try {
                        JSONObject bearerToken = new JSONObject(authToken);
                        String frappeServerURL = mAccountManager.getUserData(account, "frappeServer");
                        ERPNextContactProvider server = new ERPNextContactProvider();
                        server.getContacts(frappeServerURL,bearerToken.getString("access_token"),new FrappeServerCallback() {
                            @Override
                            public void onSuccessJSONObject(JSONObject response) {

                                lv = (ListView) findViewById(R.id.listView);

                                List<String> contactLV = new ArrayList<String>();
                                JSONArray contacts = new JSONArray();
                                try {
                                    contacts = response.getJSONArray("data");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                for(int i=0;i<contacts.length();i++){
                                    JSONObject object = null;
                                    try {
                                        object = contacts.getJSONObject(i);
                                        contactLV.add(object.getString("name"));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                                        MainActivity.this,
                                        android.R.layout.simple_expandable_list_item_1,
                                        contactLV);

                                lv.setAdapter(arrayAdapter);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.d("access_token", authToken);
                    mAccountManager.invalidateAuthToken(account.type, authToken);
                } catch (Exception e) {
                    Log.d("error", e.getMessage());
                }
            }
        },null);
    }

    private void rememberIdpSettings(String accountType, String authTokenType) {
        if (idpSettings == null) {
            idpSettings = new HashMap<String, String>();
        }
        if (!idpSettings.containsKey(accountType)) {
            idpSettings.put(accountType, authTokenType);
        }
    }

    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }

        return false;
    }
}
