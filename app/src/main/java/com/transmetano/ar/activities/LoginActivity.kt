package com.transmetano.ar.activities

import android.annotation.SuppressLint
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esri.arcgisruntime.security.OAuthTokenCredential
import com.google.android.material.textfield.TextInputEditText
import com.transmetano.ar.R
import com.transmetano.ar.db.DbHelper
import com.transmetano.ar.objects.TokenResponse
import com.transmetano.ar.retrofit.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : AppCompatActivity() {

    private val TOKEN_PREFS = "token"
    private val EXPIRES_TOKEN_PREFS = "expiresToken"

    private val PREFS = "LoginPreferences"
    private val USER_PREFS = "username"
    private val PASS_PREFS = "password"
    private val CHECK_PREFS = "check"

    private val oAuthTokenCredential: OAuthTokenCredential? = null
    private var token = ""
    private var expiresToken = ""
    private val dbHelper: DbHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val tiUsername = findViewById<TextInputEditText>(R.id.tiUsername)
        val tiPassword = findViewById<TextInputEditText>(R.id.tiPassword)
        val tvForget = findViewById<TextView>(R.id.tvForget)
        val cbRemember = findViewById<CheckBox>(R.id.cbRemember)
        val loginButton = findViewById<Button>(R.id.btnLogin)

        tvForget.setOnClickListener { v: View? ->
            Toast.makeText(
                applicationContext, getString(R.string.login_forgot),
                Toast.LENGTH_SHORT
            ).show()
        }

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        tiUsername.setText(prefs.getString(USER_PREFS, ""))
        tiPassword.setText(prefs.getString(PASS_PREFS, ""))

        token = prefs.getString(TOKEN_PREFS, "").toString()
        expiresToken = prefs.getString(EXPIRES_TOKEN_PREFS, "").toString()

        tiUsername.setText("ContratistaGIS2")
        tiPassword.setText("Innovati2022.")

        cbRemember.isChecked = prefs.getBoolean(CHECK_PREFS, false)
        loginButton.setOnClickListener {
            val user =
                Objects.requireNonNull(tiUsername.text).toString()
            val password =
                Objects.requireNonNull(tiPassword.text).toString()
            try {
                generateToken(prefs, user, password, cbRemember)
            } catch (e: NullPointerException) {
                createMessage(getString(R.string.login_error))
            } catch (e: IllegalArgumentException) {
                createMessage(getString(R.string.login_error))
            }
        }
    }

    private fun generateToken(
        prefs: SharedPreferences,
        user: String,
        password: String,
        cbRemember: CheckBox
    ) {
        val call: Call<TokenResponse> =
            ApiClient.getInstance(getString(R.string.portal_url)).getApi().getToken(user, password)
        call.enqueue(object : Callback<TokenResponse> {
            override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
                val serviceResponse: TokenResponse? = response.body()
                if (response.isSuccessful && response.body().toString() !== "null" ||
                    user == prefs.getString(USER_PREFS, "")
                    && password == prefs.getString(PASS_PREFS, "")
                ) {

                    //2 hour token expiration time
                    val milliSecond: Long = serviceResponse?.expires ?: (2 * 60 * 60)
                    val date = Date()
                    date.time = milliSecond

                    //expiration date
                    val dateformat = SimpleDateFormat().format(date)
                    token = serviceResponse?.token ?: ""
                    expiresToken = dateformat

                    //save prefes of token and expires time
                    val editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    editor.putString(TOKEN_PREFS, token)
                    editor.putString(EXPIRES_TOKEN_PREFS, expiresToken)
                    editor.apply()

                    //save prefes of user
                    savePrefs(cbRemember, user, password)
                } else {
                    createMessage(getString(R.string.login_failed))
                }
            }

            override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                //validate offline login
                if (user == prefs.getString(USER_PREFS, "")
                    && password == prefs.getString(PASS_PREFS, "")
                ) {
                    savePrefs(cbRemember, user, password)
                } else {
                    createMessage(getString(R.string.login_error))
                }
            }
        })
    }

    private fun savePrefs(cbRemember: CheckBox, user: String, password: String) {
        val editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit()
        editor.putString(USER_PREFS, if (cbRemember.isChecked) user else "")
        editor.putString(PASS_PREFS, if (cbRemember.isChecked) password else "")
        editor.putBoolean(CHECK_PREFS, cbRemember.isChecked)
        editor.apply()
        val intent = Intent(this, ArActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createMessage(message: String) {
        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
    }

    private val networkStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val manager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            @SuppressLint("MissingPermission") val ni = manager.activeNetworkInfo
            onNetworkChange(ni)
        }
    }

    private fun onNetworkChange(networkInfo: NetworkInfo?) {
        if (networkInfo != null) {
            if (networkInfo.state == NetworkInfo.State.CONNECTED) {
                if (networkInfo.isConnected) {
                    // CONNECTED
                    Log.d("State 1", networkInfo.state.toString())
                }
            } else {
                // DISCONNECTED"
                Log.d("State 3", networkInfo.state.toString())
            }
        }
    }

    private fun isOnlineNet(): Boolean? {
        try {
            val p =
                Runtime.getRuntime().exec("ping -c 1 www.google.es")
            val `val` = p.waitFor()
            return `val` == 0
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            networkStateReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    override fun onPause() {
        unregisterReceiver(networkStateReceiver)
        super.onPause()
    }
}