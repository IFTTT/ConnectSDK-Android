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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.OnLifecycleEvent;
import com.ifttt.ConnectResult;
import com.ifttt.Connection;
import com.ifttt.ErrorResponse;
import com.ifttt.IftttApiClient;
import com.ifttt.R;
import com.ifttt.Service;
import com.ifttt.api.PendingResult;
import java.util.ArrayList;
import javax.annotation.Nullable;
import okhttp3.Call;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;
import static androidx.lifecycle.Lifecycle.State.CREATED;
import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.lifecycle.Lifecycle.State.STARTED;
import static com.ifttt.Connection.Status.enabled;
import static com.ifttt.ui.ButtonUiHelper.buildButtonBackground;
import static com.ifttt.ui.ButtonUiHelper.getDarkerColor;
import static com.ifttt.ui.ButtonUiHelper.replaceKeyWithImage;
import static com.ifttt.ui.IftttConnectButton.ButtonState.CreateAccount;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Enabled;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Initial;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Login;
import static com.ifttt.ui.IftttConnectButton.ButtonState.ServiceAuthentication;

/**
 *
 */
public final class IftttConnectButton extends LinearLayout implements LifecycleOwner {

    public enum ButtonState {
        /**
         * A button state for displaying an Connection in its initial state, the user has never authenticated this Connection
         * before.
         */
        Initial,

        /**
         * A button state for the create account authentication step. In this step, the user is going to be redirected
         * to web to create an account and continue with service connection.
         */
        CreateAccount,

        /**
         * A button state for the login authentication step. In this step, the user is going to be redirected to web
         * to login to IFTTT.
         */
        Login,

        /**
         * A button state for service connection step. In this step, the user is going to be redirected to web to
         * login to the service and connect to IFTTT.
         */
        ServiceAuthentication,

        /**
         * A button state for displaying an Connection that is enabled.
         */
        Enabled
    }

    private static final ErrorResponse CANCELED_AUTH = new ErrorResponse("canceled", "Authentication canceled");
    private static final ErrorResponse UNKNOWN_STATE = new ErrorResponse("unknown_state", "Cannot verify Button state");

    private static final float FADE_OUT_PROGRESS = 0.5f;

    // Start delay for moving icon animator to coordinate check mark animation.
    private static final long ICON_MOVEMENT_START_DELAY = 3400L;

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
    private final ConnectButtonTextView connectStateTxt;
    private final ViewGroup buttonRoot;
    private final ImageButton iconImg;
    private final TextSwitcher helperTxt;

    private final ViewGroup progressRoot;
    private final TextSwitcher progressTxt;
    private final ImageView completeImg;

    private final int iconSize;

    private final LifecycleRegistry lifecycleRegistry;
    private final AnimatorLifecycleObserver animatorLifecycleObserver = new AnimatorLifecycleObserver();

    private ButtonState buttonState = Initial;
    private Connection connection;
    private Service worksWithService;

    @Nullable private ButtonStateChangeListener buttonStateChangeListener;
    @Nullable private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;
    private ButtonApiHelper buttonApiHelper;
    private IftttApiClient iftttApiClient;
    private String ownerServiceId;

    // Toggle drag events.
    private ViewDragHelper viewDragHelper;
    private IconDragHelperCallback iconDragHelperCallback;

    private boolean onDarkBackground = false;

    @Nullable private Call ongoingImageCall;

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

        buttonRoot = findViewById(R.id.ifttt_toggle_root);
        progressTxt = findViewById(R.id.ifttt_progress_text);

        Typeface boldTypeface = ResourcesCompat.getFont(context, R.font.avenir_next_ltpro_bold);

        emailEdt = findViewById(R.id.ifttt_email);
        emailEdt.setBackground(ButtonUiHelper.buildButtonBackground(context,
                ContextCompat.getColor(getContext(), R.color.ifttt_button_background)));

        connectStateTxt = findViewById(R.id.connect_with_ifttt);
        helperTxt = findViewById(R.id.ifttt_helper_text);
        iconImg = findViewById(R.id.ifttt_icon);

        // Initialize SpannableString that replaces text with logo, using the current TextView in the TextSwitcher as
        // measurement, the CharSequence will only be used there.
        iftttLogo = ContextCompat.getDrawable(getContext(), R.drawable.ic_ifttt_logo_black);
        worksWithIfttt = new SpannableString(replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                getResources().getString(R.string.ifttt_powered_by_ifttt), "IFTTT", iftttLogo));
        worksWithIfttt.setSpan(new AvenirTypefaceSpan(boldTypeface), 0, worksWithIfttt.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        progressRoot = findViewById(R.id.ifttt_progress_container);
        int progressColor = ContextCompat.getColor(getContext(), R.color.ifttt_progress_background_color);
        if (SDK_INT >= KITKAT) {
            // Only use ProgressBackgroundDrawable on Android 19 or above.
            ProgressBackgroundKitKat progressRootBg = new ProgressBackgroundKitKat();
            progressRootBg.setColor(progressColor, BLACK);
            progressRoot.setBackground(progressRootBg);
        } else {
            ProgressBackgroundJellyBean progressRootBg = new ProgressBackgroundJellyBean();
            progressRootBg.setColor(progressColor, BLACK);
            progressRoot.setBackground(progressRootBg);
        }

        int checkMarkSize = getResources().getDimensionPixelSize(R.dimen.ifttt_check_mark_size);
        int circleColor = ContextCompat.getColor(getContext(), R.color.ifttt_semi_transparent_white);
        CheckMarkDrawable drawable = new CheckMarkDrawable(checkMarkSize, circleColor, WHITE);
        completeImg = findViewById(R.id.ifttt_progress_check_mark);
        completeImg.setImageDrawable(drawable);

        iconDragHelperCallback = new IconDragHelperCallback();
        viewDragHelper = ViewDragHelper.create(buttonRoot, iconDragHelperCallback);
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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (viewDragHelper.shouldInterceptTouchEvent(ev)) {
            return true;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        viewDragHelper.processTouchEvent(event);
        return super.onTouchEvent(event);
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
     * Enable the Connect Button's Connection authentication and configuration features with an {@link IftttApiClient}
     * instance and a user email.
     *
     * Note:
     * - The redirect URI must be set for the {@link IftttApiClient} instance here.
     * - User email is a required parameter, the Button will crash if the value is not a valid email in DEBUG build.
     *
     * @param iftttApiClient IftttApiClient instance.
     * @param email This is used to pre-fill the email EditText when the user is doing Connection authentication.
     * @param ownerServiceId The id of the service that this Connect Button is used for. To ensure the Connection flow
     * works with your IFTTT user token, you should make sure the Connection that you are embedding is owned by your
     * service.
     * @param redirectUri URL string that will be used when the Connection authentication flow is completed on web view, in
     * order to return the result to the app.
     * @param oAuthCodeProvider OAuthCodeProvider implementation that returns your user's OAuth code. The code will be
     * used to automatically connect your service on IFTTT for this user.
     */
    public void setup(String email, String ownerServiceId, IftttApiClient iftttApiClient, String redirectUri,
            OAuthCodeProvider oAuthCodeProvider) {
        if (ButtonUiHelper.isEmailInvalid(email)) {
            // Crash in debug build to inform developers.
            throw new IllegalStateException(email
                    + " is not a valid email address, please make sure you pass in a valid user email string to set up IftttConnectButton.");
        }

        if (oAuthCodeProvider == null) {
            throw new IllegalStateException("OAuth token provider cannot be null.");
        }

        if (ownerServiceId == null) {
            throw new IllegalStateException("Owner service id cannot be null.");
        }

        this.iftttApiClient = iftttApiClient;
        this.ownerServiceId = ownerServiceId;
        buttonApiHelper = new ButtonApiHelper(iftttApiClient.api(), redirectUri, iftttApiClient.getInviteCode(),
                oAuthCodeProvider, getLifecycle());
        emailEdt.setText(email);
    }

    /**
     * If the button is used in a dark background, set this flag to true so that the button can adapt the UI. This
     * method must be called before {@link #setConnection(Connection)} to apply the change.
     *
     * @param onDarkBackground True if the button is used in a dark background, false otherwise.
     */
    public void setOnDarkBackground(boolean onDarkBackground) {
        this.onDarkBackground = onDarkBackground;
    }

    /**
     * Set a listener to be notified when the button's state has changed.
     */
    public void setButtonStateChangeListener(ButtonStateChangeListener listener) {
        buttonStateChangeListener = listener;
    }

    /**
     * Given an {@link ConnectResult} from web redirect, refresh the UI of the button to reflect the current
     * state of the Connection authentication flow.
     *
     * @param result Authentication flow redirect result from the web view.
     */
    public void setConnectResult(ConnectResult result) {
        if (activityLifecycleCallbacks != null) {
            // Unregister existing ActivityLifecycleCallbacks and let the AuthenticationResult handle the button
            // state change.
            ((Activity) getContext()).getApplication().unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
            activityLifecycleCallbacks = null;
        }

        switch (result.nextStep) {
            case ServiceAuthentication:
                // If the previous button state is login, complete the progress before going
                Animator animator = buttonState == Login ? ObjectAnimator.ofFloat(progressTxt, "alpha", 1f, 0f)
                        : getProcessingAnimator(getResources().getString(R.string.ifttt_connecting_account), false);
                animator.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (isCanceled()) {
                            return;
                        }

                        worksWithService = findNextServiceToConnect(result);
                        ObjectAnimator fadeOutProgressRoot = ObjectAnimator.ofFloat(progressRoot, "alpha", 1f, 0f);
                        fadeOutProgressRoot.setInterpolator(EASE_INTERPOLATOR);
                        AnimatorSet set = new AnimatorSet();
                        set.playTogether(getStartServiceAuthAnimator(worksWithService), fadeOutProgressRoot);
                        set.start();
                    }
                });
                animator.start();

                recordState(ServiceAuthentication);
                break;
            case Complete:
                complete(result.completeFromConfig);
                break;
            default:
                if (buttonState == Login) {
                    // If the previous state is Login, reset the progress animation.
                    progressRoot.setAlpha(0f);
                    connectStateTxt.setAlpha(1f);
                }

                // The authentication result doesn't contain any next step instruction.
                recordError(UNKNOWN_STATE);
        }
    }

    /**
     * Render the Connect Button to show the status of the Connection.
     *
     * @param connection Connection instance to be displayed.
     */
    public void setConnection(Connection connection) {
        if (buttonApiHelper == null) {
            throw new IllegalStateException("Connect Button is not set up, please call setup() first.");
        }

        if (!connection.getPrimaryService().id.equals(ownerServiceId)) {
            throw new IllegalStateException("The Connection is not owned by " + ownerServiceId);
        }

        this.connection = connection;
        worksWithService = findWorksWithService(connection);

        // Initialize UI.
        ObjectAnimator fadeInButtonRoot = ObjectAnimator.ofFloat(buttonRoot, "alpha", buttonRoot.getAlpha(), 1f);
        fadeInButtonRoot.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                buttonRoot.setBackground(
                        ContextCompat.getDrawable(getContext(), R.drawable.background_button_selector));

                FrameLayout buttonRoot = findViewById(R.id.ifttt_button_root);
                TextView currentHelperTextView = (TextView) helperTxt.getCurrentView();
                TextView nextHelperTextView = (TextView) helperTxt.getNextView();

                if (onDarkBackground) {
                    // Add a border.
                    buttonRoot.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.ifttt_button_border));

                    // Set helper text to white.
                    currentHelperTextView.setTextColor(WHITE);
                    nextHelperTextView.setTextColor(WHITE);

                    // Tint the logo Drawable within the text to white.
                    DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), WHITE);
                } else {
                    // Remove border.
                    buttonRoot.setForeground(null);

                    // Set helper text to black.
                    currentHelperTextView.setTextColor(BLACK);
                    nextHelperTextView.setTextColor(BLACK);

                    // Tint the logo Drawable within the text to black.
                    DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), BLACK);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                helperTxt.setCurrentText(worksWithIfttt);
            }
        });
        fadeInButtonRoot.start();

        connectStateTxt.setAlpha(1f);
        connectStateTxt.showDotsAnimation(false);
        if (connection.status != Connection.Status.enabled) {
            recordState(Initial);
            connectStateTxt.setText(getResources().getString(R.string.ifttt_connect_to, worksWithService.shortName));
            helperTxt.setOnClickListener(new DebouncingOnClickListener() {
                @Override
                void doClick(View v) {
                    getContext().startActivity(new Intent(getContext(), IftttAboutActivity.class));
                }
            });
        } else {
            recordState(Enabled);
            connectStateTxt.setText(getResources().getString(R.string.ifttt_connected));
        }

        iconDragHelperCallback.setSettledAt(connection.status);

        ongoingImageCall = ImageLoader.get().load(this, worksWithService.monochromeIconUrl, bitmap -> {
            ongoingImageCall = null;

            if (ViewCompat.isLaidOut(iconImg)) {
                setServiceIconImage(bitmap);
            } else {
                iconImg.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        iconImg.getViewTreeObserver().removeOnPreDrawListener(this);
                        setServiceIconImage(bitmap);
                        return false;
                    }
                });
            }
        });

        // Warm up icon image cache.
        for (Service service : connection.services) {
            if (service.id.equals(worksWithService.id)) {
                continue;
            }

            ImageLoader.get().fetch(getLifecycle(), service.monochromeIconUrl);
        }

        // Move the icon to the right if the Connection has already been authenticated and enabled.
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) iconImg.getLayoutParams();
        lp.gravity = connection.status == enabled ? Gravity.END : Gravity.START;
        iconImg.setLayoutParams(lp);

        if (connection.status == enabled) {
            DebouncingOnClickListener onClickListener = new DebouncingOnClickListener() {
                @Override
                void doClick(View v) {
                    helperTxt.setText(getResources().getString(R.string.slide_to_turn_off));
                    helperTxt.setClickable(false);

                    // Delay and switch back.
                    postDelayed(() -> {
                        helperTxt.setText(worksWithIfttt);
                        helperTxt.setClickable(true);
                    }, ANIM_DURATION_LONG);
                }
            };
            buttonRoot.setOnClickListener(onClickListener);
            iconImg.setOnClickListener(onClickListener);
        } else {
            DebouncingOnClickListener onClickListener = new DebouncingOnClickListener() {
                @Override
                void doClick(View v) {
                    // Cancel potential ongoing image loading task. Users have already click the button and the service
                    // icon will not be used in the next UI state.
                    if (ongoingImageCall != null) {
                        ongoingImageCall.cancel();
                        ongoingImageCall = null;
                    }

                    if (!iftttApiClient.isUserAuthenticated()) {
                        animateToEmailField(0);
                    } else {
                        getEmailValidationAnimator().start();
                    }
                }
            };

            // Clicking both the button or the icon ImageView starts the flow.
            buttonRoot.setOnClickListener(onClickListener);
            iconImg.setOnClickListener(onClickListener);
        }

        StartIconDrawable.setPressListener(iconImg);

        helperTxt.setOnClickListener(new DebouncingOnClickListener() {
            @Override
            void doClick(View v) {
                getContext().startActivity(IftttAboutActivity.intent(getContext(), connection));
            }
        });
    }

    private void setServiceIconImage(@Nullable Bitmap bitmap) {
        // Set a placeholder for the image.
        final StartIconDrawable placeHolderImage;
        if (iconImg.getBackground() == null) {
            placeHolderImage = new StartIconDrawable(getContext(), new ColorDrawable(), 0, 0, false);
            iconImg.setBackground(placeHolderImage);
            iconImg.setAlpha(1f);
        } else {
            placeHolderImage = null;
        }

        int iconBackgroundMargin = getResources().getDimensionPixelSize(R.dimen.ifttt_icon_margin);
        BitmapDrawable serviceIcon = new BitmapDrawable(getResources(), bitmap);
        StartIconDrawable drawable = new StartIconDrawable(getContext(), serviceIcon, iconSize,
                iconImg.getHeight() - iconBackgroundMargin * 2, onDarkBackground);

        ObjectAnimator fadeInIconImg =
                ObjectAnimator.ofFloat(iconImg, "alpha", placeHolderImage == null ? iconImg.getAlpha() : 0f, 1f);
        fadeInIconImg.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                iconImg.setBackground(drawable);
                drawable.setBackgroundColor(worksWithService.brandColor);

                // Set elevation.
                ViewCompat.setElevation(iconImg, getResources().getDimension(R.dimen.ifttt_icon_elevation));
            }
        });

        fadeInIconImg.start();
    }

    private void complete(boolean hasConfig) {
        buttonRoot.setBackground(buildButtonBackground(getContext(), BLACK));

        int fullDistance = buttonRoot.getWidth() - iconImg.getWidth();
        ValueAnimator iconMovement = ValueAnimator.ofInt(fullDistance / 2, fullDistance);
        iconMovement.setDuration(ANIM_DURATION_MEDIUM);
        iconMovement.setInterpolator(EASE_INTERPOLATOR);
        iconMovement.setStartDelay(ICON_MOVEMENT_START_DELAY);
        iconMovement.addUpdateListener(animation -> {
            ViewCompat.offsetLeftAndRight(iconImg, ((Integer) animation.getAnimatedValue()) - iconImg.getLeft());
            ViewCompat.offsetLeftAndRight(completeImg,
                    ((Integer) animation.getAnimatedValue()) - completeImg.getLeft());
        });

        StartIconDrawable iconDrawable = (StartIconDrawable) iconImg.getBackground();
        iconMovement.addUpdateListener(animation -> {
            int color = (int) EVALUATOR.evaluate(animation.getAnimatedFraction(), BLACK, worksWithService.brandColor);
            iconDrawable.setBackgroundColor(color);
        });

        ValueAnimator iconElevation = ValueAnimator.ofFloat(ViewCompat.getElevation(iconImg),
                getResources().getDimension(R.dimen.ifttt_icon_elevation));
        iconElevation.setDuration(ANIM_DURATION_MEDIUM);
        iconElevation.setInterpolator(EASE_INTERPOLATOR);
        iconElevation.addUpdateListener(
                animation -> ViewCompat.setElevation(iconImg, (Float) animation.getAnimatedValue()));

        AnimatorSet completeSet = new AnimatorSet();
        completeSet.playTogether(iconMovement, iconElevation);

        Animator fadeOutProgressRoot = getFadeOutProgressBarAnimator();
        fadeOutProgressRoot.setStartDelay(ICON_MOVEMENT_START_DELAY);

        CharSequence text = hasConfig ? getResources().getString(R.string.ifttt_save_settings)
                : getResources().getString(R.string.ifttt_connecting_account);
        Animator checkMarkAnimator = getProcessingAnimator(text, true);

        completeSet.playTogether(checkMarkAnimator, iconMovement, fadeOutProgressRoot);
        completeSet.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                recordState(Enabled);

                // Reset check mark drawable position back to the center.
                ViewCompat.offsetLeftAndRight(completeImg, -(progressRoot.getWidth() - completeImg.getWidth()) / 2);

                // Reset progress color.
                ((ProgressBackground) progressRoot.getBackground()).setColor(
                        ContextCompat.getColor(getContext(), R.color.ifttt_progress_background_color), BLACK);

                // After the connection has been authenticated, temporarily disable toggling feature until the new Connection
                // object has been set.
                if (connection.status != enabled) {
                    buttonRoot.setClickable(false);
                    iconImg.setClickable(false);
                }
            }
        });

        completeSet.start();

        connectStateTxt.setAlpha(1f);
        connectStateTxt.setText(getResources().getString(R.string.ifttt_connected));
    }

    private Animator getEmailValidationAnimator() {
        // Remove icon elevation when the progress bar is visible.
        ViewCompat.setElevation(iconImg, 0f);

        ValueAnimator showProgress = ValueAnimator.ofFloat(0f, 0.5f).setDuration(ANIM_DURATION_LONG);
        showProgress.setInterpolator(LINEAR_INTERPOLATOR);
        showProgress.addUpdateListener(animation -> ((ProgressBackground) progressRoot.getBackground()).setProgress(
                (float) animation.getAnimatedValue()));

        ObjectAnimator fadeInProgressContainer = ObjectAnimator.ofFloat(progressRoot, "alpha", 0f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeInProgressContainer, showProgress);
        set.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                // When the animation starts, disable the click on buttonRoot, so that the flow will not be started
                // again.
                buttonRoot.setClickable(false);
                emailEdt.setVisibility(GONE);

                progressTxt.setAlpha(1f);
                if (iftttApiClient.isUserAuthenticated()) {
                    progressTxt.setText(getResources().getString(R.string.ifttt_accessing_account));
                } else {
                    progressTxt.setText(getResources().getString(R.string.ifttt_validating_email));
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                emailEdt.setVisibility(VISIBLE);
                getFadeOutProgressBarAnimator().start();
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

                // Complete email validation progress bar.
                ValueAnimator completeProgress = ValueAnimator.ofFloat(0.5f, 1f);
                completeProgress.setInterpolator(LINEAR_INTERPOLATOR);
                completeProgress.addUpdateListener(
                        animation1 -> ((ProgressBackground) progressRoot.getBackground()).setProgress(
                                (float) animation1.getAnimatedValue()));

                if (!buttonApiHelper.isAccountFound()) {
                    completeProgress.setDuration(ANIM_DURATION_LONG);
                    recordState(CreateAccount);
                    AnimatorSet createAccountCompleteSet = new AnimatorSet();

                    // Fade out progress bar.
                    Animator fadeOutProgressRoot = getFadeOutProgressBarAnimator();

                    // Group fading out progress bar and starting service connection state animation together.
                    createAccountCompleteSet.playTogether(fadeOutProgressRoot,
                            getStartServiceAuthAnimator(worksWithService));

                    // Play fading out progress bar and its bundled animations after the progress bar has been filled.
                    createAccountCompleteSet.playSequentially(completeProgress, fadeOutProgressRoot);
                    createAccountCompleteSet.start();

                    progressTxt.setText(getResources().getString(R.string.ifttt_creating_account));
                } else {
                    completeProgress.setDuration(ANIM_DURATION_MEDIUM);
                    completeProgress.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (isCanceled()) {
                                return;
                            }

                            recordState(Login);
                            buttonApiHelper.redirectToWeb(getContext(), connection, emailEdt.getText().toString(),
                                    buttonState);
                            monitorRedirect();
                        }
                    });

                    completeProgress.start();
                }
            }
        });

        return set;
    }

    /**
     * Start the animation for Connection authentication.
     */
    private void animateToEmailField(float xvel) {
        helperTxt.setText(getResources().getText(R.string.ifttt_authorize_with));

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
        fadeOutButtonRootBackground.setDuration(ANIM_DURATION_MEDIUM);
        ObjectAnimator fadeInEmailEdit = ObjectAnimator.ofFloat(emailEdt, "alpha", 0f, 1f);
        fadeInEmailEdit.setDuration(ANIM_DURATION_MEDIUM);
        fadeInEmailEdit.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation) {
                // Hide email field and disable it when the animation starts.
                super.onAnimationStart(animation);
                emailEdt.setEnabled(false);
                emailEdt.setTextColor(Color.TRANSPARENT);
                emailEdt.setAlpha(0f);
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

        // Fade in email text.
        ValueAnimator fadeInEmailText = ValueAnimator.ofFloat(0f, 1f);
        fadeInEmailText.addUpdateListener(animation -> {
            int textColor = (int) EVALUATOR.evaluate(animation.getAnimatedFraction(), Color.TRANSPARENT, BLACK);
            emailEdt.setTextColor(textColor);
        });

        // Adjust icon elevation.
        float startButtonElevation =
                onDarkBackground ? getResources().getDimension(R.dimen.ifttt_start_icon_elevation_dark_mode) : 0f;
        ValueAnimator elevationChange = ValueAnimator.ofFloat(ViewCompat.getElevation(iconImg), startButtonElevation);
        elevationChange.addUpdateListener(
                animation -> ViewCompat.setElevation(iconImg, (Float) animation.getAnimatedValue()));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeOutConnect, fadeInEmailEdit, slideIcon, elevationChange, fadeOutButtonRootBackground);
        set.playSequentially(slideIcon, fadeInEmailText);

        // Morph service icon into the start button.
        Animator iconMorphing = ((StartIconDrawable) iconImg.getBackground()).getMorphAnimator();
        iconMorphing.setDuration(ANIM_DURATION_MEDIUM);
        set.playTogether(iconMorphing, fadeOutConnect);

        set.setInterpolator(EASE_INTERPOLATOR);

        OnClickListener startAuthOnClickListener = new DebouncingOnClickListener() {
            @Override
            void doClick(View v) {
                if (ButtonUiHelper.isEmailInvalid(emailEdt.getText())) {
                    helperTxt.setText(getResources().getString(R.string.ifttt_enter_valid_email));
                    return;
                }

                v.setClickable(false);
                // Dismiss keyboard if needed.
                InputMethodManager inputMethodManager =
                        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(emailEdt.getWindowToken(), 0);

                Animator emailValidation = getEmailValidationAnimator();
                emailValidation.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);

                        // Reset button background alpha.
                        buttonRoot.getBackground().setAlpha(255);
                    }
                });
                emailValidation.start();

                String email = emailEdt.getText().toString();
                buttonApiHelper.prepareAuthentication(email);
                helperTxt.setClickable(false);
                helperTxt.setText(worksWithIfttt);
            }
        };

        // Only enable the OnClickListener after the animation has completed.
        set.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                iconImg.setOnClickListener(startAuthOnClickListener);
                buttonRoot.setClickable(false);
            }
        });

        set.start();
    }

    /**
     * Animate the button to a state for service connection.
     */
    private Animator getStartServiceAuthAnimator(Service service) {
        buttonRoot.setBackground(buildButtonBackground(getContext(), service.brandColor));
        connectStateTxt.setText(getResources().getString(R.string.ifttt_continue_to, service.shortName));
        connectStateTxt.showDotsAnimation(true);

        Runnable clickRunnable = buttonRoot::performClick;
        buttonRoot.setOnClickListener(new DebouncingOnClickListener() {
            @Override
            void doClick(View v) {
                // Cancel auto advance.
                removeCallbacks(clickRunnable);

                buttonApiHelper.redirectToWeb(getContext(), connection, emailEdt.getText().toString(), buttonState);
                monitorRedirect();
            }
        });

        ImageLoader.get().load(this, service.monochromeIconUrl, bitmap -> {
            int iconBackgroundMargin = getResources().getDimensionPixelSize(R.dimen.ifttt_icon_margin);
            BitmapDrawable serviceIcon = new BitmapDrawable(getResources(), bitmap);
            StartIconDrawable drawable = new StartIconDrawable(getContext(), serviceIcon, iconSize,
                    iconImg.getHeight() - iconBackgroundMargin * 2, onDarkBackground);
            drawable.setBackgroundColor(service.brandColor);
            iconImg.setBackground(drawable);

            // Remove icon elevation when it is shown with a colored background.
            ViewCompat.setElevation(iconImg, 0f);
        });

        ObjectAnimator slideInConnectText = ObjectAnimator.ofFloat(connectStateTxt, "translationX", 50f, 0f);
        ObjectAnimator fadeInConnectText =
                ObjectAnimator.ofFloat(connectStateTxt, "alpha", 0f, 1f).setDuration(ANIM_DURATION_MEDIUM);
        helperTxt.setClickable(false);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(slideInConnectText, fadeInConnectText);
        set.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                // Automatically advance to next step.
                postDelayed(clickRunnable, AUTO_ADVANCE_DELAY);
            }
        });
        return set;
    }

    private Animator getFadeOutProgressBarAnimator() {
        ObjectAnimator fadeOutProgressRoot = ObjectAnimator.ofFloat(progressRoot, "alpha", 1f, 0f);
        fadeOutProgressRoot.setDuration(ANIM_DURATION_MEDIUM);
        fadeOutProgressRoot.setInterpolator(EASE_INTERPOLATOR);
        return fadeOutProgressRoot;
    }

    /**
     * Get an animator set that's used to run a "proceed and complete" animation, including a progress bar and a check
     * mark animation.
     *
     * @param text Text to be shown while the progress animation is running.
     * @return the Animator object that has timing and interpolator set up.
     */
    private Animator getProcessingAnimator(CharSequence text, boolean showCheckMark) {
        // It is possible that the progress bar is already visible in when this animation starts, for example, when
        // the user comes back from web knowing that all of the services have been authenticated. In this case, we will
        // reuse the current alpha value of the progressRoot, to avoid flashing the view.
        ObjectAnimator fadeInProgressContainer =
                ObjectAnimator.ofFloat(progressRoot, "alpha", progressRoot.getAlpha(), 1f);
        fadeInProgressContainer.setInterpolator(EASE_INTERPOLATOR);
        fadeInProgressContainer.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                progressTxt.setText(text);
                progressTxt.setAlpha(1f);
            }
        });

        ValueAnimator completeProgress = ValueAnimator.ofFloat(0f, 1f);
        completeProgress.addUpdateListener(animation -> ((ProgressBackground) progressRoot.getBackground()).setProgress(
                (float) animation.getAnimatedValue()));
        completeProgress.setDuration(ANIM_DURATION_LONG);
        completeProgress.setInterpolator(EASE_INTERPOLATOR);

        CheckMarkDrawable drawable = (CheckMarkDrawable) completeImg.getDrawable();
        Animator checkMarkAnimator = drawable.getAnimator();
        checkMarkAnimator.setStartDelay(200L);

        ObjectAnimator fadeOutProgressTxt = ObjectAnimator.ofFloat(progressTxt, "alpha", 1f, 0f);
        fadeOutProgressTxt.setInterpolator(EASE_INTERPOLATOR);
        fadeOutProgressTxt.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isCanceled()) {
                    return;
                }

                buttonRoot.setBackground(
                        ContextCompat.getDrawable(getContext(), R.drawable.background_button_selector));
            }
        });

        AnimatorSet set = new AnimatorSet();
        if (showCheckMark) {
            set.play(fadeOutProgressTxt).with(checkMarkAnimator);
        }

        set.playSequentially(fadeInProgressContainer, completeProgress, fadeOutProgressTxt);
        set.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                ProgressBackground progressBackground = (ProgressBackground) progressRoot.getBackground();

                // Reset the progress background's progress and color.
                progressBackground.setProgress(0f);
                progressBackground.setColor(worksWithService.brandColor, getDarkerColor(worksWithService.brandColor));
            }
        });

        return set;
    }

    private Service findWorksWithService(Connection connection) {
        Service otherService = null;
        if (connection.services.size() == 1) {
            // If there is only one service involved.
            otherService = connection.services.get(0);
        } else {
            for (Service service : connection.services) {
                if (!service.isPrimary) {
                    otherService = service;
                    break;
                }
            }
        }

        if (otherService == null) {
            throw new IllegalStateException("There is no primary service for this Connection.");
        }

        return otherService;
    }

    private void setProgressStateText(float progress) {
        float fadeOutProgress = progress / FADE_OUT_PROGRESS;
        connectStateTxt.setAlpha(1 - fadeOutProgress);
    }

    private void recordState(ButtonState newState) {
        if (buttonStateChangeListener != null && newState != buttonState) {
            buttonStateChangeListener.onStateChanged(newState, buttonState);
        }

        buttonState = newState;
    }

    private void recordError(ErrorResponse errorResponse) {
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
                    // Remove dots animation.
                    connectStateTxt.showDotsAnimation(false);

                    if (buttonState == Login) {
                        progressRoot.setAlpha(0f);
                        connectStateTxt.setAlpha(1f);
                    }

                    recordError(CANCELED_AUTH);
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

        void setSettledAt(Connection.Status status) {
            if (status == enabled) {
                settledAt = buttonRoot.getWidth() - iconImg.getWidth();
            } else {
                settledAt = 0;
            }
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
            setProgressStateText(progress);
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
                    settleView(0, this::disableConnection);
                }
            } else {
                int left = buttonRoot.getWidth() - iconImg.getWidth();
                if (connection.status == enabled) {
                    // Connection is already in enabled status.
                    settleView(left, null);
                } else {
                    if (!iftttApiClient.isUserAuthenticated()) {
                        settledAt = left;
                        animateToEmailField(Math.abs(xvel));
                    } else {
                        settleView(left, () -> getEmailValidationAnimator().start());
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
            Animator fadeOut = getFadeOutProgressBarAnimator();
            fadeOut.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // At the end of the animation, switch ProgressBackground back to the initial state
                    // colors.
                    super.onAnimationEnd(animation);
                    if (isCanceled()) {
                        return;
                    }
                    int progressColor = ContextCompat.getColor(getContext(), R.color.ifttt_progress_background_color);
                    ((ProgressBackground) progressRoot.getBackground()).setColor(progressColor, BLACK);
                }
            });
            Animator processing = getProcessingAnimator(getResources().getString(R.string.disconnecting), true);
            processing.start();
            buttonApiHelper.disableConnection(getLifecycle(), connection.id,
                    new PendingResult.ResultCallback<Connection>() {
                        @Override
                        public void onSuccess(Connection result) {
                            setConnection(result);

                            if (processing.isRunning()) {
                                processing.addListener(new CancelAnimatorListenerAdapter(animatorLifecycleObserver) {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (isCanceled()) {
                                            return;
                                        }

                                        fadeOut.start();
                                    }
                                });
                            } else {
                                fadeOut.start();
                            }
                        }

                        @Override
                        public void onFailure(ErrorResponse errorResponse) {
                            if (buttonStateChangeListener != null) {
                                buttonStateChangeListener.onError(errorResponse);
                            }

                            fadeOut.start();

                            // Animate the button back to the enabled state, because this callback can only be
                            // used if the user is going to disable the Connection.
                            ValueAnimator iconMovement =
                                    ValueAnimator.ofInt(0, buttonRoot.getWidth() - iconImg.getWidth());
                            iconMovement.setDuration(ANIM_DURATION_MEDIUM);
                            iconMovement.setInterpolator(EASE_INTERPOLATOR);
                            iconMovement.addUpdateListener(animation -> {
                                ViewCompat.offsetLeftAndRight(iconImg,

                                        ((Integer) animation.getAnimatedValue()) - iconImg.getLeft());
                                setProgressStateText(animation.getAnimatedFraction());
                            });
                            iconMovement.start();
                        }
                    });
        }
    }

    private static final class SavedState implements Parcelable {
        @Nullable final Parcelable superState;
        final ButtonState buttonState;
        final Connection connection;

        SavedState(@Nullable Parcelable superState, ButtonState buttonState, Connection connection) {
            this.superState = superState;
            this.buttonState = buttonState;
            this.connection = connection;
        }

        protected SavedState(Parcel in) {
            superState = in.readParcelable(IftttConnectButton.class.getClassLoader());
            buttonState = (ButtonState) in.readSerializable();
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
