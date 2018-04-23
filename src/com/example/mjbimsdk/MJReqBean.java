package com.example.mjbimsdk;

import com.google.gson.Gson;
import com.jiamm.utils.CommonUtil;

public class MJReqBean {

	public static class SdkBaseReq{
		protected String ns;
		protected String cmd;
		public String getString(){
			Gson gson = new Gson();
			String reqInfo = gson.toJson(this);
			return reqInfo;
		}
	}
	
	public static class SdkAppConfig extends SdkBaseReq{
		public String apiUrl;
		public String storagePath;
		public String packageName;
		public String timeStamp;
		
		private String appKey = "RBL4KQEJIU";
		private String appSecret = "S9wX6Cd1c3XCTqIkWdyr8w2sNpk87U";
		private String sign;
		
		public SdkAppConfig(){
			ns = "user";
			cmd = "config";
		}
		public void generateSign()
		{
			String md5Str = appKey + appSecret + timeStamp;
			String sBigSign = CommonUtil.MD5(md5Str);
			sign = sBigSign.toLowerCase();
		}
	}

	public static class SdkCreateHouse extends SdkBaseReq{
		
		public class CreateHouseInfo{
		public String village;
		public String buildingNo;
		public String contractNo;
		public String employeeNo;
		}
		public CreateHouseInfo data;
		
		public SdkCreateHouse(){
			ns = "house";
			cmd = "create_house";
			data = new CreateHouseInfo();
		}
	}
	
	public static class SdkOpenHouse extends SdkBaseReq{
		public class HouseIdInfo{
			public String contractNo;
		}
		public HouseIdInfo _id;
		public String view;
		public boolean onlyMeasurment = false;

		public SdkOpenHouse(){
			ns = "house";
			cmd = "open_house";
			view = "survey_2d";
			_id = new HouseIdInfo();
		}
	}

	public static class SdkCloseHouse extends SdkBaseReq{

		public SdkCloseHouse(){
			cmd = "close_house";
			ns = "house";
		}
	}

	public static class SdkDeleteHouse extends SdkBaseReq{
		public String contractNo;

		public SdkDeleteHouse(){
			cmd = "delete_house";
			ns = "house";
		}
	}

	public static class SdkMsgInfo{
		public String cmd;
		public int errorCode;
		public String errorMessage;
		public String result;
	}
}
