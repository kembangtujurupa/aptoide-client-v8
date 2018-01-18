package cm.aptoide.pt.analytics.analytics;

import android.support.annotation.NonNull;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.logger.Logger;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.subscriptions.CompositeSubscription;

public class AptoideBiAnalytics {

  private static final String TAG = AptoideBiAnalytics.class.getSimpleName();
  private final EventsPersistence persistence;
  private final AptoideBiEventService service;
  private final CompositeSubscription subscriptions;
  private final CrashReport crashReport;
  private final int threshHoldSize;
  private final long sendInterval;
  private final Scheduler timerScheduler;

  /**
   * @param threshHoldSize max number of events to batch before send
   * @param sendInterval max time(in milliseconds) interval between event sends
   */
  public AptoideBiAnalytics(EventsPersistence persistence, AptoideBiEventService service,
      CompositeSubscription subscriptions, CrashReport crashReport, Scheduler timerScheduler,
      int threshHoldSize, long sendInterval) {
    this.persistence = persistence;
    this.service = service;
    this.subscriptions = subscriptions;
    this.crashReport = crashReport;
    this.timerScheduler = timerScheduler;
    this.threshHoldSize = threshHoldSize;
    this.sendInterval = sendInterval;
  }

  public void log(String eventName, Map<String, Object> data, AnalyticsManager.Action action,
      String context) {
    persistence.save(new Event(eventName, data, action, context, Calendar.getInstance()
        .getTimeInMillis()))
        .subscribe(() -> {
        }, throwable -> Logger.w(TAG, "cannot save the event due to " + throwable.getMessage()));
  }

  public void setup() {
    subscriptions.add(persistence.getAll()
        .filter(this::shouldSendEvents)
        .flatMapCompletable(events -> sendEvents(events))
        .retry()
        .subscribe());

    subscriptions.add(Observable.interval(sendInterval, TimeUnit.MILLISECONDS, timerScheduler)
        .flatMap(time -> persistence.getAll()
            .first())
        .filter(events -> events.size() > 0)
        .flatMapCompletable(events -> sendEvents(events))
        .retry()
        .subscribe());
  }

  @NonNull private Completable sendEvents(List<Event> events) {
    return persistence.remove(events)
        .toSingleDefault(events)
        .toObservable()
        .flatMapIterable(__ -> events)
        .flatMap(event -> service.send(event)
            .toObservable()
            .flatMap(o -> Observable.empty())
            .cast(Event.class)
            .onErrorResumeNext(throwable -> Observable.just(event)))
        .toList()
        .filter(failedEvents -> !failedEvents.isEmpty())
        .flatMapCompletable(failedEvents -> persistence.save(failedEvents))
        .toCompletable();
  }

  private boolean shouldSendEvents(List<Event> events) {
    return events.size() > 0 && events.size() >= threshHoldSize;
  }
}
