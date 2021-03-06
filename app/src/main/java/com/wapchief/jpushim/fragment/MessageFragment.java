package com.wapchief.jpushim.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bartoszlipinski.recyclerviewheader2.RecyclerViewHeader;
import com.wapchief.jpushim.R;
import com.wapchief.jpushim.activity.ChatMsgActivity;
import com.wapchief.jpushim.activity.GroupListActivity;
import com.wapchief.jpushim.adapter.MessageRecyclerAdapter;
import com.wapchief.jpushim.entity.MessageBean;
import com.wapchief.jpushim.framework.helper.SharedPrefHelper;
import com.wapchief.jpushim.view.MyAlertDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.content.PromptContent;
import cn.jpush.im.android.api.content.TextContent;
import cn.jpush.im.android.api.enums.ContentType;
import cn.jpush.im.android.api.event.ConversationRefreshEvent;
import cn.jpush.im.android.api.event.MessageEvent;
import cn.jpush.im.android.api.event.MessageRetractEvent;
import cn.jpush.im.android.api.event.OfflineMessageEvent;
import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.model.Message;
import cn.jpush.im.android.api.model.UserInfo;

/**
 * Created by wapchief on 2017/7/18.
 */

public class MessageFragment extends Fragment {

    @BindView(R.id.fragment_main_group)
    RelativeLayout mFragmentMainGroup;
    @BindView(R.id.fragment_main_none)
    TextView mFragmentMainNone;
    @BindView(R.id.fragment_main_rf)
    SwipeRefreshLayout mFragmentMainRf;
    private List<MessageBean> data = new ArrayList<>();
    private List<Conversation> list=new ArrayList<>();
    Conversation conversation;
    @BindView(R.id.fragment_main_rv)
    RecyclerView mFragmentMainRv;
    Unbinder unbinder;
    MessageRecyclerAdapter adapter;
    @BindView(R.id.fragment_main_header)
    RecyclerViewHeader mFragmentMainHeader;
    @BindView(R.id.item_main_img)
    ImageView mItemMainImg;
    @BindView(R.id.item_main_username)
    TextView mItemMainUsername;
    @BindView(R.id.item_main_content)
    TextView mItemMainContent;
    @BindView(R.id.item_main_time)
    TextView mItemMainTime;
    private int groupID = 0;
    MessageBean bean;
    //?????????????????????
    private Message retractMsg;
    Handler handler = new Handler();
    //??????
    HandlerThread mThread;
    private static final int REFRESH_CONVERSATION_LIST = 0x3000;
    private static final int DISMISS_REFRESH_HEADER = 0x3001;
    private static final int ROAM_COMPLETED = 0x3002;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.fragment_main, null);
        unbinder = ButterKnife.bind(this, view);
        JMessageClient.registerEventReceiver(this);
        list= JMessageClient.getConversationList();
        initView();
        return view;

    }


    private void initView() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updataData();
            }
        }, 2000);
        initRefresh();
        initData();
        initGroup();
        onClickItem();
    }

    /*????????????*/
    private void initRefresh() {
        mFragmentMainRf.setColorSchemeResources(
                R.color.color_shape_right
                , R.color.colorAccent
                , R.color.aurora_msg_receive_bubble_default_color
                , R.color.oriange);
        //???????????????????????????
        mFragmentMainRf.post(new Runnable() {
            @Override
            public void run() {
                //??????
                mFragmentMainRf.setRefreshing(true);
            }
        });
        //????????????????????????
        mFragmentMainRf.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //????????????????????????1500
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //????????????
                        updataData();
                    }
                }, 1800);
            }
        });
    }
    private void updataData(){
        data.clear();
        adapter.clear();
        initDataBean();
    }

    @Override
    public void onResume() {
        updataData();
        initGroup();
        super.onResume();
    }


    @Override
    public void onDestroy() {
        JMessageClient.unRegisterEventReceiver(this);
        super.onDestroy();
    }

    /*????????????*/
    public void onEvent(final MessageEvent event) {
        final Message msg = event.getMessage();
        this.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Log.e("Log:?????????", "?????????" + msg.getContentType().name() + "\n" + msg);

                if (JMessageClient.getMyInfo().getUserName() == "1006" || JMessageClient.getMyInfo().getUserName().equals("1006")) {

                    final Message message1 =
                            JMessageClient.createSingleTextMessage(((UserInfo)msg.getTargetInfo()).getUserName(), SharedPrefHelper.getInstance().getAppKey(), "[????????????]????????????????????????");
//                    for (int i=0;i<list.size();i++){
//                        conversation = list.get(i);
//                        Message message=conversation.createSendMessage(new TextContent("[????????????]????????????????????????"));
                        JMessageClient.sendMessage(message1);
//                    }
                }
                updataData();

            }
        });

    }

    /*??????????????????*/
    public void onEvent(MessageRetractEvent event) {
        retractMsg = event.getRetractedMessage();
//        Log.e("messageF", retractMsg+"\n"+((PromptContent)retractMsg.getContent()).getPromptText());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updataData();
            }
        },500);
    }
    /**
     * ??????????????????
     *
     * @param event ??????????????????
     */
    public void onEvent(OfflineMessageEvent event) {
        Conversation conv = event.getConversation();
        Log.e("refreshOffline=====", ":" + conv);
        updataData();
    }
    /**
     * ????????????????????????
     *
     * @param event ?????????????????? ??????????????????
     */
    public void onEvent(ConversationRefreshEvent event) {
        final Conversation conv = event.getConversation();
        this.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e("refresh", "?????????"+conv);
                updataData();
            }
        });
    }


    /*??????item*/
    private void onClickItem() {
        adapter.setOnItemClickListener(new MessageRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (view != null) {
                    Intent intent = new Intent(getActivity(), ChatMsgActivity.class);
                    intent.putExtra("USERNAME", data.get(position).getUserName());
                    intent.putExtra("NAKENAME", data.get(position).getTitle());
                    intent.putExtra("MSGID", data.get(position).getMsgID());
                    intent.putExtra("AVATAR",data.get(position).getImg());
                    intent.putExtra("position", position);
//                    intent.putExtra("bean", data.get(position));
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, final int position) {
                String[] strings = {"????????????"};
                MyAlertDialog dialog = new MyAlertDialog(getActivity(), strings,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (i == 0) {
                                    JMessageClient.deleteSingleConversation(data.get(position).getUserName());
                                    data.remove(position);
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(getActivity(), "????????????", Toast.LENGTH_SHORT).show();

                                }
                            }
                        });
                dialog.initDialog();

            }
        });
    }

    /*??????*/
    private void initGroup() {
        mItemMainImg.setImageDrawable(getResources().getDrawable(R.mipmap.icon_group));
        mItemMainUsername.setText("?????????");
        mItemMainUsername.setTextSize(16);
        mItemMainContent.setText("[???1???????????????]");
        mItemMainContent.setTextColor(Color.parseColor("#E5955D"));
    }

    private void initData() {

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mFragmentMainRv.setLayoutManager(layoutManager);
        adapter = new MessageRecyclerAdapter(data, getActivity());
        //?????????
        mFragmentMainRv.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        mFragmentMainRv.setAdapter(adapter);
        mFragmentMainHeader.attachTo(mFragmentMainRv);

    }

    private void initDataBean() {
        list= JMessageClient.getConversationList();
//        conversation=list.get()
        Log.e("Log:???????????????", list.size()+"");
        if (list.size() <= 0) {
            mFragmentMainNone.setVisibility(View.VISIBLE);
            mFragmentMainRv.setVisibility(View.GONE);
        } else {
            mFragmentMainNone.setVisibility(View.GONE);
            mFragmentMainRv.setVisibility(View.VISIBLE);
            for (int i = 0; i < list.size(); i++) {
                bean = new MessageBean();
                try {
                    //?????????????????????????????????
//                    Log.e("type", list.get(i).getTitle()+","+list.get(i).getLatestMessage().getContent().getContentType());
                    if (list.get(i).getLatestMessage().getContent().getContentType()== ContentType.prompt) {
                        bean.setContent(((PromptContent) (list.get(i).getLatestMessage()).getContent()).getPromptText());
                    }else {
                        bean.setContent(((TextContent) (list.get(i).getLatestMessage()).getContent()).getText());
                    }
                } catch (Exception e) {
                        bean.setContent("?????????????????????");
                    Log.e("Exception:MessageFM", e.getMessage());
                }
                bean.setMsgID(list.get(i).getId());
                bean.setUserName(((UserInfo) list.get(i).getTargetInfo()).getUserName());
                bean.setTitle(list.get(i).getTitle());
                bean.setTime(list.get(i).getUnReadMsgCnt() + "");
                bean.setConversation(list.get(i));
//                Log.e("Log:Conversation", list.get(i).getAllMessage()+"");

                try {
                    bean.setImg(list.get(i).getAvatarFile().toURI() + "");
                } catch (Exception e) {
                }
                data.add(bean);
            }
        }

        mFragmentMainRf.setRefreshing(false);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.fragment_main_group})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.fragment_main_group:
                startActivity(new Intent(getActivity(), GroupListActivity.class));
                break;
        }
    }
}
