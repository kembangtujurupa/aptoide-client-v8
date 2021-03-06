package cm.aptoide.pt.presenter;

import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.accountmanager.AptoideCredentials;
import cm.aptoide.pt.account.AccountAnalytics;
import cm.aptoide.pt.account.view.AccountNavigator;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.view.ThrowableToStringMapper;
import java.util.Collection;
import rx.Observable;

public class LoginSignupCredentialsFlavorPresenter extends LoginSignUpCredentialsPresenter {

  private final LoginSignUpCredentialsView view;
  private final ThrowableToStringMapper errorMapper;
  private final CrashReport crashReport;
  private final AccountNavigator accountNavigator;

  public LoginSignupCredentialsFlavorPresenter(LoginSignUpCredentialsView view,
      AptoideAccountManager accountManager, CrashReport crashReport,
      boolean dismissToNavigateToMainView, boolean navigateToHome,
      AccountNavigator accountNavigator, Collection<String> permissions,
      Collection<String> requiredPermissions, ThrowableToStringMapper errorMapper,
      AccountAnalytics accountAnalytics) {
    super(view, accountManager, crashReport, dismissToNavigateToMainView, navigateToHome,
        accountNavigator, permissions, requiredPermissions, errorMapper, accountAnalytics);
    this.view = view;
    this.errorMapper = errorMapper;
    this.crashReport = crashReport;
    this.accountNavigator = accountNavigator;
  }

  @Override public void present() {

    super.present();

    showAptoideSignUpEvent();
    handleAptoideShowSignUpEvent();
    handleClickOnTermsAndConditions();
    handleClickOnPrivacyPolicy();
    showTCandPP();
  }

  protected Observable<AptoideCredentials> getAptoideSignUpEvent() {
    return view.aptoideSignUpEvent()
        .doOnNext(credentials -> showNotCheckedMessage(credentials.isChecked()))
        .filter(AptoideCredentials::isChecked);
  }

  private void handleClickOnTermsAndConditions() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.termsAndConditionsClickEvent())
        .doOnNext(__ -> accountNavigator.navigateToTermsAndConditions())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, err -> crashReport.log(err));
  }

  private void handleClickOnPrivacyPolicy() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.privacyPolicyClickEvent())
        .doOnNext(__ -> accountNavigator.navigateToPrivacyPolicy())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, err -> crashReport.log(err));
  }

  private void showTCandPP() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .doOnNext(__ -> view.showTCandPP())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe();
  }

  private void handleAptoideShowSignUpEvent() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> showAptoideSignUpEvent())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, err -> {
          view.hideLoading();
          view.showError(errorMapper.map(err));
          crashReport.log(err);
        });
  }

  private Observable<Boolean> showAptoideSignUpEvent() {
    return view.showAptoideSignUpAreaClick()
        .doOnNext(this::showNotCheckedMessage)
        .filter(event -> event)
        .doOnNext(__ -> view.showAptoideSignUpArea());
  }

  private void showNotCheckedMessage(boolean checked) {
    if (!checked) {
      view.showTermsConditionError();
    }
  }

  @Override public boolean handle() {
    return view.tryCloseLoginBottomSheet(true);
  }
}
