package com.airbnb.android.react.navigation;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class TabCoordinator {
  private static final String TAG = TabCoordinator.class.getSimpleName();

//  private final LongSparseArray<ScreenCoordinator> screenCoordinators = new LongSparseArray<>();
  private ScreenCoordinator screenCoordinator;
  private final AppCompatActivity activity;
  private final ScreenCoordinatorLayout container;

  private Integer currentTabId = null;

  public TabCoordinator(AppCompatActivity activity, ScreenCoordinatorLayout container,
          @Nullable Bundle savedInstanceState) {
    this.activity = activity;
    this.container = container;
  }

  public void showTab(Fragment startingFragment, int id) {
    if (currentTabId != null) {
      if (id == currentTabId) {
        // TODO: add support for other behavior here such as reset the tab stack.
        return;
      }

//      ScreenCoordinator coordinator = screenCoordinators.get(currentTabId);
//      screenCoordinator.dismissAll();
    }
    currentTabId = id;
    ((ReactNativeFragment) startingFragment).setTabId(id);
//    ScreenCoordinator coordinator = screenCoordinators.get(id);
    if (screenCoordinator == null) {
      screenCoordinator = new ScreenCoordinator(activity, container, null);
//      screenCoordinator.put(id, coordinator);

      screenCoordinator.presentScreen(startingFragment, ScreenCoordinator.PresentAnimation.Fade, null);
    }

    Log.d(TAG, toString());
  }

  @Nullable
  public ScreenCoordinator getCurrentScreenCoordinator() {
    if (currentTabId == null)
      return null;
    return screenCoordinator;
  }

  public boolean onBackPressed() {
    if (currentTabId == null) {
      return false;
    }

    ReactNativeFragment newFragment = screenCoordinator.pop(null);
    if (newFragment != null) {
      currentTabId = newFragment.getTabId();
    }

    return true;
  }

  @Override public String toString() {
    return "TabCoordinator{" +
        "screenCoordinator=" + screenCoordinator +
        ", activity=" + activity +
        ", container=" + container +
        ", currentTabId=" + currentTabId +
        '}';
  }
}
