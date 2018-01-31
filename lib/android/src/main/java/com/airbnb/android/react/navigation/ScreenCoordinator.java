package com.airbnb.android.react.navigation;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.AnimRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import com.airbnb.android.R;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;

import java.util.Map;
import java.util.Stack;

import static com.airbnb.android.react.navigation.ReactNativeIntents.EXTRA_CODE;

/**
 * Owner of the navigation stack for a given Activity. There should be one per activity.
 */
public class ScreenCoordinator {
  private static final String TAG = ScreenCoordinator.class.getSimpleName();
  static final String EXTRA_PAYLOAD = "payload";
  private static final String TRANSITION_GROUP = "transitionGroup";
  private static final String PREFERS_BOTTOM_BAR_HIDDEN = "prefersBottomBarHidden";

  enum PresentAnimation {
    Modal(R.anim.slide_up, R.anim.delay, R.anim.delay, R.anim.slide_down),
    ModalNoExit(R.anim.slide_up, R.anim.delay, 0, 0),
    Push(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right),
    Fade(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);

    @AnimRes int enter;
    @AnimRes int exit;
    @AnimRes int popEnter;
    @AnimRes int popExit;

    PresentAnimation(int enter, int exit, int popEnter, int popExit) {
      this.enter = enter;
      this.exit = exit;
      this.popEnter = popEnter;
      this.popExit = popExit;
    }
  }

  private final Stack<BackStack> backStacks = new Stack<>();
  private final AppCompatActivity activity;
  private final ScreenCoordinatorLayout container;
  private ReactNavigationCoordinator reactNavigationCoordinator = ReactNavigationCoordinator.sharedInstance;

  private int stackId = 0;
  /**
   * When we dismiss a back stack, the fragment manager would normally execute the latest fragment's
   * pop exit animation. However, if we present A as a modal, push, B, then dismiss(), the latest
   * pop exit animation would be from when B was pushed, not from when A was presented.
   * We want the dismiss animation to be the popExit of the original present transaction.
   */
  @AnimRes private int nextPopExitAnim;

  public ScreenCoordinator(AppCompatActivity activity, ScreenCoordinatorLayout container,
      @Nullable Bundle savedInstanceState) {
    this.activity = activity;
    this.container = container;
    container.setFragmentManager(activity.getSupportFragmentManager());
    // TODO: restore state
  }

  void onSaveInstanceState(Bundle outState) {
    // TODO
  }

  public void pushScreen(String moduleName, @Nullable Bundle props, @Nullable Bundle options, Promise promise) {
    Fragment fragment = ReactNativeFragment.newInstance(moduleName, props);
    pushScreen(fragment, options, promise);
  }

  public void pushScreen(Fragment fragment, @Nullable Bundle options, Promise promise) {
    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction()
        .setAllowOptimization(true);
    Fragment currentFragment = getCurrentFragment();
    if (currentFragment == null) {
      throw new IllegalStateException("There is no current fragment. You must present one first.");
    }

    if (ViewUtils.isAtLeastLollipop() && options != null && options.containsKey(TRANSITION_GROUP)) {
      setupFragmentForSharedElement(currentFragment, fragment, ft, options);
    } else {
      PresentAnimation anim;
      if (((ReactNativeFragment) fragment).isMainFragment()) {
        anim = PresentAnimation.Fade;
      }
      else {
        anim = PresentAnimation.Push;
      }

      ft.setCustomAnimations(anim.enter, anim.exit, anim.popEnter, anim.popExit);
    }

    if (fragment instanceof ReactNativeFragment) {
      ReactNativeFragment rnf = (ReactNativeFragment) fragment;

      boolean currentPrefersBottomBarHidden = false; // default

      if (currentFragment instanceof ReactNativeFragment) {
        ReactNativeFragment currentRnf = (ReactNativeFragment) currentFragment;
          currentPrefersBottomBarHidden = currentRnf.isPrefersBottomBarHidden();
      }

      rnf.setPrefersBottomBarHidden(options != null ? options.getBoolean(PREFERS_BOTTOM_BAR_HIDDEN, currentPrefersBottomBarHidden) : currentPrefersBottomBarHidden);
    }

    BackStack bsi = getCurrentBackStack();
    ft
            .detach(currentFragment)
            .add(container.getId(), fragment)
            .addToBackStack(null)
            .commit();
    bsi.pushFragment(fragment, promise);
    activity.getSupportFragmentManager().executePendingTransactions();
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private void setupFragmentForSharedElement(
          Fragment outFragment, Fragment inFragment, FragmentTransaction transaction, Bundle options) {
    FragmentSharedElementTransition transition = new FragmentSharedElementTransition();
    inFragment.setSharedElementEnterTransition(transition);
    inFragment.setSharedElementReturnTransition(transition);
    Fade fade = new Fade();
    inFragment.setEnterTransition(fade);
    inFragment.setReturnTransition(fade);
    ViewGroup rootView = (ViewGroup) outFragment.getView();
    ViewGroup transitionGroup = ViewUtils.findViewGroupWithTag(
            rootView,
            R.id.react_shared_element_group_id,
            options.getString(TRANSITION_GROUP));
    AutoSharedElementCallback.addSharedElementsToFragmentTransaction(transaction, transitionGroup);
  }

  public void presentScreen(String moduleName) {
    presentScreen(moduleName, null, null, null);
  }

  public void presentScreen(
      String moduleName,
      @Nullable Bundle props,
      @Nullable Bundle options,
      @Nullable Promise promise) {

    PresentAnimation anim = PresentAnimation.Modal;
    if (options != null && options.getBoolean("noExitAnimation")) {
      anim = PresentAnimation.ModalNoExit;
    }


    Fragment fragment = ReactNativeFragment.newInstance(moduleName, props);
    presentScreen(fragment, anim, promise);
  }

  public void presentScreen(Fragment fragment) {
    presentScreen(fragment, null);
  }

  private Boolean isFragmentTranslucent(Fragment fragment) {
    Bundle bundle = fragment.getArguments();
    if (bundle != null) {
      String moduleName = bundle.getString(ReactNativeIntents.EXTRA_MODULE_NAME);
      if (moduleName != null) {
        ReadableMap config = reactNavigationCoordinator.getInitialConfigForModuleName(moduleName);
        if (config != null && config.hasKey("screenColor")) {
          return Color.alpha(config.getInt("screenColor")) < 255;
        }
      }
    }
    return false;
  }

  public void presentScreen(Fragment fragment, @Nullable Promise promise) {
    presentScreen(fragment, PresentAnimation.Modal, promise);
  }

  public void presentScreen(Fragment fragment, PresentAnimation anim, @Nullable Promise promise) {
    if (fragment == null) {
      throw new IllegalArgumentException("Fragment must not be null.");
    }
    BackStack bsi = new BackStack(getNextStackTag(), anim, promise);
    backStacks.push(bsi);
    // TODO: dry this up with pushScreen
    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction()
        .setAllowOptimization(true)
        .setCustomAnimations(anim.enter, anim.exit, anim.popEnter, anim.popExit);

    Fragment currentFragment = getCurrentFragment();
//    if (currentFragment != null && !isFragmentTranslucent(fragment)) {
//      container.willDetachCurrentScreen();
//      ft.detach(currentFragment);
//    }

    if (fragment instanceof ReactNativeFragment) {
      ReactNativeFragment rnf = (ReactNativeFragment)fragment;

      // Modal should not inherit any menu state, unless it's a tab activity
      if (currentFragment == null) {
        rnf.setPrefersBottomBarHidden(false);
      } else {
        rnf.setPrefersBottomBarHidden(true);
      }
    }

    ft
        .add(container.getId(), fragment)
        .addToBackStack(bsi.getTag())
        .commit();
    activity.getSupportFragmentManager().executePendingTransactions();
    bsi.pushFragment(fragment, null);
  }

  public void dismissAll() {
    while (!backStacks.isEmpty()) {
      dismiss(0, null, false);
      activity.getFragmentManager().executePendingTransactions();
    }
  }

  public void onBackPressed() {
    pop(null);
  }

  public ReactNativeFragment pop(final Map<String, Object> payload) {
    BackStack bsi = getCurrentBackStack();
    if (bsi.getSize() == 1) {
      dismiss();
      return null;
    }
    Promise pushPromise = bsi.popFragment();
    activity.getSupportFragmentManager().popBackStackImmediate();
    activity.getSupportFragmentManager().executePendingTransactions();

    deliverPromise(pushPromise, -1, payload);

    return (bsi.getSize() > 0) ? (ReactNativeFragment) bsi.peekFragment() : null;
  }

  public void dismiss() {
    dismiss(Activity.RESULT_OK, null);
  }

  public void dismiss(int resultCode, Map<String, Object> payload) {
    dismiss(resultCode, payload, true);
  }

  private void dismiss(final int resultCode, final Map<String, Object> payload, boolean finishIfEmpty) {
    BackStack bsi = backStacks.pop();
    final Promise promise = bsi.getPresentPromise();
    // This is needed so we can override the pop exit animation to slide down.
    PresentAnimation anim = bsi.getAnimation();

    if (backStacks.isEmpty()) {
      if (finishIfEmpty) {
        deliverPromise(promise, resultCode, payload);
        activity.supportFinishAfterTransition();
        return;
      }
    } else {
      // This will be used when the fragment delegates its onCreateAnimation to this.
      nextPopExitAnim = anim.popExit;
    }

    activity.getSupportFragmentManager()
            .popBackStackImmediate(bsi.getTag(), FragmentManager.POP_BACK_STACK_INCLUSIVE);

    Fragment current = getCurrentFragment();
    if (current != null && current instanceof ReactNativeFragment) {
      ReactNativeFragment rnf = ((ReactNativeFragment) current);
      rnf.setPrefersBottomBarHidden(rnf.isPrefersBottomBarHidden());
    }

    deliverPromise(promise, resultCode, payload);
  }

  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    if (!enter && nextPopExitAnim != 0) {
      // If this fragment was pushed on to the stack, it's pop exit animation will be
      // slide out right. However, we want it to be slide down in this case.
      int animRes = nextPopExitAnim;
      nextPopExitAnim = 0;
      return AnimationUtils.loadAnimation(activity, animRes);
    }
    return null;
  }

  private void deliverPromise(Promise promise, int resultCode, Map<String, Object> payload) {
    if (promise != null) {
      if (payload != null) {
        Map<String, Object> newPayload =
            MapBuilder.of(EXTRA_CODE, resultCode, EXTRA_PAYLOAD, payload);
        promise.resolve(ConversionUtil.toWritableMap(newPayload));
      }
      else {
        promise.resolve(null);
      }
    }
  }

  private String getNextStackTag() {
    return getStackTag(stackId++);
  }

  private String getStackTag(int id) {
    return "STACK" + id;
  }

  @Nullable
  private Fragment getCurrentFragment() {
    return activity.getSupportFragmentManager().findFragmentById(container.getId());
  }

  private BackStack getCurrentBackStack() {
    return backStacks.peek();
  }

  @NonNull
  private FrameLayout createContainerView() {
    return new FrameLayout(activity);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ScreenCoordinator{");
    for (int i = 0; i < backStacks.size(); i++) {
      sb.append("Back stack ").append(i).append(":\t").append(backStacks.get(i));
    }
    sb.append('}');
    return sb.toString();
  }
}
