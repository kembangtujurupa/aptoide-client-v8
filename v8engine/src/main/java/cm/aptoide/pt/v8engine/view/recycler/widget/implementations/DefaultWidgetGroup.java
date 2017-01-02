package cm.aptoide.pt.v8engine.view.recycler.widget.implementations;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import cm.aptoide.pt.v8engine.util.RecyclerViewUtils;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.DefaultDisplayableGroup;
import cm.aptoide.pt.v8engine.view.recycler.widget.AbstractWidgetGroup;

/**
 * Created by neuro on 28-12-2016.
 */

public class DefaultWidgetGroup extends AbstractWidgetGroup<DefaultDisplayableGroup> {

  public DefaultWidgetGroup(@NonNull View itemView) {
    super(itemView);
  }

  @Override protected RecyclerView.ItemDecoration getRecyclerViewDecorator() {
    return RecyclerViewUtils.newAptoideDefaultItemDecoration(getContext());
  }
}
