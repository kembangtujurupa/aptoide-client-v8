package cm.aptoide.pt.navigation;

import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.dataprovider.model.v2.GetAdsResponse;
import cm.aptoide.pt.home.AdBundle;
import cm.aptoide.pt.home.AppBundle;
import cm.aptoide.pt.home.BottomHomeFragment;
import cm.aptoide.pt.home.Home;
import cm.aptoide.pt.home.HomeBundle;
import cm.aptoide.pt.home.HomeNavigator;
import cm.aptoide.pt.home.HomePresenter;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.view.app.Application;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Single;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdandrade on 07/03/2018.
 */

public class HomePresenterTest {

  @Mock private BottomHomeFragment view;
  @Mock private CrashReport crashReporter;
  @Mock private HomeNavigator homeNavigator;
  @Mock private Home home;

  private HomePresenter presenter;
  private PublishSubject<View.LifecycleEvent> lifecycleEvent;
  private List<HomeBundle> bundles;
  private PublishSubject<Application> appClickEvent;
  private AppBundle localTopAppsBundle;
  private Application aptoide;

  @Before public void setupHomePresenter() {
    MockitoAnnotations.initMocks(this);

    lifecycleEvent = PublishSubject.create();
    appClickEvent = PublishSubject.create();
    presenter = new HomePresenter(view, home, Schedulers.immediate(), crashReporter, homeNavigator);
    bundles = new ArrayList<>();

    List<Application> applications = getAppsList();
    List<GetAdsResponse.Ad> ads = getAdsList();
    bundles.add(new AppBundle("Editors choice", applications, AppBundle.BundleType.EDITORS));
    localTopAppsBundle = new AppBundle("Local Top Apps", applications, AppBundle.BundleType.APPS);
    bundles.add(localTopAppsBundle);
    bundles.add(new AdBundle("Highlighted", ads));

    when(view.getLifecycle()).thenReturn(lifecycleEvent);
    when(view.appClicked()).thenReturn(appClickEvent);
  }

  @Test public void loadAllBundlesFromRepositoryAndLoadIntoView() {
    //Given an initialised HomePresenter
    presenter.present();
    //When the user clicks the Home menu item
    //And loading of bundles are requested
    when(home.getHomeBundles()).thenReturn(Single.just(bundles));
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //Then the progress indicator should be hidden
    verify(view).showLoading();
    //Then the home should be displayed
    verify(view).showHomeBundles(bundles);
    //Then the progress indicator should be hidden
    verify(view).hideLoading();
  }

  @Test public void errorLoadingBundles_ShowsError() {
    IllegalStateException exception = new IllegalStateException("error test");
    //Given an initialised HomePresenter
    presenter.present();
    //When the loading of bundles is requested
    //And an unexpected error occured
    when(home.getHomeBundles()).thenReturn(Single.error(exception));
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //Then the generic error message should be shown in the UI
    verify(view).showGenericError();
    verify(crashReporter).log(exception);
  }

  @Test public void appClicked_NavigateToAppView() {
    //Given an initialised HomePresenter
    presenter.present();
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When an app is clicked
    appClickEvent.onNext(aptoide);
    //then it should navigate to the App's detail View
    verify(homeNavigator).navigateToAppView(aptoide);
  }

  private List<Application> getAppsList() {
    List<Application> tmp = new ArrayList<>();
    aptoide =
        new Application("Aptoide", "http://via.placeholder.com/350x150", 0, 1000, "cm.aptoide.pt",
            300);
    tmp.add(aptoide);
    tmp.add(new Application("Facebook", "http://via.placeholder.com/350x150", (float) 4.2, 1000,
        "katana.facebook.com", 30));
    return tmp;
  }

  private List<GetAdsResponse.Ad> getAdsList() {
    return Collections.emptyList();
  }
}