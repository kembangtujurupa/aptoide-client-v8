/*
 * Copyright (c) 2016.
 * Modified on 28/07/2016.
 */

package cm.aptoide.pt.view.recycler.widget;

import android.view.View;
import android.widget.TextView;
import cm.aptoide.pt.R;
import cm.aptoide.pt.view.recycler.displayable.FooterRowDisplayable;

/**
 * Created on 27/06/16.
 */
public class FooterRowWidget extends Widget<FooterRowDisplayable> {
  private TextView title;

  public FooterRowWidget(View itemView) {
    super(itemView);
  }

  @Override protected void assignViews(View itemView) {
    title = (TextView) itemView.findViewById(R.id.title);
  }

  @Override public void bindView(FooterRowDisplayable displayable, int position) {
    String pojo = displayable.getPojo();
    title.setText(pojo);
    title.setVisibility(View.VISIBLE);
  }
}
