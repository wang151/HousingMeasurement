package com.example.mjbimsdk;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.mjbimsdk.MJReqBean.SdkCreateHouse;
import com.example.mjbimsdk.MJReqBean.SdkOpenHouse;

import cn.jiamm.lib.JMMView;
import cn.jiamm.lib.JMMViewBaseActivity;
import cn.jiamm.lib.MJSdk;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class SurveyActivity extends JMMViewBaseActivity {

	private final int RESET_SURFACEVIEW_BACKGROUND = 10;
	private final int SHOW_MESSAGE = 11;
	private final int CLOSE_SURVEY = 12;

	private SurveyActivity pThis;
	private LoadHouseListener mLoadHouseListener;
	private boolean bNewHouse;
	private String sOrderNo, sHouseId;
	private boolean bHouseClosed = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
        if(!MJSdk.getInstance().IsInited())
        {
            Log.d("demo", "demo has been recycled, reinit ...");
            MJSdk.getInstance().InitEnv(this);

            //第二步：初始化配置
            MJReqBean.SdkAppConfig config = new MJReqBean.SdkAppConfig();
            config.packageName = getPackageName();
            config.apiUrl = "http://api.jiamm.cn/jiamm";
            //String storagePath = Environment.getExternalStorageDirectory().getPath() + "/jmm/";
            String storagePath = getFilesDir().getPath() + "/jmm/";
            config.storagePath = storagePath;
            //先获取时间戳，再调用generateSign生成签名信息
            long tm = System.currentTimeMillis();
            config.timeStamp = String.valueOf(tm);
            config.generateSign();
            String sret = MJSdk.getInstance().Execute(config.getString());
            Log.d("demo", "demo has been recycled, reinit over.");
        }

		super.onCreate(savedInstanceState);
		setContentView(R.layout.survey_view);

		pThis = this;
		//绘图视图初始化
		m_JMMView = (JMMView)findViewById(R.id.m_JiaMMView);
		m_JMMView.setActivity(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

		bNewHouse = getIntent().getBooleanExtra("newhouse",false);
        sOrderNo = getIntent().getStringExtra("orderNo");
		if(!bNewHouse)
			sHouseId = getIntent().getStringExtra("houseId");
		
		//回调消息设置
		mLoadHouseListener = new LoadHouseListener();
		MJSdk.getInstance().regMessageListener(mLoadHouseListener);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		m_JMMView.onResume();
	}

	@Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        Log.e("demo", "onSaveInstanceState.");
        outState.putString("newhouse", sOrderNo);
        outState.putString("houseId", sHouseId);
        outState.putBoolean("newhouse", false);
    }


    @Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		m_JMMView.onWindowFocusChanged(hasFocus);
	}

	@Override
	protected void onPause() {
		super.onPause();
		m_JMMView.onPause();
	}
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		if(!bHouseClosed)
		{
            JSONObject jreq = new JSONObject();
            MJReqBean.SdkCloseHouse req = new MJReqBean.SdkCloseHouse();
            final String sreq = req.getString();
            Log.d("demo", "req:"+sreq);
            m_JMMView.runOnGLThread(new Runnable() {
                public void run() {
                    String ret = MJSdk.getInstance().Execute(sreq);
                    //Log.d("demo", "create ret:"+ret);
                }
            });

            return;
		}
        Intent intent = new Intent();
        intent.putExtra("houseId", sHouseId);
        intent.putExtra("orderNo", sOrderNo);
        setResult(RESULT_OK, intent);

		super.finish();
		MJSdk.getInstance().unregMessageListener(mLoadHouseListener);
	}
	
	//继承回调类，实现回调函数
	private class LoadHouseListener implements MJSdk.MessageListener{

		@Override
		public void onSdkEvent(String arg0) {
			//SdkMsgInfo ret = GSONUtil.gson.fromJson(arg0, new TypeToken<SdkMsgInfo>() {}.getType());
			JSONObject jobj;
			try {
				jobj = new JSONObject(arg0);
				String cmd = jobj.optString("cmd");
				if(cmd.equals("event_OnInitEnd"))
				{
					if(bNewHouse)
						createNewHouse();
					else
						openHouse();
				}
				else if(cmd.equals("back_home"))
				{
					bHouseClosed = true;
					finish();
					//mHandler.sendEmptyMessage(CLOSE_SURVEY);
				}
				else if(cmd.equals("complete_house"))
				{
					bHouseClosed = true;
					JSONObject jparams = jobj.optJSONObject("params");
					int beddingRooms = jparams.optInt("beddingRooms");
					int livingRooms = jparams.optInt("livingRooms");
					int washingRooms = jparams.optInt("washingRooms");

					finish();
//					mHandler.sendEmptyMessage(CLOSE_SURVEY);
				}
				else if (cmd.equals("submit_house"))
                {
                    JSONObject jparams = jobj.optJSONObject("params");
                    int errorCode = jparams.optInt("errorCode");
                    String errorMessage = jparams.optString("errorMessage");

                }
				else if(cmd.equals("house_empty"))
				{
					Bundle b = new Bundle();
					b.putString("notify", "房屋数据未加载完成");
					Message msg = mHandler.obtainMessage(SHOW_MESSAGE);
					msg.setData(b);
					mHandler.sendMessage(msg);
				}
				else if (cmd.equals("created_house_info"))
				{
					JSONObject jparams = jobj.optJSONObject("params");
                    JSONObject jhouse = jparams.optJSONObject("houseInfo");
					sHouseId = jhouse.optString("_id");
					Log.d("demo", "create house id:"+sHouseId);
				}
				else if(cmd.equals("open_package_detail_url"))
				{
					String url = jobj.optString("url");

					Intent intent = new Intent(pThis, PackageDetailActivity.class);
					intent.putExtra("url", url);
					startActivity(intent);
				}
				else if (cmd.equals("warrning_message"))
                {
                    String txt = jobj.optString("msg");
                    Bundle b = new Bundle();
                    b.putString("notify", txt);
                    Message msg = mHandler.obtainMessage(SHOW_MESSAGE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
	//创建房屋
	private void createNewHouse()
	{
		SdkCreateHouse req = new SdkCreateHouse();
		req.data.village = "test_create";
		req.data.buildingNo = "100";
		req.data.contractNo = sOrderNo;
		req.data.employeeNo = "001";
		
		final String renderInfo = req.getString();
		m_JMMView.runOnGLThread(new Runnable() {
			public void run() {
				String ret = MJSdk.getInstance().Execute(renderInfo);
				Log.d("demo", "create ret:"+ret);
				mHandler.sendEmptyMessage(RESET_SURFACEVIEW_BACKGROUND);
			}
		});
	}
	
	//打开房屋
	private void openHouse()
	{
		SdkOpenHouse req = new SdkOpenHouse();
		req._id.contractNo = sOrderNo;
		//req.id.contractNo = "137";
		
		final String renderInfo = req.getString();
		m_JMMView.runOnGLThread(new Runnable() {
			public void run() {
				String ret = MJSdk.getInstance().Execute(renderInfo);
				Log.d("demo", "open ret:"+ret);
				mHandler.sendEmptyMessage(RESET_SURFACEVIEW_BACKGROUND);
			}
		});
	}
	
	private Handler mHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg) 
		{
			int id = msg.what;
			switch(id)
			{
				case RESET_SURFACEVIEW_BACKGROUND:
				{
					m_JMMView.resetSurfaceViewBackground();
				}
				break;
				case SHOW_MESSAGE:
				{
					Bundle b = msg.getData();
					if(b != null)
					{
						String txt = b.getString("notify");
						if(txt != null && !txt.isEmpty())
							Toast.makeText(pThis, txt, Toast.LENGTH_SHORT).show();
					}
				}
				break;
				case CLOSE_SURVEY:
				{
					finish();
				}
				break;
			}
		};
	};
	
	
}
