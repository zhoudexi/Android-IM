package com.wapchief.jpushim.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;
import com.wapchief.jpushim.MainActivity;
import com.wapchief.jpushim.R;
import com.wapchief.jpushim.entity.DefaultUser;
import com.wapchief.jpushim.entity.MyMessage;
import com.wapchief.jpushim.entity.UserStateBean;
import com.wapchief.jpushim.framework.base.BaseActivity;
import com.wapchief.jpushim.framework.helper.SharedPrefHelper;
import com.wapchief.jpushim.framework.network.NetWorkManager;
import com.wapchief.jpushim.framework.utils.GlideUtil;
import com.wapchief.jpushim.framework.utils.StringUtils;
import com.wapchief.jpushim.framework.utils.TimeUtils;
import com.wapchief.jpushim.view.ChatView;
import com.wapchief.jpushim.view.MyAlertDialog;
import com.wapchief.jpushim.view.MyViewHolder;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.jiguang.imui.chatinput.ChatInputView;
import cn.jiguang.imui.chatinput.listener.OnMenuClickListener;
import cn.jiguang.imui.chatinput.listener.RecordVoiceListener;
import cn.jiguang.imui.chatinput.model.FileItem;
import cn.jiguang.imui.commons.ImageLoader;
import cn.jiguang.imui.commons.models.IMessage;
import cn.jiguang.imui.messages.MessageList;
import cn.jiguang.imui.messages.MsgListAdapter;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.content.ImageContent;
import cn.jpush.im.android.api.content.PromptContent;
import cn.jpush.im.android.api.content.TextContent;
import cn.jpush.im.android.api.content.VoiceContent;
import cn.jpush.im.android.api.enums.ContentType;
import cn.jpush.im.android.api.enums.MessageDirect;
import cn.jpush.im.android.api.event.MessageEvent;
import cn.jpush.im.android.api.event.MessageRetractEvent;
import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.model.Message;
import cn.jpush.im.android.api.model.UserInfo;
import cn.jpush.im.api.BasicCallback;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static cn.jiguang.imui.commons.models.IMessage.MessageType.RECEIVE_TEXT;
import static cn.jiguang.imui.commons.models.IMessage.MessageType.SEND_CUSTOM;
import static cn.jiguang.imui.commons.models.IMessage.MessageType.SEND_FILE;
import static cn.jiguang.imui.commons.models.IMessage.MessageType.SEND_IMAGE;
import static cn.jiguang.imui.commons.models.IMessage.MessageType.SEND_LOCATION;
import static cn.jiguang.imui.commons.models.IMessage.MessageType.SEND_TEXT;
import static cn.jiguang.imui.commons.models.IMessage.MessageType.SEND_VIDEO;
import static cn.jpush.im.android.api.enums.ContentType.prompt;

/**
 * Created by wapchief on 2017/7/19.
 */

public class ChatMsgActivity extends BaseActivity implements ChatView.OnSizeChangedListener,
        View.OnTouchListener {
    @BindView(R.id.title_bar_back)
    ImageView mTitleBarBack;
    @BindView(R.id.title_bar_title)
    TextView mTitleBarTitle;
    @BindView(R.id.title_options_tv)
    TextView mTitleOptionsTv;
    @BindView(R.id.title_options_img)
    ImageView mTitleOptionsImg;

    @BindView(R.id.title)
    LinearLayout mTitle;
    @BindView(R.id.msg_list)
    MessageList mMsgList;
    @BindView(R.id.chat_input)
    ChatInputView mChatInput;
    @BindView(R.id.chat_view)
    ChatView mChatView;
    private SharedPrefHelper helper;

    // ??????????????????
    private int statusBarHeight;
    // ??????????????????
    private int keyboardHeight;
    // ????????????????????????
    private boolean isShowKeyboard;
    private Context mContext;
    private MsgListAdapter<MyMessage> mAdapter;
    private List<MyMessage> mData;
    private List<MyMessage> list = new ArrayList<>();
    private ImageLoader imageLoader;
    //???????????????
    private ImageView imageAvatarSend, imageAvatarReceive;
    private String userName = "";
    //?????????????????????msgid
    private String msgID = "";
    private int position;
    List<Conversation> conversations;
    Conversation conversation;
    private UserInfo userInfo;
    private String imgSend = "R.drawable.ironman";
    private String imgRecrive = "R.drawable.ironman";
    private String imgReAvatar = "";
    MyMessage myMessage;

    InputMethodManager mManager;
    Window mWindow;

    @Override
    protected int setContentView() {
        return R.layout.activity_chat;
    }

    @Override
    protected void initView() {
        this.mManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        editTextHeight();
        helper = SharedPrefHelper.getInstance();
        mContext = ChatMsgActivity.this;
        userInfo = JMessageClient.getMyInfo();
        //???????????????
        JMessageClient.registerEventReceiver(this);
        conversations = JMessageClient.getConversationList();
        userName = getIntent().getStringExtra("USERNAME");
        //????????????????????????
        JMessageClient.enterSingleConversation(userName);
        msgID = getIntent().getStringExtra("MSGID");
        //position????????????????????????????????????
        position = getIntent().getIntExtra("position", 0);
        conversation = conversations.get(position);
        View view = View.inflate(mContext, R.layout.item_receive_photo, null);
        View view1 = View.inflate(mContext, R.layout.item_send_photo, null);
        imageAvatarSend = (ImageView) view.findViewById(R.id.aurora_iv_msgitem_avatar);
        imageAvatarReceive = (ImageView) view1.findViewById(R.id.aurora_iv_msgitem_avatar);

        try {
            imgSend = StringUtils.isNull(userInfo.getAvatarFile().toURI().toString()) ? "R.drawable.icon_user" : userInfo.getAvatarFile().toURI().toString();
            imgRecrive = StringUtils.isNull(conversation.getAvatarFile().toURI().toString()) ? "R.drawable.icon_user" : conversation.getAvatarFile().toURI().toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        mData = getMessages();
        mChatInput.setMenuContainerHeight(heightDifference);
        initTitleBar();
        initMsgAdapter();
        imgReAvatar = getIntent().getStringExtra("AVATAR");
        //???????????????
        GlideUtil.loadUserHeadImg(this, userInfo.getAvatarFile(), imageAvatarSend);
        //????????????
        GlideUtil.loadUserHeadImg(this, imgReAvatar, imageAvatarReceive);
        mTitleBarBack.setVisibility(View.VISIBLE);
        initInputView();
    }

    int heightDifference = 0;

    /*?????????????????????*/
    private void editTextHeight() {
        mChatInput.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                //??????????????????????????????
                ChatMsgActivity.this.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
                //?????????????????????
                int screenHeight = ChatMsgActivity.this.getWindow().getDecorView().getRootView().getHeight();
                //????????????????????????????????????????????? ?????????????????????????????? ????????????0 ????????????????????????????????????
                if (screenHeight - r.bottom > 0) {
                    heightDifference = screenHeight - r.bottom;
                }
//                LogUtils.e(heightDifference + "");
            }
        });
    }

    /*???????????????*/
    private void initInputView() {

        mChatInput.setMenuClickListener(new OnMenuClickListener() {
            @Override
            public boolean onSendTextMessage(CharSequence charSequence) {
                sendMessage(charSequence.toString());
                return false;
            }

            @Override
            public void onSendFiles(List<FileItem> list) {

            }

            @Override
            public boolean switchToMicrophoneMode() {
                return true;
            }

            @Override
            public boolean switchToGalleryMode() {
                return true;
            }

            @Override
            public boolean switchToCameraMode() {
                return true;
            }

            @Override
            public boolean switchToEmojiMode() {
                return true;
            }
        });
        //??????
//        mChatInput.getRecordVoiceButton().setRecordVoiceListener(new RecordVoiceListener() {
//            @Override
//            public void onStartRecord() {
//                //????????????????????????
//                File rootDir = mContext.getFilesDir();
//                String fileDir = rootDir.getAbsolutePath() + "/voice";
//                mChatInput.getRecordVoiceButton().setVoiceFilePath(fileDir, TimeUtils.date2ms(
//                        Calendar.getInstance(Locale.CHINA)+"","yyyy_MMdd_hhmmss") + "");
//            }
//
//            @Override
//            public void onFinishRecord(File file, int i) {
////                MyMessage message = new MyMessage(null, IMessage.MessageType.SEND_VOICE);
////                message.setMediaFilePath(voiceFile.getPath());
////                message.setDuration(duration);
////                message.setTimeString(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
////                mAdapter.addToStart(message, true);
//            }
//
//            @Override
//            public void onCancelRecord() {
//
//            }
//        });
    }


    //?????????????????????
    private List<MyMessage> getMessages() {
        list = new ArrayList<>();
        for (int i = 0; i < conversation.getAllMessage().size(); i++) {
            MyMessage message;
            //????????????????????????????????????????????????
            if (conversation.getAllMessage().get(i).getDirect() == MessageDirect.send) {
                //????????????????????????
                if (conversation.getAllMessage().get(i).getContent().getContentType().equals(prompt)) {
                    message = new MyMessage(((PromptContent) conversation.getAllMessage().get(i).getContent()).getPromptText(), SEND_TEXT);

                } else {
                    message = new MyMessage(((TextContent) conversation.getAllMessage().get(i).getContent()).getText(), SEND_TEXT);

                }
                Log.e("conversationT", ":" + conversation.getAllMessage().get(i).getContent().getContentType());
                message.setUserInfo(new DefaultUser(userName, "IronMan", (StringUtils.isNull(imgSend)) ? "R.drawable.ironman" : imgSend));
            } else {
                //????????????????????????
                if (conversation.getAllMessage().get(i).getContent().getContentType().equals(prompt)) {
                    message = new MyMessage(((PromptContent) conversation.getAllMessage().get(i).getContent()).getPromptText(), IMessage.MessageType.RECEIVE_TEXT);

                } else {
                    message = new MyMessage(((TextContent) conversation.getAllMessage().get(i).getContent()).getText(), IMessage.MessageType.RECEIVE_TEXT);
                }
                message.setUserInfo(new DefaultUser(JMessageClient.getMyInfo().getUserName(), "DeadPool", (StringUtils.isNull(imgRecrive)) ? "R.drawable.ironman" : imgRecrive));

            }
            message.setPosition(i);
            message.setMessage(conversation.getAllMessage().get(i));
            message.setMsgID(conversation.getAllMessage().get(i).getServerMessageId());
            message.setTimeString(TimeUtils.ms2date("MM-dd HH:mm", conversation.getAllMessage().get(i).getCreateTime()));
            list.add(message);

        }
        Collections.reverse(list);
        conversation.resetUnreadCount();
        return list;
    }

    @Override
    protected void onResume() {
        friendState();
        super.onResume();
    }

    /*??????????????????*/
    public void onEvent(MessageEvent event) {
        final Message message = event.getMessage();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //????????????????????????
                myMessage = new MyMessage(((TextContent) message.getContent()).getText(), IMessage.MessageType.RECEIVE_TEXT);
                myMessage.setMessage(message);
                myMessage.setMsgID(message.getServerMessageId());
                myMessage.setText(((TextContent) message.getContent()).getText() + "");
                myMessage.setTimeString(TimeUtils.ms2date("MM-dd HH:mm", message.getCreateTime()));
                myMessage.setUserInfo(new DefaultUser(JMessageClient.getMyInfo().getUserName(), "DeadPool", imgRecrive));

                if (message.getContentType() == ContentType.text || message.getContentType().equals("text")) {
                    mAdapter.addToStart(myMessage, true);
                    mAdapter.notifyDataSetChanged();
                }
                //?????????????????????????????????
                list.add(myMessage);
            }

        });

        //do your own business
    }

    /*????????????????????????*/
    public void onEvent(MessageRetractEvent event) {
        final Message message = event.getRetractedMessage();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e("dataSize", mData.size() + "," + list.size());
                for (int i = 0; i <= list.size(); i++) {
                    Log.e("messageRetract", "message:" + message + "\nMymessage:" + list.get(i));
                    if (list.get(i).getMsgID() == message.getServerMessageId()) {
                        mAdapter.delete(list.get(i));
                        MyMessage message1 = new MyMessage("[???????????????????????????]", IMessage.MessageType.RECEIVE_TEXT);
                        mAdapter.addToStart(message1, true);
                        mAdapter.notifyDataSetChanged();
                        mAdapter.updateMessage(message1);
                    }
                }
            }

        });

    }

    @Override
    protected void onDestroy() {
        JMessageClient.unRegisterEventReceiver(this);
        JMessageClient.exitConversation();
        super.onDestroy();
        //??????????????????
    }

    //?????????adapter
    private void initMsgAdapter() {
        //???????????????????????????
        imageLoader = new ImageLoader() {
            @Override
            public void loadAvatarImage(ImageView imageView, String s) {
                GlideUtil.loadUserHeadImg(mContext, s, imageView);
            }

            @Override
            public void loadImage(ImageView imageView, String s) {
                //?????????
                GlideUtil.loadUserHeadImg(mContext, s, imageView);
            }

        };


        /**
         * ?????????viewholder
         */
        class MyTexViewHolder extends MyViewHolder<IMessage> {
            public MyTexViewHolder(View itemView, boolean isSender) {
                super(itemView, isSender);
            }
        }

        /**
         * 1???Sender Id: ????????? Id(????????????)???
         * 2???HoldersConfig??????????????????????????????????????????????????? ViewHolder ??????????????????
         * ??????????????????????????????????????????
         * 3???ImageLoader ?????????????????????????????????????????????????????????????????????
         */
        final MsgListAdapter.HoldersConfig holdersConfig = new MsgListAdapter.HoldersConfig();
//
//        holdersConfig.setSenderTxtMsg(MyTexViewHolder.class,R.layout.item_msglist_send);
//        holdersConfig.setReceiverTxtMsg(MyTexViewHolder.class,R.layout.item_msglist_send);
        mAdapter = new MsgListAdapter<MyMessage>(helper.getUserId(), holdersConfig, imageLoader);
        //???????????????????????????????????????????????????????????????
        mAdapter.setOnMsgClickListener(new MsgListAdapter.OnMsgClickListener<MyMessage>() {
            @Override
            public void onMessageClick(MyMessage message) {
                // do something
                if (message.getType() == IMessage.MessageType.RECEIVE_VIDEO
                        || message.getType() == IMessage.MessageType.SEND_VIDEO) {
                    if (!TextUtils.isEmpty(message.getMediaFilePath())) {
                        Intent intent = new Intent(mContext, MsgVideoActivity.class);
//                        intent.putExtra(MsgVideoActivity.VIDEO_PATH, message.getMediaFilePath());
                        startActivity(intent);
                    }
                } else {
                    showToast(ChatMsgActivity.this, "???????????????");
                }
            }
        });

        //????????????
        mAdapter.setMsgLongClickListener(new MsgListAdapter.OnMsgLongClickListener<MyMessage>() {
            @Override
            public void onMessageLongClick(final MyMessage message) {
//                Log.e("mymessage", "id:" + mMsgList.getId()
//                        + "\nposition:" + message.getPosition()
//                        + "\ntype:" + message.getType()
//                        + "\nmessage:"+message.getMessage()
//                +"\nMsgId:"+message.getMsgId());
                String[] strings;
                msgID = message.getMsgId();
//                Log.e("msgIDLong", msgID);

                //??????????????????
                if (message.getType() == SEND_TEXT
                        || message.getType() == SEND_CUSTOM
                        || message.getType() == SEND_FILE
                        || message.getType() == SEND_IMAGE
                        || message.getType() == SEND_LOCATION
                        || message.getType() == SEND_VIDEO) {
                    strings = new String[]{"??????", "??????", "??????", "??????"};
                } else {
                    strings = new String[]{"??????", "??????", "??????"};
                }
                final MyAlertDialog dialog = new MyAlertDialog(ChatMsgActivity.this,
                        strings
                        , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                //?????????????????????????????????????????????????????????
                                if (message.getType().equals(SEND_TEXT)
                                        || message.getType() == SEND_TEXT
                                        || message.getType() == RECEIVE_TEXT) {
                                    if (Build.VERSION.SDK_INT > 11) {
                                        ClipboardManager clipboard = (ClipboardManager) mContext
                                                .getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText("Simple text", message.getText());
                                        clipboard.setPrimaryClip(clip);
                                    } else {
                                        android.text.ClipboardManager clip = (android.text.ClipboardManager) mContext
                                                .getSystemService(Context.CLIPBOARD_SERVICE);
                                        if (clip.hasText()) {
                                            clip.getText();
                                        }
                                    }

                                    showToast(ChatMsgActivity.this, "????????????");
                                } else {
                                    showToast(ChatMsgActivity.this, "??????????????????");
                                }
                                break;

                            case 1:
                                //??????????????????????????????
                                conversation.retractMessage(message.getMessage(), new BasicCallback() {
                                    @Override
                                    public void gotResult(int i, String s) {
                                        if (i == 0) {
                                            showToast(ChatMsgActivity.this, "?????????????????????");
                                            mAdapter.deleteById(message.getMsgId());
//                                            MyMessage myMessage = new MyMessage("", SEND_TEXT);
//                                            message.setTimeString("[????????????????????????]");
//                                            mAdapter.addToStart(message,false);
                                            mAdapter.updateMessage(message);
//                                            mAdapter.notifyDataSetChanged();
                                        } else {
                                            showToast(ChatMsgActivity.this, "???????????????" + s);
                                        }
                                    }
                                });
                                break;
                            case 2:
                                //??????
                                break;
                            default:
                                //2\???????????????
                                conversation.deleteMessage(new Integer(message.getMsgId()));
                                //????????????
                                Log.e("msgIDdelect", message.getMsgId());
                                mAdapter.deleteById(message.getMsgId());
                                mAdapter.notifyDataSetChanged();
                                break;
                        }
                    }
                });

                dialog.initDialog(Gravity.CENTER);
                dialog.dialogSize(200, 0, 0, 55);
            }


        });

        //????????????
        mAdapter.setOnAvatarClickListener(new MsgListAdapter.OnAvatarClickListener<MyMessage>() {
            @Override
            public void onAvatarClick(MyMessage message) {
                DefaultUser userInfo = (DefaultUser) message.getFromUser();
                Intent intent;
                if (message.getType() == SEND_TEXT) {
                    intent = new Intent(mContext, UserActivty.class);
                } else {
                    intent = new Intent(mContext, UserInfoActivity.class);
                    intent.putExtra("USERNAME", userName);
                }
                Log.e("userName", userInfo + "\n" + userName);
                startActivity(intent);
            }
        });

        //????????????
        mAdapter.setMsgResendListener(new MsgListAdapter.OnMsgResendListener<MyMessage>() {
            @Override
            public void onMessageResend(MyMessage message) {
                // resend message here
            }
        });

        MyMessage message = new MyMessage("Hello World", IMessage.MessageType.RECEIVE_TEXT);
        message.setUserInfo(new DefaultUser("0", "Deadpool", "R.drawable.deadpool"));

        mAdapter.addToEnd(mData);
        mAdapter.setOnLoadMoreListener(new MsgListAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int page, int totalCount) {
                if (totalCount <= mData.size()) {
                    Log.i("MessageListActivity", "Loading next page");
//                    loadNextPage();
                }
            }
        });

        mMsgList.setAdapter(mAdapter);
        mAdapter.getLayoutManager().scrollToPosition(0);
    }


    /*????????????*/
    private void loadNextPage() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.addToEnd(mData);
            }
        }, 1000);
    }

    private void initKeyBoard() {
        Log.e("height======", "" + keyboardHeight);

    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (oldh - h > 300) {
//            mChatInput.setMenuContainerHeight(oldh - h);
//            mChatView.setMenuHeight(oldh - h);
        }
        scrollToBottom();
    }

    /*???????????????*/
    private void scrollToBottom() {
        mAdapter.getLayoutManager().scrollToPosition(0);
    }

//    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
//
//        @Override
//        public void onGlobalLayout() {
//            // ??????????????????????????????????????????????????????????????????
//            // ??????ActionBar??????????????????????????????????????????????????????
//            Rect r = new Rect();
//            mChatView.getWindowVisibleDisplayFrame(r);
//
//            // ??????????????????????????????????????????????????????
//            int screenHeight = mChatView.getRootView().getHeight();
//
//            int heightDiff = screenHeight - (r.bottom - r.top);
//
//            // ???????????????????????????heightDiff????????????????????????
//            // ????????????????????????heightDiff???????????????????????????????????????????????????
//            // ??????heightDiff???????????????????????????????????????????????????
//            // ???????????????????????????????????????heightDiff????????????????????????
//            if (keyboardHeight == 0 && heightDiff > statusBarHeight) {
//                keyboardHeight = heightDiff - statusBarHeight;
//            }
//            Log.e("onkeyboardHeight", ":" + keyboardHeight);
//            mChatInput.setMenuContainerHeight(keyboardHeight);
//
//            if (isShowKeyboard) {
//                // ??????????????????????????????????????????heightDiff??????????????????????????????
//                // ?????????????????????????????????
//                if (heightDiff <= statusBarHeight) {
//                    isShowKeyboard = false;
//                    onHideKeyboard();
//                }
//            } else {
//                // ??????????????????????????????????????????heightDiff????????????????????????
//                // ?????????????????????????????????
//                if (heightDiff > statusBarHeight) {
//                    isShowKeyboard = true;
//                    onShowKeyboard();
//                }
//            }
//        }
//    };


    private void onShowKeyboard() {
        // ???????????????????????????????????????
        Log.e("onShowKeyboard : key = ", "" + keyboardHeight);
//        mChatInput.setMenuContainerHeight(keyboardHeight);


    }

    /**
     * ????????????
     */
    private void onHideKeyboard() {
        // ???????????????????????????????????????
        try {
            InputMethodManager inputMethodManage = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManage.hideSoftInputFromWindow(this
                            .getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*?????????*/
    private void initTitleBar() {
        mTitleBarBack.setVisibility(View.INVISIBLE);
        mTitleBarBack.setImageDrawable(getResources().getDrawable(R.mipmap.icon_back));
        mTitleOptionsImg.setVisibility(View.GONE);
        mTitleOptionsTv.setVisibility(View.VISIBLE);
        mTitleBarTitle.setText(userName);
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: add setContentView(...) invocation
        ButterKnife.bind(this);
    }

    @OnClick({R.id.title_bar_back, R.id.title_bar_title, R.id.title_options_tv})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.title_bar_title:
                Intent intent = new Intent(mContext, UserInfoActivity.class);
                intent.putExtra("USERNAME", userName);
                break;
            case R.id.title_bar_back:
                finish();
                //??????????????????
                conversation.resetUnreadCount();
                break;
            case R.id.title_options_tv:
                //
                MyAlertDialog dialog = new MyAlertDialog(this, new String[]{"??????????????????", "?????????????????????"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                showProgressDialog("????????????????????????");
                                if (conversation.deleteAllMessage()) {
                                    mAdapter.clear();
                                    mData.clear();
                                    mAdapter.notifyDataSetChanged();
                                    dismissProgressDialog();
                                }

                                break;
                            case 1:
                                showProgressDialog("????????????");
                                if (JMessageClient.deleteSingleConversation(userName)) {
                                    startActivity(new Intent(ChatMsgActivity.this, MainActivity.class));
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });
                dialog.initDialog(Gravity.RIGHT | Gravity.TOP);
                dialog.dialogSize(200, 0, 0, 55);

                break;
            default:
                break;
        }
    }

    /*?????????????????????????????????????????????*/
    private void sendMessage(String msg) {
        TextContent content = new TextContent(msg);
        Message message1 = conversation.createSendMessage(content);
        final MyMessage myMessage = new MyMessage(msg, SEND_TEXT);
        myMessage.setMessage(message1);
        myMessage.setTimeString(TimeUtils.ms2date("MM-dd HH:mm", message1.getCreateTime()));
        myMessage.setUserInfo(new DefaultUser(JMessageClient.getMyInfo().getUserName(), "DeadPool", imgSend));

        message1.setOnSendCompleteCallback(new BasicCallback() {
            @Override
            public void gotResult(int i, String s) {
                if (i == 0) {
                    mAdapter.addToStart(myMessage, true);
                    mChatInput.getInputView().setText("");
                    mChatInput.dismissMenuLayout();
                    mChatInput.dismissEmojiLayout();
                    mChatInput.dismissPhotoLayout();
                    mChatInput.dismissRecordVoiceLayout();
                    onHideKeyboard();
                } else {
                    Log.e("???????????????", s);
                }
            }
        });
        JMessageClient.sendMessage(message1);
        if (mData != null) {
            mData.clear();
        }
    }

    /*????????????????????????*/
    String state;

    public void friendState() {
        NetWorkManager.isFriendState(userName, new Callback<UserStateBean>() {
            @Override
            public void onResponse(Call<UserStateBean> call, Response<UserStateBean> response) {
                if (response.code() == 200) {
                    if (response.body().online) {
                        state = "[??????]";
                    } else {
                        state = "[??????]";
                    }
                    mTitleBarTitle.setText(userName + state);
                } else {
                    mTitleBarTitle.setText(userName);
                }
            }

            @Override
            public void onFailure(Call<UserStateBean> call, Throwable throwable) {
//                LogUtils.e(throwable.getMessage()+".");
            }
        });

    }

    @Override
//    public void onKeyBoardStateChanged(int state) {
//        switch (state) {
//            case ChatInputView.KEYBOARD_STATE_INIT:
//                ChatInputView chatInputView = mChatView.getChatInputView();
//                if (mManager != null) {
//                    mManager.isActive();
//                }
//                if (chatInputView.getMenuState() == View.INVISIBLE
//                        || (!chatInputView.getSoftInputState()
//                        && chatInputView.getMenuState() == View.GONE)) {
//
//                    mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
//                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    chatInputView.dismissMenuLayout();
//                }
//                break;
//        }
//    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                ChatInputView chatInputView = mChatView.getChatInputView();

                if (view.getId() == chatInputView.getInputView().getId()) {

                    if (chatInputView.getMenuState() == View.VISIBLE
                    ) {
                        chatInputView.dismissMenuLayout();
                        return false;
                    } else {
                        return false;
                    }
                }
                if (chatInputView.getMenuState() == View.VISIBLE) {
                    chatInputView.dismissMenuLayout();
                }
                try {
                    View v = getCurrentFocus();
                    if (mManager != null && v != null) {
                        mManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                        view.clearFocus();
                        chatInputView.setAddStatesFromChildren(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        return false;
    }

}
