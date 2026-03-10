package com.abaijan.todo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import com.abaijan.todo.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ProgressBar progressBar;
    private Button signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("Firebase", "Auth: " + (auth != null ? "OK" : "ERROR"));
        Log.d("Firebase", "Firestore: " + (db != null ? "OK" : "ERROR"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Инициализация Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Проверка: если пользователь уже залогинен
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: " + currentUser.getEmail());
            goToMainActivity();
            return;
        }

        // Настройка Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // UI элементы
        signInButton = findViewById(R.id.sign_in_button);
        progressBar = findViewById(R.id.progress_bar);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    private void signIn() {
        Log.d(TAG, "Starting sign-in process");
        showProgress(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google sign-in succeeded: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign-in failed", e);
                showProgress(false);
                Toast.makeText(this, "Ошибка входа: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "Authenticating with Firebase");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase authentication succeeded");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this,
                                    "Добро пожаловать, " + user.getDisplayName() + "!",
                                    Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        } else {
                            Log.w(TAG, "Firebase authentication failed", task.getException());
                            showProgress(false);
                            Toast.makeText(LoginActivity.this,
                                    "Ошибка аутентификации",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, ToDoListActivity.class);
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        signInButton.setEnabled(!show);
    }
}