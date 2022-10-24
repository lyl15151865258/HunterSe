package com.ott.hunterse.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.ott.hunterse.R;
import com.ott.hunterse.bean.EventMsg;
import com.ott.hunterse.constant.Constant;
import com.ott.hunterse.contentprovider.SPHelper;
import com.ott.hunterse.tcp.TcpClient;
import com.ott.hunterse.utils.LogUtils;
import com.ott.hunterse.utils.MathUtils;
import com.ott.hunterse.view.RockerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 主界面
 * Created at 2022-06-13 0013 14:23:10
 *
 * @author LiYuliang
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private Context mContext;
    private TcpClient tcpClient = null;

    public final static int WIFI1 = 1;
    public final static int WIFI2 = 2;
    public final static int WIFI3 = 3;
    public final static int WIFI4 = 4;
    public final static int WIFI5 = 5;

    private AppCompatButton btnGoHead, btnGoBack, btnTurnLeft, btnTurnRight;
    private AppCompatTextView tvCarStatus, tvControlMode, tvVoltage, tvCarErrorCode, tvSpeed, tvDirection, tvReadZero;
    private AppCompatEditText etSpeed, etDirection, etSetZero, etSpeedFixed, etDirectionFixed;
    private AppCompatImageView ivConnectStatus;
    private RadioButton rbRocker;
    private RadioButton rbSetting;
    private RadioButton rbFix;
    private LinearLayout llSettingSpeed, llSettingDirection, llRockerSpeed, llRockerDirection, llSpeedSetting, llDirectionSetting,
            llSpeedMax, llDirectionMax, llFixedSpeed, llFixedDirection, llZeroRead, llZeroSetting;

    private boolean goHead, goBack, turnLeft, turnRight;
    // 默认运动指令（静止）
    private String command = Constant.DEFAULT_CAN_COMMAND;
    private ExecutorService executorService;
    private boolean sendCommand = true;

    private Vibrator vibrator;

    private boolean isCanMode = false;
    // 固定值的速度和方向
    private int speedSetting = 0, directionSetting = 0;
    // 速度和方向的修正值
    private float speedFixedSetting = 0, directionFixedSetting = 0;
    // 摇杆模式下速度和方向
    private int speedRocker = 0, directionRocker = 0;
    // 连接状态标记
    private boolean isConnected = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mContext = this;
        executorService = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
                new SynchronousQueue<>(), (r) -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        EventBus.getDefault().register(this);

        btnGoHead = findViewById(R.id.btnGoHead);
        btnGoHead.setOnTouchListener(onTouchListener);
        btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnTouchListener(onTouchListener);
        btnTurnLeft = findViewById(R.id.btnTurnLeft);
        btnTurnLeft.setOnTouchListener(onTouchListener);
        btnTurnRight = findViewById(R.id.btnTurnRight);
        btnTurnRight.setOnTouchListener(onTouchListener);

        etSpeed = findViewById(R.id.etSpeed);
        etDirection = findViewById(R.id.etDirection);
        etSetZero = findViewById(R.id.etSetZero);
        etSpeed.addTextChangedListener(etSpeedWatcher);
        etDirection.addTextChangedListener(etDirectionWatcher);
        etSetZero.addTextChangedListener(etSetZeroWatcher);
        findViewById(R.id.btnGetZero).setOnClickListener(onClickListener);
        findViewById(R.id.btnSetZero).setOnClickListener(onClickListener);
        etSpeedFixed = findViewById(R.id.etSpeedFixed);
        etDirectionFixed = findViewById(R.id.etDirectionFixed);
        etSpeedFixed.addTextChangedListener(etSpeedFixedWatcher);
        etDirectionFixed.addTextChangedListener(etDirectionFixedWatcher);

        ivConnectStatus = findViewById(R.id.ivConnectStatus);
        tvCarStatus = findViewById(R.id.tvCarStatus);
        tvControlMode = findViewById(R.id.tvControlMode);
        tvVoltage = findViewById(R.id.tvVoltage);
        tvCarErrorCode = findViewById(R.id.tvCarErrorCode);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvDirection = findViewById(R.id.tvDirection);
        tvReadZero = findViewById(R.id.tvReadZero);

        llSettingSpeed = findViewById(R.id.llSettingSpeed);
        llSettingDirection = findViewById(R.id.llSettingDirection);
        llRockerSpeed = findViewById(R.id.llRockerSpeed);
        llRockerDirection = findViewById(R.id.llRockerDirection);

        llSpeedSetting = findViewById(R.id.llSpeedSetting);
        llDirectionSetting = findViewById(R.id.llDirectionSetting);
        llSpeedMax = findViewById(R.id.llSpeedMax);
        llDirectionMax = findViewById(R.id.llDirectionMax);
        llZeroRead = findViewById(R.id.llZeroRead);
        llZeroSetting = findViewById(R.id.llZeroSetting);
        llFixedSpeed = findViewById(R.id.llFixedSpeed);
        llFixedDirection = findViewById(R.id.llFixedDirection);

        rbRocker = findViewById(R.id.rbRocker);
        rbSetting = findViewById(R.id.rbSetting);
        rbFix = findViewById(R.id.rbFix);
        ((RadioGroup) findViewById(R.id.rgModeType)).setOnCheckedChangeListener(typeCheckedChangeListener);

        RockerView rvDirection = findViewById(R.id.rvDirection);
        RockerView rvSpeed = findViewById(R.id.rvSpeed);
        rvSpeed.setOnDistanceLevelListener(onDistanceLevelListener1);
        rvDirection.setOnDistanceLevelListener(onDistanceLevelListener2);
        rvSpeed.setOnShakeListener(RockerView.DirectionMode.DIRECTION_2_VERTICAL, onShakeListener1);
        rvDirection.setOnShakeListener(RockerView.DirectionMode.DIRECTION_2_HORIZONTAL, onShakeListener2);

        tcpClient = new TcpClient();

        // Wifi信号强度
        getWifiStrength();
    }

    @Override
    protected void onResume() {
        super.onResume();
        etSpeed.setText(SPHelper.getString("speed", ""));
        etDirection.setText(SPHelper.getString("direction", ""));
        etSetZero.setText(SPHelper.getString("zero", ""));
        etSpeedFixed.setText(SPHelper.getString("speedFixed", "1"));
        etDirectionFixed.setText(SPHelper.getString("directionFixed", "1"));
        // 连接小车的Tcp Server
        try {
            InetAddress ipAddress = InetAddress.getByName(Constant.TCP_SERVER_IP);
            tcpClient.setInetAddress(ipAddress);
            tcpClient.setPort(Constant.TCP_SERVER_PORT);
            executorService.submit(tcpClient);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private final View.OnClickListener onClickListener = view -> {
        if (view.getId() == R.id.btnGetZero) {
            // 查询转向零点
            vibrator.vibrate(20);
            if (!isConnected) {
                Toast.makeText(mContext, "请先连接小车", Toast.LENGTH_SHORT).show();
            } else {
                Runnable runnable = () -> {
                    tcpClient.sendMessage("0100000433AA");
                    tcpClient.sendMessage(command);
                };
                executorService.submit(runnable);
            }
        } else if (view.getId() == R.id.btnSetZero) {
            // 设置转向零点
            vibrator.vibrate(20);
            if (!isConnected) {
                Toast.makeText(mContext, "请先连接小车", Toast.LENGTH_SHORT).show();
            } else {
                if (TextUtils.isEmpty(Objects.requireNonNull(etSetZero.getText()).toString())) {
                    Toast.makeText(mContext, "请输入转向零点设定值", Toast.LENGTH_SHORT).show();
                } else {
                    int zero = Integer.parseInt(etSetZero.getText().toString());
                    if (zero >= 12000 && zero <= 32000) {
                        Runnable runnable = () -> {
                            String cmd = "0100000432" + MathUtils.addZeroForLeft(Integer.toHexString(zero), 4);
                            LogUtils.d(TAG, "转向零点设定值：" + cmd);
                            tcpClient.sendMessage(cmd);
                        };
                        executorService.submit(runnable);
                    } else {
                        Toast.makeText(mContext, "设定值超出范围", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    // 摇杆模式下速度键滑动距离监听
    private final RockerView.OnDistanceLevelListener onDistanceLevelListener1 = (x, y, level) -> {
        speedRocker = (int) (-4.8 * 1000 * y);
        LogUtils.d("X方向：" + x + "，Y方向：" + y + "，level：" + level + "，速度：" + speedRocker);
        setCommand();
    };

    // 摇杆模式下方向键滑动距离监听
    private final RockerView.OnDistanceLevelListener onDistanceLevelListener2 = (x, y, level) -> {
        directionRocker = (int) (-400 * x);
        LogUtils.d("X方向：" + x + "，Y方向：" + y + "，level：" + level + "，方向：" + directionRocker);
        setCommand();
    };

    // 摇杆模式下速度键滑动监听
    private final RockerView.OnShakeListener onShakeListener1 = new RockerView.OnShakeListener() {
        @Override
        public void onStart() {
            vibrator.vibrate(100);
            if (!isConnected) {
                Toast.makeText(mContext, "请先连接小车", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void direction(RockerView.Direction direction) {
            if (direction == RockerView.Direction.DIRECTION_CENTER) {
                // 摇杆回到中心
                LogUtils.d("速度摇杆归位");
                speedRocker = 0;
                setCommand();
            }
        }

        @Override
        public void onFinish() {

        }
    };

    // 摇杆模式下方向键滑动监听
    private final RockerView.OnShakeListener onShakeListener2 = new RockerView.OnShakeListener() {
        @Override
        public void onStart() {
            vibrator.vibrate(100);
            if (!isConnected) {
                Toast.makeText(mContext, "请先连接小车", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void direction(RockerView.Direction direction) {
            if (direction == RockerView.Direction.DIRECTION_CENTER) {
                // 摇杆回到中心
                LogUtils.d("方向摇杆归位");
                directionRocker = 0;
                setCommand();
            }
        }

        @Override
        public void onFinish() {

        }
    };

    /**
     * 摇杆模式下指令设定
     */
    private void setCommand() {
        if (rbRocker.isChecked()) {
            command = "0800000111" +
                    MathUtils.addZeroForLeft(Integer.toHexString(speedRocker), 4) +
                    // 保留位
                    "00000000" +
                    // 转弯
                    MathUtils.addZeroForLeft(Integer.toHexString(directionRocker), 4);
        }
    }

    // 模式切换监听器
    private final RadioGroup.OnCheckedChangeListener typeCheckedChangeListener = (radioGroup, i) -> {
        if (i == R.id.rbRocker) {
            // 摇杆模式
            llSettingSpeed.setVisibility(View.GONE);
            llSettingDirection.setVisibility(View.GONE);
            llRockerSpeed.setVisibility(View.VISIBLE);
            llRockerDirection.setVisibility(View.VISIBLE);

            llSpeedSetting.setVisibility(View.GONE);
            llDirectionSetting.setVisibility(View.GONE);
            llSpeedMax.setVisibility(View.VISIBLE);
            llDirectionMax.setVisibility(View.VISIBLE);
            llZeroRead.setVisibility(View.GONE);
            llZeroSetting.setVisibility(View.GONE);
            llFixedSpeed.setVisibility(View.GONE);
            llFixedDirection.setVisibility(View.GONE);
        } else if (i == R.id.rbSetting) {
            // 定值模式
            llSettingSpeed.setVisibility(View.VISIBLE);
            llSettingDirection.setVisibility(View.VISIBLE);
            llRockerSpeed.setVisibility(View.GONE);
            llRockerDirection.setVisibility(View.GONE);

            llSpeedSetting.setVisibility(View.VISIBLE);
            llDirectionSetting.setVisibility(View.VISIBLE);
            llSpeedMax.setVisibility(View.GONE);
            llDirectionMax.setVisibility(View.GONE);
            llZeroRead.setVisibility(View.GONE);
            llZeroSetting.setVisibility(View.GONE);
            llFixedSpeed.setVisibility(View.GONE);
            llFixedDirection.setVisibility(View.GONE);
        } else if (i == R.id.rbZero) {
            // 零点设置
            llSpeedSetting.setVisibility(View.GONE);
            llDirectionSetting.setVisibility(View.GONE);
            llSpeedMax.setVisibility(View.GONE);
            llDirectionMax.setVisibility(View.GONE);
            llZeroRead.setVisibility(View.VISIBLE);
            llZeroSetting.setVisibility(View.VISIBLE);
            llFixedSpeed.setVisibility(View.GONE);
            llFixedDirection.setVisibility(View.GONE);
            // 转向零点设置，发送一条查询指令
            Runnable runnable = () -> tcpClient.sendMessage("0100000433AA");
            executorService.submit(runnable);
        } else if (i == R.id.rbFix) {
            // 修正值设置
            llSettingSpeed.setVisibility(View.VISIBLE);
            llSettingDirection.setVisibility(View.VISIBLE);
            llRockerSpeed.setVisibility(View.GONE);
            llRockerDirection.setVisibility(View.GONE);

            llSpeedSetting.setVisibility(View.GONE);
            llDirectionSetting.setVisibility(View.GONE);
            llSpeedMax.setVisibility(View.GONE);
            llDirectionMax.setVisibility(View.GONE);
            llZeroRead.setVisibility(View.GONE);
            llZeroSetting.setVisibility(View.GONE);
            llFixedSpeed.setVisibility(View.VISIBLE);
            llFixedDirection.setVisibility(View.VISIBLE);
        }
    };

    // 速度设置输入框监听器
    private final TextWatcher etSpeedWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //如果有小数点,限制小数位数不大于2个
            if (charSequence.toString().contains(".")) {
                if (charSequence.length() - 1 - charSequence.toString().indexOf(".") > 3) {
                    charSequence = charSequence.toString().subSequence(0, charSequence.toString().indexOf(".") + 4);
                    etDirection.setText(charSequence);
                    //设置光标在末尾
                    etDirection.setSelection(charSequence.length());
                }
            }
            //除了小数开头不能为0，而且开头不允许连续出现0
            if (charSequence.toString().startsWith("0") && charSequence.toString().trim().length() > 1) {
                if (charSequence.toString().charAt(1) != '.') {
                    etSpeed.setText(charSequence.subSequence(0, 1));
                    etSpeed.setSelection(1);
                }
            }
            //如果直接输入小数点，前面自动补0
            if (charSequence.toString().trim().equals(".")) {
                charSequence = "0" + charSequence;
                etSpeed.setText(charSequence);
                etSpeed.setSelection(2);
            }
            if (charSequence.length() > 0) {
                if (Float.floatToIntBits(Float.parseFloat(charSequence.toString())) > Float.floatToIntBits(4.8f)) {
                    charSequence = charSequence.subSequence(0, charSequence.length() - 1);
                    etSpeed.setText(charSequence);
                    etSpeed.setSelection(charSequence.length());
                }
                if (charSequence.length() > 0) {
                    speedSetting = (int) (Float.parseFloat(charSequence.toString()) * 1000);
                    LogUtils.d(TAG, "速度设定值为：" + speedSetting);
                }
            } else {
                speedSetting = 0;
            }
            SPHelper.save("speed", Objects.requireNonNull(etSpeed.getText()).toString());
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    // 方向设置输入框监听器
    private final TextWatcher etDirectionWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //如果有小数点,限制小数位数不大于2个
            if (charSequence.toString().contains(".")) {
                if (charSequence.length() - 1 - charSequence.toString().indexOf(".") > 2) {
                    charSequence = charSequence.toString().subSequence(0, charSequence.toString().indexOf(".") + 3);
                    etDirection.setText(charSequence);
                    //设置光标在末尾
                    etDirection.setSelection(charSequence.length());
                }
            }
            //除了小数开头不能为0，而且开头不允许连续出现0
            if (charSequence.toString().startsWith("0") && charSequence.toString().trim().length() > 1) {
                if (charSequence.toString().charAt(1) != '.') {
                    etDirection.setText(charSequence.subSequence(0, 1));
                    etDirection.setSelection(1);
                }
            }
            //如果直接输入小数点，前面自动补0
            if (charSequence.toString().trim().equals(".")) {
                charSequence = "0" + charSequence;
                etDirection.setText(charSequence);
                etDirection.setSelection(2);
            }
            if (charSequence.length() > 0) {
                if (Float.floatToIntBits(Float.parseFloat(charSequence.toString())) > Float.floatToIntBits(22.9f)) {
                    charSequence = charSequence.subSequence(0, charSequence.length() - 1);
                    etDirection.setText(charSequence);
                    etDirection.setSelection(charSequence.length());
                }
                if (charSequence.length() > 0) {
                    directionSetting = (int) (Float.parseFloat(charSequence.toString()) * 1000f * Math.PI / 180);
                    LogUtils.d(TAG, "方向设定值为：" + directionSetting);
                }
            } else {
                directionSetting = 0;
            }
            SPHelper.save("direction", Objects.requireNonNull(etDirection.getText()).toString());
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    // 转向零点设置输入框监听器
    private final TextWatcher etSetZeroWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (charSequence.length() > 0) {
                if (Integer.parseInt(charSequence.toString()) > 32000) {
                    charSequence = charSequence.subSequence(0, charSequence.length() - 1);
                    etSetZero.setText(charSequence);
                    etSetZero.setSelection(charSequence.length());
                }
            }
            SPHelper.save("zero", Objects.requireNonNull(etSetZero.getText()).toString());
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    // 速度修正设置输入框监听器
    private final TextWatcher etSpeedFixedWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //如果有小数点,限制小数位数不大于2个
            if (charSequence.toString().contains(".")) {
                if (charSequence.length() - 1 - charSequence.toString().indexOf(".") > 3) {
                    charSequence = charSequence.toString().subSequence(0, charSequence.toString().indexOf(".") + 4);
                    etSpeedFixed.setText(charSequence);
                    //设置光标在末尾
                    etSpeedFixed.setSelection(charSequence.length());
                }
            }
            //除了小数开头不能为0，而且开头不允许连续出现0
            if (charSequence.toString().startsWith("0") && charSequence.toString().trim().length() > 1) {
                if (charSequence.toString().charAt(1) != '.') {
                    etSpeedFixed.setText(charSequence.subSequence(0, 1));
                    etSpeedFixed.setSelection(1);
                }
            }
            //如果直接输入小数点，前面自动补0
            if (charSequence.toString().trim().equals(".")) {
                charSequence = "0" + charSequence;
                etSpeedFixed.setText(charSequence);
                etSpeedFixed.setSelection(2);
            }
            if (charSequence.length() > 0) {
                if (Float.floatToIntBits(Float.parseFloat(charSequence.toString())) > Float.floatToIntBits(5f)) {
                    charSequence = charSequence.subSequence(0, charSequence.length() - 1);
                    etSpeedFixed.setText(charSequence);
                    etSpeedFixed.setSelection(charSequence.length());
                }
                if (charSequence.length() > 0) {
                    speedFixedSetting = Float.parseFloat(charSequence.toString());
                    LogUtils.d(TAG, "速度修正系数为：" + speedFixedSetting);
                }
            } else {
                speedFixedSetting = 0;
            }
            SPHelper.save("speedFixed", Objects.requireNonNull(etSpeedFixed.getText()).toString());
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    // 方向修正设置输入框监听器
    private final TextWatcher etDirectionFixedWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //如果有小数点,限制小数位数不大于2个
            if (charSequence.toString().contains(".")) {
                if (charSequence.length() - 1 - charSequence.toString().indexOf(".") > 2) {
                    charSequence = charSequence.toString().subSequence(0, charSequence.toString().indexOf(".") + 3);
                    etDirectionFixed.setText(charSequence);
                    //设置光标在末尾
                    etDirectionFixed.setSelection(charSequence.length());
                }
            }
            //除了小数开头不能为0，而且开头不允许连续出现0
            if (charSequence.toString().startsWith("0") && charSequence.toString().trim().length() > 1) {
                if (charSequence.toString().charAt(1) != '.') {
                    etDirectionFixed.setText(charSequence.subSequence(0, 1));
                    etDirectionFixed.setSelection(1);
                }
            }
            //如果直接输入小数点，前面自动补0
            if (charSequence.toString().trim().equals(".")) {
                charSequence = "0" + charSequence;
                etDirectionFixed.setText(charSequence);
                etDirectionFixed.setSelection(2);
            }
            if (charSequence.length() > 0 && !charSequence.toString().equals("-")) {
                if (Float.floatToIntBits(Float.parseFloat(charSequence.toString())) > Float.floatToIntBits(5f)) {
                    charSequence = charSequence.subSequence(0, charSequence.length() - 1);
                    etDirectionFixed.setText(charSequence);
                    etDirectionFixed.setSelection(charSequence.length());
                }
                if (charSequence.length() > 0) {
                    directionFixedSetting = Float.parseFloat(charSequence.toString());
                    LogUtils.d(TAG, "方向修正系数为：" + directionFixedSetting);
                }
            } else {
                directionFixedSetting = 0;
            }
            SPHelper.save("directionFixed", Objects.requireNonNull(etDirectionFixed.getText()).toString());
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    // 定速模式按钮触摸监听
    private final View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!isConnected) {
                        Toast.makeText(mContext, "请先连接小车", Toast.LENGTH_SHORT).show();
                    }
                    vibrator.vibrate(100);
                    if (view.getId() == R.id.btnGoHead) {
                        goHead = true;
                        btnGoBack.setEnabled(false);
                    }
                    if (view.getId() == R.id.btnGoBack) {
                        goBack = true;
                        btnGoHead.setEnabled(false);
                    }
                    if (view.getId() == R.id.btnTurnLeft) {
                        turnLeft = true;
                        btnTurnRight.setEnabled(false);
                    }
                    if (view.getId() == R.id.btnTurnRight) {
                        turnRight = true;
                        btnTurnLeft.setEnabled(false);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (view.getId() == R.id.btnGoHead) {
                        goHead = false;
                        btnGoBack.setEnabled(true);
                    }
                    if (view.getId() == R.id.btnGoBack) {
                        goBack = false;
                        btnGoHead.setEnabled(true);
                    }
                    if (view.getId() == R.id.btnTurnLeft) {
                        turnLeft = false;
                        btnTurnRight.setEnabled(true);
                    }
                    if (view.getId() == R.id.btnTurnRight) {
                        turnRight = false;
                        btnTurnLeft.setEnabled(true);
                    }
                    break;
                default:
                    break;
            }
            if (rbSetting.isChecked() || rbFix.isChecked()) {
                StringBuilder cmd = new StringBuilder();
                cmd.append("0800000111");
                // 前进后退
                if (goHead) {
                    cmd.append(MathUtils.addZeroForLeft(Integer.toHexString((int) (speedSetting * speedFixedSetting)), 4));
                } else if (goBack) {
                    cmd.append(MathUtils.addZeroForLeft(Integer.toHexString((int) (-speedSetting * speedFixedSetting)), 4));
                } else {
                    cmd.append("0000");
                }
                // 保留位
                cmd.append("00000000");
                // 转弯
                if (turnLeft) {
                    cmd.append(MathUtils.addZeroForLeft(Integer.toHexString((int) (directionSetting * directionFixedSetting)), 4));
                } else if (turnRight) {
                    cmd.append(MathUtils.addZeroForLeft(Integer.toHexString((int) (-directionSetting * directionFixedSetting)), 4));
                } else {
                    cmd.append("0000");
                }
                command = cmd.toString();
            }
            return false;
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessage(EventMsg msg) {
        switch (msg.getTag()) {
            case Constant.TCP_CLIENT_CONNECTED_FAIL:
                // 连接断开
            case Constant.TCP_CLIENT_DESTINATION_NOT_FOUND:
                // 未找到目标地址
            case Constant.TCP_CLIENT_ERROR_WRITE:
                // 客户端模式发送数据失败
                LogUtils.d("tcpClient", "连接断开");
                // 客户端模式断开连接
                isConnected = false;
                ivConnectStatus.setImageResource(R.drawable.ic_wifi_disconnect);
                // 清空实时数据显示
                tvSpeed.setText("0.0");
                tvDirection.setText("0.0");
                tvCarStatus.setText("");
                tvControlMode.setText("");
                tvVoltage.setText("");
                tvCarErrorCode.setText("");
                // 重新连接TCP Server
                try {
                    tcpClient = new TcpClient();
                    InetAddress ipAddress = InetAddress.getByName(Constant.TCP_SERVER_IP);
                    tcpClient.setInetAddress(ipAddress);
                    tcpClient.setPort(Constant.TCP_SERVER_PORT);
                    executorService.submit(tcpClient);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            case Constant.TCP_CLIENT_CONNECTED_SUCCESS:
                // 连接小车成功
                isConnected = true;
                // 发送指令（发送间隔20ms）
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (sendCommand) {
                            // 如果不是CAN指令模式，则发送模式指令
                            if (!isCanMode && isConnected) {
                                tcpClient.sendMessage("010000042101");
                            }
                            // 如果是CAN模式，则不断发送指令（不超过500ms，建议20ms）
                            if (isCanMode && isConnected && command != null) {
                                tcpClient.sendMessage(command);
                            }
                        }
                    }
                }, 0, Constant.SEND_FREQUENCY);
                break;
            case Constant.TCP_CLIENT_CORRECT_READ:
                // 客户端模式接收到数据
                LogUtils.d(TAG, "收到数据：" + msg.getMsg());
                parseData(msg.getMsg());
                break;
            case Constant.TCP_CLIENT_CORRECT_WRITE:
                // 客户端模式发送数据成功
//                LogUtils.d(TAG, "发送数据成功");
                break;
            case WIFI1:
                // Wifi信号1
                ivConnectStatus.setImageResource(R.drawable.ic_wifi5);
                break;
            case WIFI2:
                // Wifi信号1
                ivConnectStatus.setImageResource(R.drawable.ic_wifi4);
                break;
            case WIFI3:
                // Wifi信号1
                ivConnectStatus.setImageResource(R.drawable.ic_wifi3);
                break;
            case WIFI4:
                // Wifi信号1
                ivConnectStatus.setImageResource(R.drawable.ic_wifi2);
                break;
            case WIFI5:
                // Wifi信号1
                ivConnectStatus.setImageResource(R.drawable.ic_wifi_disconnect);
                break;
            default:
                break;
        }
    }

    /**
     * 数据解析
     *
     * @param dataPackage 原始数据包
     */
    private void parseData(String dataPackage) {
        // 根据长度进行拆包
        StringBuilder message = new StringBuilder(dataPackage);
        boolean exception = false;
        while (message.length() >= 2 && !exception) {
            try {
                // 计算数据包长度：2为数据的长度，占1位2个字节；8为指令ID，占4位8个字节
//                int dataLength = 2 * Integer.parseInt(message.substring(0, 2));
                int totalLength = 2 + 8 + 2 * Integer.parseInt(message.substring(0, 2));
                String data = message.substring(10, totalLength);
//                LogUtils.d(TAG, "数据长度：" + dataLength + "，数据包总长度：" + totalLength + "，id：" + message.substring(2, 10) + "，消息：" + data);
                // 解析数据包
                switch (message.substring(2, 10)) {
                    case "00000211":
                        // 底盘系统状态回馈帧
                        // 车体状态
                        switch (data.substring(0, 2)) {
                            case "00":
                                // 系统正常
                                tvCarStatus.setText("系统正常");
                                break;
                            case "01":
                                // 紧急停车
                                tvCarStatus.setText("紧急停车");
                                break;
                            case "02":
                                // 系统异常
                                tvCarStatus.setText("系统异常");
                                break;
                            default:
                                break;
                        }
                        // 控制模式
                        switch (data.substring(2, 4)) {
                            case "00":
                                // 待机模式
                                isCanMode = false;
                                tvControlMode.setText("待机模式");
                                break;
                            case "01":
                                // CAN指令控制模式
                                isCanMode = true;
                                tvControlMode.setText("CAN指令控制");
                                break;
                            case "02":
                                // 遥控模式
                                isCanMode = false;
                                tvControlMode.setText("遥控模式");
                                break;
                            default:
                                break;
                        }
                        // 电压
                        float voltage = MathUtils.formatFloat(Integer.parseInt(data.substring(4, 8), 16) / 10f, 1);
                        tvVoltage.setText(voltage + "V");
                        // 车体故障信息
                        tvCarErrorCode.setText(data.substring(8, 12));
                        break;
                    case "00000221":
                        // 运动控制回馈帧
                        // 移动速度
                        float speed = MathUtils.formatFloat(Integer.valueOf(data.substring(0, 4), 16).shortValue() / 1000f, 2);
                        tvSpeed.setText(String.valueOf(speed));
                        // 方向
                        double direction = MathUtils.formatDouble(Integer.valueOf(data.substring(12, 16), 16).shortValue() / 1000f * 180 / Math.PI, 2);
                        tvDirection.setText(String.valueOf(direction));
                        break;
                    case "0000043B":
                        // 转向零点设定反馈
                        float zero = Integer.valueOf(data.substring(0, 4), 16).shortValue();
                        tvReadZero.setText(String.valueOf(zero));
                        break;
                    default:
                        break;
                }
                // 删掉当前数据包的内容
                message.replace(0, totalLength, "");
            } catch (Exception e) {
                e.printStackTrace();
                exception = true;
            }
        }
    }

    /**
     * 获取WIFI信号的强弱
     */
    public void getWifiStrength() {
        // 获得WifiManager
        final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        // 使用定时器,每隔1秒获得一次信号强度值
        Timer timer = new Timer();
        EventMsg msg = new EventMsg();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isConnected) {
                    // 如果已连接，则进行信号强度判断，否则即使连接了Wifi，也不判断强度
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    //获得信号强度值
                    int level = wifiInfo.getRssi();
                    LogUtils.d("Wifi", "信号强度：" + level);
                    //根据获得的信号强度发送信息
                    if (level <= 0 && level >= -50) {
                        msg.setTag(MainActivity.WIFI1);
                    } else if (level < -50 && level >= -70) {
                        msg.setTag(MainActivity.WIFI2);
                    } else if (level < -70 && level >= -80) {
                        msg.setTag(MainActivity.WIFI3);
                    } else if (level < -80 && level >= -100) {
                        msg.setTag(MainActivity.WIFI4);
                    } else {
                        msg.setTag(MainActivity.WIFI5);
                    }
                } else {
                    msg.setTag(MainActivity.WIFI5);
                }
                EventBus.getDefault().post(msg);
            }
        }, 1000, 1000);
    }

    /**
     * 获取点击事件
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (isHideInput(view, ev)) {
                HideSoftInput(view.getWindowToken());
                view.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 判定是否需要隐藏
     */
    private boolean isHideInput(View v, MotionEvent ev) {
        if ((v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left + v.getWidth();
            return !(ev.getX() > left) || !(ev.getX() < right) || !(ev.getY() > top) || !(ev.getY() < bottom);
        }
        return false;
    }

    /**
     * 隐藏软键盘
     */
    private void HideSoftInput(IBinder token) {
        if (token != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendCommand = false;
        tcpClient.close();
        EventBus.getDefault().unregister(this);
        executorService.shutdown();
    }

}