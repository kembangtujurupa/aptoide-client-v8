package cm.aptoide.pt.social.data;

import android.os.Handler;
import android.os.Looper;
import rx.Single;

/**
 * Created by jdandrade on 14/08/2017.
 */

public class AdPost extends DummyPost {
  private final TimelineAdsRepository adsRepository;

  public AdPost(TimelineAdsRepository adsRepository) {
    this.adsRepository = adsRepository;
  }

  @Override public String getCardId() {
    return CardType.AD.name();
  }

  @Override public CardType getType() {
    return CardType.AD;
  }

  public Single<AdResponse> getAdView() {
    return adsRepository.getAd();
  }

  public void init() {
    Handler handler = new Handler(Looper.getMainLooper());
    handler.post(() -> adsRepository.fetchAd());
  }
}
