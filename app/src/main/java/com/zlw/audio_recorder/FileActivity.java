package com.zlw.audio_recorder;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.ComponentActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileActivity extends ComponentActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    private ArrayAdapter<String> adapter;
    private List<String> fileList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        ListView listView = findViewById(R.id.file_list);
        fileList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 处理录音文件点击事件
                playRecording(fileList.get(position));
            }
        });

        loadRecordingFiles();
    }

    private void loadRecordingFiles() {
        // 加载录音文件并更新列表
        File directory = new File(getExternalFilesDir(null), "recordings");
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    fileList.add(file.getName());
                }
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void playRecording(String fileName) {
        // 播放录音文件
        File file = new File(getExternalFilesDir(null), "recordings/" + fileName);
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        // 处理其他点击事件
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        // 处理选择事件
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // 处理未选择事件
    }
}
