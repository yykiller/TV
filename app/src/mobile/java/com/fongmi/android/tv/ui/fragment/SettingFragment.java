package com.fongmi.android.tv.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.Updater;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.FragmentSettingBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.custom.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.custom.dialog.LiveDialog;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.permissionx.guolindev.PermissionX;

public class SettingFragment extends BaseFragment implements ConfigCallback, SiteCallback, LiveCallback {

    private FragmentSettingBinding mBinding;
    private int type;

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.vodUrl.setText(ApiConfig.getUrl());
        mBinding.liveUrl.setText(LiveConfig.getUrl());
        mBinding.wallUrl.setText(WallConfig.getUrl());
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        mBinding.sizeText.setText(ResUtil.getStringArray(R.array.select_size)[Prefers.getSize()]);
        mBinding.scaleText.setText(ResUtil.getStringArray(R.array.select_scale)[Prefers.getScale()]);
        mBinding.playerText.setText(ResUtil.getStringArray(R.array.select_player)[Prefers.getPlayer()]);
        mBinding.decodeText.setText(ResUtil.getStringArray(R.array.select_decode)[Prefers.getDecode()]);
        mBinding.renderText.setText(ResUtil.getStringArray(R.array.select_render)[Prefers.getRender()]);
    }

    @Override
    protected void initEvent() {
        mBinding.vodHome.setOnClickListener(view -> SiteDialog.create(this).all().show());
        mBinding.liveHome.setOnClickListener(view -> LiveDialog.create(this).show());
        mBinding.vod.setOnClickListener(view -> ConfigDialog.create(this).type(type = 0).show());
        mBinding.live.setOnClickListener(view -> ConfigDialog.create(this).type(type = 1).show());
        mBinding.wall.setOnClickListener(view -> ConfigDialog.create(this).type(type = 2).show());
        mBinding.vodHistory.setOnClickListener(view -> HistoryDialog.create(this).type(type = 0).show());
        mBinding.liveHistory.setOnClickListener(view -> HistoryDialog.create(this).type(type = 1).show());
        mBinding.wallDefault.setOnClickListener(view -> setWallDefault());
        mBinding.wallRefresh.setOnClickListener(view -> setWallRefresh());
        mBinding.version.setOnLongClickListener(view -> onVersion(true));
        mBinding.version.setOnClickListener(view -> onVersion(false));
        mBinding.player.setOnClickListener(view -> setPlayer());
        mBinding.decode.setOnClickListener(view -> setDecode());
        mBinding.render.setOnClickListener(view -> setRender());
        mBinding.scale.setOnClickListener(view -> setScale());
        mBinding.size.setOnClickListener(view -> setSize());
    }

    @Override
    public void setConfig(Config config) {
        checkPermission(config);
    }

    private void checkPermission(Config config) {
        if (config.getUrl().startsWith("file") && !Utils.hasPermission(getActivity())) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> load(config));
        } else {
            load(config);
        }
    }

    private void load(Config config) {
        switch (config.getType()) {
            case 0:
                Notify.progress(getActivity(), true);
                mBinding.vodUrl.setText(config.getUrl());
                ApiConfig.get().clear().config(config).load(getCallback(config));
                break;
            case 1:
                Notify.progress(getActivity(), true);
                mBinding.liveUrl.setText(config.getUrl());
                LiveConfig.get().clear().config(config).load(getCallback(config));
                break;
            case 2:
                mBinding.wallUrl.setText(config.getUrl());
                WallConfig.get().clear().config(config).load(getCallback(config));
                break;
        }
    }

    private Callback getCallback(Config config) {
        return new Callback() {
            @Override
            public void success() {
                setConfig();
            }

            @Override
            public void error(int resId) {
                Notify.show(resId);
                config.delete();
                setConfig();
            }
        };
    }

    private void setConfig() {
        switch (type) {
            case 0:
                Notify.dismiss();
                RefreshEvent.video();
                mBinding.vodUrl.setText(ApiConfig.getUrl());
                mBinding.liveUrl.setText(LiveConfig.getUrl());
                mBinding.wallUrl.setText(WallConfig.getUrl());
                break;
            case 1:
                Notify.dismiss();
                mBinding.liveUrl.setText(LiveConfig.getUrl());
                break;
            case 2:
                mBinding.wallUrl.setText(WallConfig.getUrl());
                break;
        }
    }

    @Override
    public void setSite(Site item) {
        ApiConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void onChanged() {
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
    }

    private boolean onVersion(boolean dev) {
        if (dev) Updater.get().force().dev().start();
        else Updater.get().force().start();
        return true;
    }

    private void setPlayer() {
        int index = Prefers.getPlayer();
        String[] array = ResUtil.getStringArray(R.array.select_player);
        Prefers.putPlayer(index = index == array.length - 1 ? 0 : ++index);
        mBinding.playerText.setText(array[index]);
    }

    private void setDecode() {
        int index = Prefers.getDecode();
        String[] array = ResUtil.getStringArray(R.array.select_decode);
        Prefers.putDecode(index = index == array.length - 1 ? 0 : ++index);
        mBinding.decodeText.setText(array[index]);
    }

    private void setRender() {
        int index = Prefers.getRender();
        String[] array = ResUtil.getStringArray(R.array.select_render);
        Prefers.putRender(index = index == array.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(array[index]);
    }

    private void setScale() {
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_scale).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(array, Prefers.getScale(), (dialog, which) -> {
            mBinding.scaleText.setText(array[which]);
            Prefers.putScale(which);
            dialog.dismiss();
        }).show();
    }

    private void setSize() {
        String[] array = ResUtil.getStringArray(R.array.select_size);
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_size).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(array, Prefers.getSize(), (dialog, which) -> {
            mBinding.sizeText.setText(array[which]);
            Prefers.putSize(which);
            RefreshEvent.size();
            dialog.dismiss();
        }).show();
    }

    private void setWallDefault() {
        WallConfig.refresh(Prefers.getWall() == 4 ? 1 : Prefers.getWall() + 1);
    }

    private void setWallRefresh() {
        WallConfig.get().load();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE) return;
        setConfig(Config.find("file:/" + FileChooser.getPathFromUri(getContext(), data.getData()).replace(FileUtil.getRootPath(), ""), type));
    }
}
