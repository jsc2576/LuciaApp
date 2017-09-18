package com.jsc5565.hiruashi.lucia;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import hiruashi.jsc5565.packingproject.Packing.PackHttpTask;
import hiruashi.jsc5565.packingproject.Packing.PackRecyclerView;
import hiruashi.jsc5565.packingproject.util.ViewUtil;

public class CalendarFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    View view;
    PackRecyclerView packGridView;
    SwipeRefreshLayout swipeRefreshLayout;

    int start_date, end_date;
    int month, year;
    Calendar cal;

    String sch_url = "/sch/get";

    int[] color_arr = {Color.rgb(250, 237, 125), Color.rgb(178, 235, 244), Color.rgb(255, 178, 217), Color.rgb(189, 189, 189), Color.rgb(178, 235, 244)};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.calendar_fragment, container, false);

        swipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.calender_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        packGridView = (PackRecyclerView)view.findViewById(R.id.calenderview);
        packGridView.setLayoutManager(new GridLayoutManager(getContext(), 7));

        packGridView.setLayout(R.layout.calendar_item);
        packGridView.setIdOrder(R.id.calenar_linear, R.id.daytext, R.id.schedule1, R.id.schedule2, R.id.schedule3, R.id.schedule4, R.id.schedule5);
        packGridView.setViewOrder(-1, ViewUtil.TEXT, ViewUtil.TEXT, ViewUtil.TEXT, ViewUtil.TEXT, ViewUtil.TEXT, ViewUtil.TEXT);

        cal = Calendar.getInstance();
        month = cal.get(Calendar.MONTH);
        year = cal.get(Calendar.YEAR);

        ImageView prev_arrow = (ImageView)view.findViewById(R.id.before_month);
        ImageView next_arrow = (ImageView)view.findViewById(R.id.after_month);

        prev_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearItem();
                beforeMonth();
                setSchedule();
            }
        });

        next_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearItem();
                afterMonth();
                setSchedule();
            }
        });

        setCalendar();
        setSchedule();
        return view;
    }


    public void beforeMonth(){
        if(month <= 0){
            month = 11;
            year--;
        }
        else{
            month--;
        }
        setCalendar();
    }

    public void afterMonth(){
        if(month >= 11){
            month = 0;
            year++;
        }
        else{
            month++;
        }

        setCalendar();
    }

    public void clearItem(){
        int count = packGridView.getCount();
        for(int i=0; i<count; i++){
            packGridView.removeItem(0);
        }
    }

    public void setCalendar(){
        TextView month_text = (TextView)view.findViewById(R.id.month_text);
        month_text.setText(year+"년 "+(month+1)+"월");

        cal.set(year, month, 1);
        start_date = cal.get(Calendar.DAY_OF_WEEK)-1;
        end_date = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Log.i("month: "+cal.get(Calendar.MONTH), "end_date: "+end_date);

        for(int i=0; i<start_date; i++){ // 시작 요일을 맞추기 위한 설정
            packGridView.addItem(packGridView.getCount(), "", "", "", "", "", "", "");
        }

        for(int i=0; i<end_date; i++){ // 날짜 입력
            packGridView.addItem(packGridView.getCount(), "", ""+(i+1), "", "", "", "", "");
        }
    }

    public void setSchedule(){ // 스케줄 가져오기 및 캘린더에 설정
        PackHttpTask packHttpTask = new PackHttpTask(Variables.EndPoint+sch_url);
        packHttpTask.setRequestMethod("POST");
        packHttpTask.setContentType("application/json;charset=UTF-8");

        HashMap req_data = new HashMap();
        req_data.put("sch_sta_year", year);
        req_data.put("sch_sta_month", month+1);

        JSONObject jsonObject = new JSONObject(req_data);
        packHttpTask.setData(jsonObject.toString());

        try {//스케줄 데이터 통신
            String result = packHttpTask.execute().get();

            JSONArray res_data = new JSONArray(result);
            final ArrayList<int[]> sch_arr = new ArrayList<>();
            final ArrayList<String> sch_nm_arr = new ArrayList<>();

            for(int i=0; i<res_data.length(); i++){

                JSONObject res_obj = res_data.getJSONObject(i);
                int sta_month = res_obj.getInt("sch_sta_month");
                int sta_date = res_obj.getInt("sch_sta_date");
                int end_month = res_obj.getInt("sch_end_month");
                int end_date = res_obj.getInt("sch_end_date");
                String sch_nm = res_obj.getString("sch_nm");

                if(sta_month < month+1){ // 스케줄이 이전달과 현재달에 걸쳐서 있는 경우
                    sta_date = 1;
                }
                else if(end_month > month+1){ // 스케줄이 현재 달과 다음 달에 걸쳐서 있는 경우
                    end_date = this.end_date;
                }
                sch_arr.add(new int[]{sta_date, end_date}); // 시작, 종료 날짜 저장
                sch_nm_arr.add(sch_nm); // 스케줄 이름 저장
            }

            //스케줄 레벨 체크(같은 날의 스케줄이 겹쳐서 보이지 않도록)
            for(int i=sch_arr.size()-1; i>=0; i--){
                int level = 0;
                //마지막꺼부터 설정
                for(int j=i-1; j>=0; j--){
                    //시작날짜 혹은 종료날짜가 다른 스케줄 사이에 걸리는 경우 레벨 증가
                    if((sch_arr.get(i)[0] >= sch_arr.get(j)[0] && sch_arr.get(i)[0] <= sch_arr.get(j)[1]) || (sch_arr.get(i)[1] >= sch_arr.get(j)[0] && sch_arr.get(i)[1] <= sch_arr.get(j)[1])){
                        level++;
                    }
                }
                // 스케줄 배경 색상 설정 및 스케줄 배열 저장
                sch_arr.set(i, new int[]{sch_arr.get(i)[0], sch_arr.get(i)[1], level, color_arr[i%5]});
            }

            packGridView.setHolderActionListener(new ViewUtil.HolderActionListner() {
                @Override
                public void getChildHolder(PackRecyclerView.PackRecyclerAdapter.PackViewHolder packViewHolder, int i) {
                    final int cur_date = i-start_date+1;
                    final ArrayList<Integer> sch_list = new ArrayList<Integer>();

                    TextView sch[] = {(TextView)packViewHolder.getViewData().get(2),
                            (TextView)packViewHolder.getViewData().get(3),
                            (TextView)packViewHolder.getViewData().get(4),
                            (TextView)packViewHolder.getViewData().get(5),
                            (TextView)packViewHolder.getViewData().get(6)};

                    for(int s=0; s<sch.length; s++){
                        sch[s].setBackgroundColor(Color.TRANSPARENT);
                    }

                    for(int j=0; j<sch_arr.size(); j++){
                        if(sch_arr.get(j)[0] <= cur_date && sch_arr.get(j)[1] >= cur_date){
                            sch_list.add(j);
                            TextView textView = null;
                            if(sch_arr.get(j)[2] == 0){
                                textView = (TextView)packViewHolder.getViewData().get(2);
                            }
                            else if(sch_arr.get(j)[2] == 1){
                                textView = (TextView)packViewHolder.getViewData().get(3);
                            }
                            else if(sch_arr.get(j)[2] == 2){
                                textView = (TextView)packViewHolder.getViewData().get(4);
                            }
                            else if(sch_arr.get(j)[2] == 3){
                                textView = (TextView)packViewHolder.getViewData().get(5);
                            }
                            else if(sch_arr.get(j)[2] == 4){
                                textView = (TextView)packViewHolder.getViewData().get(6);
                            }
                            textView.setBackgroundColor(sch_arr.get(j)[3]);
                            textView.setText(sch_nm_arr.get(j));
                        }
                    }

                    ((LinearLayout)packViewHolder.getViewData().get(0)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.i("item click", "success: "+cur_date);

                            if(sch_list.size() > 0) {
                                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                                String sch_cont = "";

                                for(int i=0; i<sch_list.size(); i++){
                                    int pos = sch_list.get(i);
                                    sch_cont += "\n"+(i+1)+". "+sch_nm_arr.get(pos);
                                }
                                alert.setTitle(year+"년 "+(month+1)+"월 "+cur_date+"일");
                                alert.setMessage(sch_cont);
                                alert.setPositiveButton("확인", null);
                                alert.show();
                            }
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
            Toast.makeText(getContext(), "네트워크를 연결하세요.", Toast.LENGTH_LONG).show();
            packGridView.setHolderActionListener(new ViewUtil.HolderActionListner() {
                @Override
                public void getChildHolder(PackRecyclerView.PackRecyclerAdapter.PackViewHolder packViewHolder, int i) {
                    int cur_date = i - start_date + 1;

                    TextView sch[] = {(TextView) packViewHolder.getViewData().get(1),
                            (TextView) packViewHolder.getViewData().get(2),
                            (TextView) packViewHolder.getViewData().get(3),
                            (TextView) packViewHolder.getViewData().get(4),
                            (TextView) packViewHolder.getViewData().get(5)};

                    for (int s = 0; s < sch.length; s++) {
                        sch[s].setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            });
        }
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                clearItem();
                setCalendar();
                setSchedule();
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1000);

    }
}
