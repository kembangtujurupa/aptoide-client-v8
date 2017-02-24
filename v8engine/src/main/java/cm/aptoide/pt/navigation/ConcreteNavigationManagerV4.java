package cm.aptoide.pt.navigation;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.dataprovider.util.CommentType;
import cm.aptoide.pt.dataprovider.ws.v7.V7;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.model.v7.Event;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;

class ConcreteNavigationManagerV4 implements NavigationManagerV4 {

  private static final int EXIT_ANIMATION = android.R.anim.fade_out;
  private static final int ENTER_ANIMATION = android.R.anim.fade_in;

  private static final ConcurrentLinkedQueue<Fragment> fragmentStack =
      new ConcurrentLinkedQueue<>();

  private final WeakReference<FragmentActivity> weakReference;

  ConcreteNavigationManagerV4(FragmentActivity fragmentActivity) {
    this.weakReference = new WeakReference<>(fragmentActivity);
  }

  @Override public void navigateUsing(Event event, String storeTheme, String title, String tag,
      StoreContext storeContext) {
    Fragment fragment;

    // TODO: 22/12/2016 sithengineer refactor this using the rules present in "StoreTabGridRecyclerFragment.java"
    if (event.getName() == Event.Name.listComments) {
      String action = event.getAction();
      String url = action != null ? action.replace(V7.BASE_HOST, "") : null;

      fragment =
          V8Engine.getFragmentProvider().newCommentGridRecyclerFragmentUrl(CommentType.STORE, url);
    } else {
      fragment = V8Engine.getFragmentProvider()
          .newStoreTabGridRecyclerFragment(event, title, storeTheme, tag, storeContext);
    }

    navigateTo(fragment);
  }

  @Override public void navigateTo(Fragment fragment) {
    FragmentActivity activity = weakReference.get();

    if (activity == null) {
      CrashReport.getInstance()
          .log(new RuntimeException(
              "Activity is null in " + ConcreteNavigationManagerV4.class.getName()));
      return;
    }

    final FragmentManager fragmentManager = activity.getSupportFragmentManager();

    // add current fragment
    String tag = Integer.toString(fragmentManager.getBackStackEntryCount());
    fragmentManager.beginTransaction()
        .setCustomAnimations(ENTER_ANIMATION, EXIT_ANIMATION, ENTER_ANIMATION, EXIT_ANIMATION)
        .addToBackStack(tag)
        .replace(R.id.fragment_placeholder, fragment, tag)
        .commit();

    fragmentStack.add(fragment);
  }

  /*
  @Override public void navigateTo(Fragment fragment, @Nullable List<Fragment> newBackStack) {
    FragmentActivity activity = weakReference.get();

    if (activity == null) {
      CrashReport.getInstance()
          .log(new RuntimeException(
              "Activity is null in " + ConcreteNavigationManagerV4.class.getName()));
      return;
    }

    final FragmentManager fragmentManager = activity.getSupportFragmentManager();

    int backStackTag = 0;
    if (newBackStack != null && newBackStack.size() > 0) {
      // replace the entire back stack in the current fragment manager
      cleanBackStack(fragmentManager);
      fragmentManager.executePendingTransactions();
      for (Fragment f : newBackStack) {
        String tag = Integer.toString(backStackTag++);
        fragmentManager.beginTransaction()
            .setCustomAnimations(ENTER_ANIMATION, EXIT_ANIMATION, ENTER_ANIMATION, EXIT_ANIMATION)
            .addToBackStack(tag)
            .add(R.id.fragment_placeholder, f, tag)
            .commitNow();

        fragmentStack.add(f);
      }
    }

    // add current fragment
    String tag = Integer.toString(backStackTag);
    fragmentManager.beginTransaction()
        .setCustomAnimations(ENTER_ANIMATION, EXIT_ANIMATION, ENTER_ANIMATION, EXIT_ANIMATION)
        .addToBackStack(tag)
        .add(R.id.fragment_placeholder, fragment, tag)
        .commitNow();

    fragmentStack.add(fragment);
  }
  */

  @Override public void cleanBackStack() {
    FragmentActivity activity = weakReference.get();

    if (activity == null) {
      CrashReport.getInstance()
          .log(new RuntimeException(
              "Activity is null in " + ConcreteNavigationManagerV4.class.getName()));
      return;
    }

    cleanBackStack(activity.getSupportFragmentManager());
  }

  @Override public Fragment peekLast() {
    return fragmentStack.size() > 0 ? fragmentStack.peek() : null;
  }

  @Override public void navigateToWithoutBackSave(Fragment fragment) {
    FragmentActivity activity = weakReference.get();

    if (activity == null) {
      CrashReport.getInstance()
          .log(new RuntimeException(
              "Activity is null in " + ConcreteNavigationManagerV4.class.getName()));
      return;
    }

    final FragmentManager fragmentManager = activity.getSupportFragmentManager();

    // add current fragment
    fragmentManager.beginTransaction()
        .setCustomAnimations(ENTER_ANIMATION, EXIT_ANIMATION, ENTER_ANIMATION, EXIT_ANIMATION)
        .replace(R.id.fragment_placeholder, fragment)
        .commit();

    fragmentStack.add(fragment);
  }

  private synchronized void cleanBackStack(FragmentManager fragmentManager) {
    for (int i = 0; i < fragmentManager.getBackStackEntryCount(); ++i) {
      fragmentManager.popBackStack();
    }

    fragmentStack.clear();
  }
}
