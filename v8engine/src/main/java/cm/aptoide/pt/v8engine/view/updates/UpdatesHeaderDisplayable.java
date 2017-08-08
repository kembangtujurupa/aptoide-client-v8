package cm.aptoide.pt.v8engine.view.updates;

import cm.aptoide.pt.downloadmanager.Download;
import cm.aptoide.pt.v8engine.InstallManager;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.download.DownloadEvent;
import cm.aptoide.pt.v8engine.download.DownloadEventConverter;
import cm.aptoide.pt.v8engine.download.DownloadInstallBaseEvent;
import cm.aptoide.pt.v8engine.download.InstallEvent;
import cm.aptoide.pt.v8engine.download.InstallEventConverter;
import cm.aptoide.pt.v8engine.view.recycler.displayable.Displayable;
import lombok.Getter;

/**
 * Created by neuro on 02-08-2016.
 */
public class UpdatesHeaderDisplayable extends Displayable {

  @Getter private String label;
  private Analytics analytics;
  private DownloadEventConverter converter;
  @Getter private InstallManager installManager;
  private InstallEventConverter installConverter;

  public UpdatesHeaderDisplayable() {
  }

  public UpdatesHeaderDisplayable(InstallManager installManager, String label, Analytics analytics,
      DownloadEventConverter downloadInstallEventConverter,
      InstallEventConverter installConverter) {
    this.installManager = installManager;
    this.label = label;
    this.analytics = analytics;
    this.converter = downloadInstallEventConverter;
    this.installConverter = installConverter;
  }

  @Override protected Configs getConfig() {
    return new Configs(1, true);
  }

  @Override public int getViewLayout() {
    return R.layout.updates_header_row;
  }

  public void setupDownloadEvent(Download download) {
    DownloadEvent report =
        converter.create(download, DownloadEvent.Action.CLICK, DownloadEvent.AppContext.UPDATE_TAB,
            DownloadEvent.Origin.UPDATE_ALL);
    analytics.save(download.getPackageName() + download.getVersionCode(), report);

    InstallEvent installEvent =
        installConverter.create(download, DownloadInstallBaseEvent.Action.CLICK,
            DownloadInstallBaseEvent.AppContext.UPDATE_TAB, DownloadEvent.Origin.UPDATE_ALL);
    analytics.save(download.getPackageName() + download.getVersionCode(), installEvent);
  }
}
