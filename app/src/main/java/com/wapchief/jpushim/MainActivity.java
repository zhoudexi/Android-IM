package com.wapchief.jpushim;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.flyco.tablayout.listener.CustomTabEntity;
import com.flyco.tablayout.listener.OnTabSelectListener;
import com.flyco.tablayout.utils.UnreadMsgUtils;
import com.flyco.tablayout.widget.MsgView;
import com.flyco.tablayout.CommonTabLayout;
import com.squareup.picasso.Picasso;
import com.wapchief.jpushim.activity.AboutActivity;
import com.wapchief.jpushim.activity.AddFriendsActivity;
import com.wapchief.jpushim.activity.GroupCreateActivity;
import com.wapchief.jpushim.activity.SettingActivity;
import com.wapchief.jpushim.activity.UserActivty;
import com.wapchief.jpushim.activity.WebViewActivity;
import com.wapchief.jpushim.adapter.MessageRecyclerAdapter;
import com.wapchief.jpushim.entity.TabEntity;
import com.wapchief.jpushim.entity.UserStateBean;
import com.wapchief.jpushim.fragment.FragmentFactory;
import com.wapchief.jpushim.framework.base.BaseActivity;
import com.wapchief.jpushim.framework.helper.GreenDaoHelper;
import com.wapchief.jpushim.framework.helper.SharedPrefHelper;
import com.wapchief.jpushim.framework.network.NetWorkManager;
import com.wapchief.jpushim.framework.system.MyDataCleanManager;
import com.wapchief.jpushim.framework.system.SystemStatusManager;
import com.wapchief.jpushim.framework.utils.BitMapUtils;
import com.wapchief.jpushim.framework.utils.StringUtils;
import com.wapchief.jpushim.framework.utils.UIUtils;
import com.wapchief.jpushim.greendao.RequestListDao;
import com.wapchief.jpushim.greendao.SearchAddDao;
import com.wapchief.jpushim.greendao.model.RequestList;
import com.wapchief.jpushim.greendao.model.SearchAdd;
import com.wapchief.jpushim.view.MyAlertDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.jiguang.analytics.android.api.JAnalyticsInterface;
import cn.jiguang.api.JCoreInterface;
import cn.jiguang.imui.commons.ImageLoader;
import cn.jpush.im.android.api.ContactManager;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.callback.GetUserInfoCallback;
import cn.jpush.im.android.api.content.EventNotificationContent;
import cn.jpush.im.android.api.event.BaseNotificationEvent;
import cn.jpush.im.android.api.event.ContactNotifyEvent;
import cn.jpush.im.android.api.event.ConversationRefreshEvent;
import cn.jpush.im.android.api.event.MessageEvent;
import cn.jpush.im.android.api.event.OfflineMessageEvent;
import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.model.Message;
import cn.jpush.im.android.api.model.UserInfo;
import cn.jpush.im.android.eventbus.EventBus;
import cn.jpush.im.api.BasicCallback;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {

    @BindView(R.id.title_bar_back)
    ImageView mTitleBarBack;
    @BindView(R.id.title_bar_title)
    TextView mTitleBarTitle;
    @BindView(R.id.title_options_img)
    ImageView mTitleOptionsImg;
    @BindView(R.id.main_root_vp)
    ViewPager mMainRootVp;
    @BindView(R.id.main_nv)
    NavigationView mMainNv;
    @BindView(R.id.side_main)
    DrawerLayout mSideMain;
    @BindView(R.id.main_root)
    LinearLayout mMainRoot;
    @BindView(R.id.main_root_tab)
    CommonTabLayout mMainRootTab;
    @BindView(R.id.title_options_tv)
    TextView mTitleOptionsTv;
    //NavigationViewHeader
    private LinearLayout nav_header_ll;
    private ImageView nav_header_img;
    private TextView nav_header_name, nav_header_id;
    private ArrayList<Fragment> mFragments = new ArrayList<>();
    private ArrayList<CustomTabEntity> mTabEntities = new ArrayList<>();
    private String[] mTitles = {"??????", "?????????", "??????"};
    private int[] tabIconGray = new int[]{R.mipmap.icon_tab_message_gray, R.mipmap.icon_tab_im_gray, R.mipmap.icon_tab_d_gray};
    private int[] tabIcon = new int[]{R.mipmap.icon_tab_message, R.mipmap.icon_tab_im, R.mipmap.icon_tab_d};
    private SharedPrefHelper helper;
    private GreenDaoHelper daoHelper;
    private RequestListDao dao;
    private ImageLoader imageLoader;
    private UserInfo userInfo;
    @Override
    protected int setContentView() {
        return R.layout.activity_main;

    }

    @Override
    protected void initView() {

        //?????????
        new SystemStatusManager(this).setTranslucentStatus(R.drawable.shape_titlebar);
        userInfo = JMessageClient.getMyInfo();
        initLoginType();
        userInfo = JMessageClient.getMyInfo();
        daoHelper = new GreenDaoHelper(this);
        dao = daoHelper.initDao().getRequestListDao();
        helper = SharedPrefHelper.getInstance();
        mTitleOptionsImg.setVisibility(View.VISIBLE);
        mMainRootVp.setOffscreenPageLimit(3);
        //??????NavigationView
        initNaView();
        initSideDrawer();
        initTab();
        initPageAdapter();
        initNVHeader();
        JAnalyticsInterface.onPageStart(this,this.getClass().getCanonicalName());
    }
    /*??????????????????,????????????????????????*/
    private void initLoginType() {
        File file;
        Log.e("login_type", "" + getIntent().getIntExtra("LOGINTYPE", 0));
        if (getIntent().getIntExtra("LOGINTYPE", 0) == 1) {
                Bitmap bitmap = BitMapUtils.drawable2Bitmap(getResources().getDrawable(R.drawable.icon_user));
            //????????????
            String path = Environment.getExternalStorageDirectory()
                    .getPath() + "/Pic";
            //????????????????????????
            file = new File(path);
//            Log.e("file", file.getPath());
            //???????????????
            file.mkdirs();
            //?????????????????????????????????
            long i = System.currentTimeMillis();
            //??????????????????
            file = new File(file.toString() + "/" + i + ".png");
//            Log.e("fileNew", file.getPath());
            //???????????????
            OutputStream out = null;
            try {
                out = new FileOutputStream(file.getPath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            //????????????
            try {
                boolean flag = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            } catch (Exception e) {
                e.printStackTrace();
            }
            JMessageClient.updateUserAvatar(file, new BasicCallback() {
                @Override
                public void gotResult(int i, String s) {
                    if (i == 0) {
                        Log.e("initUserInfo", "???????????????");
                    } else {
                        Log.e("initUserInfo", "???????????????");
                    }
                }
            });
            userInfo.setAddress("??????");
            userInfo.setGender(UserInfo.Gender.valueOf(StringUtils.string2contant("??????")));
            userInfo.setBirthday(System.currentTimeMillis());
            userInfo.setSignature("???????????????????????????");
            userInfo.setNickname(userInfo.getUserName());
            JMessageClient.updateMyInfo(UserInfo.Field.all, userInfo, new BasicCallback() {
                @Override
                public void gotResult(int i, String s) {
                    if (i == 0) {

                    }
                }
            });
        }
    }


    /*????????????????????????*/
    public void onEventMainThread(final ContactNotifyEvent event) {

        Log.e("Log:????????????", "???????????????" + event.getFromUsername().toString() + "\n?????????"
                + event.getFromUsername());
        //?????????????????????
        if (JMessageClient.getMyInfo().getUserName().equals("1006") || JMessageClient.getMyInfo().getUserName() == "1006")
            ContactManager.acceptInvitation(event.getFromUsername(), "", new BasicCallback() {
                @Override
                public void gotResult(int i, String s) {
                    if (i == 0) {
                        Log.e("Log:?????????????????????", event.getFromUsername() + "");
                    } else {
                        Log.e("Log:???????????????", s + "");
                    }
                }
            });
        //????????????????????????
        mMainRootTab.showDot(1);
        mMainRootTab.setMsgMargin(1, -6, 5);
        MsgView rtv_2_2 = mMainRootTab.getMsgView(1);
        if (rtv_2_2 != null) {
            //??????????????????????????????
            UnreadMsgUtils.setSize(rtv_2_2, UIUtils.dip2px(this, 7.5f));
        }
        //???????????????????????????????????????.????????????????????????????????????????????????
        List<RequestList> list = dao.queryBuilder()
                .where(RequestListDao.Properties.UserName.eq(event.getFromUsername())).build().list();
        dao.deleteInTx(list);
        //??????
        JMessageClient.getUserInfo(event.getFromUsername(),     new GetUserInfoCallback() {
            @Override
            public void gotResult(int i, String s, UserInfo userInfo) {
                if (i == 0)
                    dao.insert(new RequestList(null, event.getReason().toString(), event.getFromUsername().toString(), userInfo.getNickname(), "", userInfo.getAvatarFile().toURI().toString()));
            }
        });

    }
    /*?????????NavigationView????????????*/
    private void initNVHeader() {
//        mMainNv.setItemTextColor(getResources().getColorStateList(R.drawable.nav_select_tv,null));
        mMainNv.setItemIconTintList(null);
        View headerView = mMainNv.getHeaderView(0);
        nav_header_ll = (LinearLayout) headerView.findViewById(R.id.nav_header_ll);
        nav_header_name = (TextView) headerView.findViewById(R.id.nav_header_name);
        nav_header_name.setText(JMessageClient.getMyInfo().getNickname());
        nav_header_id = (TextView) headerView.findViewById(R.id.nav_header_id);
        nav_header_id.setText("ID:  " + helper.getUserId());
        nav_header_img = (ImageView) headerView.findViewById(R.id.nav_header_img);
        Picasso.with(MainActivity.this)
                .load(JMessageClient.getMyInfo().getAvatarFile())
                .placeholder(R.mipmap.icon_user)
                .into(nav_header_img);
        nav_header_ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, UserActivty.class);
                startActivity(intent);
            }
        });

    }

    /*?????????tab??????*/
    private void initTab() {
        //???xml?????????????????????????????????????????????
        mMainRootTab.setTextSelectColor(getResources().getColor(R.color.colorTheme));
        mMainRootTab.setTextUnselectColor(getResources().getColor(R.color.colorText));
        for (int i = 0; i < mTitles.length; i++) {
            mTabEntities.add(new TabEntity(mTitles[i], tabIcon[i], tabIconGray[i]));
        }

        mMainRootTab.setTabData(mTabEntities);

        mMainRootVp.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

    }

    /*??????????????????*/
    private void initMsgCount() {

        Log.e("Log:???????????????", JMessageClient.getAllUnReadMsgCount() + "");
        if (JMessageClient.getAllUnReadMsgCount() > 0) {
            mMainRootTab.showMsg(0, JMessageClient.getAllUnReadMsgCount());
            mMainRootTab.setMsgMargin(0, -6, 5);
        } else {
            mMainRootTab.clearFocus();
            mMainRootTab.hideMsg(0);
        }

    }

    /*????????????*/
    public void onEvent(final MessageEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initMsgCount();
            }
        });
    }
    @Override
    protected void onResume() {
//        Log.e("info-Main", ""+JMessageClient.getMyInfo());
        initMsgCount();
        mMainRootTab.hideMsg(1);
        initNVHeader();
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        JMessageClient.unRegisterEventReceiver(this);
        JAnalyticsInterface.onPageEnd(this,this.getClass().getCanonicalName());
        super.onDestroy();
    }

    /*??????????????????????????????Main???????????????*/
    private void initSideDrawer() {
        mSideMain.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                //?????????????????????
                WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                Display display = manager.getDefaultDisplay();
                mMainRoot.layout(mMainNv.getRight(), 0, mMainNv.getRight() + display.getWidth(), display.getHeight());
            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    /*NavigationView????????????*/
    private void initNaView() {
        mMainNv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.side_bar1:
                        Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                        intent.putExtra("URL", "https://github.com");
                        startActivity(intent);
                        break;
                    case R.id.side_bar2:
                        Intent intent1 = new Intent(MainActivity.this, WebViewActivity.class);
                        intent1.putExtra("URL", "http://blog.csdn.net");
                        startActivity(intent1);
                        break;
                    case R.id.side_bar3:
                        Intent intent2 = new Intent(MainActivity.this, WebViewActivity.class);
                        intent2.putExtra("URL", "http://www.jianshu.com");
                        startActivity(intent2);
                        break;
                    case R.id.side_bar4:
                        Intent intent3 = new Intent(MainActivity.this, WebViewActivity.class);
                        intent3.putExtra("URL", "http://github.io");
                        startActivity(intent3);
                        break;
                    case R.id.side_bar5:
                        Intent intent4 = new Intent(MainActivity.this, SettingActivity.class);
                        startActivity(intent4);
                        break;
                    case R.id.side_bar6:
                        Intent intent5 = new Intent(MainActivity.this, AboutActivity.class);
                        startActivity(intent5);
                        break;
                    case R.id.side_bar7:
                        String size = null;
                        try {
                            size = MyDataCleanManager.getTotalCacheSize(getApplicationContext());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        showLongToast(MainActivity.this,size);
                        break;
                }

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.side_main);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });


    }

    @Override
    protected void initData() {
        //???????????????????????????
        Picasso.with(MainActivity.this)
                .load(JMessageClient.getMyInfo().getAvatarFile())
                .placeholder(R.mipmap.icon_user)
                .into(mTitleBarBack);
    }

    @OnClick({R.id.title_bar_back, R.id.title_options_img, R.id.title_options_tv})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.title_bar_back:
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.side_main);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
                break;
            case R.id.title_options_img:
                MyAlertDialog dialog = new MyAlertDialog(
                        MainActivity.this, new String[]{"????????????", "??????????????????"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0:
                                        startActivity(new Intent(MainActivity.this, GroupCreateActivity.class));
                                        break;
                                    case 1:
                                        Intent intent = new Intent(MainActivity.this, AddFriendsActivity.class);
                                        startActivity(intent);
                                        break;
                                }
                            }
                        });
                dialog.initDialog(Gravity.RIGHT | Gravity.TOP);
                dialog.dialogSize(200, 0, 0, 55);
                break;
            case R.id.title_options_tv:
                MyAlertDialog dialog1 = new MyAlertDialog(
                        MainActivity.this, new String[]{"????????????", "??????????????????"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0:
                                        showToast(MainActivity.this, "????????????");
                                        break;
                                    case 1:
                                        Intent intent = new Intent(MainActivity.this, AddFriendsActivity.class);
                                        startActivity(intent);
                                        break;
                                }
                            }
                        });
                dialog1.initDialog(Gravity.TOP | Gravity.RIGHT);
                dialog1.dialogSize(200, 0, 0, 55);
                break;
        }
    }

    /*?????????Pager*/
    private void initPageAdapter() {
        mMainRootTab.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelect(int position) {
                titleChange(position);
                mMainRootVp.setCurrentItem(position);
            }

            @Override
            public void onTabReselect(int position) {
//                Log.e("positionTAB", ":"+position);
                if (position == 0) {
//                    mMainRootTab.showMsg(0, mRandom.nextInt(100) + 1);
//                    UnreadMsgUtils.show(mTabLayout_2.getMsgView(0), mRandom.nextInt(100) + 1);
                }
            }
        });

        mMainRootVp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                titleChange(position);
                mMainRootTab.setCurrentTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mMainRootVp.setCurrentItem(0);
    }

    /*??????title??????*/
    private void titleChange(int position) {
        switch (position) {
            case 0:
                mTitleBarTitle.setText("??????");
                mTitleOptionsImg.setVisibility(View.VISIBLE);
                mTitleOptionsTv.setVisibility(View.GONE);
                break;
            case 1:
                mTitleBarTitle.setText("?????????");
                mTitleOptionsImg.setVisibility(View.GONE);
                mTitleOptionsTv.setVisibility(View.VISIBLE);
                mTitleOptionsTv.setText("??????");
                break;
            case 2:
                mTitleBarTitle.setText("??????");
                mTitleOptionsImg.setVisibility(View.GONE);
                mTitleOptionsTv.setVisibility(View.VISIBLE);
                mTitleOptionsTv.setText("??????");
                break;
        }
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = FragmentFactory.createFragment(position);
            return fragment;

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: add setContentView(...) invocation
        ButterKnife.bind(this);
    }

    class AsyncTaskWrapper extends AsyncTask<Void, Integer, Object>{

        @Override
        protected Object doInBackground(Void... voids) {
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
        }
    }

    /**
     * ????????????
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitBy2Click();
        }
        return false;
    }

    /**
     * ????????????
     */
    private static Boolean isExit = false;

    private void exitBy2Click() {
        Timer tExit = null;
        if (isExit == false) {
            isExit = true; // ????????????
            ToastUtils.showShort("????????????????????????");
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false; // ????????????
                }
            }, 2000); // ??????2?????????????????????????????????????????????????????????????????????????????????

        } else {
            finish();
            System.exit(0);
        }
    }
}
