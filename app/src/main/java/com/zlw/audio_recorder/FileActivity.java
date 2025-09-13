package com.zlw.audio_recorder;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.ComponentActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileActivity extends ComponentActivity {
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
        // 接收录音目录路径
        String recordDirPath = getIntent().getStringExtra("recordFilePath");
        Log.d("FileActivity", "Received recordDirPath: " + recordDirPath);

        if (recordDirPath != null) {
            File recordDir = new File(recordDirPath);

            // 检查目录是否存在
            if (recordDir.exists() && recordDir.isDirectory()) {
                // 列出目录中的所有文件
                File[] files = recordDir.listFiles();

                if (files != null) {
                    fileList.clear(); // 清空之前的数据
                    // 处理文件列表
                    for (File file : files) {
                        if (file.isFile()) {
                            fileList.add(file.getName());
                            Log.d("FileActivity", "Found file: " + file.getName());
                        }
                    }
                    adapter.notifyDataSetChanged(); // 更新列表
                }
            }
        } else {
            Log.e("FileActivity", "recordDirPath is null.");
        }
    }


    private void playRecording(String fileName) {
        // 从 Intent 中获取录音目录路径
        String recordDirPath = getIntent().getStringExtra("recordDirPath");

        if (recordDirPath != null) {
            // 构建完整文件路径
            File file = new File(recordDirPath, fileName);
            MediaPlayer mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(file.getAbsolutePath());
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
