package com.ifttt.connect.ui;

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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.ifttt.connect.R;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import com.ifttt.connect.api.ErrorResponse;
import com.ifttt.connect.api.PendingResult;
import com.ifttt.connect.api.Service;
import java.util.ArrayList;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import okhttp3.Call;

import static android.graphics.Color.BLACK;
import static androidx.lifecycle.Lifecycle.State.CREATED;
import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.lifecycle.Lifecycle.State.STARTED;
import static com.ifttt.connect.api.Connection.Status.disabled;
import static com.ifttt.connect.api.Connection.Status.enabled;
import static com.ifttt.connect.api.Connection.Status.never_enabled;
import static com.ifttt.connect.ui.ButtonUiHelper.adjustTextViewLayout;
import static com.ifttt.connect.ui.ButtonUiHelper.buildButtonBackground;
import static com.ifttt.connect.ui.ButtonUiHelper.findWorksWithService;
import static com.ifttt.connect.ui.ButtonUiHelper.getDarkerColor;
import static com.ifttt.connect.ui.ButtonUiHelper.replaceKeyWithImage;
import static com.ifttt.connect.ui.CheckMarkDrawable.AnimatorType.ENABLE;
import static com.ifttt.connect.ui.ConnectButtonState.CreateAccount;
import static com.ifttt.connect.ui.ConnectButtonState.Disabled;
import static com.ifttt.connect.ui.ConnectButtonState.Enabled;
import static com.ifttt.connect.ui.ConnectButtonState.Initial;
import static com.ifttt.connect.ui.ConnectButtonState.Login;
import static com.ifttt.connect.ui.ConnectButtonState.Unknown;

/**
 * Internal implementation of a Connect Button widget, all of the states and transitions are implemented here.
 */
final class BaseConnectButton extends LinearLayout implements LifecycleOwner {

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
    private final RevertableHandler revertableHandler = new RevertableHandler();

    private final EditText emailEdt;
    private final TextSwitcher connectStateTxt;
    private final ImageView iconImg;
    private final TextSwitcher helperTxt;
    private final ButtonParentView buttonRoot;

    private final int iconSize;

    private final LifecycleRegistry lifecycleRegistry;
    private final AnimatorLifecycleObserver animatorLifecycleObserver = new AnimatorLifecycleObserver();

    private final ArrayList<ButtonStateChangeListener> listeners = new ArrayList<>();

    private final int borderSize = getResources().getDimensionPixelSize(R.dimen.ifttt_button_border_width);
    private final Drawable borderDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ifttt_button_border);

    private ConnectButtonState buttonState = Unknown;
    private Connection connection;
    private Service worksWithService;

    @Nullable private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;
    private ButtonApiHelper buttonApiHelper;

    // Toggle drag events.
    private ViewDragHelper viewDragHelper;
    private IconDragHelperCallback iconDragHelperCallback;

    private boolean onDarkBackground = false;
    private boolean skipConnectionConfiguration = false;

    @Nullable private Call ongoingImageCall;

    public BaseConnectButton(Context context) {
        this(context, null);
    }

    public BaseConnectButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseConnectButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setClipToPadding(false);
        setClipChildren(false);

        iconSize = getResources().getDimensionPixelSize(R.dimen.ifttt_icon_image_size);

        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.addObserver(animatorLifecycleObserver);
        lifecycleRegistry.setCurrentState(CREATED);

        inflate(context, R.layout.view_ifttt_connect, this);
        buttonRoot = findViewById(R.id.ifttt_button_root);

        emailEdt = findViewById(R.id.ifttt_email);
        emailEdt.setBackground(
                buildButtonBackground(context, ContextCompat.getColor(getContext(), R.color.ifttt_button_background)));

        connectStateTxt = findViewById(R.id.connect_with_ifttt);
        iconImg = findViewById(R.id.ifttt_icon);

        // Initialize SpannableString that replaces text with logo, using the current TextView in the TextSwitcher as
        // measurement, the CharSequence will only be used there.
        helperTxt = findViewById(R.id.ifttt_helper_text);
        iftttLogo = ContextCompat.getDrawable(getContext(), R.drawable.ic_ifttt_logo_black);
        worksWithIfttt = new SpannableString(replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                getResources().getString(R.string.works_with_ifttt), "IFTTT", iftttLogo));

        // Tint the logo Drawable within the text to black.
        DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), ContextCompat.getColor(context, R.color.ifttt_footer_text_black));

        helperTxt.setCurrentText(worksWithIfttt);

        iconDragHelperCallback = new IconDragHelperCallback();
        viewDragHelper = buttonRoot.getViewDragHelperCallback(iconDragHelperCallback);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lifecycleRegistry.setCurrentState(STARTED);
        AnalyticsManager.getInstance(getContext())
                .flushTrackedEvents();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        AnalyticsManager.getInstance(getContext())
                .flushTrackedEvents();
        lifecycleRegistry.setCurrentState(DESTROYED);
        revertableHandler.clear();
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

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // Prepare the border Drawable to have the right size and positioning.
        borderDrawable.setBounds(buttonRoot.getLeft() - borderSize, buttonRoot.getTop() - borderSize,
                buttonRoot.getRight() + borderSize, buttonRoot.getBottom() + borderSize);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        borderDrawable.draw(canvas);
    }

    void setErrorMessage(CharSequence text, OnClickListener listener) {
        helperTxt.setText(text);
        helperTxt.setOnClickListener(v -> {
            listener.onClick(v);

            // Revert back to the original text.
            helperTxt.setClickable(false);
            helperTxt.showNext();
        });
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
     * @param redirectUri Uri that will be used when the Connection authentication flow is completed on web view, in
     * order to return the result to the app.
     * @param credentialsProvider CredentialsProvider implementation that returns your user's OAuth code. The code will be
     * used to automatically connect your service on IFTTT for this user.
     * @param inviteCode Optional invite code to access an IFTTT service that has not yet published.
     * @param skipConnectionConfiguration Optional flag that configures the ConnectButton so that the connection
     * configuration step is skipped.
     */
    void setup(
        String email,
        ConnectionApiClient connectionApiClient,
        Uri redirectUri,
        CredentialsProvider credentialsProvider,
        @Nullable String inviteCode,
        boolean skipConnectionConfiguration
    ) {
        buttonApiHelper =
                new ButtonApiHelper(connectionApiClient, redirectUri, inviteCode, credentialsProvider, getLifecycle(),
                    skipConnectionConfiguration);
        emailEdt.setText(email);

        this.skipConnectionConfiguration = skipConnectionConfiguration;
    }

    /**
     * If the button is used in a dark background, set this flag to true so that the button can adapt the UI.
     * @param onDarkBackground True if the button is used in a dark background, false otherwise.
     */
    void setOnDarkBackground(boolean onDarkBackground) {
        if (this.onDarkBackground == onDarkBackground) {
            return;
        }

        this.onDarkBackground = onDarkBackground;

        TextView currentHelperTextView = (TextView) helperTxt.getCurrentView();
        TextView nextHelperTextView = (TextView) helperTxt.getNextView();

        if (onDarkBackground) {
            // Set helper text to white.
            int semiTransparentWhite = ContextCompat.getColor(getContext(), R.color.ifttt_footer_text_white);
            currentHelperTextView.setTextColor(semiTransparentWhite);
            currentHelperTextView.setLinkTextColor(semiTransparentWhite);
            nextHelperTextView.setTextColor(semiTransparentWhite);
            nextHelperTextView.setLinkTextColor(semiTransparentWhite);

            // Tint the logo Drawable within the text to white.
            DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), semiTransparentWhite);
        } else {
            // Set helper text to black.
            int semiTransparentBlack = ContextCompat.getColor(getContext(), R.color.ifttt_footer_text_black);
            currentHelperTextView.setTextColor(semiTransparentBlack);
            currentHelperTextView.setLinkTextColor(semiTransparentBlack);
            nextHelperTextView.setTextColor(semiTransparentBlack);
            nextHelperTextView.setLinkTextColor(semiTransparentBlack);

            // Tint the logo Drawable within the text to black.
            DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), semiTransparentBlack);
        }

        invalidate();
    }

    /**
     * Add a listener to be notified when the button's state has changed.
     */
    void addButtonStateChangeListener(ButtonStateChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a previously registered listener.
     */
    void removeButtonStateChangeListener(ButtonStateChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Given an {@link ConnectResult} from web redirect, refresh the UI of the button to reflect the current
     * state of the Connection authentication flow.
     *
     * @param result Authentication flow redirect result from the web view.
     */
    void setConnectResult(@Nullable ConnectResult result) {
        if (activityLifecycleCallbacks != null) {
            // Unregister existing ActivityLifecycleCallbacks and let the AuthenticationResult handle the button
            // state change.
            ((Activity) getContext()).getApplication().unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
            activityLifecycleCallbacks = null;
        }

        if (result == null) {
            return;
        }

        cleanUpViews(ProgressView.class);
        switch (result.nextStep) {
            case Complete:
                CharSequence text = getResources().getString(R.string.connecting);
                ProgressView progressView = ProgressView.addTo(buttonRoot, worksWithService.brandColor,
                        getDarkerColor(worksWithService.brandColor));
                Animator progress = progressView.progress(0f, 1f, text, ANIM_DURATION_LONG);
                progress.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        progressView.setProgressText(null);
                        complete(progressView);
                    }
                });

                progress.start();
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

        revertableHandler.revertAll();
        iconDragHelperCallback.isEnabled = true;

        this.connection = connection;
        worksWithService = findWorksWithService(connection);

        emailEdt.setVisibility(GONE);

        helperTxt.setCurrentText(worksWithIfttt);

        iconDragHelperCallback.setSettledAt(connection.status);

        setServiceIconImage(null);
        ongoingImageCall = ImageLoader.get().load(getLifecycle(), worksWithService.monochromeIconUrl, bitmap -> {
            ongoingImageCall = null;
            setServiceIconImage(bitmap);
        });

        connectStateTxt.setAlpha(1f);
        buttonRoot.setBackground(buildButtonBackground(getContext(), BLACK));

        if (connection.status == enabled) {
            dispatchState(Enabled);
            connectStateTxt.setCurrentText(getResources().getString(R.string.connected));
            adjustTextViewLayout(connectStateTxt, buttonState);

            OnClickListener onClickListener = v -> {
                buttonRoot.setOnClickListener(null);
                iconImg.setOnClickListener(null);

                Animator moveToggle = buildSlideIconAnimator(iconEnabledPosition(), 0, true);
                AnimatorSet set = new AnimatorSet();
                set.playSequentially(moveToggle, buildDisconnectAnimator());
                set.start();
                AnalyticsManager.getInstance(getContext())
                        .trackUiClick(AnalyticsObject.ConnectionAnalyticsObject.fromConnection(connection), AnalyticsLocation.fromConnectButton(getContext()));
            };

            buttonRoot.setOnClickListener(onClickListener);
            iconImg.setOnClickListener(onClickListener);

            iconDragHelperCallback.setTrackEndColor(BLACK);
        } else {
            if (connection.status == Connection.Status.disabled) {
                dispatchState(Disabled);
                connectStateTxt.setCurrentText(
                        getResources().getString(R.string.connect_service, worksWithService.shortName));
                adjustTextViewLayout(connectStateTxt, buttonState);
                iconDragHelperCallback.setTrackEndColor(BLACK);
            } else {
                dispatchState(Initial);
                connectStateTxt.setCurrentText(
                        getResources().getString(R.string.connect_service, worksWithService.shortName));
                adjustTextViewLayout(connectStateTxt, buttonState);

                // Depending on whether we need to show the email field, use different track colors.
                int trackEndColor = !buttonApiHelper.shouldPresentEmail(getContext()) ? BLACK
                        : ContextCompat.getColor(getContext(), R.color.ifttt_button_background);

                iconDragHelperCallback.setTrackEndColor(trackEndColor);
            }

            OnClickListener onClickListener = v -> {
                buttonRoot.setOnClickListener(null);
                iconImg.setOnClickListener(null);
                revertableHandler.revertAll();
                // Cancel potential ongoing image loading task. Users have already click the button and the service
                // icon will not be used in the next UI state.
                if (ongoingImageCall != null) {
                    ongoingImageCall.cancel();
                    ongoingImageCall = null;
                }

                // Cancel potential disable connection API call.
                buttonApiHelper.cancelDisconnect();

                startEnableFlow(0f, 0, iconEnabledPosition());

                AnalyticsManager.getInstance(getContext())
                        .trackUiClick(AnalyticsObject.ConnectionAnalyticsObject.fromConnection(connection), AnalyticsLocation.fromConnectButton(getContext()));
            };

            // Clicking both the button or the icon ImageView starts the flow.
            buttonRoot.setOnClickListener(onClickListener);
            iconImg.setOnClickListener(onClickListener);
        }

        // Set button position.
        setIconPosition(connection.status);

        StartIconDrawable.setPressListener(iconImg);

        helperTxt.setOnClickListener(v -> {
            getContext().startActivity(AboutIftttActivity.intent(getContext(), connection));
            AnalyticsManager.getInstance(getContext())
                    .trackUiClick(AnalyticsObject.WORKS_WITH_IFTTT, AnalyticsLocation.WORKS_WITH_IFTTT);
        });

        AnalyticsManager.getInstance(getContext())
                .trackUiImpression(AnalyticsObject.ConnectionAnalyticsObject.fromConnection(connection),
                AnalyticsLocation.fromConnectButtonWithId(connection.id));
    }

    private void setIconPosition(Connection.Status status) {
        Runnable updatePosition = () -> {
            int startPosition = status == enabled ? 0 : iconEnabledPosition();
            int endPosition = status == enabled ? iconEnabledPosition() : 0;
            if (endPosition == iconImg.getLeft()) {
                buttonRoot.trackViewLeftAndRightOffset(iconImg, endPosition);
            } else {
                buildSlideIconAnimator(startPosition, endPosition, false).start();
            }
        };

        if (ViewCompat.isLaidOut(buttonRoot)) {
            updatePosition.run();
        } else {
            buttonRoot.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                    int oldTop, int oldRight, int oldBottom) {
                    buttonRoot.removeOnLayoutChangeListener(this);
                    updatePosition.run();
                }
            });
        }
    }

    private void setServiceIconImage(@Nullable Bitmap bitmap) {
        // Set a placeholder for the image.
        if (bitmap == null) {
            StartIconDrawable placeHolderImage = new StartIconDrawable(getContext(), new ColorDrawable(), 0, 0);
            placeHolderImage.setBackgroundColor(worksWithService.brandColor);
            iconImg.setBackground(placeHolderImage);
            iconImg.setContentDescription(getContext().getString(R.string.content_desc_start_icon));
        } else {
            int iconBackgroundMargin = getResources().getDimensionPixelSize(R.dimen.ifttt_icon_margin);
            BitmapDrawable serviceIcon = new BitmapDrawable(getResources(), bitmap);
            StartIconDrawable drawable = new StartIconDrawable(getContext(), serviceIcon, iconSize,
                    iconImg.getHeight() - iconBackgroundMargin * 2);

            iconImg.setBackground(drawable);
            drawable.setBackgroundColor(worksWithService.brandColor);
            iconImg.setContentDescription(
                    getContext().getString(R.string.content_desc_service_icon, worksWithService.name));
        }

        // Set elevation.
        ViewCompat.setElevation(iconImg, getResources().getDimension(R.dimen.ifttt_icon_elevation));
    }

    private void startEnableFlow(float xvel, int iconStartPosition, int iconEnabledPosition) {
        if (connection.status == enabled) {
            throw new IllegalStateException("An enabled connection cannot start enable flow again.");
        }

        if (connection.status != disabled) {
            // For never_enabled and unknown state, start the regular enable hand-off flow.
            if (buttonApiHelper.shouldPresentEmail(getContext())) {
                buildEmailTransitionAnimator(xvel).start();
            } else {
                Animator slideIcon = buildSlideIconAnimator(iconStartPosition, iconEnabledPosition, true);
                Animator emailValidation = buildEmailValidationAnimator();
                AnimatorSet set = new AnimatorSet();
                set.playSequentially(slideIcon, emailValidation);
                set.start();
            }
        } else {
            // For disabled state, try using the re-enable API to enable the connection.
            int primaryProgressColor =
                ContextCompat.getColor(getContext(), R.color.ifttt_progress_background_color);
            ProgressView progressView = ProgressView.addTo(buttonRoot, BLACK, primaryProgressColor);
            Animator progress = progressView.progress(
                0f,
                1f,
                getResources().getString(R.string.connecting),
                ANIM_DURATION_LONG);
            progress.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    if (isCanceled()) {
                        return;
                    }

                    // Remove icon elevation when the progress bar is visible.
                    ViewCompat.setElevation(iconImg, 0f);

                    buttonApiHelper.reenableConnection(getLifecycle(), connection.id,
                        new PendingResult.ResultCallback<Connection>() {
                            @Override
                            public void onSuccess(Connection result) {
                                processAndRun(progress, () -> {
                                    setConnection(result);

                                    progressView.setProgressText(null);
                                    complete(progressView);
                                });
                            }

                            @Override
                            public void onFailure(ErrorResponse errorResponse) {
                                processAndRun(progress, () -> {
                                    if (skipConnectionConfiguration) {
                                        // Skipping connection configuration also means we will surface the
                                        // error instead of redirecting.
                                        dispatchError(errorResponse);
                                    } else {
                                        // Otherwise, redirect to web to resolve any potential
                                        // issues and continue the re-enable flow.
                                        buttonApiHelper.connect(getContext(),
                                            connection,
                                            emailEdt.getText().toString(),
                                            buttonState
                                        );
                                        monitorRedirect();
                                    }
                                });
                            }
                        });
                }
            });

            if (iconStartPosition == iconEnabledPosition) {
                progress.start();
            } else {
                AnimatorSet set = new AnimatorSet();
                Animator slideIcon = buildSlideIconAnimator(iconStartPosition, iconEnabledPosition, true);
                set.playSequentially(slideIcon, progress);
                set.start();
            }
        }
    }

    private void complete(ProgressView currentProgress) {
        CheckMarkView checkMarkView = CheckMarkView.create(buttonRoot);
        Animator check = checkMarkView.getAnimator(ENABLE);
        check.setStartDelay(100L);

        int iconMargin = getResources().getDimensionPixelSize(R.dimen.ifttt_icon_margin);
        int fullDistance = iconEnabledPosition();
        ValueAnimator iconMovement =
                ValueAnimator.ofInt(fullDistance / 2 + iconMargin, fullDistance).setDuration(ANIM_DURATION_MEDIUM);
        iconMovement.setInterpolator(EASE_INTERPOLATOR);
        iconMovement.addUpdateListener(new SlideIconAnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                super.onAnimationUpdate(animation);
                ViewCompat.offsetLeftAndRight(checkMarkView,
                        ((Integer) animation.getAnimatedValue()) - checkMarkView.getLeft());
            }
        });

        ValueAnimator fadeOutCheckMark = ValueAnimator.ofFloat(1f, 0f).setDuration(ANIM_DURATION_MEDIUM);
        fadeOutCheckMark.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            checkMarkView.setAlpha(alpha);
        });

        int elevation = getResources().getDimensionPixelSize(R.dimen.ifttt_icon_elevation);
        ValueAnimator changeElevation = ValueAnimator.ofFloat(ViewCompat.getElevation(iconImg), elevation);
        changeElevation.addUpdateListener(
                animation -> ViewCompat.setElevation(iconImg, (Float) animation.getAnimatedValue()));
        changeElevation.setInterpolator(EASE_INTERPOLATOR);
        changeElevation.setDuration(ANIM_DURATION_MEDIUM);

        AnimatorSet animatorSet = new AnimatorSet();

        animatorSet.playSequentially(check, iconMovement);
        Animator fadeOutProgress =
                ObjectAnimator.ofFloat(currentProgress, "alpha", 1f, 0f).setDuration(ANIM_DURATION_MEDIUM);
        fadeOutProgress.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (isCanceled()) {
                    return;
                }

                currentProgress.setProgressText(null);
            }
        });
        animatorSet.playTogether(iconMovement, fadeOutCheckMark, changeElevation, fadeOutProgress);
        animatorSet.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (isCanceled()) {
                    return;
                }

                // Reset email UI.
                emailEdt.setVisibility(GONE);
                ((StartIconDrawable) iconImg.getBackground()).reset();
                ((StartIconDrawable) iconImg.getBackground()).setBackgroundColor(worksWithService.brandColor);

                // Reset Button background.
                buttonRoot.setBackground(buildButtonBackground(getContext(), BLACK));

                // Reset Button text.
                connectStateTxt.setAlpha(1f);
                connectStateTxt.setText(getResources().getString(R.string.connected));
                adjustTextViewLayout(connectStateTxt, Enabled);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                cleanUpViews(ProgressView.class);
                cleanUpViews(CheckMarkView.class);

                PendingResult.ResultCallback<Connection> callback = new PendingResult.ResultCallback<Connection>() {
                    @Override
                    public void onSuccess(Connection result) {
                        setConnection(result);
                    }

                    @Override
                    public void onFailure(ErrorResponse errorResponse) {
                        dispatchError(errorResponse);
                    }
                };

                buttonApiHelper.fetchConnection(lifecycleRegistry, connection.id, callback);
            }
        });

        buttonRoot.addView(checkMarkView);
        animatorSet.start();
    }

    private Animator buildEmailValidationAnimator() {
        int primaryProgressColor = ContextCompat.getColor(getContext(), R.color.ifttt_progress_background_color);
        ProgressView progressView = ProgressView.addTo(buttonRoot, primaryProgressColor, BLACK);

        CharSequence text = getResources().getString(R.string.connecting);
        Animator showProgress = progressView.progress(0f, 0.5f, text, ANIM_DURATION_LONG);
        showProgress.setInterpolator(LINEAR_INTERPOLATOR);
        showProgress.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (isCanceled()) {
                    return;
                }

                // Remove icon elevation when the progress bar is visible.
                ViewCompat.setElevation(iconImg, 0f);

                buttonApiHelper.prepareAuthentication(emailEdt.getText().toString());

                // When the animation starts, disable the click on buttonRoot, so that the flow will not be started
                // again.
                emailEdt.setVisibility(GONE);
                iconDragHelperCallback.isEnabled = false;
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

                ((StartIconDrawable) iconImg.getBackground()).reset();
                ((StartIconDrawable) iconImg.getBackground()).setBackgroundColor(worksWithService.brandColor);

                if (buttonApiHelper.shouldPresentCreateAccount(getContext())) {
                    AnimatorSet set = new AnimatorSet();
                    Animator completeProgress =
                            progressView.progress(0.5f, 1f, getResources().getString(R.string.creating_account),
                                    ANIM_DURATION_LONG);
                    completeProgress.setInterpolator(LINEAR_INTERPOLATOR);
                    completeProgress.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            if (isCanceled()) {
                                return;
                            }

                            dispatchState(CreateAccount);
                            CharSequence emailPrompt = new SpannableString(
                                    replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                                            getContext().getString(R.string.new_account_for, emailEdt.getText()),
                                            "IFTTT", iftttLogo));
                            helperTxt.setText(emailPrompt);
                        }
                    });

                    set.playSequentially(completeProgress, buildStartServiceAuthAnimator(worksWithService));
                    set.start();
                } else {
                    Animator completeProgress =
                            progressView.progress(0.5f, 1f, getResources().getString(R.string.connecting),
                                    ANIM_DURATION_MEDIUM);
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
        int endPosition = iconEnabledPosition();

        // Adjust duration based on the dragging velocity.
        long duration = xvel > 0 ? (long) ((endPosition - startPosition) / xvel * 1000L)
                : (long) (ANIM_DURATION_MEDIUM * (1f - startPosition / (float) endPosition));
        ValueAnimator slideIcon = ValueAnimator.ofInt(startPosition, endPosition);
        slideIcon.addUpdateListener(new SlideIconAnimatorUpdateListener());
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
                iconDragHelperCallback.isEnabled = false;
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

        ValueAnimator elevationChange = ValueAnimator.ofFloat(ViewCompat.getElevation(iconImg), 0f);
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
            AnalyticsManager.getInstance(getContext())
                    .trackUiClick(AnalyticsObject.fromConnectionEmail(connection.id),
                    AnalyticsLocation.fromConnectButtonWithId(connection.id));

            revertableHandler.revertAll();
            if (ButtonUiHelper.isEmailInvalid(emailEdt.getText())) {
                Revertable revertable = new Revertable() {
                    @Override
                    public void revert() {
                        helperTxt.showNext();
                        helperTxt.setClickable(true);
                    }

                    @Override
                    public void run() {
                        SpannableString errorMessage =
                                new SpannableString(getResources().getString(R.string.enter_valid_email));
                        errorMessage.setSpan(
                                new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.ifttt_error_red)),
                                0, errorMessage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        helperTxt.setText(errorMessage);
                        helperTxt.getCurrentView().setAlpha(1f);
                        helperTxt.setClickable(false);
                    }
                };

                revertableHandler.run(revertable, ANIM_DURATION_LONG);
                return;
            }

            v.setOnClickListener(null);
            // Dismiss keyboard if needed.
            InputMethodManager inputMethodManager =
                    (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(emailEdt.getWindowToken(), 0);

            buildEmailValidationAnimator().start();
            helperTxt.setClickable(false);
        };

        // Only enable the OnClickListener after the animation has completed.
        set.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                SpannableString authorizePrompt = new SpannableString(
                        replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                                getContext().getString(R.string.secured_with_ifttt), "IFTTT", iftttLogo));

                String termLearnMore = getContext().getString(R.string.term_learn_more);
                int learnMoreIndex = getContext().getString(R.string.secured_with_ifttt).indexOf(termLearnMore);
                if (learnMoreIndex != -1) {
                    authorizePrompt.setSpan(new UnderlineSpan(), learnMoreIndex, learnMoreIndex + termLearnMore.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                helperTxt.setText(authorizePrompt);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                iconImg.setOnClickListener(startAuthOnClickListener);
                emailEdt.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        startAuthOnClickListener.onClick(v);
                    }
                    return false;
                });

                AnalyticsManager.getInstance(getContext()).trackUiImpression(
                    AnalyticsObject.fromConnectionEmail(connection.id),
                    AnalyticsLocation.fromConnectButtonWithId(connection.id)
                );
                helperTxt.setClickable(true);
            }
        });

        return set;
    }

    /**
     * Animate the button to a state for service connection.
     */
    @CheckReturnValue
    private Animator buildStartServiceAuthAnimator(Service service) {
        ProgressView progressView =
                ProgressView.addTo(buttonRoot, service.brandColor, ButtonUiHelper.getDarkerColor(service.brandColor));
        Runnable clickRunnable = progressView::performClick;
        progressView.setOnClickListener(v -> {
            // Cancel auto advance.
            removeCallbacks(clickRunnable);

            buttonApiHelper.connect(getContext(), connection, emailEdt.getText().toString(), buttonState);
            monitorRedirect();
        });

        Animator animator =
                progressView.progress(0f, 1f, getResources().getString(R.string.going_to_service, service.name),
                        AUTO_ADVANCE_DELAY);
        animator.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                helperTxt.setText(worksWithIfttt);
                helperTxt.setClickable(false);
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

    @CheckReturnValue
    private ValueAnimator buildSlideIconAnimator(int startPosition, int endPosition, boolean changeProgressTextAlpha) {
        ValueAnimator moveToggle = ValueAnimator.ofInt(startPosition, endPosition);
        moveToggle.setDuration(ANIM_DURATION_MEDIUM);
        moveToggle.setInterpolator(EASE_INTERPOLATOR);
        moveToggle.addUpdateListener(new SlideIconAnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (changeProgressTextAlpha) {
                    setProgressStateTextAlpha(animation.getAnimatedFraction());
                }

                super.onAnimationUpdate(animation);
            }
        });

        return moveToggle;
    }

    @CheckReturnValue
    private Animator buildDisconnectAnimator() {
        int primaryProgressColor = ContextCompat.getColor(getContext(), R.color.ifttt_progress_background_color);
        ProgressView progressView = ProgressView.addTo(buttonRoot, BLACK, primaryProgressColor);
        Animator showProgress = progressView.progress(0f, 1f, getResources().getString(R.string.disconnecting),
                ANIM_DURATION_LONG);
        showProgress.setInterpolator(LINEAR_INTERPOLATOR);
        showProgress.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation, boolean isReverse) {
                super.onAnimationStart(animation, isReverse);

                // Remove icon elevation when the progress bar is visible.
                ViewCompat.setElevation(iconImg, 0f);

                buttonApiHelper.disableConnection(getLifecycle(), connection.id,
                        new PendingResult.ResultCallback<Connection>() {
                            @Override
                            public void onSuccess(Connection result) {
                                processAndRun(animation, () -> {
                                    progressView.setProgressText(getResources().getString(R.string.disconnected));
                                    progressView.postDelayed(() -> {
                                        cleanUpViews(ProgressView.class);
                                        dispatchState(Disabled);
                                        setConnection(result);
                                    }, ANIM_DURATION_LONG);
                                });
                            }

                            @Override
                            public void onFailure(ErrorResponse errorResponse) {
                                processAndRun(animation, () -> {
                                    connectStateTxt.setAlpha(1f);
                                    cleanUpViews(ProgressView.class);
                                    dispatchError(errorResponse);

                                    // Reset knob position.
                                    buildSlideIconAnimator(iconImg.getLeft(), iconEnabledPosition(), false).start();
                                });
                            }
                        });
            }
        });

        return showProgress;
    }

    private void processAndRun(Animator animator, Runnable runnable) {
        if (animator.isRunning()) {
            animator.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
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
            runnable.run();
        }
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

    private void setProgressStateTextAlpha(float progress) {
        float fadeOutProgress = progress / FADE_OUT_PROGRESS;
        connectStateTxt.setAlpha(1 - fadeOutProgress);
    }

    private void dispatchState(ConnectButtonState newState) {
        if (newState != buttonState) {
            for (ButtonStateChangeListener listener : listeners) {
                listener.onStateChanged(newState, buttonState, connection);
            }
        }

        buttonState = newState;
    }

    private void dispatchError(ErrorResponse errorResponse) {
        for (ButtonStateChangeListener listener : listeners) {
            listener.onError(errorResponse);
        }

        // Reset the button state.
        if (buttonApiHelper.shouldPresentEmail(getContext())) {
            Animator animator = buildEmailTransitionAnimator(0);
            // Immediately end the animation and move to the email field state.
            animator.start();
            animator.end();
        } else if (connection != null) {
            setConnection(connection);

            cleanUpViews(ProgressView.class);
            cleanUpViews(CheckMarkView.class);
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
                        // Immediately end the animation and move to the email field state.
                        animator.start();
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

    private int iconEnabledPosition() {
        return buttonRoot.getWidth() - iconImg.getWidth();
    }

    /**
     * {@link ViewDragHelper} subclass that helps handle dragging events for the icon view.
     */
    private final class IconDragHelperCallback extends ViewDragHelper.Callback {

        private int settledAt = 0;
        private int trackEndColor = Color.BLACK;

        boolean isEnabled = true;

        void setSettledAt(Connection.Status status) {
            if (status == enabled) {
                settledAt = iconEnabledPosition();
            } else {
                settledAt = 0;
            }
        }

        void setTrackEndColor(@ColorInt int endColor) {
            this.trackEndColor = endColor;
        }

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            if (!isEnabled) {
                // Do not capture any view if disabled.
                return false;
            }

            return child == iconImg;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            // Only allow the iconImg to be dragged within the button.
            return Math.min(iconEnabledPosition(), Math.max(0, left));
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return iconImg.getTop();
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            float progress = Math.abs((left - settledAt) / (float) (iconEnabledPosition()));

            DrawableCompat.setTint(DrawableCompat.wrap(buttonRoot.getBackground()),
                    (Integer) EVALUATOR.evaluate(progress, Color.BLACK, trackEndColor));

            float textFadingProgress = Math.max(Math.min(1f, progress * 1.5f), 0f);
            setProgressStateTextAlpha(textFadingProgress);

            buttonApiHelper.cancelDisconnect();
            revertableHandler.revertAll();

            buttonRoot.trackViewLeftAndRightOffset(changedView, left);
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            if (child == iconImg) {
                return iconEnabledPosition();
            }

            return 0;
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            if ((releasedChild.getLeft() + releasedChild.getWidth() / 2f) / (float) buttonRoot.getWidth() <= 0.5f) {
                if (connection.status != enabled) {
                    // Connection is already in disabled status.
                    settleView(releasedChild, 0, null);
                } else {
                    settleView(releasedChild, 0, () -> buildDisconnectAnimator().start());
                }
            } else {
                int left = iconEnabledPosition();
                if (connection.status == enabled) {
                    // Connection is already in enabled status.
                    settleView(releasedChild, left, null);
                } else {
                    if (buttonApiHelper.shouldPresentEmail(getContext()) && connection.status == never_enabled) {
                        // Special case for the animation as we don't use the typical icon sliding animation, but
                        // instead a completely different, transitioning to email TextView animation.
                        settledAt = left;
                        buildEmailTransitionAnimator(xvel).start();
                    } else {
                        // In the case of enabling, we will always start the flow when the icon is settled in the
                        // enabled position.
                        int iconPosition = iconEnabledPosition();
                        settleView(releasedChild, left, () -> startEnableFlow(xvel, iconPosition, iconPosition));
                    }
                }
            }
        }

        private void settleView(View changedView, int left, @Nullable Runnable endAction) {
            Runnable settlingAnimation = new Runnable() {
                @Override
                public void run() {
                    if (viewDragHelper.continueSettling(false)) {
                        post(this);
                    } else {
                        settledAt = left;
                        buttonRoot.trackViewLeftAndRightOffset(changedView, left);
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                }
            };

            if (viewDragHelper.settleCapturedViewAt(left, iconImg.getTop())) {
                post(settlingAnimation);
            } else {
                settlingAnimation.run();
            }
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
            superState = in.readParcelable(BaseConnectButton.class.getClassLoader());
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

    /**
     * {@link ValueAnimator.AnimatorUpdateListener} used on ValueAnimators that moves the button position.
     */
    private class SlideIconAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {

        @CallSuper
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int value = (Integer) animation.getAnimatedValue();
            int offset = value - iconImg.getLeft();
            ViewCompat.offsetLeftAndRight(iconImg, offset);
            buttonRoot.trackViewLeftAndRightOffset(iconImg, value);
        }
    }
}
