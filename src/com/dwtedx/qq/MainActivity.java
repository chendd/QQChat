package com.dwtedx.qq;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dwtedx.qq.adapter.ChatLVAdapter;
import com.dwtedx.qq.adapter.FaceGVAdapter;
import com.dwtedx.qq.adapter.FaceVPAdapter;
import com.dwtedx.qq.bean.ChatInfo;
import com.dwtedx.qq.view.DropdownListView;
import com.dwtedx.qq.view.DropdownListView.OnRefreshListenerHeader;
import com.dwtedx.qq.view.MyEditText;
import com.dwtedx.tst.R;

/**
 * */
public class MainActivity extends Activity implements OnClickListener,OnRefreshListenerHeader{
	private ViewPager mViewPager;
	private LinearLayout mDotsLayout;
	private MyEditText input;
	private Button send;
	private DropdownListView mListView;
	private ChatLVAdapter mLvAdapter;
	
	private LinearLayout chat_face_container;
	private ImageView image_face;//表情图标
	// 7列3行
	private int columns = 6;
	private int rows = 4;
	private List<View> views = new ArrayList<View>();
	private List<String> staticFacesList;
	private LinkedList<ChatInfo> infos = new LinkedList<ChatInfo>();
	private SimpleDateFormat sd;
	
	private String reply="";//模拟回复

	@SuppressLint("SimpleDateFormat")
	private void initViews() {
		mListView = (DropdownListView) findViewById(R.id.message_chat_listview);
		sd=new SimpleDateFormat("MM-dd HH:mm");
		//模拟收到信息
		infos.add(getChatInfoFrom("你好啊！"));
		infos.add(getChatInfoFrom("认识你很高兴#[face/png/f_static_018.png]#"));
		mLvAdapter = new ChatLVAdapter(this, infos);
		mListView.setAdapter(mLvAdapter);
		//表情图标
		image_face=(ImageView) findViewById(R.id.image_face);
		//表情布局
		chat_face_container=(LinearLayout) findViewById(R.id.chat_face_container);
		mViewPager = (ViewPager) findViewById(R.id.face_viewpager);
		mViewPager.setOnPageChangeListener(new PageChange());
		//表情下小圆点
		mDotsLayout = (LinearLayout) findViewById(R.id.face_dots_container);
		input = (MyEditText) findViewById(R.id.input_sms);
		input.setOnClickListener(this);
		send = (Button) findViewById(R.id.send_sms);
		InitViewPager();
		//表情按钮
		image_face.setOnClickListener(this);
		// 发送
		send.setOnClickListener(this);
		
		mListView.setOnRefreshListenerHead(this);
		mListView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if(arg1.getAction()==MotionEvent.ACTION_DOWN){
					if(chat_face_container.getVisibility()==View.VISIBLE){
						chat_face_container.setVisibility(View.GONE);
					}
				}
				return false;
			}
		});
	}
	

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.input_sms://输入框
			if(chat_face_container.getVisibility()==View.VISIBLE){
				chat_face_container.setVisibility(View.GONE);
			}
			break;
		case R.id.image_face://表情
			hideSoftInputView();//隐藏软键盘
			if(chat_face_container.getVisibility()==View.GONE){
				chat_face_container.setVisibility(View.VISIBLE);
			}else{
				chat_face_container.setVisibility(View.GONE);
			}
			break;
		case R.id.send_sms://发送
			reply=input.getText().toString();
			if (!TextUtils.isEmpty(reply)) {
				infos.add(getChatInfoTo(reply));
				mLvAdapter.setList(infos);
				mLvAdapter.notifyDataSetChanged();
				mListView.setSelection(infos.size() - 1);
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						infos.add(getChatInfoFrom(reply));
						mLvAdapter.setList(infos);
						mLvAdapter.notifyDataSetChanged();
						mListView.setSelection(infos.size() - 1);
					}
				}, 1000);
				input.setText("");
			}
			break;

		default:
			break;
		}
	}

	/*
	 * 初始表情 *
	 */
	private void InitViewPager() {
		// 获取页数
		for (int i = 0; i < getPagerCount(); i++) {
			views.add(viewPagerItem(i));
			LayoutParams params = new LayoutParams(16, 16);
			mDotsLayout.addView(dotsItem(i), params);
		}
		FaceVPAdapter mVpAdapter = new FaceVPAdapter(views);
		mViewPager.setAdapter(mVpAdapter);
		mDotsLayout.getChildAt(0).setSelected(true);
	}

	private View viewPagerItem(int position) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.face_gridview, null);//表情布局
		GridView gridview = (GridView) layout.findViewById(R.id.chart_face_gv);
		/**
		 * 注：因为每一页末尾都有一个删除图标，所以每一页的实际表情columns *　rows　－　1; 空出最后一个位置给删除图标
		 * */
		List<String> subList = new ArrayList<String>();
		subList.addAll(staticFacesList
				.subList(position * (columns * rows - 1),
						(columns * rows - 1) * (position + 1) > staticFacesList
								.size() ? staticFacesList.size() : (columns
								* rows - 1)
								* (position + 1)));
		/**
		 * 末尾添加删除图标
		 * */
		subList.add("emotion_del_normal.png");
		FaceGVAdapter mGvAdapter = new FaceGVAdapter(subList, this);
		gridview.setAdapter(mGvAdapter);
		gridview.setNumColumns(columns);
		// 单击表情执行的操作
		gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
				try {
					String png = ((TextView) ((LinearLayout) view).getChildAt(1)).getText().toString();
					if (!png.contains("emotion_del_normal")) {// 如果不是删除图标
						insert(getFace(png));
					} else {
						delete();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		return gridview;
	}

	private SpannableStringBuilder getFace(String png) {
		SpannableStringBuilder sb = new SpannableStringBuilder();
		try {
			/**
			 * 经过测试，虽然这里tempText被替换为png显示，但是但我单击发送按钮时，获取到輸入框的内容是tempText的值而不是png
			 * 所以这里对这个tempText值做特殊处理
			 * 格式：#[face/png/f_static_000.png]#，以方便判斷當前圖片是哪一個
			 * */
			String tempText = "#[" + png + "]#";
			sb.append(tempText);
			sb.setSpan(
					new ImageSpan(MainActivity.this, BitmapFactory
							.decodeStream(getAssets().open(png))), sb.length()
							- tempText.length(), sb.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return sb;
	}

	/**
	 * 向输入框里添加表情
	 * */
	private void insert(CharSequence text) {
		int iCursorStart = Selection.getSelectionStart((input.getText()));
		int iCursorEnd = Selection.getSelectionEnd((input.getText()));
		if (iCursorStart != iCursorEnd) {
			((Editable) input.getText()).replace(iCursorStart, iCursorEnd, "");
		}
		int iCursor = Selection.getSelectionEnd((input.getText()));
		((Editable) input.getText()).insert(iCursor, text);
	}

	/**
	 * 删除图标执行事件
	 * 注：如果删除的是表情，在删除时实际删除的是tempText即图片占位的字符串，所以必需一次性删除掉tempText，才能将图片删除
	 * */
	private void delete() {
		if (input.getText().length() != 0) {
			int iCursorEnd = Selection.getSelectionEnd(input.getText());
			int iCursorStart = Selection.getSelectionStart(input.getText());
			if (iCursorEnd > 0) {
				if (iCursorEnd == iCursorStart) {
					if (isDeletePng(iCursorEnd)) {
						String st = "#[face/png/f_static_000.png]#";
						((Editable) input.getText()).delete(
								iCursorEnd - st.length(), iCursorEnd);
					} else {
						((Editable) input.getText()).delete(iCursorEnd - 1,
								iCursorEnd);
					}
				} else {
					((Editable) input.getText()).delete(iCursorStart,
							iCursorEnd);
				}
			}
		}
	}

	/**
	 * 判断即将删除的字符串是否是图片占位字符串tempText 如果是：则讲删除整个tempText
	 * **/
	private boolean isDeletePng(int cursor) {
		String st = "#[face/png/f_static_000.png]#";
		String content = input.getText().toString().substring(0, cursor);
		if (content.length() >= st.length()) {
			String checkStr = content.substring(content.length() - st.length(),
					content.length());
			String regex = "(\\#\\[face/png/f_static_)\\d{3}(.png\\]\\#)";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(checkStr);
			return m.matches();
		}
		return false;
	}

	private ImageView dotsItem(int position) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dot_image, null);
		ImageView iv = (ImageView) layout.findViewById(R.id.face_dot);
		iv.setId(position);
		return iv;
	}

	/**
	 * 根据表情数量以及GridView设置的行数和列数计算Pager数量
	 * @return
	 */
	private int getPagerCount() {
		int count = staticFacesList.size();
		return count % (columns * rows - 1) == 0 ? count / (columns * rows - 1)
				: count / (columns * rows - 1) + 1;
	}

	/**
	 * 初始化表情列表staticFacesList
	 */
	private void initStaticFaces() {
		try {
			staticFacesList = new ArrayList<String>();
			String[] faces = getAssets().list("face/png");
			//将Assets中的表情名称转为字符串一一添加进staticFacesList
			for (int i = 0; i < faces.length; i++) {
				staticFacesList.add(faces[i]);
			}
			//去掉删除图片
			staticFacesList.remove("emotion_del_normal.png");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 表情页改变时，dots效果也要跟着改变
	 * */
	class PageChange implements OnPageChangeListener {
		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}
		@Override
		public void onPageSelected(int arg0) {
			for (int i = 0; i < mDotsLayout.getChildCount(); i++) {
				mDotsLayout.getChildAt(i).setSelected(false);
			}
			mDotsLayout.getChildAt(arg0).setSelected(true);
		}

	}

	/**
	 * 发送的信息
	 * @param message
	 * @return
	 */
	private ChatInfo getChatInfoTo(String message) {
		ChatInfo info = new ChatInfo();
		info.content = message;
		info.fromOrTo = 1;
		info.time=sd.format(new Date());
		return info;
	}
	
	/**
	 * 接收的信息
	 * @param message
	 * @return
	 */
	private ChatInfo getChatInfoFrom(String message) {
		ChatInfo info = new ChatInfo();
		info.content = message;
		info.fromOrTo = 0;
		info.time=sd.format(new Date());
		return info;
	}
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				mLvAdapter.setList(infos);
				mLvAdapter.notifyDataSetChanged();
				mListView.onRefreshCompleteHeader();
				break;
			}
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_main);
		initStaticFaces();
		initViews();
	}

	@Override
	public void onRefresh() {
		new Thread() {
			@Override
			public void run() {
				try {
					sleep(1000);
					Message msg = mHandler.obtainMessage(0);
					mHandler.sendMessage(msg);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	public void hideSoftInputView() {
		InputMethodManager manager = ((InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE));
		if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
			if (getCurrentFocus() != null)
				manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

}
