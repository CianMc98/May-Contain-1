package com.c17206413.maycontain;

//Group 06
//Eoin Gohery 17206413
//Cian McInerney 17232724

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import android.widget.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    protected Button logIn, scanButton, accountButton, addButton, signOut;

    private String Uid, language,  currentDocRef;
    private String contains = "";
    private boolean gluten, lactose, nuts;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final String TAG = "GoogleActivity";
    private static final int RC_SIGN_IN = 9001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        logIn = findViewById(R.id.LoginButton);

        logIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn(view);
            }
        });

        signOut = findViewById(R.id.SignoutButton);

        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut(view);
            }
        });
        updateUI(1);

        //if user has logged in before it will be auto recognised and  local info set
        if (user!=null) {
            Uid =user.getUid();
            DocumentReference userIdRef = db.collection("users").document(Uid);
            userIdRef.get().addOnCompleteListener(this, new OnCompleteListener<DocumentSnapshot>() {
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            try {
                                // following code checks the user info so that if database has been tampered, the user will reset and the app will not crash
                                language = document.get("language").toString();
                                if (!language.matches("en") && !language.matches("fr")) {
                                    language= "en";
                                }
                                if (document.get("gluten") instanceof Boolean) {
                                    gluten = document.getBoolean("gluten");
                                } else {
                                    userReset(Uid);
                                }
                                if (document.get("lactose") instanceof Boolean) {
                                    lactose = document.getBoolean("lactose");
                                } else {
                                    userReset(Uid);
                                }
                                if (document.get("nuts") instanceof Boolean) {
                                    nuts = document.getBoolean("nuts");
                                } else {
                                    userReset(Uid);
                                }
                                Configuration config = new Configuration();
                                Locale locale = new Locale(language);
                                Locale.setDefault(locale);
                                config.setLocale(locale);
                                getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
                                updateUI(1);
                            } catch (NullPointerException e) {
                                userReset(Uid);
                            }
                        } else {
                            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(getString(R.string.default_web_client_id))
                                    .requestEmail()
                                    .build();
                            updateUI(0);
                            // [END config_signin]

                            mGoogleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);
                            mAuth = FirebaseAuth.getInstance();
                        }
                    }
                }
            });
         // if not recognised will go to login screen
        } else {
            updateUI(0);
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            // [END config_signin]

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            mAuth = FirebaseAuth.getInstance();
        }

        // CLick listener fo Search button
        scanButton = findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openScannerActivity(v);
            }
        });

        // CLick listener fo Add Product button
        addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addProduct();
            }
        });

        // CLick listener fo Account Settings button
        accountButton = findViewById(R.id.accountButton);
        accountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettingsActivity();
            }
        });
    }

    public void openScannerActivity(View v) {
        Intent intent = new Intent(this, Scanner.class);
        startActivityForResult(intent, 1);
    }

    public void openSettingsActivity() {
        Intent intent = new Intent(this, AccountSettings.class);
        intent.putExtra("USER_REF_ID", Uid);
        startActivityForResult(intent, 2);
    }

    private void addProduct() {
        Intent intent = new Intent(this, AddProduct.class);
        intent.putExtra("EXTRA_DOC_REF_ID", currentDocRef);
        intent.putExtra("EXTRA_LANGUAGE", language);
        startActivity(intent);
        updateUI(1);
    }

    private void signIn(View v) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut(View v) {
        mAuth.signOut();
        updateUI(0);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            user = mAuth.getCurrentUser();
                            Uid = user.getUid();
                            if (user!=null) {
                                DocumentReference docIdRef = db.collection("users").document(Uid);
                                docIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document.exists()) {
                                                try {
                                                    // following code checks the user info so that if database has been tampered, the user will reset and the app will not crash
                                                    language = document.get("language").toString();
                                                    if (!language.matches("en") && !language.matches("fr")) {
                                                        language= "en";
                                                    }
                                                    if (document.get("gluten") instanceof Boolean) {
                                                        gluten = document.getBoolean("gluten");
                                                    } else {
                                                        userReset(Uid);
                                                    }
                                                    if (document.get("lactose") instanceof Boolean) {
                                                        lactose = document.getBoolean("lactose");
                                                    } else {
                                                        userReset(Uid);
                                                    }
                                                    if (document.get("nuts") instanceof Boolean) {
                                                        nuts = document.getBoolean("nuts");
                                                    } else {
                                                        userReset(Uid);
                                                    }
                                                    Configuration config = new Configuration();
                                                    Locale locale = new Locale(language);
                                                    Locale.setDefault(locale);
                                                    config.setLocale(locale);
                                                    getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
                                                    updateUI(1);
                                                } catch (RuntimeException e) {
                                                    userReset(Uid);
                                                }
                                            } else {
                                                userReset(Uid);
                                            }
                                        } else {
                                            Log.d(TAG, "Failed with: ", task.getException());
                                        }
                                    }
                                });
                                updateUI(1);
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            updateUI(0);
                        }
                    }
                });
    }

    // this function will reset the user to default values if the user document has incorrect data types or missing fields
    private void userReset( String Uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Your Name?");
        user.put("gluten", false);
        user.put("lactose", false);
        user.put("nuts", false);
        user.put("language", "en");


        db.collection("users").document(Uid)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed
                Log.w(TAG, "Google sign in failed", e);
            }
        }
        if (requestCode == 2) {
            Uid =user.getUid();
            DocumentReference userIdRef = db.collection("users").document(Uid);
            userIdRef.get().addOnCompleteListener(this, new OnCompleteListener<DocumentSnapshot>() {
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            language = document.get("language").toString();
                            gluten = document.getBoolean("gluten");
                            lactose = document.getBoolean("lactose");
                            nuts = document.getBoolean("nuts");
                            Configuration config = new Configuration();
                            Locale locale = new Locale(language);
                            Locale.setDefault(locale);
                            config.setLocale(locale);
                            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
                            recreate();
                        }
                    }
                }
            });
        }
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String result = data.getStringExtra("result");
                currentDocRef = result;
                final DocumentReference docIdRef = db.collection("products").document(result);
                docIdRef.get().addOnCompleteListener(this, new OnCompleteListener<DocumentSnapshot>() {
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                try {
                                    Boolean check= true;
                                    String productName;
                                    productName = document.get("name").toString();

                                    // following code checks if product data types are correct
                                    if (document.get("gluten") instanceof Boolean) {
                                        check = document.getBoolean("gluten");
                                        if (document.get("nuts") instanceof Boolean) {
                                            check = document.getBoolean("nuts");
                                            if (document.get("lactose") instanceof Boolean) {
                                                check = document.getBoolean("lactose");
                                                if (isSafe(document)) {
                                                    updateUI(2);
                                                } else {
                                                    updateUI(3);
                                                }
                                            } else {
                                                deleteProduct(document);
                                            }
                                        } else {
                                            deleteProduct(document);
                                        }
                                    } else {
                                        deleteProduct(document);
                                    }

                                } catch (NullPointerException e) {
                                    deleteProduct(document);
                                }
                            } else {
                                updateUI(4);
                            }
                        }
                    }
                });
            }
            if (resultCode == RESULT_CANCELED) {
                updateUI(1);
            }
        }
    }

    //deletes a product from database if the product has missing fields or incorrect data types
    private void deleteProduct(DocumentSnapshot document) {
        db.collection("products").document(document.getId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting document", e);
                    }
                });
        updateUI(4);
    }

    private boolean isSafe (DocumentSnapshot doc) {
        boolean safe =true;
        if (doc.getBoolean("gluten") && gluten) {
            contains += getString(R.string.gluten_contains);
            contains += "\n";
            safe =false;
        }
        if (doc.getBoolean("lactose") && lactose) {
            contains += getString(R.string.lactose_contains);
            contains += "\n";
            safe =false;
        }
        if (doc.getBoolean("nuts") && nuts) {
            contains += getString(R.string.nuts_contains);
            contains += "\n";
            safe =false;
        }

        return safe;
    }

    //updates the main activity UI to 4 possible versions
    private void updateUI(int result) {
        TextView description = findViewById(R.id.description);
        ImageView image = findViewById(R.id.image);
        if (result == 0) { // login
            description.setText(R.string.desc_google_sign_in);

            findViewById(R.id.addButton).setVisibility(View.GONE);
            findViewById(R.id.accountButton).setVisibility(View.GONE);
            findViewById(R.id.LoginButton).setVisibility(View.VISIBLE);
            findViewById(R.id.scanButton).setVisibility(View.GONE);
            findViewById(R.id.SignoutButton).setVisibility(View.GONE);
            image.setImageResource(R.drawable.common_google_signin_btn_icon_light_normal);
        } else if (result == 1) { // search
            description.setText(R.string.search);

            findViewById(R.id.addButton).setVisibility(View.GONE);
            findViewById(R.id.accountButton).setVisibility(View.VISIBLE);
            findViewById(R.id.LoginButton).setVisibility(View.GONE);
            findViewById(R.id.SignoutButton).setVisibility(View.VISIBLE);
            findViewById(R.id.scanButton).setVisibility(View.VISIBLE);
            image.setImageResource(R.drawable.search_icon);
        } else if (result == 2) { // safe
            description.setText(R.string.this_product_is_safe_to_use);

            findViewById(R.id.addButton).setVisibility(View.GONE);
            findViewById(R.id.accountButton).setVisibility(View.VISIBLE);
            findViewById(R.id.LoginButton).setVisibility(View.GONE);
            findViewById(R.id.SignoutButton).setVisibility(View.VISIBLE);
            findViewById(R.id.scanButton).setVisibility(View.VISIBLE);
            image.setImageResource(R.drawable.safe_tick);
        } else if (result == 3) { // unsafe
            String unsafeProduct = getString(R.string.this_product_is_not_safe);
            String productContains = getString(R.string.productContains);
            description.setMovementMethod(new ScrollingMovementMethod());
            description.setText(String.format("%s\n%s%s", unsafeProduct, productContains + "\t", contains));
            contains = "";

            findViewById(R.id.addButton).setVisibility(View.GONE);
            findViewById(R.id.accountButton).setVisibility(View.VISIBLE);
            findViewById(R.id.LoginButton).setVisibility(View.GONE);
            findViewById(R.id.SignoutButton).setVisibility(View.VISIBLE);
            findViewById(R.id.scanButton).setVisibility(View.VISIBLE);
            image.setImageResource(R.drawable.x_mark);
        } else if (result == 4) { // unknown
            description.setText(R.string.unknown_item);

            findViewById(R.id.addButton).setVisibility(View.VISIBLE);
            findViewById(R.id.accountButton).setVisibility(View.VISIBLE);
            findViewById(R.id.LoginButton).setVisibility(View.GONE);
            findViewById(R.id.SignoutButton).setVisibility(View.VISIBLE);
            findViewById(R.id.scanButton).setVisibility(View.VISIBLE);
            image.setImageResource(R.drawable.unknown_icon);
        }
    }
}
