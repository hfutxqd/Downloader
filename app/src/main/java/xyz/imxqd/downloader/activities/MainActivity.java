package xyz.imxqd.downloader.activities;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import xyz.imxqd.downloader.R;
import xyz.imxqd.downloader.adapter.SwipeDismissListViewTouchListener;
import xyz.imxqd.downloader.adapter.TaskListAdapter;
import xyz.imxqd.downloader.model.DownloadTaskInfo;
import xyz.imxqd.downloader.utils.FileUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        DialogInterface.OnClickListener, AdapterView.OnItemClickListener, AbsListView.OnScrollListener, AdapterView.OnItemLongClickListener {

    private FloatingActionButton fab;
    private AlertDialog dialog;
    private EditText etUrl;
    private View dialogView;
    private ListView listView;
    private TaskListAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setUpEvents();
    }

    private void initViews() {
        adapter = new TaskListAdapter();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_task, null);
        etUrl = (EditText) dialogView.findViewById(R.id.new_task_url);
        listView = (ListView) findViewById(R.id.task_list);
        TextView view = (TextView) findViewById(R.id.list_empty);
        listView.setEmptyView(view);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(this);
        setSupportActionBar(toolbar);
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.new_task)
                .setPositiveButton(R.string.confirm, this)
                .setNegativeButton(R.string.cancel, null)
                .setView(dialogView)
                .create();
    }

    private void setUpEvents() {
        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(this);
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(listView, new SwipeDismissListViewTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(int position) {
                        DownloadTaskInfo info = (DownloadTaskInfo) adapter.getItem(position);
                        return info.getState() != DownloadTaskInfo.STATE_DOWNLOADING;
                    }

                    @Override
                    public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                        for (int position : reverseSortedPositions) {
                            adapter.remove(position);
                        }
                        adapter.update();
                        adapter.notifyDataSetChanged();
                    }
                });
        listView.setOnTouchListener(touchListener);
        fab.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear) {
            DownloadTaskInfo.deleteAll(DownloadTaskInfo.class);
            adapter.update();
            adapter.notifyDataSetChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        etUrl.setText("");
        dialog.show();
    }

    @Override
    public void onClick(DialogInterface dia, int which) {
        String url = etUrl.getText().toString();
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            Toast.makeText(this, R.string.new_task_url_error, Toast.LENGTH_SHORT).show();
            etUrl.setError(getString(R.string.new_task_url_error));
            return;
        }
        DownloadTaskInfo info = new DownloadTaskInfo();
        info.setCurrentSize(0);
        info.setTotalSize(0);
        info.setUrl(url);
        adapter.add(info);
        listView.smoothScrollToPosition(adapter.getCount() - 1);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DownloadTaskInfo info = (DownloadTaskInfo) adapter.getItem(position);
        if(info.getState() == DownloadTaskInfo.STATE_PAUSE) {
            adapter.start(info.getId());
        } else if(info.getState() == DownloadTaskInfo.STATE_ERROR) {
            adapter.start(info.getId());
        } else if(info.getState() == DownloadTaskInfo.STATE_FINISH) {
            StringBuilder str = new StringBuilder(getString(R.string.task_detail_filename));
            File file = new File(info.getPath());
            String tmp = getString(R.string.file_state);
            if(file.exists()) {
                tmp += getString(R.string.file_state_normal);
            } else {
                tmp += getString(R.string.file_state_removed);
            }
            str.append(info.getFilename()).append("\n");
            str.append(getString(R.string.task_detail_path));
            str.append(info.getPath()).append("\n");
            str.append(getString(R.string.task_detail_filesize));
            str.append(FileUtil.parseFileSize(info.getTotalSize())).append("\n");
            str.append(tmp);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.task_detail_title)
                    .setMessage(str)
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        } else if(info.getState() == DownloadTaskInfo.STATE_DOWNLOADING) {
            adapter.stop(info.getId());
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    int lastFirst = 0;
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if(lastFirst > firstVisibleItem) {
            lastFirst = firstVisibleItem;
            fab.show();
        }else if(lastFirst < firstVisibleItem){
            lastFirst = firstVisibleItem;
            fab.hide();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        final DownloadTaskInfo info = (DownloadTaskInfo) adapter.getItem(position);
        if(info.getState() == DownloadTaskInfo.STATE_FINISH
                || info.getState() == DownloadTaskInfo.STATE_ERROR
                || info.getState() == DownloadTaskInfo.STATE_PAUSE) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.remove_title)
                    .setMessage(R.string.remove_message)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            File file = new File(info.getPath());
                            if(file.exists()) {
                                file.delete();
                            }
                            adapter.remove(position);
                            adapter.update();
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
        return true;
    }
}
