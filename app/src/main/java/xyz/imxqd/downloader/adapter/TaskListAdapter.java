package xyz.imxqd.downloader.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import xyz.imxqd.downloader.App;
import xyz.imxqd.downloader.R;
import xyz.imxqd.downloader.model.DownloadTaskInfo;
import xyz.imxqd.downloader.utils.FileUtil;

/**
 * Created by imxqd on 2016/7/4.
 * 下载任务列表的适配器
 */
public class TaskListAdapter extends BaseAdapter{
    private static final File DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    private ArrayList<DownloadTaskInfo> list;
    private HashMap<Long, DoDownloadTask> taskHashMap;
    public TaskListAdapter() {
        Iterator<DownloadTaskInfo> infoIterator =
                DownloadTaskInfo.findAll(DownloadTaskInfo.class);
        list = new ArrayList<>(4);
        taskHashMap = new HashMap<>();
        while (infoIterator.hasNext()) {
            DownloadTaskInfo info = infoIterator.next();
            list.add(info);
        }
    }

    public void update() {
        list.clear();
        Iterator<DownloadTaskInfo> infoIterator =
                DownloadTaskInfo.findAll(DownloadTaskInfo.class);
        while (infoIterator.hasNext()) {
            DownloadTaskInfo info = infoIterator.next();
            list.add(info);
        }
    }

    public void add(DownloadTaskInfo info) {
        long id = info.save();
        update();
        DoDownloadTask task = new DoDownloadTask(id);
        task.execute();
        taskHashMap.put(id, task);
    }

    public void start(long id) {
        DoDownloadTask task = new DoDownloadTask(id);
        task.execute();
        taskHashMap.put(id, task);
    }

    public void stop(long id) {
        DoDownloadTask task = taskHashMap.get(id);
        if(task != null) {
            task.stop();
        } else {
            DownloadTaskInfo info = DownloadTaskInfo.findById(DownloadTaskInfo.class, id);
            if (info != null) {
                info.setState(DownloadTaskInfo.STATE_PAUSE);
                info.save();
                update();
                notifyDataSetChanged();
            }
        }
    }

    public void remove(int pos) {
        DownloadTaskInfo info = list.get(pos);
        stop(info.getId());
        info.delete();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context c = parent.getContext();
        DownloadTaskInfo info = list.get(position);
        LayoutInflater layoutInflater = LayoutInflater.from(c);
        ViewHolder holder;
        if( convertView == null) {
            convertView = layoutInflater.inflate(R.layout.new_task_list_item, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        holder = (ViewHolder) convertView.getTag();
        holder.filename.setText(info.getFilename());
        if(info.getTotalSize() != -1) {
            holder.progress.setText(c.getString(R.string.item_progress
                    ,FileUtil.parseFileSize(info.getCurrentSize())
                    , FileUtil.parseFileSize(info.getTotalSize()))
            );
        } else {
            holder.progress.setText(c.getString(R.string.item_progress
                    ,FileUtil.parseFileSize(info.getCurrentSize())
                    , c.getString(R.string.unkown))
            );
        }

        if(info.getState() == DownloadTaskInfo.STATE_CREATE) {
            holder.state.setText(c.getString(R.string.item_state_create));
        } else if (info.getState() == DownloadTaskInfo.STATE_PAUSE) {
            holder.state.setText(c.getString(R.string.item_state_pause));
        } else if (info.getState() == DownloadTaskInfo.STATE_DOWNLOADING) {
            holder.state.setText(c.getString(R.string.item_state_downloading));
        } else if (info.getState() == DownloadTaskInfo.STATE_ERROR) {
            holder.state.setText(c.getString(R.string.item_state_error));
        } else if (info.getState() == DownloadTaskInfo.STATE_FINISH) {
            holder.state.setText(c.getString(R.string.item_state_finish));
        }
        return holder.itemView;
    }

    public class DoDownloadTask extends AsyncTask<Void, Integer, Boolean> {

        URL url;
        HttpURLConnection conn;
        DownloadTaskInfo info;
        boolean paused = false;

        public DoDownloadTask(long infoId) {
            info = DownloadTaskInfo.findById(DownloadTaskInfo.class, infoId);
            try {
                url = new URL(info.getUrl());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setRequestProperty("Referer", url.toString());
                conn.setRequestProperty("Charset", "UTF-8");
                conn.setRequestProperty("Connection", "Keep-Alive");
                if(info.getState() == DownloadTaskInfo.STATE_PAUSE){
                    conn.setRequestProperty("Range", "bytes="+(info.getCurrentSize())
                            + "-" + info.getTotalSize());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            update();
            notifyDataSetChanged();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                conn.connect();
                System.out.println(conn.getHeaderFields());
                File file;
                int length;
                boolean append = true;
                publishProgress();
                if(info.getState() == DownloadTaskInfo.STATE_CREATE) {
                    length = conn.getContentLength();
                    String accept = conn.getHeaderField("Accept-Ranges");
                    if(length == -1 || (accept != null && accept.contains("none"))) {
                        info.setBreakpoint(false);
                    } else {
                        info.setBreakpoint(true);
                    }
                    String filename = FileUtil.getFileName(conn.getHeaderField("Content-Disposition"));
                    if(filename == null) {
                        filename = FileUtil.getFileName(conn.getURL());
                    }
                    if(filename == null) {
                        filename = FileUtil.createFileName(conn.getContentType());
                    }

                    file = new File(DIR, filename);
                    info.setFilename(filename);
                    info.setPath(file.getAbsolutePath());
                    info.setTotalSize(length);
                    append = false;
                } else if(info.getState() == DownloadTaskInfo.STATE_ERROR) {
                    file = new File(info.getPath());
                    length = info.getTotalSize();
                    append = false;
                } else {
                    file = new File(info.getPath());
                    length = info.getTotalSize();
                }
                info.setState(DownloadTaskInfo.STATE_DOWNLOADING);
                info.save();
                publishProgress();
                InputStream is = conn.getInputStream();
                FileOutputStream os = new FileOutputStream(file, append);
                byte[] buffer = new byte[4 * 1024];
                int readSize;
                int readSum = info.getCurrentSize();
                while (!paused && (readSize = is.read(buffer)) > 0) {
                    readSum += readSize;
                    os.write(buffer, 0, readSize);
                    info.setCurrentSize(readSum);
                    info.save();
                    publishProgress();
                }
                if(length != -1) {
                    if(readSum == length) {
                        info.setState(DownloadTaskInfo.STATE_FINISH);
                    } else if(readSum < length){
                        info.setState(DownloadTaskInfo.STATE_PAUSE);
                    } else {
                        info.setState(DownloadTaskInfo.STATE_ERROR);
                    }
                } else {
                    info.setState(DownloadTaskInfo.STATE_FINISH);
                }
                info.save();
                os.close();
                is.close();
                conn.disconnect();
                publishProgress();
            } catch (IOException e) {
                info.setState(DownloadTaskInfo.STATE_ERROR);
                info.save();
                publishProgress();
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            taskHashMap.remove(info.getId());
            super.onPostExecute(result);
        }

        public void stop() {
            if(info.isBreakpoint()) {
                info.setState(DownloadTaskInfo.STATE_PAUSE);
                info.save();
                publishProgress();
                paused = true;
                cancel(false);
            } else {
                Toast.makeText(App.getApp(),R.string.task_not_support_breakpoint, Toast.LENGTH_SHORT)
                        .show();
            }
        }

        @Override
        protected void onCancelled(Boolean success) {
            taskHashMap.remove(info.getId());
        }

        @Override
        protected void onCancelled() {
            taskHashMap.remove(info.getId());
        }
    }

    public class ViewHolder {
        View itemView;
        TextView filename, progress, state;
        public ViewHolder(View itemView) {
            this.itemView = itemView;
            filename = (TextView) itemView.findViewById(R.id.item_filename);
            progress = (TextView) itemView.findViewById(R.id.item_progress);
            state = (TextView) itemView.findViewById(R.id.item_state);
        }
    }
}
