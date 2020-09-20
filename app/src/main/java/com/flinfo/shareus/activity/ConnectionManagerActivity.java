package com.flinfo.shareus.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentTransaction;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.flinfo.shareus.db.AccessDatabase;
import com.flinfo.shareus.fragment.QRConnectFragment;
import com.flinfo.shareus.fragment.HotspotManagerFragment;
import com.flinfo.shareus.fragment.NetworkDeviceListFragment;
import com.flinfo.shareus.fragment.NetworkManagerFragment;
import com.flinfo.shareus.util.UIConnectionUtils;
import com.flinfo.shareus.model.UITask;
import com.flinfo.shareus.util.NetworkDeviceSelectedListener;
import com.flinfo.shareus.model.TitleSupport;
import com.flinfo.shareus.util.ConnectionSetUpAssistant;
import com.flinfo.shareus.util.AppUtils;
import com.flinfo.shareus.util.ConnectionUtils;
import com.flinfo.shareus.util.NetworkDeviceLoader;
import com.flinfo.shareus.R;
import com.flinfo.shareus.dialog.ManualIpAddressConnectionDialog;
import com.flinfo.shareus.model.NetworkDevice;
import com.flinfo.shareus.service.CommunicationService;
import com.flinfo.shareus.view.RadarScanView;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.android.framework.util.Interrupter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;

public class ConnectionManagerActivity
        extends Activity
        implements SnackbarSupport
{
    public static final String ACTION_CHANGE_FRAGMENT = "com.com.abc.mxcloneshare.intent.action.CONNECTION_MANAGER_CHANGE_FRAGMENT";
    public static final String EXTRA_FRAGMENT_ENUM = "extraFragmentEnum";
    public static final String EXTRA_DEVICE_ID = "extraDeviceId";
    public static final String EXTRA_CONNECTION_ADAPTER = "extraConnectionAdapter";
    public static final String EXTRA_REQUEST_TYPE = "extraRequestType";
    public static final String EXTRA_ACTIVITY_SUBTITLE = "extraActivitySubtitle";

    private final IntentFilter mFilter = new IntentFilter();
    private HotspotManagerFragment mHotspotManagerFragment;
    private QRConnectFragment mBarcodeConnectFragment;
    private NetworkManagerFragment mNetworkManagerFragment;
  //  private NetworkDeviceListFragment mDeviceListFragment;
    private OptionsFragment mOptionsFragment;
    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mToolbarLayout;
    private ProgressBar mProgressBar;
    private String mTitleProvided;
    private RequestType mRequestType = RequestType.RETURN_RESULT;

    private final NetworkDeviceSelectedListener mDeviceSelectionListener = new NetworkDeviceSelectedListener()
    {
        @Override
        public boolean onNetworkDeviceSelected(NetworkDevice networkDevice, NetworkDevice.Connection connection)
        {
            if (mRequestType.equals(RequestType.RETURN_RESULT)) {
                setResult(RESULT_OK, new Intent()
                        .putExtra(EXTRA_DEVICE_ID, networkDevice.deviceId)
                        .putExtra(EXTRA_CONNECTION_ADAPTER, connection.adapterName));

                finish();
            } else {
                ConnectionUtils connectionUtils = ConnectionUtils.getInstance(ConnectionManagerActivity.this);
                UIConnectionUtils uiConnectionUtils = new UIConnectionUtils(connectionUtils, ConnectionManagerActivity.this);

                UITask uiTask = new UITask()
                {
                    @Override
                    public void updateTaskStarted(Interrupter interrupter)
                    {
                        mProgressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void updateTaskStopped()
                    {
                        mProgressBar.setVisibility(View.GONE);
                    }
                };

                NetworkDeviceLoader.OnDeviceRegisteredListener registeredListener = new NetworkDeviceLoader.OnDeviceRegisteredListener()
                {
                    @Override
                    public void onDeviceRegistered(AccessDatabase database, final NetworkDevice device, final NetworkDevice.Connection connection)
                    {
                        createSnackbar(R.string.mesg_completing).show();
                    }
                };

                uiConnectionUtils.makeAcquaintance(ConnectionManagerActivity.this, uiTask,
                        connection.ipAddress, -1, registeredListener);
            }

            return true;
        }

        @Override
        public boolean isListenerEffective()
        {
            return true;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (ACTION_CHANGE_FRAGMENT.equals(intent.getAction())
                    && intent.hasExtra(EXTRA_FRAGMENT_ENUM)) {
                String fragmentEnum = intent.getStringExtra(EXTRA_FRAGMENT_ENUM);

                try {
                    AvailableFragment value = AvailableFragment.valueOf(fragmentEnum);

                    if (AvailableFragment.EnterIpAddress.equals(value))
                        showEnterIpAddressDialog();
                    else
                        setFragment(value);
                } catch (Exception e) {
                    // do nothing
                }
            } else if (mRequestType.equals(RequestType.RETURN_RESULT)) {
                if (CommunicationService.ACTION_DEVICE_ACQUAINTANCE.equals(intent.getAction())
                        && intent.hasExtra(CommunicationService.EXTRA_DEVICE_ID)
                        && intent.hasExtra(CommunicationService.EXTRA_CONNECTION_ADAPTER_NAME)) {
                    NetworkDevice device = new NetworkDevice(intent.getStringExtra(CommunicationService.EXTRA_DEVICE_ID));
                    NetworkDevice.Connection connection = new NetworkDevice.Connection(device.deviceId, intent.getStringExtra(CommunicationService.EXTRA_CONNECTION_ADAPTER_NAME));

                    try {
                        AppUtils.getDatabase(ConnectionManagerActivity.this).reconstruct(device);
                        AppUtils.getDatabase(ConnectionManagerActivity.this).reconstruct(connection);

                        mDeviceSelectionListener.onNetworkDeviceSelected(device, connection);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (mRequestType.equals(RequestType.MAKE_ACQUAINTANCE)) {
                if (CommunicationService.ACTION_INCOMING_TRANSFER_READY.equals(intent.getAction())
                        && intent.hasExtra(CommunicationService.EXTRA_GROUP_ID)) {
                    ViewTransferActivity.startInstance(ConnectionManagerActivity.this,
                            intent.getLongExtra(CommunicationService.EXTRA_GROUP_ID, -1));
                    finish();
                }
            }
        }
    };
    private InterstitialAd interstitialAd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        AudienceNetworkAds.initialize(this);
        interstitialAd = new InterstitialAd(this,getResources().getString(R.string.facebookadId));

        InterstitialAdListener intAdLis=new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {

            }

            @Override
            public void onInterstitialDismissed(Ad ad) {

            }

            @Override
            public void onError(Ad ad, AdError adError) {

            }

            @Override
            public void onAdLoaded(Ad ad) {
                interstitialAd.show();

            }

            @Override
            public void onAdClicked(Ad ad) {

            }

            @Override
            public void onLoggingImpression(Ad ad) {

            }
        };

        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_connection_manager);
      //  AdView mAdMobAdView = (AdView) findViewById(R.id.admob_adview);
     //   AdRequest adRequest = new AdRequest.Builder()
          //      .build();
       // mAdMobAdView.loadAd(adRequest);
        FragmentFactory factory = getSupportFragmentManager().getFragmentFactory();
        Toolbar toolbar = findViewById(R.id.toolbar);
        mAppBarLayout = findViewById(R.id.app_bar);
        mProgressBar = findViewById(R.id.activity_connection_establishing_progress_bar);
        mToolbarLayout = findViewById(R.id.toolbar_layout);
        mOptionsFragment = (OptionsFragment) factory.instantiate(getClassLoader(), OptionsFragment.class.getName());
        mBarcodeConnectFragment = (QRConnectFragment) factory.instantiate(getClassLoader(), QRConnectFragment.class.getName());
        mHotspotManagerFragment = (HotspotManagerFragment) factory.instantiate(getClassLoader(), HotspotManagerFragment.class.getName());
        mNetworkManagerFragment = (NetworkManagerFragment) factory.instantiate(getClassLoader(), NetworkManagerFragment.class.getName());
       // mDeviceListFragment = (NetworkDeviceListFragment) factory.instantiate(getClassLoader(), NetworkDeviceListFragment.class.getName());

        mFilter.addAction(ACTION_CHANGE_FRAGMENT);
        mFilter.addAction(CommunicationService.ACTION_DEVICE_ACQUAINTANCE);
        mFilter.addAction(CommunicationService.ACTION_INCOMING_TRANSFER_READY);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_REQUEST_TYPE))
                try {
                    mRequestType = RequestType.valueOf(getIntent().getStringExtra(EXTRA_REQUEST_TYPE));
                } catch (Exception e) {
                    // do nothing
                }

            if (getIntent().hasExtra(EXTRA_ACTIVITY_SUBTITLE))
                mTitleProvided = getIntent().getStringExtra(EXTRA_ACTIVITY_SUBTITLE);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        checkFragment();
        registerReceiver(mReceiver, mFilter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onBackPressed()
    {
        if (getShowingFragment() instanceof OptionsFragment){
            startActivity(new Intent(this, HistoryActivity.class));
        finish(); }
           // super.onBackPressed();
        else{
        setFragment(AvailableFragment.Options);
    }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home){
            startActivity(new Intent(this, HistoryActivity.class));
        finish(); }
        else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void applyViewChanges(Fragment fragment, String mTitleProvided)
    {
        boolean isOptions = fragment instanceof OptionsFragment;

        if (fragment instanceof DeviceSelectionSupport)
            ((DeviceSelectionSupport) fragment).setDeviceSelectedListener(mDeviceSelectionListener);

        if (getSupportActionBar() != null) {
            CharSequence titleCurrent = fragment instanceof TitleSupport
                    ? ((TitleSupport) fragment).getTitle(ConnectionManagerActivity.this)
                    : getString(R.string.text_connectDevices);

            if (isOptions)
                mToolbarLayout.setTitle(mTitleProvided != null ? mTitleProvided : titleCurrent);
            else
                mToolbarLayout.setTitle(titleCurrent);
        }

        mAppBarLayout.setExpanded(isOptions, true);
    }

    private void checkFragment()
    {
        Fragment currentFragment = getShowingFragment();

        if (currentFragment == null)
            setFragment(AvailableFragment.Options);
        else
            applyViewChanges(currentFragment, mTitleProvided);
    }

    @Override
    public Snackbar createSnackbar(int resId, Object... objects)
    {
        return Snackbar.make(findViewById(R.id.activity_connection_establishing_content_view), getString(resId, objects), Snackbar.LENGTH_LONG);
    }

    @SuppressLint("SupportAnnotationUsage")
    @IdRes
    public AvailableFragment getShowingFragmentId()
    {
        Fragment fragment = getShowingFragment();

        if (fragment instanceof QRConnectFragment)
            return AvailableFragment.ScanQrCode;
        else if (fragment instanceof HotspotManagerFragment)
            return AvailableFragment.CreateHotspot;
        else if (fragment instanceof NetworkManagerFragment)
            return AvailableFragment.UseExistingNetwork;
        else if (fragment instanceof NetworkDeviceListFragment)
            return AvailableFragment.UseKnownDevice;

        // Probably OptionsFragment
        return AvailableFragment.Options;
    }

    @Nullable
    public Fragment getShowingFragment()
    {
        return getSupportFragmentManager().findFragmentById(R.id.activity_connection_establishing_content_view);
    }

    public void setFragment(AvailableFragment fragment)
    {
        @Nullable
        Fragment activeFragment = getShowingFragment();
        Fragment fragmentCandidate = null;

        switch (fragment) {
            case ScanQrCode:
                //fragmentCandidate = mBarcodeConnectFragment;
                if (mOptionsFragment.isAdded())
                    mOptionsFragment.startCodeScanner();
                return;
            case CreateHotspot:
                fragmentCandidate = mHotspotManagerFragment;
                break;
            case UseExistingNetwork:
                fragmentCandidate = mNetworkManagerFragment;
                break;
          //  case UseKnownDevice:
               // fragmentCandidate = mDeviceListFragment;
               // break;
            default:
                fragmentCandidate = mOptionsFragment;
        }

        if (activeFragment == null || fragmentCandidate != activeFragment) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            if (activeFragment != null)
                transaction.remove(activeFragment);

            if (activeFragment != null && fragmentCandidate instanceof OptionsFragment)
                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
            else
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left);

            transaction.add(R.id.activity_connection_establishing_content_view, fragmentCandidate);
            transaction.commit();

            applyViewChanges(fragmentCandidate, mTitleProvided);
        }
    }

    protected void showEnterIpAddressDialog()
    {
        ConnectionUtils connectionUtils = ConnectionUtils.getInstance(this);
        UIConnectionUtils uiConnectionUtils = new UIConnectionUtils(connectionUtils, this);
        new ManualIpAddressConnectionDialog(this, uiConnectionUtils, mDeviceSelectionListener).show();
    }

    public enum RequestType
    {
        RETURN_RESULT,
        MAKE_ACQUAINTANCE
    }

    public enum AvailableFragment
    {
        Options,
        UseExistingNetwork,
        UseKnownDevice,
        ScanQrCode,
        CreateHotspot,
        EnterIpAddress
    }

    public interface DeviceSelectionSupport
    {
        void setDeviceSelectedListener(NetworkDeviceSelectedListener listener);
    }

    public static class OptionsFragment
            extends com.genonbeta.android.framework.app.Fragment
            implements DeviceSelectionSupport
    {
        public static final int REQUEST_CHOOSE_DEVICE = 100;

        private NetworkDeviceSelectedListener mListener;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
        {
            View view = inflater.inflate(R.layout.fragment_coptions, container, false);

            RadarScanView radarView;
            radarView = view.findViewById(R.id.radar);
            radarView.start();

        AdView mAdMobAdView = (AdView) view.findViewById(R.id.admob_adview);
         AdRequest adRequest = new AdRequest.Builder()
        .build();
         mAdMobAdView.loadAd(adRequest);

            Configuration configuration = getActivity().getResources().getConfiguration();
            if (Build.VERSION.SDK_INT >= 24) {
                LocaleList list = configuration.getLocales();

                if (list.size() > 0)
                    for (int pos = 0; pos < list.size(); pos++)
                        if (list.get(pos).toLanguageTag().startsWith("en")) {
                            break;
                        }
            }

            NetworkDevice localDevice = AppUtils.getLocalDevice(getActivity());


         //   ImageView imageView = view.findViewById(R.id.layout_profile_picture_image_default);
         //   ImageView editImageView = view.findViewById(R.id.layout_profile_picture_image_preferred);
            TextView deviceNameText = view.findViewById(R.id.header_default_device_name_text);
            TextView versionText = view.findViewById(R.id.header_default_device_version_text);

            deviceNameText.setText(localDevice.nickname);
          //  versionText.setText(localDevice.versionName);





            View.OnClickListener listener = new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    switch (v.getId()) {
                     //   case R.id.connection_option_devices:
                          //  updateFragment(AvailableFragment.UseKnownDevice);
                         //   break;
                        case R.id.connection_option_hotspot:
                            updateFragment(AvailableFragment.CreateHotspot);
                            break;
                        case R.id.connection_option_network:
                            updateFragment(AvailableFragment.UseExistingNetwork);
                            break;
                        case R.id.connection_option_manual_ip:
                            updateFragment(AvailableFragment.EnterIpAddress);
                            break;
                        case R.id.connection_option_scan:
                            startCodeScanner();
                    }
                }
            };

            //view.findViewById(R.id.connection_option_devices).setOnClickListener(listener);
            view.findViewById(R.id.connection_option_hotspot).setOnClickListener(listener);
            view.findViewById(R.id.connection_option_network).setOnClickListener(listener);
            view.findViewById(R.id.connection_option_scan).setOnClickListener(listener);
            view.findViewById(R.id.connection_option_manual_ip).setOnClickListener(listener);

            view.findViewById(R.id.connection_option_guide).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    new ConnectionSetUpAssistant(getActivity())
                            .startShowing();
                }
            });

            return view;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data)
        {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_CHOOSE_DEVICE)
                if (resultCode == RESULT_OK && data != null) {
                    try {
                        Toast.makeText(getContext(),"its ok",Toast.LENGTH_LONG).show();
                        NetworkDevice device = new NetworkDevice(data.getStringExtra(BarcodeScannerActivity.EXTRA_DEVICE_ID));
                        AppUtils.getDatabase(getContext()).reconstruct(device);
                        NetworkDevice.Connection connection = new NetworkDevice.Connection(device.deviceId, data.getStringExtra(BarcodeScannerActivity.EXTRA_CONNECTION_ADAPTER));
                        AppUtils.getDatabase(getContext()).reconstruct(connection);

                        if (mListener != null)
                            mListener.onNetworkDeviceSelected(device, connection);
                    } catch (Exception e) {
                        // do nothing
                    }
                }
        }

        private void startCodeScanner()
        {
            startActivityForResult(new Intent(getActivity(), BarcodeScannerActivity.class),
                    REQUEST_CHOOSE_DEVICE);
        }

        public void updateFragment(AvailableFragment fragment)
        {
            if (getContext() != null)
                getContext().sendBroadcast(new Intent(ACTION_CHANGE_FRAGMENT)
                        .putExtra(EXTRA_FRAGMENT_ENUM, fragment.toString()));
        }

        @Override
        public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener)
        {
            mListener = listener;
        }
    }
}