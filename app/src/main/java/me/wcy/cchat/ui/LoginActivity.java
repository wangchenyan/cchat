package me.wcy.cchat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import me.wcy.cchat.R;
import me.wcy.cchat.AppCache;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText etAccount;
    private EditText etToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etAccount = findViewById(R.id.et_account);
        etToken = findViewById(R.id.et_token);
    }

    @Override
    public void onClick(View v) {
        AppCache.getService().login(etAccount.getText().toString(), etToken.getText().toString(), (code, msg, aVoid) -> {
            if (code == 200) {
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "登录失败 code=" + code + ", msg=" + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
