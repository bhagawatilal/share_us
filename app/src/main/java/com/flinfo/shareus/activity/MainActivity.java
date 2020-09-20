package com.flinfo.shareus.activity;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.flinfo.shareus.BuildConfig;
import com.flinfo.shareus.fragment.HomeFragment;
import com.flinfo.shareus.util.PowerfulActionModeSupport;
import com.flinfo.shareus.util.AppUtils;
import com.flinfo.shareus.R;
import com.flinfo.shareus.model.NetworkDevice;
import com.flinfo.shareus.service.CommunicationService;
import com.afollestad.materialdialogs.MaterialDialog;
import com.genonbeta.android.framework.widget.PowerfulActionMode;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE;

public class MainActivity
        extends com.flinfo.shareus.activity.Activity
        implements NavigationView.OnNavigationItemSelectedListener, PowerfulActionModeSupport
{
    public static final int REQUEST_PERMISSION_ALL = 1;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int MY_REQUEST_CODE =789 ;
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private PowerfulActionMode mActionMode;
    private HomeFragment mHomeFragment;
    private IntentFilter mFilter = new IntentFilter();
    private BroadcastReceiver mReceiver = null;
    NetworkDevice localDevice;
    private long mExitPressTime;
    private int mChosenMenuItemId;
    AdView mAdMobAdView;
    private AppUpdateManager appUpdateManager;
    private View coordi;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        appupdate();


       // Toast.makeText(getApplicationContext(), Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID),Toast.LENGTH_SHORT).show();

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (checkPermission()) {


        } else {
            requestPermission();

        }
        mHomeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.activitiy_home_fragment);
        mActionMode = findViewById(R.id.content_powerful_action_mode);
        mNavigationView = findViewById(R.id.nav_view);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.text_navigationDrawerOpen, R.string.text_navigationDrawerClose);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();


       mAdMobAdView = (AdView) findViewById(R.id.admob_adview);
      AdRequest adRequest = new AdRequest.Builder()
      .build();
       mAdMobAdView.loadAd(adRequest);
        LinearLayout actionHistory = (LinearLayout) findViewById(R.id.history);
        actionHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, com.flinfo.shareus.activity.HistoryActivity.class));
            }
        });
/*
        LinearLayout actionInvite = (LinearLayout) findViewById(R.id.invite);
        actionInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, InviteActivity.class));
            }
        });

*/

        Configuration configuration = getResources().getConfiguration();

        if (Build.VERSION.SDK_INT >= 24) {
            LocaleList list = configuration.getLocales();

            if (list.size() > 0)
                for (int pos = 0; pos < list.size(); pos++)
                    if (list.get(pos).toLanguageTag().startsWith("en")) {
                        break;
                    }
        }

        localDevice = AppUtils.getLocalDevice(this);
/*
        ImageView imageView = findViewById(R.id.layout_profile_picture_image_default);
        ImageView editImageView = findViewById(R.id.layout_profile_picture_image_preferred);
        TextView deviceNameText = findViewById(R.id.header_default_device_name_text);
        TextView versionText = findViewById(R.id.header_default_device_version_text);

        deviceNameText.setText(localDevice.nickname);
        versionText.setText(localDevice.versionName);
        loadProfilePictureInto(localDevice.nickname, imageView);

        editImageView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startProfileEditor();
            }
        });

*/
        mFilter.addAction(CommunicationService.ACTION_TRUSTZONE_STATUS);
        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener()
        {
            @Override
            public void onDrawerClosed(View drawerView)
            {
                applyAwaitingDrawerAction();
            }
        });

        mNavigationView.setNavigationItemSelectedListener(this);
        mActionMode.setOnSelectionTaskListener(new PowerfulActionMode.OnSelectionTaskListener()
        {
            @Override
            public void onSelectionTask(boolean started, PowerfulActionMode actionMode)
            {
                toolbar.setVisibility(!started ? View.VISIBLE : View.GONE);
            }
        });

      //  requestRequiredPermissions(false);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(MainActivity.this, "not update", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void appupdate() {

         appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());

        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.clientVersionStalenessDays() != null
                    && appUpdateInfo.isUpdateTypeAllowed(FLEXIBLE)) {
                // Request the update.
                try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, FLEXIBLE,this,MY_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }

            }
        });
    }


    @Override
    protected void onStart()
    {
        super.onStart();
        createHeaderView();
    }

    @Override
    protected void onResume()
    {
        super.onResume();



        registerReceiver(mReceiver = new ActivityReceiver(), mFilter);
        requestTrustZoneStatus();
        // Checks that the update is not stalled during 'onResume()'.
// However, you should execute this check at all entry points into the app.
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
              
                    // If the update is downloaded but not installed,
                    // notify the user to complete the update.
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate();
                    }
                });


    }

    private void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar =
                Snackbar.make(
                        coordi,
                        "An update has just been downloaded.",
                        Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("RESTART", view -> appUpdateManager.completeUpdate());
        snackbar.setActionTextColor(getResources().getColor(R.color.colorTranslucent));
        snackbar.show();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (mReceiver != null)
            unregisterReceiver(mReceiver);

        mReceiver = null;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        mChosenMenuItemId = item.getItemId();

        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onBackPressed()
    {
            super.onBackPressed();
    }

    @Override
    public void onUserProfileUpdated()
    {
        createHeaderView();
    }

    private void applyAwaitingDrawerAction()
    {
        if (mChosenMenuItemId == 0) {
        } else if (R.id.menu_activity_main_manage_devices == mChosenMenuItemId) {
            startActivity(new Intent(MainActivity.this, com.flinfo.shareus.activity.ConnectionManagerActivity.class)
                    .putExtra(com.flinfo.shareus.activity.ConnectionManagerActivity.EXTRA_REQUEST_TYPE, com.flinfo.shareus.activity.ConnectionManagerActivity.RequestType.RETURN_RESULT.toString())
                    .putExtra(com.flinfo.shareus.activity.ConnectionManagerActivity.EXTRA_ACTIVITY_SUBTITLE, "Manage Devices"));

          //  startActivity(new Intent(this, ManageDevicesActivity.class));

        } else if (R.id.menu_activity_home == mChosenMenuItemId) {
            startActivity(new Intent(this, MainActivity.class));
         //   finish();

        } else if (R.id.menu_activity_main_web_share == mChosenMenuItemId) {
            startActivity(new Intent(this, com.flinfo.shareus.activity.WebShareActivity.class));
        } else if (R.id.menu_activity_share == mChosenMenuItemId) {
            startActivity(new Intent(this, com.flinfo.shareus.activity.ContentSharingActivity.class));

        } else if (R.id.menu_activity_receive == mChosenMenuItemId) {
            startActivity(new Intent(this, com.flinfo.shareus.activity.ConnectionManagerActivity.class)
                    .putExtra(com.flinfo.shareus.activity.ConnectionManagerActivity.EXTRA_ACTIVITY_SUBTITLE, getString(R.string.text_receive))
                    .putExtra(com.flinfo.shareus.activity.ConnectionManagerActivity.EXTRA_REQUEST_TYPE, com.flinfo.shareus.activity.ConnectionManagerActivity.RequestType.MAKE_ACQUAINTANCE.toString()));

        } /*else if (R.id.nav_share == mChosenMenuItemId) {

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = "Best File Sharing app download now. https://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName();
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Share App");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));

        } */else if (R.id.about_me == mChosenMenuItemId) {
            aboutMyApp();
        } else if (R.id.privacypolicy == mChosenMenuItemId) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Privacy Policy");

            WebView wv = new WebView(this);
            wv.getSettings().setJavaScriptEnabled(true);
            wv.loadUrl("https://nexaapp.blogspot.com/2020/09/share-us.html?m=1");
            wv.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.getSettings().setJavaScriptEnabled(true);
                    view.loadUrl(url);

                    return true;
                }
            });

            alert.setView(wv);
            alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            alert.show();
        } /*else if (R.id.rate_us == mChosenMenuItemId) {

            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getApplicationContext().getPackageName())));

        }*/ else if (R.id.moreapp == mChosenMenuItemId) {

            Uri uri = Uri.parse("https://play.google.com/store/search?q=pub%3ANexa%20Infotech&c=apps&hl=en"); //Developer AC Name
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/search?q=pub:" + "PA Production"))); //Developer AC Name
            }
        }

        mChosenMenuItemId = 0;
    }

    private void aboutMyApp() {

        MaterialDialog.Builder bulder = new MaterialDialog.Builder(this)
                .title(R.string.app_name)
                .customView(R.layout.about, true)
                .backgroundColor(getResources().getColor(R.color.colorPrimaryDark))
                .titleColorRes(android.R.color.white)
                .positiveText("MORE APPS")
                .positiveColor(getResources().getColor(android.R.color.white))
                .icon(getResources().getDrawable(R.mipmap.ic_launcher))
                .limitIconToDefaultSize()
                .onPositive((dialog, which) -> {

                    Uri uri = Uri.parse("market://search?q=pub:" + "PA Production"); //Developer AC Name
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/search?q=pub:" + "PA Production"))); //Developer AC Name
                    }
                });

        MaterialDialog materialDialog = bulder.build();

        TextView versionCode = (TextView) materialDialog.findViewById(R.id.version_code);
        TextView versionName = (TextView) materialDialog.findViewById(R.id.version_name);
        versionCode.setText(String.valueOf("Version Code : " + BuildConfig.VERSION_CODE));
        versionName.setText(String.valueOf("Version Name : " + BuildConfig.VERSION_NAME));

        materialDialog.show();
    }

    private void createHeaderView()
    {
        View headerView = mNavigationView.getHeaderView(0);
        Configuration configuration = getApplication().getResources().getConfiguration();

        if (Build.VERSION.SDK_INT >= 24) {
            LocaleList list = configuration.getLocales();

            if (list.size() > 0)
                for (int pos = 0; pos < list.size(); pos++)
                    if (list.get(pos).toLanguageTag().startsWith("en")) {
                        break;
                    }
        }
        if (headerView != null) {
            NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

            ImageView imageView = headerView.findViewById(R.id.layout_profile_picture_image_default);
            ImageView editImageView = headerView.findViewById(R.id.layout_profile_picture_image_preferred);
            TextView deviceNameText = headerView.findViewById(R.id.header_default_device_name_text);
            TextView versionText = headerView.findViewById(R.id.header_default_device_version_text);

            deviceNameText.setText(localDevice.nickname);
            versionText.setText(localDevice.versionName);
            loadProfilePictureInto(localDevice.nickname, imageView);

            editImageView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    startProfileEditor();
                }
            });
        }
    }

    public void onStateUpdate(InstallState state) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // After the update is downloaded, show a notification
            // and request user confirmation to restart the app.
            popupSnackbarForCompleteUpdate();
        }

    }
    @Override
    public PowerfulActionMode getPowerfulActionMode()
    {
        return mActionMode;
    }



    public void requestTrustZoneStatus()
    {
        AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class)
                .setAction(CommunicationService.ACTION_REQUEST_TRUSTZONE_STATUS));
    }


    private class ActivityReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {

        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_PHONE_STATE);
        return result == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean contactAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (locationAccepted && contactAccepted)
                        Toast.makeText(this, "Permission Granted, Now you can access this app.", Toast.LENGTH_LONG).show();

                    else {
                        Toast.makeText(this, "Permission Denied, You cannot use this app.", Toast.LENGTH_LONG).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_PHONE_STATE},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }

                    }
                }


                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

}
