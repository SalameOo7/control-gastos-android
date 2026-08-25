package ec.edu.istg.controlgastos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private val preferences by lazy {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val savedName = preferences.getString(USER_NAME_KEY, null)
        if (!savedName.isNullOrBlank()) {
            openMainActivity()
            return
        }

        setContentView(R.layout.activity_login)

        val nameInput = findViewById<EditText>(R.id.editTextUserName)
        findViewById<Button>(R.id.buttonLogin).setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                nameInput.error = getString(R.string.login_name_required)
                nameInput.requestFocus()
                return@setOnClickListener
            }

            preferences.edit()
                .putString(USER_NAME_KEY, name)
                .apply()
            openMainActivity()
        }
    }

    private fun openMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        const val PREFERENCES_NAME = "control_gastos_preferences"
        const val USER_NAME_KEY = "user_name"
    }
}
