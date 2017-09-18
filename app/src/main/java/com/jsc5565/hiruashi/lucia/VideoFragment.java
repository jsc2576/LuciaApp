package com.jsc5565.hiruashi.lucia;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeStandalonePlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import hiruashi.jsc5565.packingproject.Packing.PackHttpTask;
import hiruashi.jsc5565.packingproject.Packing.PackRecyclerView;
import hiruashi.jsc5565.packingproject.util.ViewUtil;

import static com.jsc5565.hiruashi.lucia.Variables.EndPoint;

public class VideoFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    PackRecyclerView recyclerView;
    Spinner spinner;
    SwipeRefreshLayout video_refresh;
    String ytbList_Path = "/video/download/url";
    String Thumnail_url = "http://img.youtube.com/vi/";
    String YMList_url = "/video/ym/list";

    ArrayList<String> ytb_url; // 유튜브 링크 저장 배열
    ArrayList<String> spinner_res_arr; // 스피너 데이터 저장 배열
    int cur_select; // spinner에서 선택한 아이템 번호
    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.video_fragment, container, false);

        ytb_url = new ArrayList<>();

        spinner = (Spinner)view.findViewById(R.id.spinner);
        video_refresh = (SwipeRefreshLayout)view.findViewById(R.id.video_refresh);
        video_refresh.setOnRefreshListener(this);

        recyclerView = (PackRecyclerView)view.findViewById(R.id.video_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setLayout(R.layout.video_item);
        recyclerView.setIdOrder(R.id.ytb_item, R.id.ytb_thumbnail, R.id.ytb_text);
        recyclerView.setViewOrder(-1, ViewUtil.BITMAP, ViewUtil.TEXT);

        try {
            PackHttpTask packHttpTask = new PackHttpTask(EndPoint + YMList_url);
            packHttpTask.setRequestMethod("POST");
            packHttpTask.setContentType("application/json;charset=UTF-8");

            String result = packHttpTask.execute().get();
            JSONArray res_json = new JSONArray(result);

            spinner_res_arr = new ArrayList<>();
            for(int i=0; i<res_json.length(); i++){
                JSONObject res_obj = res_json.getJSONObject(i);
                spinner_res_arr.add(res_obj.getString("con_ym"));
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, spinner_res_arr);
            spinner.setAdapter(spinnerAdapter);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    cur_select = i;
                    RefreshData();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

        }catch (ExecutionException e){
            e.printStackTrace();
        }catch (InterruptedException e){
            e.printStackTrace();
        }catch (JSONException e){
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
            Toast.makeText(getActivity(), "네트워크를 연결하세요.", Toast.LENGTH_LONG).show();
        }
        //http 통신

        //http 통신 데이터

        return view;
    }

    public void RefreshData(){
        HashMap hashMap = new HashMap();
        hashMap.put("con_ym", spinner_res_arr.get(cur_select));
        VideoListRequest(hashMap);
    }

    public void VideoListRequest(HashMap map){
        PackHttpTask packHttpTask = new PackHttpTask(EndPoint+ ytbList_Path);
        packHttpTask.setRequestMethod("POST");
        packHttpTask.setContentType("application/json;charset=UTF-8");

        JSONObject req_json = new JSONObject(map);
        packHttpTask.setData(req_json.toString());
        try {
            String result_data = packHttpTask.execute().get();
            JSONArray jsonArray = new JSONArray(result_data);

            int count = recyclerView.getCount();
            for(int i=0; i<count; i++){
                recyclerView.removeItem(0);
                ytb_url.remove(0);
            }
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                final String video_nm = jsonObject.getString("video_nm");
                final String video_url = jsonObject.getString("video_url");
                String concert_nm = jsonObject.getString("con_ym");
                final String lcs = jsonObject.getString("lcs");

                ThumbnailTask thumbnailTask = new ThumbnailTask(recyclerView, ytb_url, video_url, Thumnail_url+video_url+"/1.jpg", video_nm+" / "+concert_nm+"\n"+lcs);
                thumbnailTask.execute();
            }

            this.recyclerView.setHolderActionListener(new ViewUtil.HolderActionListner() {
                @Override
                public void getChildHolder(PackRecyclerView.PackRecyclerAdapter.PackViewHolder packViewHolder, int i) {
                    final int pos = i;
                    ((LinearLayout)packViewHolder.getViewData().get(0)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int position = pos;
                            Intent intent = YouTubeStandalonePlayer.createVideoIntent(
                                    getActivity(), Variables.DEVELOPER_KEY, ytb_url.get(position), 0, true, true);
                            startActivity(intent);
                        }
                    });
                }
            });

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e){
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }

    }

    @Override
    public void onRefresh() {
        video_refresh.postDelayed(new Runnable() {
            @Override
            public void run() {
                RefreshData();
                video_refresh.setRefreshing(false);
            }
        },1000);
    }
}

class ThumbnailTask extends AsyncTask<Void, Void, Bitmap> {
    PackRecyclerView packRecyclerView;
    String url, content, key;
    ArrayList<String> ytb_url;

    public ThumbnailTask(PackRecyclerView packRecyclerView, ArrayList ytb_url, String ytb_key, String url, String content){
        this.packRecyclerView = packRecyclerView;
        this.url = url;
        this.content = content;
        this.ytb_url = ytb_url;
        this.key = ytb_key;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {

        try {
            URL url = new URL(this.url);
            URLConnection conn = url.openConnection();
            conn.connect();
            BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
            Bitmap bm = BitmapFactory.decodeStream(bis);
            bis.close();

            return bm;
        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bm) {
        super.onPostExecute(bm);

        this.ytb_url.add(ytb_url.size(), this.key);
        this.packRecyclerView.addItem(packRecyclerView.getCount(), "", bm, content);
    }
}