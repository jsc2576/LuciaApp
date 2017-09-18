package com.jsc5565.hiruashi.lucia;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import hiruashi.jsc5565.packingproject.Packing.PackHttpTask;
import hiruashi.jsc5565.packingproject.Packing.PackRecyclerView;
import hiruashi.jsc5565.packingproject.util.ViewUtil;

import static com.jsc5565.hiruashi.lucia.Variables.EndPoint;
import static com.jsc5565.hiruashi.lucia.Variables.S3_EndPoint;

public class GallaryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener{

    PackRecyclerView recyclerView;
    SwipeRefreshLayout gallery_refresh;

    String ImgList_Path = "/gallery/download";

    View view;

    int offset = 0;
    final int limit = 5;
    int width;

    HashMap<Integer, String> url_map;

    boolean is_bottom = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.gallery_fragment, container, false);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels/2;

        gallery_refresh = (SwipeRefreshLayout)view.findViewById(R.id.gallery_refresh);
        gallery_refresh.setOnRefreshListener(this);
        // recyclerview
        recyclerView = (PackRecyclerView)view.findViewById(R.id.gallery_recyclerview);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setLayout(R.layout.gallery_item);
        recyclerView.setIdOrder(R.id.text, R.id.gallery_download, R.id.gallery_img);
        recyclerView.setViewOrder(ViewUtil.TEXT, ViewUtil.IMAGE_RESOURCE, ViewUtil.BITMAP);


        // 스크롤로 바닥까지 닿았을 때 새로운 이미지 가져오는 동작 구현
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(is_bottom && newState == RecyclerView.SCROLL_STATE_IDLE){
                    RefreshData();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager)recyclerView.getLayoutManager();
                int totalItemCount = layoutManager.getItemCount();
                int[] i = {0,1}; //두개의 행
                int[] last_visible = layoutManager.findLastVisibleItemPositions(i);

                if(last_visible[0] == totalItemCount-1 || last_visible[1] == totalItemCount-1){ // 각각에 대해서 체크
                    if(totalItemCount-1 < offset*limit){ // offset으로 체크해서 중복되서 가져오는 동작 방지
                        is_bottom = true;
                    }
                    else{
                        is_bottom = false;
                    }
                }
            }
        });
        // httptask
        PackHttpTask packHttpTask = new PackHttpTask(EndPoint+ImgList_Path);
        packHttpTask.setRequestMethod("POST");
        packHttpTask.setContentType("application/json;charset=UTF-8");

        HashMap map = new HashMap();
        map.put("offset", Integer.toString(offset*limit));
        map.put("limit", Integer.toString(limit));
        JSONObject req_json = new JSONObject(map);
        packHttpTask.setData(req_json.toString());

        addItem(packHttpTask);


        //upload button
        ImageView upload_btn = (ImageView)view.findViewById(R.id.upload_btn);
        upload_btn.setOnClickListener(this);

        return view;
    }

    public void RefreshData(){
        PackHttpTask packHttpTask = new PackHttpTask(EndPoint+ImgList_Path);
        packHttpTask.setRequestMethod("POST");
        packHttpTask.setContentType("application/json;charset=UTF-8");

        HashMap map = new HashMap();
        map.put("offset", Integer.toString(offset*limit));
        map.put("limit", Integer.toString(limit));
        JSONObject req_json = new JSONObject(map);
        packHttpTask.setData(req_json.toString());

        addItem(packHttpTask);
    }

    public void addItem(PackHttpTask packHttpTask){

        try {//json 처리
            Log.i("req_data", packHttpTask.getData());
            String result_data = packHttpTask.execute().get();
            JSONArray jsonArray = new JSONArray(result_data);

            url_map = new HashMap<>();

            //parsing json
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String img_path = jsonObject.getString("img_path");
                String img_file_nm = jsonObject.getString("img_file_nm");
                String img_nm = jsonObject.getString("img_nm");
                String lcs = jsonObject.getString("lcs");
                String img_url = jsonObject.getString("img_url");

                url_map.put(offset*limit+i, img_url);
                BitmapTask bitmapTask = new BitmapTask(getActivity(), recyclerView, url_map, img_url, img_nm, lcs, width); // 이미지 url 처리
                bitmapTask.execute();

            }
            if(jsonArray.length() != 0) {
                offset++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e){
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
            Toast.makeText(getContext(), "네트워크를 연결해주세요.", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onRefresh() {
        gallery_refresh.postDelayed(new Runnable() {
            @Override
            public void run() {
                int count = recyclerView.getCount();

                for(int i=0; i<count; i++){
                    recyclerView.removeItem(0);
                }
                offset=0;
                RefreshData();
                gallery_refresh.setRefreshing(false);
            }
        },1000);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(getActivity(), UploadWebActivity.class);
        startActivity(intent);
    }
}

class BitmapTask extends AsyncTask<Void, Void, Bitmap>{
    PackRecyclerView packRecyclerView;
    String url, img_nm, lcs;
    Context context;
    HashMap<Integer, String> url_map;
    int width;

    public BitmapTask(Context context, PackRecyclerView packRecyclerView, HashMap url_map, String url, String img_nm, String lcs, int width){
        this.packRecyclerView = packRecyclerView;
        this.url = url;
        this.img_nm = img_nm;
        this.lcs = lcs;
        this.context = context;
        this.url_map = url_map;
        this.width = width;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        try{
            URL url = new URL(this.url);
            URLConnection conn = url.openConnection();
            conn.connect();
            BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());

            Bitmap bm = BitmapFactory.decodeStream(bis); //받아온 이미지 buffer를 bitmap으로 변환
            bis.close();

            bm = Bitmap.createScaledBitmap(bm, width, bm.getHeight()*width/ bm.getWidth(), true);

            return bm;

        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);

        if(bitmap != null) {
            this.packRecyclerView.addItem(this.packRecyclerView.getCount(), lcs, R.drawable.ic_download, bitmap);

            this.packRecyclerView.setHolderActionListener(new ViewUtil.HolderActionListner() {
                @Override
                public void getChildHolder(PackRecyclerView.PackRecyclerAdapter.PackViewHolder packViewHolder, int i) {
                    final int position = i; // 아이템 position

                    // 아이템별 다운로드 클릭 이벤트
                    ((ImageView)packViewHolder.getViewData().get(1)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //다운로드 비동기 통신
                            DownloadTask downloadTask = new DownloadTask(url_map.get(position));
                            try {
                                //다운로드 시작
                                Boolean result = downloadTask.execute().get();

                                if(result){//성공하였을 경우
                                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_LONG).show();
                                }
                                else{ // 실패하였을 경우
                                    Toast.makeText(context, "저장에 실패하였습니다.", Toast.LENGTH_LONG).show();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }//getChildHolder
            });//setHolderActionListener
        }//if
    }//onPostExecute
}

class DownloadTask extends AsyncTask<Void, Void, Boolean>{

    String url;

    String save_folder = "/lucia_image"; // 저장될 폴더

    DownloadTask(String url){
        this.url = url;
    }
    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            String url = this.url;
            URL img_url = new URL(url);
            String savePath = Environment.getExternalStorageDirectory().toString() + save_folder; // 저장 경로
            String save_nm = url.substring(url.lastIndexOf("/"), url.length()); // 파일 이름

            File dir = new File(savePath);

            //상위 디렉토리가 존재하지 않을 경우 생성
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String img_nm = save_nm.substring(0, save_nm.indexOf(".")); // 이미지 이름
            String extension = save_nm.substring(save_nm.indexOf("."), save_nm.length()); // 이미지 확장자

            //중복되는 이름이 있을 경우
            for(int i=2; new File(savePath + "/" + save_nm).exists() == true; i++) {
                save_nm = img_nm+"("+i+")"+extension;
            }

            //통신 시작
            HttpURLConnection conn = (HttpURLConnection)img_url.openConnection();

            int len = conn.getContentLength();

            byte[] tmpByte = new byte[len];
            InputStream is = conn.getInputStream();
            File file = new File(savePath+"/"+save_nm);

            //파일 저장 스트림 생성

            FileOutputStream fos = new FileOutputStream(file);

            int read;
            //입력 스트림을 파일로 저장
            for (;;) {
                read = is.read(tmpByte);
                if (read <= 0) {
                    break;
                }
                fos.write(tmpByte, 0, read); //file 생성

            }
            is.close();
            fos.close();
            conn.disconnect();

            return true;
        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }
}