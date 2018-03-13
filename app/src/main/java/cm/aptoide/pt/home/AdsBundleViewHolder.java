package cm.aptoide.pt.home;

import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import cm.aptoide.pt.R;
import cm.aptoide.pt.dataprovider.model.v2.GetAdsResponse;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.view.app.Application;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import rx.subjects.PublishSubject;

/**
 * Created by jdandrade on 13/03/2018.
 */

class AdsBundleViewHolder extends AppBundleViewHolder {
  private final TextView bundleTitle;
  private final Button moreButton;
  private final RecyclerView appsList;
  private final AdsInBundleAdapter appsInBundleAdapter;
  private final PublishSubject<HomeClick> uiEventsListener;
  private final LinearLayoutManager layoutManager;

  public AdsBundleViewHolder(View view, PublishSubject<HomeClick> uiEventsListener,
      DecimalFormat oneDecimalFormatter, PublishSubject<Application> appClickedEvents) {
    super(view);
    this.uiEventsListener = uiEventsListener;
    bundleTitle = (TextView) view.findViewById(R.id.bundle_title);
    moreButton = (Button) view.findViewById(R.id.bundle_more);
    appsList = (RecyclerView) view.findViewById(R.id.apps_list);
    appsInBundleAdapter = new AdsInBundleAdapter(new ArrayList<>(), oneDecimalFormatter);
    layoutManager =
        new LinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false);
    appsList.addItemDecoration(new RecyclerView.ItemDecoration() {
      @Override public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
          RecyclerView.State state) {
        int margin = AptoideUtils.ScreenU.getPixelsForDip(5, view.getResources());
        outRect.set(margin, margin, 0, margin);
      }
    });
    appsList.setLayoutManager(layoutManager);
    appsList.setAdapter(appsInBundleAdapter);
  }

  @Override public void setBundle(HomeBundle homeBundle, int position) {
    if (!(homeBundle instanceof AdBundle)) {
      throw new IllegalStateException(this.getClass()
          .getName() + " is getting non AdBundle instance!");
    }
    bundleTitle.setText(homeBundle.getTitle());
    appsInBundleAdapter.update((List<GetAdsResponse.Ad>) homeBundle.getContent());

    moreButton.setOnClickListener(
        v -> uiEventsListener.onNext(new HomeClick(homeBundle, HomeClick.Type.MORE)));
  }
}