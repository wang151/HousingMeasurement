package com.example.mjbimsdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.example.mjbimsdk.MJReqBean.SdkAppConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jiamm.bluetooth.MeasureDevice;
import com.jiamm.utils.GSONUtil;

import cn.jiamm.lib.MJSdk;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/*
 * JMMSDK 使用说明：
 * 
 * 第一步：初始化环境
 * JMMController.getInstance().InitEnv(this);
 * 
 * 第二步：初始化配置
 * 		
 *      SdkAppConfig config = MJSdk.getInstance().new SdkAppConfig();
 *      //配置服务器地址
        config.apiUrl = "http://api.jiamm.cn/jiamm";
        //配置存储路径
        String storagePath = Environment.getExternalStorageDirectory().getPath() + "/jmm";
        config.storagePath = storagePath;
		MJSdk.getInstance().Execute(config.getString());

	前两步必须按照以上流程，在sdk第一次加载时初始化；完成这些后就可以创建房屋或打开房屋。具体内容依据协议书写；

	向sdk请求命令：
	MJSdk.getInstance().Execute(String);
	根据协议的命令，已经在MJReqBean中定义了相应的类。
	环境配置：SdkAppConfig；
	创建房屋：SdkCreateHouse
	打开房屋：SdkOpenHouse

	
	接收sdk消息：
		1，继承MJSdk.MessageListener，实现public void onSdkEvent(String arg0)，
		2，注册消息回调
		MJSdk.getInstance().setMessageListener(mLoadHouseListener);
		收到的消息是json字符串，命令字字段是“cmd”，根据cmd字段，区分不同的回调消息，做不同处理；
		
	
	绘图窗口activity初始化：
	m_JMMView = (JMMView)findViewById(R.id.m_JiaMMView);
	m_JMMView.setActivity(this);
	（资源参见res/layout/survey_view.xml）
	<cn.jiamm.lib.JMMView
        android:id="@+id/m_JiaMMView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" >
    </cn.jiamm.lib.JMMView>
 * 
 */
public class MainActivity extends Activity {

	private final int SHOW_MESSAGE = 11;
	private final int NEW_HOUSE = 0;
	private final int OPEN_HOUSE = 1;
	private MainActivity pThis;
	private String sOrderListFile;
	private ArrayList<DemOrder> mList;
	private DemoListAdapter mListAdapter;
	private LoadHouseListener mLoadHouseListener;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pThis = this;
        
        setContentView(R.layout.activity_main);
        TextView tv1 = (TextView)findViewById(R.id.demo_newhouse);
        tv1.setOnClickListener(mClickListener);
        ListView lv = (ListView)findViewById(R.id.order_list);
        
    	mListAdapter = new DemoListAdapter();
    	lv.setAdapter(mListAdapter);
    	
        //第一步：初始化环境
        MJSdk.getInstance().InitEnv(this);
        
        //第二步：初始化配置
        SdkAppConfig config = new SdkAppConfig();
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
		Log.d("demo", "config ret:"+sret);
		
			
		String sPath = storagePath;
		sOrderListFile = sPath + "/orderlist.json";
		loadOrderList();

		//回调消息设置
		mLoadHouseListener = new MainActivity.LoadHouseListener();
		MJSdk.getInstance().regMessageListener(mLoadHouseListener);
    }

    @Override
    public void finish() {
    	// TODO Auto-generated method stub
    	super.finish();
    	
    	//在这个Activity中调用过MJSdk.getInstance().InitEnv(this);就需要在退出的时候
    	//调用设置下测距仪的相关的activity为空，否则会在测距仪自动关闭时发生崩溃
    	MeasureDevice.getInstance().setActivity(null);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
		@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			// TODO Auto-generated method stub
    	super.onActivityResult(requestCode, resultCode, data);
    	switch(requestCode)
		{
    	case NEW_HOUSE:
		{
			String houseId = data.getStringExtra("houseId");
			String orderNo = data.getStringExtra("orderNo");

			for (DemOrder order : mList)
            {
                if(order.name.equals(orderNo))
                {
                    order.id = houseId;
                    break;
                }
            }
            saveOrderList();
			mListAdapter.notifyDataSetInvalidated();
		}
    		break;
    	case OPEN_HOUSE:
    		break;
    	}
    }
    
    private void loadOrderList()
    {   	
    	StringBuffer txt = readFileByChars(sOrderListFile);
    	if(txt.length() == 0)
    		return;
    	String sList = txt.toString();
    	mList = GSONUtil.gson.fromJson(sList, new TypeToken<List<DemOrder>>() {}.getType());
				
    	mListAdapter.notifyDataSetInvalidated();
    }
				
    private void saveOrderList()
	{
    	Type type=new TypeToken<List<DemOrder>>(){}.getType();
		Gson mGson = new Gson();
		String sret = mGson.toJson(mList, type);
    	
    	saveFile(sOrderListFile, sret);
	}

    private class DemOrder{
    	public String name;
    	public String id;
	}
    
    private class OrderHolder{
    	public TextView order_name, order_id, order_delete;
			}
    
    private class DemoListAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			if(mList == null)
				return 0;
			else
				return mList.size();
			}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			OrderHolder holder = null;
			if(convertView == null)
			{
				convertView = LayoutInflater.from( pThis ).inflate( R.layout.list_item, null );
				holder = new OrderHolder();
				holder.order_name = (TextView)convertView.findViewById(R.id.order_name);
				holder.order_delete = (TextView)convertView.findViewById(R.id.order_delete);
				holder.order_id = (TextView)convertView.findViewById(R.id.order_id);

				holder.order_name.setOnClickListener(mClickListener);
				holder.order_delete.setOnClickListener(mClickListener);
				convertView.setTag(holder);
			}
			else
				holder = (OrderHolder)convertView.getTag();
			
			DemOrder order = mList.get(position);
			holder.order_name.setTag(order);
			holder.order_delete.setTag(order);
//			convertView.setOnLongClickListener(new View.OnLongClickListener() {
//				@Override
//				public boolean onLongClick(View v) {
//					DemOrder order = (DemOrder) v.getTag();
//					MJReqBean.SdkDeleteHouse req = new MJReqBean.SdkDeleteHouse();
//					req.contractNo = order.name;
//					MJSdk.getInstance().Execute(req.getString());
//					return false;
//				}
//			});

			holder.order_name.setText(mList.get(position).name);
			holder.order_id.setText(mList.get(position).id);

			return convertView;
		}
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			int id = v.getId();
			switch(id)
			{
			case R.id.demo_newhouse:
			{
				long tm = System.currentTimeMillis();
				String sDemoOrder = String.valueOf(tm);
				DemOrder order = new DemOrder();
				order.name = sDemoOrder;
				if(mList == null)
					mList = new ArrayList();
				mList.add(order);
				saveOrderList();
				
				Intent intent = new Intent(pThis, SurveyActivity.class);
				intent.putExtra("newhouse", true);
				intent.putExtra("orderNo", sDemoOrder);
				startActivityForResult(intent, NEW_HOUSE);
			}
				break;
			case R.id.order_name:
			{
				DemOrder order = (DemOrder)v.getTag();
				Intent intent = new Intent(pThis, SurveyActivity.class);
				intent.putExtra("newhouse", false);
                intent.putExtra("orderNo", order.name);
                intent.putExtra("houseId", order.id);
				startActivityForResult(intent, OPEN_HOUSE);
			}
			break;
			case R.id.order_delete:
			{
				final DemOrder order = (DemOrder)v.getTag();

				new AlertDialog.Builder(pThis)
						.setMessage("您确定要删除房屋?")
						.setPositiveButton("OK", new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface dialog, int which) {
								MJReqBean.SdkDeleteHouse req1 = new MJReqBean.SdkDeleteHouse();
								req1.contractNo = order.name;
								String ret = MJSdk.getInstance().Execute(req1.getString());
								Log.d("demo", ret);
								mList.remove(order);
								mListAdapter.notifyDataSetChanged();
								saveOrderList();

							}
						})
						.setNegativeButton("Cancel", null)
						.create()
						.show();

			}
			break;
//			case R.id.order_layout:
//			{
//				DemOrder order = (DemOrder)v.getTag();
//				SdkUpdateHouseLayout req2 = new SdkUpdateHouseLayout();
//				req2.contractNo = order.name;
//	    		req2.beddingRooms = "2";
//	    		req2.livingRooms = "1";
//	    		req2.washingRooms = "1";
//	    		String ret = MJSdk.getInstance().Execute(req2.getString());
//	    		Log.d("demo", ret);
//			}
//				break;
//			case R.id.imgnote:
//			{
//				Intent intent = new Intent(pThis, com.jiamm.imagenote.JMMImgNoteActivity.class);
//				startActivity(intent);
//			}
//				break;
			}
		}
	};

	//继承回调类，实现回调函数
	private class LoadHouseListener implements MJSdk.MessageListener{
		@Override
		public void onSdkEvent(String arg0){
			JSONObject jobj;
			try {
				jobj = new JSONObject(arg0);
				String cmd = jobj.optString("cmd");

				if (cmd.equals("warrning_message"))
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

	private Handler mHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			int id = msg.what;
			switch(id)
			{
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
			}
		};
	};

	public static void saveFile(String fileName, String txt) {
		FileWriter fw;
		try {
			fw = new FileWriter(fileName);
			fw.write(txt);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	/**
     * 以字符为单位读取文件，常用于读文本，数字等类型的文件
     */
    public static StringBuffer readFileByChars(String fileName) {
        File file = new File(fileName);
        Reader reader = null;
        StringBuffer buf = new StringBuffer();
        
        try {
            //System.out.println("以字符为单位读取文件内容，一次读多个字节：");
            // 一次读多个字符
            char[] tempchars = new char[30];
            int charread = 0;
            reader = new InputStreamReader(new FileInputStream(fileName));
            // 读入多个字符到字符数组中，charread为一次读取字符数
            while ((charread = reader.read(tempchars)) != -1) {
                // 同样屏蔽掉\r不显示
                if ((charread == tempchars.length)
                        && (tempchars[tempchars.length - 1] != '\r')) {
                    System.out.print(tempchars);
                    buf.append(tempchars);
                } else {
                    for (int i = 0; i < charread; i++) {
                        if (tempchars[i] == '\r') {
                            continue;
                        } else {
                            System.out.print(tempchars[i]);
                            buf.append(tempchars[i]);
                        }
                    }
                }
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return buf;
    }


}
