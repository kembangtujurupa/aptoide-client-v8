/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 29/09/2016.
 */

package cm.aptoide.pt.install;

import android.content.Context;
import androidx.annotation.NonNull;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.BuildConfig;
import cm.aptoide.pt.ads.MinimalAdMapper;
import cm.aptoide.pt.database.AccessorFactory;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.database.realm.StoredMinimalAd;
import cm.aptoide.pt.download.DownloadInstallationProvider;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.install.installer.DefaultInstaller;
import cm.aptoide.pt.packageinstaller.AppInstaller;
import cm.aptoide.pt.preferences.toolbox.ToolboxManager;
import cm.aptoide.pt.repository.RepositoryFactory;
import cm.aptoide.pt.utils.FileUtils;

/**
 * Created by marcelobenites on 9/29/16.
 */

public class InstallerFactory {

  public static final int DEFAULT = 0;
  private final MinimalAdMapper adMapper;
  private final InstallerAnalytics installerAnalytics;
  private final AppInstaller appInstaller;
  private final int installingStateTimeout;
  private final AppInstallerStatusReceiver appInstallerStatusReceiver;
  private final RootInstallerProvider rootInstallerProvider;

  public InstallerFactory(MinimalAdMapper adMapper, InstallerAnalytics installerAnalytics,
      AppInstaller appInstaller, int installingStateTimeout,
      AppInstallerStatusReceiver appInstallerStatusReceiver,
      RootInstallerProvider rootInstallerProvider) {
    this.adMapper = adMapper;
    this.installerAnalytics = installerAnalytics;
    this.appInstaller = appInstaller;
    this.installingStateTimeout = installingStateTimeout;
    this.appInstallerStatusReceiver = appInstallerStatusReceiver;
    this.rootInstallerProvider = rootInstallerProvider;
  }

  public Installer create(Context context) {
    return getDefaultInstaller(context);
  }

  @NonNull private DefaultInstaller getDefaultInstaller(Context context) {
    return new DefaultInstaller(context.getPackageManager(), getInstallationProvider(
        ((AptoideApplication) context.getApplicationContext()).getDownloadManager(),
        context.getApplicationContext()), appInstaller, new FileUtils(), ToolboxManager.isDebug(
        ((AptoideApplication) context.getApplicationContext()).getDefaultSharedPreferences())
        || BuildConfig.DEBUG,
        RepositoryFactory.getInstalledRepository(context.getApplicationContext()), 180000,
        ((AptoideApplication) context.getApplicationContext()).getRootAvailabilityManager(),
        ((AptoideApplication) context.getApplicationContext()).getDefaultSharedPreferences(),
        installerAnalytics, installingStateTimeout, appInstallerStatusReceiver,
        rootInstallerProvider);
  }

  @NonNull private DownloadInstallationProvider getInstallationProvider(
      AptoideDownloadManager downloadManager, Context context) {
    return new DownloadInstallationProvider(downloadManager, AccessorFactory.getAccessorFor(
        ((AptoideApplication) context.getApplicationContext()).getDatabase(), Download.class),
        RepositoryFactory.getInstalledRepository(context), adMapper, AccessorFactory.getAccessorFor(
        ((AptoideApplication) context.getApplicationContext()
            .getApplicationContext()).getDatabase(), StoredMinimalAd.class));
  }
}