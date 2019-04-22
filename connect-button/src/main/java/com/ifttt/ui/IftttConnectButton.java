package com.ifttt.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.OnLifecycleEvent;
import com.ifttt.Connection;
import com.ifttt.ConnectionApiClient;
import com.ifttt.ErrorResponse;
import com.ifttt.R;
import com.ifttt.Service;
import com.ifttt.api.PendingResult;
import java.util.ArrayList;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import okhttp3.Call;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static androidx.lifecycle.Lifecycle.State.CREATED;
import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.lifecycle.Lifecycle.State.STARTED;
import static com.ifttt.Connection.Status.enabled;
import static com.ifttt.ui.ConnectButtonState.CreateAccount;
import static com.ifttt.ui.ConnectButtonState.Disabled;
import static com.ifttt.ui.ConnectButtonState.Enabled;
import static com.ifttt.ui.ConnectButtonState.Initial;
import static com.ifttt.ui.ConnectButtonState.Login;
import static com.ifttt.ui.ConnectButtonState.ServiceAuthentication;
import static com.ifttt.ui.ButtonUiHelper.adjustPadding;
import static com.ifttt.ui.ButtonUiHelper.buildButtonBackground;
import static com.ifttt.ui.ButtonUiHelper.findWorksWithService;
import static com.ifttt.ui.ButtonUiHelper.getDarkerColor;
import static com.ifttt.ui.ButtonUiHelper.replaceKeyWithImage;
import static com.ifttt.ui.ButtonUiHelper.setTextSwitcherTextColor;
import static com.ifttt.ui.CheckMarkDrawable.AnimatorType.ENABLE;

/**
 * Internal implementation of a Connect Button widget, all of the states and transitions are implemented here.
 */
final class IftttConnectButton extends LinearLayout implements LifecycleOwner {

    private static final ErrorResponse UNKNOWN_STATE = new ErrorResponse("unknown_state", "Cannot verify Button state");

    private static final float FADE_OUT_PROGRESS = 0.5f;

    private static final long ANIM_DURATION_SHORT = 400L;
    private static final long ANIM_DURATION_MEDIUM = 700L;
    private static final long ANIM_DURATION_LONG = 1500L;
    private static final long AUTO_ADVANCE_DELAY = 2400L;
    private static final LinearInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final FastOutSlowInInterpolator EASE_INTERPOLATOR = new FastOutSlowInInterpolator();

    private static final ArgbEvaluator EVALUATOR = new ArgbEvaluator();

    // Spannable text that replaces the text "IFTTT" with IFTTT logo.
    private final SpannableString worksWithIfttt;
    private final Drawable iftttLogo;

    private final EditText emailEdt;
    private final TextSwitcher connectStateTxt;
    private final ImageView iconImg;
    private final TextSwitcher helperTxt;
    private final DragParentView buttonRoot;

    private final int iconSize;

    private final LifecycleRegistry lifecycleRegistry;
    private final AnimatorLifecycleObserver animatorLifecycleObserver = new AnimatorLifecycleObserver();

    private ConnectButtonState buttonState = Initial;
    private Connection connection;
    private Service worksWithService;

    @Nullable private ButtonStateChangeListener buttonStateChangeListener;
    @Nullable private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;
    private ButtonApiHelper buttonApiHelper;

    // Toggle drag events.
    private ViewDragHelper viewDragHelper;
    private IconDragHelperCallback iconDragHelperCallback;

    private boolean onDarkBackground = false;

    @Nullable private Call ongoingImageCall;
    @Nullable private Runnable resetTextRunnable;

    public IftttConnectButton(Context context) {
        this(context, null);
    }

    public IftttConnectButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IftttConnectButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setClipToPadding(false);
        setClipChildren(false);

        iconSize = getResources().getDimensionPixelSize(R.dimen.ifttt_icon_image_size);

        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.addObserver(animatorLifecycleObserver);
        lifecycleRegistry.markState(CREATED);

        inflate(context, R.layout.view_ifttt_connect, this);
        buttonRoot = findViewById(R.id.ifttt_button_root);

        emailEdt = findViewById(R.id.ifttt_email);
        emailEdt.setBackground(ButtonUiHelper.buildButtonBackground(context,
                ContextCompat.getColor(getContext(), R.color.ifttt_button_background)));

        connectStateTxt = findViewById(R.id.connect_with_ifttt);
        iconImg = findViewById(R.id.ifttt_icon);

        // Initialize SpannableString that replaces text with logo, using the current TextView in the TextSwitcher as
        // measurement, the CharSequence will only be used there.
        helperTxt = findViewById(R.id.ifttt_helper_text);
        iftttLogo = ContextCompat.getDrawable(getContext(), R.drawable.ic_ifttt_logo_black);
        worksWithIfttt = new SpannableString(replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                getResources().getString(R.string.ifttt_powered_by_ifttt), "IFTTT", iftttLogo));

        iconDragHelperCallback = new IconDragHelperCallback();
        viewDragHelper = buttonRoot.getViewDragHelperCallback(iconDragHelperCallback);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lifecycleRegistry.markState(STARTED);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        lifecycleRegistry.markState(DESTROYED);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), buttonState, connection);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.superState);

        this.buttonState = savedState.buttonState;
        if (savedState.connection != null) {
            setConnection(savedState.connection);
        }
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    /**
     * Clear the footer text. This should only be used internally, coordinating with {@link ConnectButton}.
     */
    void resetFooterText() {
        helperTxt.setCurrentText(null);
    }

    /**
     * Enable the Connect Button's Connection authentication and configuration features with an {@link ConnectionApiClient}
     * instance and a user email.
     *
     * Note:
     * - The redirect URI must be set for the {@link ConnectionApiClient} instance here.
     * - User email is a required parameter, the Button will crash if the value is not a valid email in DEBUG build.
     *
     * @param connectionApiClient ConnectionApiClient instance.
     * @param email This is used to pre-fill the email EditText when the user is doing Connection authentication.
     * @param redirectUri URL string that will be used when the Connection authentication flow is completed on web view, in
     * order to return the result to the app.
     * @param credentialsProvider CredentialsProvider implementation that returns your user's OAuth code. The code will be
     * used to automatically connect your service on IFTTT for this user.
     */
    void setup(String email, ConnectionApiClient connectionApiClient, Uri redirectUri,
            CredentialsProvider credentialsProvider) {
        buttonApiHelper = new ButtonApiHelper(connectionApiClient, redirectUri, connectionApiClient.getInviteCode(),
                credentialsProvider, getLifecycle());
        emailEdt.setText(email);
    }

    /**
     * If the button is used in a dark background, set this flag to true so that the button can adapt the UI. This
     * method must be called before {@link #setConnection(Connection)} to apply the change.
     *
     * @param onDarkBackground True if the button is used in a dark background, false otherwise.
     */
    void setOnDarkBackground(boolean onDarkBackground) {
        this.onDarkBackground = onDarkBackground;

        TextView currentHelperTextView = (TextView) helperTxt.getCurrentView();
        TextView nextHelperTextView = (TextView) helperTxt.getNextView();

        if (onDarkBackground) {
            // Add a border.
            buttonRoot.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.ifttt_button_border));

            // Set helper text to white.
            int semiTransparentWhite = ContextCompat.getColor(getContext(), R.color.ifttt_footer_text_white);
            currentHelperTextView.setTextColor(semiTransparentWhite);
            nextHelperTextView.setTextColor(semiTransparentWhite);

            // Tint the logo Drawable within the text to white.
            DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), semiTransparentWhite);
        } else {
            // Remove border.
            buttonRoot.setForeground(null);

            // Set helper text to black.
            int semiTransparentBlack = ContextCompat.getColor(getContext(), R.color.ifttt_footer_text_black);
            currentHelperTextView.setTextColor(semiTransparentBlack);
            nextHelperTextView.setTextColor(semiTransparentBlack);

            // Tint the logo Drawable within the text to black.
            DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), semiTransparentBlack);
        }
    }

    /**
     * Set a listener to be notified when the button's state has changed.
     */
    void setButtonStateChangeListener(ButtonStateChangeListener listener) {
        buttonStateChangeListener = listener;
    }

    /**
     * Given an {@link ConnectResult} from web redirect, refresh the UI of the button to reflect the current
     * state of the Connection authentication flow.
     *
     * @param result Authentication flow redirect result from the web view.
     */
    void setConnectResult(ConnectResult result) {
        if (activityLifecycleCallbacks != null) {
            // Unregister existing ActivityLifecycleCallbacks and let the AuthenticationResult handle the button
            // state change.
            ((Activity) getContext()).getApplication().unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
            activityLifecycleCallbacks = null;
        }

        cleanUpViews(ProgressView.class);
        switch (result.nextStep) {
            case ServiceAuthentication:
                worksWithService = findNextServiceToConnect(result);
                getStartServiceAuthAnimator(worksWithService).start();
                dispatchState(ServiceAuthentication);
                break;
            case Complete:
                complete();
                break;
            case Error:
                if (result.errorType == null) {
                    dispatchError(UNKNOWN_STATE);
                    break;
                }

                dispatchError(new ErrorResponse(result.errorType, ""));
                break;
            default:
                if (buttonState == Login) {
                    // If the previous state is Login, reset the progress animation.
                    connectStateTxt.setAlpha(1f);
                }

                // The authentication result doesn't contain any next step instruction.
                dispatchError(UNKNOWN_STATE);
        }
    }

    /**
     * Render the Connect Button to show the status of the Connection.
     *
     * @param connection Connection instance to be displayed.
     */
    void setConnection(Connection connection) {
        if (buttonApiHelper == null) {
            throw new IllegalStateException("Connect Button is not set up, please call setup() first.");
        }

        if (resetTextRunnable != null) {
            removeCallbacks(resetTextRunnable);
            resetTextRunnable = null;
        }

        this.connection = connection;
        worksWithService = findWorksWithService(connection);

        emailEdt.setVisibility(GONE);
        iconImg.setTranslationX(0);

        helperTxt.setCurrentText(worksWithIfttt);

        if (onDarkBackground) {
            // Add a border.
            buttonRoot.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.ifttt_button_border));

            // Set helper text to white.
            setTextSwitcherTextColor(helperTxt, WHITE);

            // Tint the logo Drawable within the text to white.
            DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), WHITE);
        } else {
            // Remove border.
            buttonRoot.setForeground(null);

            // Set helper text to black.
            setTextSwitcherTextColor(helperTxt, BLACK);

            // Tint the logo Drawable within the text to black.
            DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), BLACK);
        }

        iconDragHelperCallback.setSettledAt(connection.status);

        setServiceIconImage(null);
        ongoingImageCall = ImageLoader.get().load(getLifecycle(), worksWithService.monochromeIconUrl, bitmap -> {
            ongoingImageCall = null;
            setServiceIconImage(bitmap);
        });

        // Move the icon to the right if the Connection has already been authenticated and enabled.
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) iconImg.getLayoutParams();
        lp.gravity = connection.status == enabled ? Gravity.END : Gravity.START;
        iconImg.setLayoutParams(lp);

        connectStateTxt.setAlpha(1f);

        if (connection.status == enabled) {
            dispatchState(Enabled);
            connectStateTxt.setText(getResources().getString(R.string.ifttt_connected));
            adjustPadding(connectStateTxt);

            buttonRoot.setBackground(buildButtonBackground(getContext(), BLACK));

            OnClickListener onClickListener = v -> {
                connectStateTxt.setText(getResources().getString(R.string.ifttt_slide_to_turn_off));
                helperTxt.setClickable(false);

                // Delay and switch back.
                resetTextRunnable = () -> {
                    resetTextRunnable = null;
                    connectStateTxt.setText(getResources().getString(R.string.ifttt_connected));
                    helperTxt.setClickable(true);
                };
                postDelayed(resetTextRunnable, ANIM_DURATION_LONG);
            };
            buttonRoot.setOnClickListener(onClickListener);
            iconImg.setOnClickListener(onClickListener);

            iconDragHelperCallback.setTrackColor(BLACK,
                    ContextCompat.getColor(getContext(), R.color.ifttt_disabled_background));
        } else {
            if (connection.status == Connection.Status.disabled) {
                dispatchState(Disabled);
                connectStateTxt.setText(
                        getResources().getString(R.string.ifttt_connect_to, worksWithService.shortName));
                adjustPadding(connectStateTxt);

                buttonRoot.setBackground(buildButtonBackground(getContext(),
                        ContextCompat.getColor(getContext(), R.color.ifttt_disabled_background)));
                iconDragHelperCallback.setTrackColor(
                        ContextCompat.getColor(getContext(), R.color.ifttt_disabled_background), BLACK);
            } else {
                dispatchState(Initial);
                connectStateTxt.setText(
                        getResources().getString(R.string.ifttt_connect_to, worksWithService.shortName));
                ButtonUiHelper.adjustPadding(connectStateTxt);

                buttonRoot.setBackground(buildButtonBackground(getContext(), BLACK));
                // Depending on whether we need to show the email field, use different track colors.
                int trackEndColor = !buttonApiHelper.shouldPresentEmail(getContext()) ? BLACK
                        : ContextCompat.getColor(getContext(), R.color.ifttt_button_background);

                iconDragHelperCallback.setTrackColor(BLACK, trackEndColor);
            }

            helperTxt.setOnClickListener(
                    v -> getContext().startActivity(AboutIftttActivity.intent(getContext(), connection)));

            OnClickListener onClickListener = v -> {
                buttonRoot.setOnClickListener(null);
                iconImg.setOnClickListener(null);
                // Cancel potential ongoing image loading task. Users have already click the button and the service
                // icon will not be used in the next UI state.
                if (ongoingImageCall != null) {
                    ongoingImageCall.cancel();
                    ongoingImageCall = null;
                }

                // Cancel potential disable connection API call.
                buttonApiHelper.cancelDisconnect();

                if (buttonApiHelper.shouldPresentEmail(getContext())) {
                    buildEmailTransitionAnimator(0).start();
                } else {
                    int startPosition = iconImg.getLeft();
                    int endPosition = buttonRoot.getWidth() - iconImg.getWidth();
                    ValueAnimator moveToggle = ValueAnimator.ofFloat(startPosition, endPosition);
                    moveToggle.setDuration(ANIM_DURATION_MEDIUM);
                    moveToggle.setInterpolator(EASE_INTERPOLATOR);
                    moveToggle.addUpdateListener(animation -> {
                        setProgressStateText(animation.getAnimatedFraction());
                        iconImg.setTranslationX((Float) animation.getAnimatedValue());
                    });
                    Animator emailValidation = buildEmailValidationAnimator();
                    AnimatorSet set = new AnimatorSet();
                    set.playSequentially(moveToggle, emailValidation);
                    set.start();
                }
            };

            // Clicking both the button or the icon ImageView starts the flow.
            buttonRoot.setOnClickListener(onClickListener);
            iconImg.setOnClickListener(onClickListener);
        }

        StartIconDrawable.setPressListener(iconImg);
    }

    private void setServiceIconImage(@Nullable Bitmap bitmap) {
        // Set a placeholder for the image.
        if (bitmap == null) {
            StartIconDrawable placeHolderImage = new StartIconDrawable(getContext(), new ColorDrawable(), 0, 0, false);
            iconImg.setBackground(placeHolderImage);
        } else {
            int iconBackgroundMargin = getResources().getDimensionPixelSize(R.dimen.ifttt_icon_margin);
            BitmapDrawable serviceIcon = new BitmapDrawable(getResources(), bitmap);
            StartIconDrawable drawable = new StartIconDrawable(getContext(), serviceIcon, iconSize,
                    iconImg.getHeight() - iconBackgroundMargin * 2, onDarkBackground);

            iconImg.setBackground(drawable);
            drawable.setBackgroundColor(worksWithService.brandColor);
        }

        // Set elevation.
        ViewCompat.setElevation(iconImg, getResources().getDimension(R.dimen.ifttt_icon_elevation));
    }

    private void complete() {
        buttonRoot.setBackground(buildButtonBackground(getContext(), BLACK));

        ProgressView progressView = ProgressView.create(buttonRoot, worksWithService.brandColor,
                getDarkerColor(worksWithService.brandColor));
        CheckMarkView checkMarkView = CheckMarkView.create(buttonRoot);

        CharSequence text = getResources().getString(R.string.ifttt_connecting_account);
        Animator progress = progressView.progress(0f, 1f, text, ANIM_DURATION_LONG);
        Animator check = checkMarkView.getAnimator(ENABLE);
        check.setStartDelay(100L);
        progress.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressView.hideText();
            }
        });

        int fullDistance = buttonRoot.getWidth() - iconImg.getWidth();
        ValueAnimator iconMovement =
                ValueAnimator.ofInt(fullDistance / 2, fullDistance).setDuration(ANIM_DURATION_MEDIUM);
        iconMovement.setInterpolator(EASE_INTERPOLATOR);
        iconMovement.addUpdateListener(anim -> {
            ViewCompat.offsetLeftAndRight(iconImg, ((Integer) anim.getAnimatedValue()) - iconImg.getLeft());
            ViewCompat.offsetLeftAndRight(checkMarkView, ((Integer) anim.getAnimatedValue()) - checkMarkView.getLeft());

            int color = (int) EVALUATOR.evaluate(anim.getAnimatedFraction(), BLACK, worksWithService.brandColor);
            ((StartIconDrawable) iconImg.getBackground()).setBackgroundColor(color);
        });

        ValueAnimator fadeOutProgress = ValueAnimator.ofFloat(1f, 0f).setDuration(ANIM_DURATION_MEDIUM);
        fadeOutProgress.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            progressView.setAlpha(alpha);
            checkMarkView.setAlpha(alpha);
        });

        AnimatorSet checkMarkAnimator = new AnimatorSet();
        checkMarkAnimator.playSequentially(progress, check, iconMovement);
        checkMarkAnimator.playTogether(iconMovement, fadeOutProgress);
        checkMarkAnimator.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                cleanUpViews(ProgressView.class);
                cleanUpViews(CheckMarkView.class);
            }
        });

        checkMarkAnimator.start();

        connectStateTxt.setAlpha(1f);
        connectStateTxt.setText(getResources().getString(R.string.ifttt_connected));
        ButtonUiHelper.adjustPadding(connectStateTxt);
        dispatchState(Enabled);
    }

    private Animator buildEmailValidationAnimator() {
        // Remove icon elevation when the progress bar is visible.
        ViewCompat.setElevation(iconImg, 0f);

        int primaryProgressColor = ContextCompat.getColor(getContext(), R.color.ifttt_progress_background_color);
        ProgressView progressView = ProgressView.create(buttonRoot, primaryProgressColor, BLACK);

        CharSequence text = getResources().getString(R.string.ifttt_verifying);
        Animator showProgress = progressView.progress(0f, 0.5f, text, ANIM_DURATION_LONG);
        showProgress.setInterpolator(LINEAR_INTERPOLATOR);
        showProgress.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                buttonApiHelper.prepareAuthentication(emailEdt.getText().toString());

                // When the animation starts, disable the click on buttonRoot, so that the flow will not be started
                // again.
                emailEdt.setVisibility(GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                emailEdt.setVisibility(VISIBLE);
                cleanUpViews(ProgressView.class);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                // Reset the icon's StartIconDrawable back to initial state.
                iconImg.setTranslationX(0f);
                ((StartIconDrawable) iconImg.getBackground()).reset();
                ((StartIconDrawable) iconImg.getBackground()).setBackgroundColor(worksWithService.brandColor);

                if (buttonApiHelper.shouldPresentCreateAccount(getContext())) {
                    Animator completeProgress =
                            progressView.progress(0.5f, 1f, getResources().getString(R.string.ifttt_creating_account),
                                    ANIM_DURATION_LONG);
                    completeProgress.setInterpolator(LINEAR_INTERPOLATOR);
                    dispatchState(CreateAccount);
                    AnimatorSet createAccountCompleteSet = new AnimatorSet();
                    createAccountCompleteSet.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            CharSequence emailPrompt = new SpannableString(
                                    replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                                            getContext().getString(R.string.ifttt_new_account_with, emailEdt.getText()),
                                            "IFTTT", iftttLogo));
                            helperTxt.setText(emailPrompt);
                        }
                    });

                    // Play fading out progress bar and its bundled animations after the progress bar has been filled.
                    createAccountCompleteSet.playSequentially(completeProgress,
                            getStartServiceAuthAnimator(worksWithService));
                    createAccountCompleteSet.start();
                } else {
                    Animator completeProgress = progressView.progress(0.5f, 1f, text, ANIM_DURATION_MEDIUM);
                    completeProgress.setInterpolator(LINEAR_INTERPOLATOR);
                    completeProgress.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (isCanceled()) {
                                return;
                            }

                            dispatchState(Login);
                            buttonApiHelper.connect(getContext(), connection, emailEdt.getText().toString(),
                                    buttonState);
                            monitorRedirect();
                        }
                    });

                    completeProgress.start();
                }
            }
        });

        return showProgress;
    }

    /**
     * Start the animation for Connection authentication.
     */
    @CheckReturnValue
    private Animator buildEmailTransitionAnimator(float xvel) {
        // Fade out "Connect X" text.
        ObjectAnimator fadeOutConnect =
                ObjectAnimator.ofFloat(connectStateTxt, "alpha", connectStateTxt.getAlpha(), 0f);
        fadeOutConnect.setDuration(ANIM_DURATION_MEDIUM);

        // Move service icon.
        int startPosition = iconImg.getLeft();
        int endPosition = buttonRoot.getWidth() - iconImg.getWidth();

        // Adjust duration based on the dragging velocity.
        long duration = xvel > 0 ? (long) ((endPosition - startPosition) / xvel * 1000L) : ANIM_DURATION_MEDIUM;
        ObjectAnimator slideIcon = ObjectAnimator.ofFloat(iconImg, "translationX", startPosition, endPosition);
        slideIcon.setDuration(duration);

        // Fade in email EditText.
        ObjectAnimator fadeOutButtonRootBackground = ObjectAnimator.ofInt(buttonRoot.getBackground(), "alpha", 255, 0);
        fadeOutButtonRootBackground.setDuration(duration);
        ObjectAnimator fadeInEmailEdit = ObjectAnimator.ofFloat(emailEdt, "alpha", 0f, 1f);
        fadeInEmailEdit.setDuration(duration);
        fadeInEmailEdit.setStartDelay(duration / 2);
        fadeInEmailEdit.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation) {
                // Hide email field and disable it when the animation starts.
                super.onAnimationStart(animation);
                emailEdt.setEnabled(false);
                emailEdt.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Re-enable email field.
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                emailEdt.setEnabled(true);
            }
        });

        // Adjust icon elevation.
        float startButtonElevation =
                onDarkBackground ? getResources().getDimension(R.dimen.ifttt_start_icon_elevation_dark_mode) : 0f;
        ValueAnimator elevationChange = ValueAnimator.ofFloat(ViewCompat.getElevation(iconImg), startButtonElevation);
        elevationChange.addUpdateListener(
                animation -> ViewCompat.setElevation(iconImg, (Float) animation.getAnimatedValue()));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeOutConnect, fadeInEmailEdit, slideIcon, elevationChange, fadeOutButtonRootBackground);

        // Morph service icon into the start button.
        Animator iconMorphing = ((StartIconDrawable) iconImg.getBackground()).getMorphAnimator();
        if (xvel == 0) {
            // Add a slight delay if the icon is stationary before the animation starts.
            iconMorphing.setDuration(Math.max(ANIM_DURATION_SHORT, duration * 2 / 3));
            iconMorphing.setStartDelay(duration / 3);
        } else {
            iconMorphing.setDuration(Math.max(ANIM_DURATION_SHORT, duration));
        }
        set.playTogether(iconMorphing, fadeOutConnect);
        set.setInterpolator(EASE_INTERPOLATOR);

        OnClickListener startAuthOnClickListener = v -> {
            if (ButtonUiHelper.isEmailInvalid(emailEdt.getText())) {
                helperTxt.setText(getResources().getString(R.string.ifttt_enter_valid_email));
                return;
            }

            v.setOnClickListener(null);
            // Dismiss keyboard if needed.
            InputMethodManager inputMethodManager =
                    (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(emailEdt.getWindowToken(), 0);

            Animator emailValidation = buildEmailValidationAnimator();
            emailValidation.start();
            String email = emailEdt.getText().toString();
            buttonApiHelper.prepareAuthentication(email);
            helperTxt.setOnClickListener(null);
        };

        // Only enable the OnClickListener after the animation has completed.
        set.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                CharSequence authorizePrompt = new SpannableString(
                        replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                                getContext().getString(R.string.ifttt_authorize_with), "IFTTT", iftttLogo));
                helperTxt.setText(authorizePrompt);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                iconImg.setOnClickListener(startAuthOnClickListener);
            }
        });

        return set;
    }

    /**
     * Animate the button to a state for service connection.
     */
    private Animator getStartServiceAuthAnimator(Service service) {
        ProgressView progressView =
                ProgressView.create(buttonRoot, service.brandColor, ButtonUiHelper.getDarkerColor(service.brandColor));
        Runnable clickRunnable = progressView::performClick;
        progressView.setOnClickListener(v -> {
            // Cancel auto advance.
            removeCallbacks(clickRunnable);

            buttonApiHelper.connect(getContext(), connection, emailEdt.getText().toString(), buttonState);
            monitorRedirect();
        });

        Animator animator =
                progressView.progress(0f, 1f, getResources().getString(R.string.ifttt_continue_to, service.name),
                        AUTO_ADVANCE_DELAY);
        animator.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                helperTxt.setText(worksWithIfttt);
                helperTxt.setOnClickListener(null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                // Automatically advance to next step.
                clickRunnable.run();
            }
        });
        return animator;
    }

    private void cleanUpViews(Class<? extends View> clazz) {
        // Remove all invisible progress views.
        boolean isFirst = false;
        for (int i = buttonRoot.getChildCount() - 1; i >= 0; i--) {
            View child = buttonRoot.getChildAt(i);
            if (clazz.isInstance(child)) {
                if (!isFirst) {
                    isFirst = true;
                    // Fade out and then remove the last, visible progress view.
                    child.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            buttonRoot.removeView(child);
                        }
                    }).start();
                } else {
                    buttonRoot.removeView(child);
                }
            }
        }
    }

    private void setProgressStateText(float progress) {
        float fadeOutProgress = progress / FADE_OUT_PROGRESS;
        connectStateTxt.setAlpha(1 - fadeOutProgress);
    }

    private void dispatchState(ConnectButtonState newState) {
        if (buttonStateChangeListener != null && newState != buttonState) {
            buttonStateChangeListener.onStateChanged(newState, buttonState);
        }

        buttonState = newState;
    }

    private void dispatchError(ErrorResponse errorResponse) {
        if (buttonStateChangeListener != null) {
            buttonStateChangeListener.onError(errorResponse);
        }

        // Reset the button state.
        if (connection != null) {
            setConnection(connection);
        }
    }

    private void monitorRedirect() {
        Context context = getContext();
        if (!(context instanceof Activity)) {
            return;
        }

        Application application = ((Activity) context).getApplication();
        if (activityLifecycleCallbacks != null) {
            throw new IllegalStateException("There is an existing ActivityLifecycleCallback.");
        }

        activityLifecycleCallbacks = new AbsActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                if (activity == context) {
                    iconImg.setVisibility(VISIBLE);
                    connectStateTxt.setAlpha(1f);

                    cleanUpViews(ProgressView.class);
                    cleanUpViews(CheckMarkView.class);

                    if (buttonApiHelper.shouldPresentEmail(getContext())) {
                        Animator animator = buildEmailTransitionAnimator(0);
                        animator.start();
                        // Immediately end the animation and move to the email field state.
                        animator.end();
                    } else if (connection != null) {
                        setConnection(connection);
                    }

                    activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                    activityLifecycleCallbacks = null;
                }
            }
        };
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    @Nullable
    private Service findNextServiceToConnect(ConnectResult result) {
        // Find the next service to connect.
        Service nextService = null;
        for (Service service : connection.services) {
            if (service.id.equals(result.serviceId)) {
                nextService = service;
                break;
            }
        }

        return nextService;
    }

    /**
     * {@link ViewDragHelper} subclass that helps handle dragging events for the icon view.
     */
    private final class IconDragHelperCallback extends ViewDragHelper.Callback {

        private int settledAt = 0;
        private int trackEndColor = Color.BLACK;
        private int trackStartColor = Color.BLACK;

        void setSettledAt(Connection.Status status) {
            if (status == enabled) {
                settledAt = buttonRoot.getWidth() - iconImg.getWidth();
            } else {
                settledAt = 0;
            }
        }

        void setTrackColor(@ColorInt int startColor, @ColorInt int endColor) {
            this.trackStartColor = startColor;
            this.trackEndColor = endColor;
        }

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child == iconImg;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            // Only allow the iconImg to be dragged within the button.
            return Math.min(buttonRoot.getWidth() - iconImg.getWidth(), Math.max(0, left));
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            float progress = Math.abs((left - settledAt) / (float) (buttonRoot.getWidth() - iconImg.getWidth()));

            DrawableCompat.setTint(DrawableCompat.wrap(buttonRoot.getBackground()),
                    (Integer) EVALUATOR.evaluate(progress, trackStartColor, trackEndColor));

            float textFadingProgress = Math.max(Math.min(1f, progress * 1.5f), 0f);
            setProgressStateText(textFadingProgress);

            buttonApiHelper.cancelDisconnect();
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            if (child == iconImg) {
                return buttonRoot.getWidth() - iconImg.getWidth();
            }

            return 0;
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            if ((releasedChild.getLeft() + releasedChild.getWidth() / 2) / (float) buttonRoot.getWidth() <= 0.5f) {
                if (connection.status != enabled) {
                    // Connection is already in disabled status.
                    settleView(0, null);
                } else {
                    settleView(0, () -> {
                        // Record the settled position for the view. The animation for disabling Connection will involve
                        // adding/removing views.
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) releasedChild.getLayoutParams();
                        lp.gravity = Gravity.START;
                        releasedChild.setLayoutParams(lp);
                        disableConnection();
                    });
                }
            } else {
                int left = buttonRoot.getWidth() - iconImg.getWidth();
                if (connection.status == enabled) {
                    // Connection is already in enabled status.
                    settleView(left, null);
                } else {
                    if (buttonApiHelper.shouldPresentEmail(getContext())) {
                        settledAt = left;
                        buildEmailTransitionAnimator(xvel).start();
                    } else {
                        settleView(left, () -> {
                            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) releasedChild.getLayoutParams();
                            lp.gravity = Gravity.END;
                            releasedChild.setLayoutParams(lp);

                            buildEmailValidationAnimator().start();
                        });
                    }
                }
            }
        }

        private void settleView(int left, @Nullable Runnable endAction) {
            Runnable settlingAnimation = new Runnable() {
                @Override
                public void run() {
                    if (viewDragHelper.continueSettling(false)) {
                        post(this);
                    } else {
                        settledAt = left;
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                }
            };

            if (viewDragHelper.settleCapturedViewAt(left, 0)) {
                post(settlingAnimation);
            } else {
                settlingAnimation.run();
            }
        }

        private void disableConnection() {
            AnimatorSet processing = new AnimatorSet();
            ObjectAnimator moveIcon = ObjectAnimator.ofFloat(iconImg, "translationX", iconImg.getLeft(), 0);
            moveIcon.setInterpolator(EASE_INTERPOLATOR);
            ObjectAnimator fadeInConnect = ObjectAnimator.ofFloat(connectStateTxt, "alpha", 0f, 0.5f);
            fadeInConnect.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    // Assume the network call will be successful, change the text before the animation starts.
                    connectStateTxt.setCurrentText(
                            getResources().getString(R.string.ifttt_connect_to, worksWithService.shortName));
                    adjustPadding(connectStateTxt);
                }
            });
            processing.playTogether(fadeInConnect, moveIcon);
            processing.setDuration(ANIM_DURATION_SHORT);
            processing.start();
            buttonApiHelper.disableConnection(getLifecycle(), connection.id,
                    new PendingResult.ResultCallback<Connection>() {
                        @Override
                        public void onSuccess(Connection result) {
                            connectStateTxt.animate().alpha(1f).start();
                            setConnection(result);
                            processAndRun(() -> cleanUpViews(ProgressView.class));
                        }

                        @Override
                        public void onFailure(ErrorResponse errorResponse) {
                            if (buttonStateChangeListener != null) {
                                buttonStateChangeListener.onError(errorResponse);
                            }

                            processAndRun(() -> {
                                connectStateTxt.animate().alpha(1f).start();
                                cleanUpViews(ProgressView.class);
                                dispatchError(errorResponse);
                            });
                        }

                        private void processAndRun(Runnable runnable) {
                            if (processing.isRunning()) {
                                processing.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (isCanceled()) {
                                            return;
                                        }

                                        runnable.run();
                                    }
                                });
                            } else {
                                cleanUpViews(ProgressView.class);
                                runnable.run();
                            }
                        }
                    });
        }
    }

    private static final class SavedState implements Parcelable {
        @Nullable final Parcelable superState;
        final ConnectButtonState buttonState;
        final Connection connection;

        SavedState(@Nullable Parcelable superState, ConnectButtonState buttonState, Connection connection) {
            this.superState = superState;
            this.buttonState = buttonState;
            this.connection = connection;
        }

        protected SavedState(Parcel in) {
            superState = in.readParcelable(IftttConnectButton.class.getClassLoader());
            buttonState = (ConnectButtonState) in.readSerializable();
            connection = in.readParcelable(Connection.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(superState, flags);
            dest.writeSerializable(buttonState);
            dest.writeParcelable(connection, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * {@link LifecycleObserver} that records the Animators used in this class, and cancel the ongoing ones when the
     * Activity is stopped.
     */
    private static final class AnimatorLifecycleObserver implements LifecycleObserver {

        private final ArrayList<Animator> ongoingAnimators = new ArrayList<>();

        void addAnimator(Animator animator) {
            ongoingAnimators.add(animator);
        }

        void removeAnimator(Animator animator) {
            ongoingAnimators.remove(animator);
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        void onStop() {
            for (Animator animator : ongoingAnimators) {
                animator.cancel();
            }
            ongoingAnimators.clear();
        }
    }

    /**
     * Helper AnimatorListener for {@link AnimatorLifecycleObserver} to add/remove animators as they are started or
     * stopped.
     */
    private static class CancelAnimatorListenerAdapter extends AnimatorListenerAdapter {

        private boolean isCanceled = false;

        private final AnimatorLifecycleObserver observer;

        private CancelAnimatorListenerAdapter(AnimatorLifecycleObserver observer) {
            this.observer = observer;
        }

        @Override
        @CallSuper
        public void onAnimationCancel(Animator animation) {
            isCanceled = true;
        }

        @Override
        @CallSuper
        public void onAnimationStart(Animator animation) {
            observer.addAnimator(animation);
            isCanceled = false;
        }

        @Override
        @CallSuper
        public void onAnimationEnd(Animator animation) {
            observer.removeAnimator(animation);
        }

        boolean isCanceled() {
            return isCanceled;
        }
    }
}
