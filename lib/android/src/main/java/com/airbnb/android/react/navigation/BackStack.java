package com.airbnb.android.react.navigation;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;

import com.facebook.react.bridge.Promise;

import java.util.Stack;

class BackStack {

  private final Stack<Pair<Fragment, Promise>> fragments = new Stack<>();
  private final String tag;
  private final ScreenCoordinator.PresentAnimation animation;
  private final Promise presentPromise;

  BackStack(String tag, ScreenCoordinator.PresentAnimation animation, Promise promise) {
    this.tag = tag;
    this.animation = animation;
    this.presentPromise = promise;
  }

  String getTag() {
    return tag;
  }

  ScreenCoordinator.PresentAnimation getAnimation() {
    return animation;
  }

  Promise getPresentPromise() {
    return presentPromise;
  }

  @Nullable
  Fragment peekFragment() {
    if (fragments.isEmpty()) {
      return null;
    }
    return fragments.peek().first;
  }

  void pushFragment(Fragment fragment, Promise promise) {
    fragments.push(new Pair<>(fragment, promise));
  }

  Promise popFragment() {
    if (fragments.isEmpty()) {
      throw new IllegalStateException("Cannot pop empty stack.");
    }
    return fragments.remove(fragments.size() - 1).second;
  }

  int getSize() {
    return fragments.size();
  }

  @Override
  public String toString() {
    return "BackStack{" + ", tag='" + tag +
            ", size=" + fragments.size() +
            ", animation=" + animation +
            ", promise?=" + (presentPromise != null) +
            '}';
  }
}
