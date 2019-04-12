package cm.aptoide.pt.home;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import cm.aptoide.pt.ads.data.ApplicationAd;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.reactions.network.ReactionsResponse;
import cm.aptoide.pt.view.app.Application;
import java.util.Collections;
import java.util.List;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.exceptions.OnErrorNotImplementedException;

import static cm.aptoide.pt.home.HomeBundle.BundleType.APPCOINS_ADS;
import static cm.aptoide.pt.home.HomeBundle.BundleType.EDITORIAL;
import static cm.aptoide.pt.home.HomeBundle.BundleType.EDITORS;

/**
 * Created by jdandrade on 07/03/2018.
 */

public class HomePresenter implements Presenter {

  private final HomeView view;
  private final Home home;
  private final Scheduler viewScheduler;
  private final CrashReport crashReporter;
  private final HomeNavigator homeNavigator;
  private final AdMapper adMapper;
  private final HomeAnalytics homeAnalytics;

  public HomePresenter(HomeView view, Home home, Scheduler viewScheduler, CrashReport crashReporter,
      HomeNavigator homeNavigator, AdMapper adMapper, HomeAnalytics homeAnalytics) {
    this.view = view;
    this.home = home;
    this.viewScheduler = viewScheduler;
    this.crashReporter = crashReporter;
    this.homeNavigator = homeNavigator;
    this.adMapper = adMapper;
    this.homeAnalytics = homeAnalytics;
  }

  @Override public void present() {
    onCreateLoadBundles();

    handleAppClick();

    handleRecommendedAppClick();

    handleAdClick();

    handleMoreClick();

    handleBottomReached();

    handlePullToRefresh();

    handleBottomNavigationEvents();

    handleRetryClick();

    handleBundleScrolledRight();

    handleKnowMoreClick();

    handleDismissClick();

    handleActionBundlesImpression();

    handleEditorialCardClick();
    handleInstallWalletOfferClick();

    handleReactionClick();

    handleLongPressedReactionButton();

    handleUserReaction();

    handleLogInClick();
  }

  private void handleInstallWalletOfferClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.walletOfferCardInstallWalletClick())
        .observeOn(viewScheduler)
        .doOnNext(event -> homeAnalytics.sendActionItemTapOnCardInteractEvent(event.getBundle()
            .getTag(), event.getBundlePosition()))
        .map(HomeEvent::getBundle)
        .filter(homeBundle -> homeBundle instanceof ActionBundle)
        .cast(ActionBundle.class)
        .doOnNext(bundle -> view.sendDeeplinkToWalletAppView(bundle.getActionItem()
            .getUrl()))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(lifecycleEvent -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void handleLongPressedReactionButton() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.reactionButtonLongPress())
        .doOnNext(homeEvent -> {
          homeAnalytics.sendReactionButtonClickEvent();
          view.showReactionsPopup(homeEvent.getCardId(), homeEvent.getGroupId(),
              homeEvent.getBundlePosition());
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(lifecycleEvent -> {
        }, crashReporter::log);
  }

  private Single<List<HomeBundle>> loadReactionModel(String cardId, String groupId) {
    return home.loadReactionModel(cardId, groupId)
        .observeOn(viewScheduler)
        .doOnSuccess(view::updateEditorialCards);
  }

  private Observable<List<HomeBundle>> loadHomeAndReactions() {
    return loadHome().toObservable()
        .flatMapIterable(HomeBundlesModel::getList)
        .filter(actionBundle -> actionBundle.getType() == EDITORIAL)
        .filter(homeBundle -> homeBundle instanceof ActionBundle)
        .cast(ActionBundle.class)
        .flatMapSingle(actionBundle -> loadReactionModel(actionBundle.getActionItem()
            .getCardId(), actionBundle.getActionItem()
            .getType()));
  }

  private Observable<List<HomeBundle>> loadFreshBundlesAndReactions() {
    return loadFreshBundles().toObservable()
        .flatMapIterable(HomeBundlesModel::getList)
        .filter(actionBundle -> actionBundle.getType() == EDITORIAL)
        .filter(homeBundle -> homeBundle instanceof ActionBundle)
        .cast(ActionBundle.class)
        .flatMapSingle(actionBundle -> loadReactionModel(actionBundle.getActionItem()
            .getCardId(), actionBundle.getActionItem()
            .getType()));
  }

  private Observable<List<HomeBundle>> loadNextBundlesAndReactions() {
    return loadNextBundles().toObservable()
        .flatMapIterable(HomeBundlesModel::getList)
        .filter(actionBundle -> actionBundle.getType() == EDITORIAL)
        .filter(homeBundle -> homeBundle instanceof ActionBundle)
        .cast(ActionBundle.class)
        .flatMapSingle(actionBundle -> loadReactionModel(actionBundle.getActionItem()
            .getCardId(), actionBundle.getActionItem()
            .getType()));
  }

  @VisibleForTesting public void handleActionBundlesImpression() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.visibleBundles())
        .filter(homeEvent -> homeEvent.getBundle() instanceof ActionBundle)
        .doOnNext(homeEvent -> {
          if (homeEvent.getBundle()
              .getType()
              .equals(HomeBundle.BundleType.INFO_BUNDLE) || homeEvent.getBundle()
              .getType()
              .equals(HomeBundle.BundleType.WALLET_ADS_OFFER)) {
            homeAnalytics.sendActionItemImpressionEvent(homeEvent.getBundle()
                .getTag(), homeEvent.getBundlePosition());
          } else {
            ActionBundle actionBundle = (ActionBundle) homeEvent.getBundle();
            homeAnalytics.sendEditorialImpressionEvent(actionBundle.getTag(),
                homeEvent.getBundlePosition(), actionBundle.getActionItem()
                    .getCardId());
          }
        })
        .filter(homeEvent -> homeEvent.getBundle()
            .getType()
            .equals(HomeBundle.BundleType.INFO_BUNDLE) || homeEvent.getBundle()
            .getType()
            .equals(HomeBundle.BundleType.WALLET_ADS_OFFER))
        .map(HomeEvent::getBundle)
        .cast(ActionBundle.class)
        .flatMapCompletable(home::actionBundleImpression)
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(actionBundle -> {
        }, throwable -> {
          crashReporter.log(throwable);
        });
  }

  @VisibleForTesting public void handleKnowMoreClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.infoBundleKnowMoreClicked())
        .observeOn(viewScheduler)
        .doOnNext(homeEvent -> {
          homeAnalytics.sendActionItemTapOnCardInteractEvent(homeEvent.getBundle()
              .getTag(), homeEvent.getBundlePosition());
          homeNavigator.navigateToAppCoinsInformationView();
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(lifecycleEvent -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  @VisibleForTesting public void handleReactionClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.reactionsButtonClicked())
        .observeOn(viewScheduler)
        .flatMapSingle(editorialHomeEvent -> singlePressReactionButtonAction(editorialHomeEvent))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(lifecycleEvent -> {
        }, throwable -> crashReporter.log(throwable));
  }

  @VisibleForTesting public void handleUserReaction() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.reactionClicked())
        .flatMap(homeEvent -> home.setReaction(homeEvent.getCardId(), homeEvent.getGroupId(),
            homeEvent.getReaction())
            .toObservable()
            .filter(reactionsResponse -> !reactionsResponse.sameReaction())
            .observeOn(viewScheduler)
            .doOnNext(reactionsResponse -> handleReactionsResponse(reactionsResponse))
            .flatMapSingle(__ -> loadReactionModel(homeEvent.getCardId(), homeEvent.getGroupId())))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(lifecycleEvent -> {
        }, crashReporter::log);
  }

  private void handleReactionsResponse(ReactionsResponse reactionsResponse) {
    if (reactionsResponse.wasSuccess()) {
      homeAnalytics.sendReactedEvent();
    } else if (reactionsResponse.reactionsExceeded()) {
      view.showLogInDialog();
    } else {
      view.showErrorToast();
    }
  }

  @VisibleForTesting public void handleLogInClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.snackLogInClick())
        .doOnNext(homeEvent -> homeNavigator.navigateToLogIn())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(lifecycleEvent -> {
        }, crashReporter::log);
  }

  @VisibleForTesting public void handleDismissClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.dismissBundleClicked())
        .filter(homeEvent -> homeEvent.getBundle() instanceof ActionBundle)
        .doOnNext(homeEvent -> homeAnalytics.sendActionItemDismissInteractEvent(
            homeEvent.getBundle()
                .getTag(), homeEvent.getBundlePosition()))
        .flatMap(homeEvent -> home.remove((ActionBundle) homeEvent.getBundle())
            .andThen(Observable.just(homeEvent)))
        .observeOn(viewScheduler)
        .doOnNext(homeEvent -> view.hideBundle(homeEvent.getBundlePosition()))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(lifecycleEvent -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  @VisibleForTesting public void onCreateLoadBundles() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .doOnNext(created -> view.showLoading())
        .flatMap(__ -> loadHomeAndReactions())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReporter::log);
  }

  private Single<Boolean> showNativeAds() {
    return home.shouldLoadNativeAd()
        .observeOn(viewScheduler)
        .doOnSuccess(showNatives -> view.setAdsTest(showNatives));
  }

  private Single<HomeBundlesModel> loadHome() {
    return Single.zip(showNativeAds(), loadBundles(), (aBoolean, bundlesModel) -> bundlesModel)
        .observeOn(viewScheduler)
        .doOnSuccess(bundlesModel -> handleBundlesResult(bundlesModel));
  }

  @NonNull private Single<HomeBundlesModel> loadBundles() {
    return home.loadHomeBundles();
  }

  private void handleBundlesResult(HomeBundlesModel bundlesModel) {
    if (bundlesModel.hasErrors()) {
      handleError(bundlesModel.getError());
    } else if (!bundlesModel.isLoading()) {
      view.hideLoading();
      view.showBundles(bundlesModel.getList());
    }
  }

  private void handleError(HomeBundlesModel.Error error) {
    switch (error) {
      case NETWORK:
        view.showNetworkError();
        break;
      case GENERIC:
        view.showGenericError();
        break;
    }
  }

  @VisibleForTesting public void handleAppClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.appClicked()
            .doOnNext(click -> homeAnalytics.sendTapOnAppInteractEvent(click.getApp()
                    .getRating(), click.getApp()
                    .getPackageName(), click.getAppPosition(), click.getBundlePosition(),
                click.getBundle()
                    .getTag(), click.getBundle()
                    .getContent()
                    .size()))
            .observeOn(viewScheduler)
            .doOnNext(click -> {
              Application app = click.getApp();
              if (click.getBundle()
                  .getType()
                  .equals(EDITORS)) {
                homeNavigator.navigateWithEditorsPosition(click.getApp()
                    .getAppId(), click.getApp()
                    .getPackageName(), "", "", click.getApp()
                    .getTag(), String.valueOf(click.getAppPosition()));
              } else if (click.getBundle()
                  .getType()
                  .equals(APPCOINS_ADS)) {
                RewardApp rewardApp = (RewardApp) app;
                homeAnalytics.convertAppcAdClick(rewardApp.getClickUrl());
                homeNavigator.navigateWithDownloadUrlAndReward(rewardApp.getAppId(),
                    rewardApp.getPackageName(), rewardApp.getTag(), rewardApp.getDownloadUrl(),
                    rewardApp.getReward());
              } else {
                homeNavigator.navigateToAppView(app.getAppId(), app.getPackageName(), app.getTag());
              }
            })
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(homeClick -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  @VisibleForTesting public void handleRecommendedAppClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.recommendedAppClicked()
            .observeOn(viewScheduler)
            .doOnNext(click -> homeNavigator.navigateToRecommendsAppView(click.getApp()
                .getAppId(), click.getApp()
                .getPackageName(), click.getApp()
                .getTag(), click.getType()))
            .doOnNext(click -> homeAnalytics.sendRecommendedAppInteractEvent(click.getApp()
                .getRating(), click.getApp()
                .getPackageName(), click.getBundlePosition(), click.getBundle()
                .getTag(), ((SocialBundle) click.getBundle()).getCardType(), click.getType()))
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(homeClick -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void handleEditorialCardClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.editorialCardClicked()
            .observeOn(viewScheduler)
            .doOnNext(click -> {
              homeAnalytics.sendEditorialInteractEvent(click.getBundle()
                  .getTag(), click.getBundlePosition(), click.getCardId());
              homeNavigator.navigateToEditorial(click.getCardId());
            })
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(homeClick -> {
        }, crashReporter::log);
  }

  private void handleBottomNavigationEvents() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> homeNavigator.bottomNavigation())
        .observeOn(viewScheduler)
        .doOnNext(navigated -> view.scrollToTop())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> crashReporter.log(throwable));
  }

  @VisibleForTesting public void handleAdClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.adClicked()
            .doOnNext(adHomeEvent -> homeAnalytics.sendAdClickEvent(adHomeEvent.getAdClick()
                .getAd()
                .getStars(), adHomeEvent.getAdClick()
                .getAd()
                .getPackageName(), adHomeEvent.getBundlePosition(), adHomeEvent.getBundle()
                .getTag(), adHomeEvent.getType(), ApplicationAd.Network.SERVER))
            .map(adHomeEvent -> adHomeEvent.getAdClick())
            .map(adMapper.mapAdToSearchAd())
            .observeOn(viewScheduler)
            .doOnError(throwable -> Logger.getInstance()
                .e(this.getClass()
                    .getCanonicalName(), throwable))
            .doOnNext(homeNavigator::navigateToAppView)
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(homeClick -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  @VisibleForTesting public void handleMoreClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.moreClicked()
            .doOnNext(homeMoreClick -> homeAnalytics.sendTapOnMoreInteractEvent(
                homeMoreClick.getBundlePosition(), homeMoreClick.getBundle()
                    .getTag(), homeMoreClick.getBundle()
                    .getContent()
                    .size()))
            .observeOn(viewScheduler)
            .doOnNext(homeNavigator::navigateWithAction)
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(homeClick -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  @VisibleForTesting public void handleBundleScrolledRight() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.bundleScrolled()
            .doOnNext(click -> homeAnalytics.sendScrollRightInteractEvent(click.getBundlePosition(),
                click.getBundle()
                    .getTag(), click.getBundle()
                    .getContent()
                    .size()))
            .doOnError(crashReporter::log)
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(scroll -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  @VisibleForTesting public void handleBottomReached() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.reachesBottom()
            .filter(__ -> home.hasMore())
            .observeOn(viewScheduler)
            .doOnNext(bottomReached -> view.showLoadMore())
            .flatMap(bottomReached -> loadNextBundlesAndReactions())
            .doOnNext(__ -> homeAnalytics.sendLoadMoreInteractEvent())
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(bundles -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  @NonNull private Single<HomeBundlesModel> loadNextBundles() {
    return home.loadNextHomeBundles()
        .observeOn(viewScheduler)
        .doOnSuccess(bundlesModel -> {
          if (bundlesModel.hasErrors()) {
            handleError(bundlesModel.getError());
          } else {
            if (!bundlesModel.isLoading()) {
              view.showMoreHomeBundles(bundlesModel.getList());
              view.hideLoading();
            }
          }
          view.hideShowMore();
        });
  }

  @VisibleForTesting public void handlePullToRefresh() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.refreshes()
            .doOnNext(__ -> homeAnalytics.sendPullRefreshInteractEvent())
            .flatMap(refreshed -> loadFreshBundlesAndReactions())
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(bundles -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  @NonNull private Single<HomeBundlesModel> loadFreshBundles() {
    return home.loadFreshHomeBundles()
        .observeOn(viewScheduler)
        .doOnSuccess(bundlesModel -> {
          view.hideRefresh();
          if (bundlesModel.hasErrors()) {
            handleError(bundlesModel.getError());
          } else {
            if (!bundlesModel.isLoading()) {
              view.showBundles(bundlesModel.getList());
            }
          }
        });
  }

  @VisibleForTesting public void handleRetryClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(viewCreated -> view.retryClicked()
            .observeOn(viewScheduler)
            .doOnNext(click -> view.showLoading())
            .flatMap(click -> loadNextBundlesAndReactions())
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(notificationUrl -> {
        }, crashReporter::log);
  }

  private Single<List<HomeBundle>> singlePressReactionButtonAction(
      EditorialHomeEvent editorialHomeEvent) {
    boolean isFirstReaction =
        home.isFirstReaction(editorialHomeEvent.getCardId(), editorialHomeEvent.getGroupId());
    if (isFirstReaction) {
      homeAnalytics.sendReactionButtonClickEvent();
      view.showReactionsPopup(editorialHomeEvent.getCardId(), editorialHomeEvent.getGroupId(),
          editorialHomeEvent.getBundlePosition());
      return Single.just(Collections.emptyList());
    } else {
      homeAnalytics.sendDeleteEvent();
      return home.deleteReaction(editorialHomeEvent.getCardId(), editorialHomeEvent.getGroupId())
          .flatMap(__ -> loadReactionModel(editorialHomeEvent.getCardId(),
              editorialHomeEvent.getGroupId()));
    }
  }
}
