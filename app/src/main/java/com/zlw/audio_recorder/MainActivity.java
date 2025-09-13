package com.zlw.audio_recorder;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;
import com.zlw.audio_recorder.base.MyApp;
import com.zlw.audio_recorder.widget.AudioView;
import com.zlw.loggerlib.Logger;
import com.zlw.main.recorderlib.RecordManager;
import com.zlw.main.recorderlib.recorder.RecordConfig;
import com.zlw.main.recorderlib.recorder.RecordHelper;
import com.zlw.main.recorderlib.recorder.listener.RecordFftDataListener;
import com.zlw.main.recorderlib.recorder.listener.RecordResultListener;
import com.zlw.main.recorderlib.recorder.listener.RecordSoundSizeListener;
import com.zlw.main.recorderlib.recorder.listener.RecordStateListener;

import java.io.File;
import java.util.Locale;

public class MainActivity extends ComponentActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    Button btRecord;
    Button btStop;
    TextView tvState;
    TextView tvSoundSize;
    TextView tvRecordDuration;
    RadioGroup rgAudioFormat;
    RadioGroup rgSimpleRate;
    RadioGroup tbEncoding;
    RadioGroup tbSource;
    AudioView audioView;
    private boolean isStart = false;
    private boolean isPause = false;
    private long startTime = 0;
    private final RecordManager recordManager = RecordManager.getInstance();
    private MediaProjectionManager mediaProjectionManager;
    private Handler timeHandler = new Handler();
    private final Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            if (isStart && !isPause) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedTime / 1000) % 60;
                int minutes = (int) (elapsedTime / (1000 * 60));
                tvRecordDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                timeHandler.postDelayed(timeRunnable, 1000);
            }
        }
    };
    private final ActivityResultLauncher<Intent> screenCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), result.getData());
                    recordManager.setMediaProjection(mediaProjection);
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initAudioView();
        initEvent();
        initRecord();
        AndPermission.with(this)
                .runtime()
                .permission(new String[]{Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE,
                        Permission.RECORD_AUDIO})
                .start();
    }

    private void initView() {
        btRecord = findViewById(R.id.btRecord);
        btStop = findViewById(R.id.btStop);
        tvState = findViewById(R.id.tvState);
        btRecord = findViewById(R.id.btRecord);
        tvSoundSize = findViewById(R.id.tvSoundSize);
        tvRecordDuration = findViewById(R.id.tvRecordDuration);
        rgAudioFormat = findViewById(R.id.rgAudioFormat);
        rgSimpleRate = findViewById(R.id.rgSimpleRate);
        tbEncoding = findViewById(R.id.tbEncoding);
        audioView = findViewById(R.id.audioView);
        tbSource = findViewById(R.id.tbSource);
        btRecord.setOnClickListener(this);
        btStop.setOnClickListener(this);
        findViewById(R.id.jumpFileActivity).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initRecordEvent();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initAudioView() {
        tvState.setVisibility(View.VISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    private void initEvent() {
        rgAudioFormat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbPcm) {
                    recordManager.changeFormat(RecordConfig.RecordFormat.PCM);
                } else if (checkedId == R.id.rbMp3) {
                    recordManager.changeFormat(RecordConfig.RecordFormat.MP3);
                } else if (checkedId == R.id.rbWav) {
                    recordManager.changeFormat(RecordConfig.RecordFormat.WAV);
                }
            }
        });

        rgSimpleRate.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb8K) {
                    recordManager.changeRecordConfig(recordManager.getRecordConfig().setSampleRate(8000));
                } else if (checkedId == R.id.rb16K) {
                    recordManager.changeRecordConfig(recordManager.getRecordConfig().setSampleRate(16000));
                } else if (checkedId == R.id.rb44K) {
                    recordManager.changeRecordConfig(recordManager.getRecordConfig().setSampleRate(44100));
                } else if (checkedId == R.id.rb48K) {
                    recordManager.changeRecordConfig(recordManager.getRecordConfig().setSampleRate(48000));
                }
            }
        });

        tbEncoding.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb8Bit) {
                    recordManager.changeRecordConfig(recordManager.getRecordConfig().setEncodingConfig(AudioFormat.ENCODING_PCM_8BIT));
                } else if (checkedId == R.id.rb16Bit) {
                    recordManager.changeRecordConfig(recordManager.getRecordConfig().setEncodingConfig(AudioFormat.ENCODING_PCM_16BIT));
                }
            }
        });

        tbSource.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbMic) {
                    recordManager.setSource(RecordConfig.SOURCE_MIC);
                } else if (checkedId == R.id.rbSystem) {
                    recordManager.setSource(RecordConfig.SOURCE_SYSTEM);
                    mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                    screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent());
                }
            }
        });
    }

    private void initRecord() {
        recordManager.init(MyApp.getInstance(), true);
        recordManager.changeFormat(RecordConfig.RecordFormat.WAV);
        String recordDir = String.format(Locale.getDefault(), "%s/SimpleAudioRecorder/",
                Environment.getExternalStorageDirectory().getAbsolutePath());
        recordManager.changeRecordDir(recordDir);
        Logger.i(TAG, "recordDir: %s", recordDir);
        initRecordEvent();
    }

    private void initRecordEvent() {
        recordManager.setRecordStateListener(new RecordStateListener() {
            @Override
            public void onStateChange(RecordHelper.RecordState state) {
                Logger.i(TAG, "onStateChange %s", state.name());

                switch (state) {
                    case PAUSE:
                        tvState.setText("暂停中");
                        break;
                    case IDLE:
                        tvState.setText("空闲中");
                        break;
                    case RECORDING:
                        tvState.setText("录音中");
                        break;
                    case STOP:
                        tvState.setText("停止");
                        break;
                    case FINISH:
                        tvState.setText("录音结束");
                        tvSoundSize.setText("---");
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(String error) {
                Logger.i(TAG, "onError %s", error);
            }
        });
        recordManager.setRecordSoundSizeListener(new RecordSoundSizeListener() {
            @Override
            public void onSoundSize(int soundSize) {
                tvSoundSize.setText(String.format(Locale.getDefault(), "%s db", soundSize));
            }
        });
        recordManager.setRecordResultListener(new RecordResultListener() {
            @Override
            public void onResult(File result) {
                Toast.makeText(MainActivity.this, "录音文件： " + result.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
        });
        recordManager.setRecordFftDataListener(new RecordFftDataListener() {
            @Override
            public void onFftData(byte[] data) {
                audioView.setWaveData(data);
            }
        });
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btRecord) {
            doPlay();
        } else if (id == R.id.btStop) {
            doStop();
        } else if (id == R.id.jumpFileActivity) {
            // 传递录音文件路径
            Intent intent = new Intent(this, FileActivity.class);
            String recordDir = String.format(Locale.getDefault(), "%s/SimpleAudioRecorder/",
                    Environment.getExternalStorageDirectory().getAbsolutePath());
            intent.putExtra("recordFilePath", recordDir);
            Log.d("recordFilePath", recordDir);
            startActivity(intent);
        }
    }

    private void doStop() {
        recordManager.stop();
        btRecord.setText("开始");
        isPause = false;
        isStart = false;
        timeHandler.removeCallbacks(timeRunnable);
    }

    private void doPlay() {
        if (isStart) {
            recordManager.pause();
            btRecord.setText("开始");
            isPause = true;
            isStart = false;
            timeHandler.removeCallbacks(timeRunnable);
        } else {
            if (isPause) {
                recordManager.resume();
            } else {
                recordManager.start();
                startTime = System.currentTimeMillis();
            }
            btRecord.setText("暂停");
            isStart = true;
            isPause = false;
            timeHandler.postDelayed(timeRunnable, 1000);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //nothing
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Handler getTimeHandler() {
        return timeHandler;
    }

    public void setTimeHandler(Handler timeHandler) {
        this.timeHandler = timeHandler;
    }
}
