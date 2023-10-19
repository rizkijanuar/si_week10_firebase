package id.kiki.siweek9firebase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.sql.Time
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        val RC_GOOGLE_SIGNIN = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // inisiasi button
        val btnSignin = findViewById<SignInButton>(R.id.btnSignin)

        // inisasi buat kebutuhan tombol signin with google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_application_id)) // Mengatur token aplikasi web untuk autentikasi Firebase
            .requestEmail() // Meminta izin email saat login dengan Google
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // inisiasi firebase auth
        auth = Firebase.auth

        // add data mengunakan layanan firestore
        val db = Firebase.firestore

        btnSignin.setOnClickListener {
            val currentTime = Calendar.getInstance().time
            db.collection("Times").add(TestObj(currentTime = currentTime.time)) // Menambahkan data ke koleksi "Times" di Firestore
            db.collection("Current Time").document("current")
                .set(TestObj(currentTime = currentTime.time)) // Mengatur data di dokumen "current" di koleksi "Current Time"
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_GOOGLE_SIGNIN) // Memulai proses login Google ketika tombol sign-in ditekan
        }
    }

    data class TestObj( val currentTime: Long )

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGNIN) {

            // ambil data google account yang dipakai oleh pengguna
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                // Google signin berhasil, otentikasi dengan Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d("kiki task", "firebaseAuthWithGoogle:$account")
                firebaseAuthWithGoogle(account.idToken!!) // Otentikasi pengguna dengan token Google
            } catch (e: ApiException) {
                Log.e("kiki task", "eror -> ${e.localizedMessage}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d("kiki task", "token -> $idToken")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("kiki task", "signInWithCredential:success")
                    val user = auth.currentUser
                    Toast.makeText(
                        this,
                        "Berhasil sign in ${user?.displayName}",
                        Toast.LENGTH_SHORT
                    )
                        .show() // Menampilkan pesan berhasil sign-in dengan nama pengguna
                } else {
                    Log.w("kiki task", "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Gagal sign in",
                        Toast.LENGTH_SHORT
                    )
                        .show() // Menampilkan pesan gagal sign-in
                }
            }
    }
}