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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import com.ifttt.ConnectResult;
import com.ifttt.Connection;
import com.ifttt.ErrorResponse;
import com.ifttt.IftttApiClient;
import com.ifttt.R;
import com.ifttt.Service;
import com.ifttt.api.PendingResult.ResultCallback;
import javax.annotation.Nullable;
import okhttp3.Call;

import static android.graphics.Color.BLACK;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.lifecycle.Lifecycle.State.CREATED;
import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.lifecycle.Lifecycle.State.STARTED;
import static com.ifttt.Connection.Status.enabled;
import static com.ifttt.ui.ButtonUiHelper.TextTransitionType.Appear;
import static com.ifttt.ui.ButtonUiHelper.TextTransitionType.Change;
import static com.ifttt.ui.ButtonUiHelper.buildButtonBackground;
import static com.ifttt.ui.ButtonUiHelper.buildStateListButtonBackground;
import static com.ifttt.ui.ButtonUiHelper.getDarkerColor;
import static com.ifttt.ui.ButtonUiHelper.getTextTransitionAnimator;
import static com.ifttt.ui.ButtonUiHelper.replaceKeyWithImage;
import static com.ifttt.ui.ButtonUiHelper.setConnectStateText;
import static com.ifttt.ui.ButtonUiHelper.setTextSwitcherText;
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

    private static final float FADE_OUT_PROGRESS = 0.3f;
    private static final float FADE_IN_PROGRESS = 0.6f;

    // Start delay for moving icon animator to coordinate check mark animation.
    private static final long ICON_MOVEMENT_START_DELAY = 3400L;

    private static final long ANIM_DURATION_MEDIUM = 700L;
    private static final long ANIM_DURATION_LONG = 1500L;
    private static final LinearInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final FastOutSlowInInterpolator EASE_INTERPOLATOR = new FastOutSlowInInterpolator();

    private static final String AVENIR_MEDIUM = "avenir_next_ltpro_medium.otf";
    private static final String AVENIR_BOLD = "avenir_next_ltpro_bold.otf";

    private static final ArgbEvaluator EVALUATOR = new ArgbEvaluator();

    private final ResultCallback<Connection> connectionResultCallback = new ResultCallback<Connection>() {
        @Override
        public void onSuccess(Connection result) {
            String connectText = getResources().getString(R.string.ifttt_connect_to, worksWithService.name);
            setConnectStateText(connectStateTxt, connectText, getResources().getString(R.string.ifttt_connect));
            setTextSwitcherText(helperTxt, poweredByIfttt);

            Animator connectTextReset = getTextTransitionAnimator(connectStateTxt, Change, connectText);
            connectTextReset.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setConnection(result);
                }
            });

            connectTextReset.start();
        }

        @Override
        public void onFailure(ErrorResponse errorResponse) {
            iconDragHelperCallback.setDragEnabled(true);

            if (buttonStateChangeListener != null) {
                buttonStateChangeListener.onError(errorResponse);
            }
        }
    };

    // Spannable text that replaces the text "IFTTT" with IFTTT logo.
    private final SpannableString poweredByIfttt;
    private final CharSequence manageConnection;
    private final Drawable iftttLogo;

    private final EditText emailEdt;
    private final TextView connectStateTxt;
    private final ViewGroup buttonRoot;
    private final ImageButton iconImg;
    private final TextSwitcher helperTxt;

    private final ViewGroup progressRoot;
    private final TextView progressTxt;
    private final ImageView completeImg;
    private final ImageView progressIconImg;

    private final int iconSize;

    private final LifecycleRegistry lifecycleRegistry;

    private boolean isUserAuthenticated = false;

    private ButtonState buttonState = Initial;
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
        lifecycleRegistry.markState(CREATED);

        inflate(context, R.layout.view_ifttt_connect, this);

        Typeface mediumTypeface = Typeface.createFromAsset(context.getAssets(), AVENIR_MEDIUM);
        Typeface boldTypeface = Typeface.createFromAsset(context.getAssets(), AVENIR_BOLD);

        emailEdt = findViewById(R.id.ifttt_email);
        emailEdt.setTypeface(mediumTypeface);
        Drawable emailBackground = ButtonUiHelper.buildButtonBackground(context,
                ContextCompat.getColor(getContext(), R.color.ifttt_button_background));
        emailEdt.setBackground(emailBackground);

        connectStateTxt = findViewById(R.id.connect_with_ifttt);
        connectStateTxt.setTypeface(boldTypeface);

        helperTxt = findViewById(R.id.ifttt_helper_text);
        helperTxt.setFactory(() -> {
            TextView textView = (TextView) LayoutInflater.from(context)
                    .inflate(R.layout.view_ifttt_helper_text, IftttConnectButton.this, false);

            // Workaround: TextSwitcher is looking for the View's LayoutParams to be a FrameLayout.LayoutParams. So
            // set one up here so that it doesn't complain.
            textView.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            textView.setTypeface(mediumTypeface);
            return textView;
        });
        helperTxt.setInAnimation(context, R.anim.ifttt_helper_text_in);
        helperTxt.setOutAnimation(context, R.anim.ifttt_helper_text_out);

        // Initialize SpannableString that replaces text with logo, using the current TextView in the TextSwitcher as
        // measurement, the CharSequence will only be used there.
        iftttLogo = ContextCompat.getDrawable(getContext(), R.drawable.ic_ifttt_logo_black);
        poweredByIfttt = new SpannableString(replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                getResources().getString(R.string.ifttt_powered_by_ifttt), "IFTTT", iftttLogo));
        poweredByIfttt.setSpan(new AvenirTypefaceSpan(boldTypeface), 0, poweredByIfttt.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        manageConnection = replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                getResources().getString(R.string.ifttt_all_set), "IFTTT", iftttLogo);

        helperTxt.setOnClickListener(new DebouncingOnClickListener() {
            @Override
            void doClick(View v) {
                getContext().startActivity(IftttAboutActivity.intent(context, connection));
            }
        });

        buttonRoot = findViewById(R.id.ifttt_toggle_root);

        progressRoot = findViewById(R.id.ifttt_progress_container);
        if (SDK_INT >= KITKAT) {
            // Only use ProgressBackgroundDrawable on Android 19 or above.
            ProgressBackgroundKitKat progressRootBg = new ProgressBackgroundKitKat();
            progressRootBg.setColor(ContextCompat.getColor(getContext(), R.color.ifttt_progress_background_color),
                    BLACK);
            progressRoot.setBackground(progressRootBg);
        } else {
            ProgressBackgroundJellyBean progressRootBg = new ProgressBackgroundJellyBean();
            progressRootBg.setColor(ContextCompat.getColor(getContext(), R.color.ifttt_progress_background_color),
                    BLACK);
            progressRoot.setBackground(progressRootBg);
        }

        progressTxt = findViewById(R.id.ifttt_progress_text);
        progressTxt.setTypeface(boldTypeface);

        int checkMarkSize = getResources().getDimensionPixelSize(R.dimen.ifttt_check_mark_size);
        int circleColor = ContextCompat.getColor(getContext(), R.color.ifttt_semi_transparent_white);
        int dotColor = Color.WHITE;
        CheckMarkDrawable drawable = new CheckMarkDrawable(checkMarkSize, circleColor, dotColor);
        completeImg = findViewById(R.id.ifttt_progress_check_mark);
        completeImg.setImageDrawable(drawable);
        progressIconImg = findViewById(R.id.ifttt_progress_service_icon);

        iconImg = findViewById(R.id.ifttt_icon);

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
        setConnection(savedState.connection);
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
     * @param redirectUri URL string that will be used when the Connection authentication flow is completed on web view, in
     * order to return the result to the app.
     * @param oAuthCodeProvider OAuthCodeProvider implementation that returns your user's OAuth code. The code will be
     * used to automatically connect your service on IFTTT for this user.
     */
    public void setup(String email, IftttApiClient iftttApiClient, String redirectUri,
            OAuthCodeProvider oAuthCodeProvider) {
        if (ButtonUiHelper.isEmailInvalid(email)) {
            // Crash in debug build to inform developers.
            throw new IllegalStateException(email
                    + " is not a valid email address, please make sure you pass in a valid user email string to set up IftttConnectButton.");
        }

        if (oAuthCodeProvider == null) {
            throw new IllegalStateException("OAuth token provider cannot be null.");
        }

        buttonApiHelper = new ButtonApiHelper(iftttApiClient, redirectUri, oAuthCodeProvider, getLifecycle());
        isUserAuthenticated = iftttApiClient.isUserAuthenticated();
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
                Animator animator = buttonState == Login ? getCheckMarkAnimator()
                        : getServiceAuthProcessingAnimator(getResources().getString(R.string.ifttt_connecting_account));
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
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

        this.connection = connection;
        worksWithService = findWorksWithService(connection);

        // Initialize UI.
        ObjectAnimator fadeInButtonRoot = ObjectAnimator.ofFloat(buttonRoot, "alpha", 0f, 1f);
        fadeInButtonRoot.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                buttonRoot.setBackground(buildStateListButtonBackground(getContext(), BLACK,
                        ContextCompat.getColor(getContext(), R.color.ifttt_background_touch_color)));

                FrameLayout buttonRoot = findViewById(R.id.ifttt_button_root);
                TextView currentHelperTextView = (TextView) helperTxt.getCurrentView();
                TextView nextHelperTextView = (TextView) helperTxt.getNextView();

                if (onDarkBackground) {
                    // Add a border.
                    buttonRoot.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.ifttt_button_border));

                    // Set helper text to white.
                    currentHelperTextView.setTextColor(Color.WHITE);
                    nextHelperTextView.setTextColor(Color.WHITE);

                    // Tint the logo Drawable within the text to white.
                    DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), Color.WHITE);
                } else {
                    // Remove border.
                    buttonRoot.setForeground(null);

                    // Set helper text to black.
                    currentHelperTextView.setTextColor(Color.BLACK);
                    nextHelperTextView.setTextColor(Color.BLACK);

                    // Tint the logo Drawable within the text to black.
                    DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), Color.BLACK);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (connection.status == enabled) {
                    setTextSwitcherText(helperTxt, manageConnection);
                } else {
                    setTextSwitcherText(helperTxt, poweredByIfttt);
                }
            }
        });
        fadeInButtonRoot.start();

        final Context context = getContext();
        if (connection.status != Connection.Status.enabled) {
            recordState(Initial);

            setConnectStateText(connectStateTxt,
                    getResources().getString(R.string.ifttt_connect_to, worksWithService.name),
                    getResources().getString(R.string.ifttt_connect));

            helperTxt.setClickable(true);
            iconDragHelperCallback.setDragEnabled(false);
        } else {
            recordState(Enabled);

            // Remove the email validation click listener.
            iconImg.setOnClickListener(null);

            CharSequence enabledText = getResources().getText(R.string.ifttt_connected);
            CharSequence disabledText = getResources().getText(R.string.ifttt_connection_off);
            iconDragHelperCallback.setDragEnabled(true);
            iconDragHelperCallback.setTexts(enabledText, disabledText);

            float progress = 1f;
            setConnectTextState(progress, enabledText, disabledText);

            helperTxt.setOnClickListener(new DebouncingOnClickListener() {
                @Override
                void doClick(View v) {
                    buttonApiHelper.redirectToPlayStore(context);
                }
            });
        }

        ongoingImageCall = ImageLoader.get().load(this, worksWithService.monochromeIconUrl, bitmap -> {
            ongoingImageCall = null;
            BitmapDrawable serviceIcon = new BitmapDrawable(getResources(), bitmap);
            int iconBackgroundMargin = getResources().getDimensionPixelSize(R.dimen.ifttt_icon_margin);
            StartIconDrawable drawable = new StartIconDrawable(context, serviceIcon, iconSize,
                    iconImg.getHeight() - iconBackgroundMargin * 2, onDarkBackground);

            ObjectAnimator fadeInIconImg = ObjectAnimator.ofFloat(iconImg, "alpha", 0f, 1f);
            fadeInIconImg.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    iconImg.setBackground(drawable);
                    drawable.setBackgroundColor(worksWithService.brandColor);

                    // Set elevation.
                    ViewCompat.setElevation(iconImg, getResources().getDimension(R.dimen.ifttt_icon_elevation));
                }
            });

            fadeInIconImg.start();
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
            buttonRoot.setOnClickListener(new DebouncingOnClickListener() {
                @Override
                void doClick(View v) {
                    setTextSwitcherText(helperTxt, getResources().getString(R.string.slide_to_turn_off));
                }
            });
        } else {
            buttonRoot.setOnClickListener(new DebouncingOnClickListener() {
                @Override
                void doClick(View v) {
                    // Cancel potential ongoing image loading task. Users have already click the button and the service
                    // icon will not be used in the next UI state.
                    if (ongoingImageCall != null) {
                        ongoingImageCall.cancel();
                        ongoingImageCall = null;
                    }

                    if (!isUserAuthenticated) {
                        animateToEmailField();
                    } else {
                        animateEmailValidation();
                    }
                }
            });
        }
    }

    private void complete(boolean hasConfig) {
        buttonRoot.setBackground(buildButtonBackground(getContext(), BLACK));

        int fullDistance = buttonRoot.getWidth() - iconImg.getWidth();
        ValueAnimator iconMovement = ValueAnimator.ofInt(fullDistance / 2, fullDistance);
        iconMovement.setDuration(ANIM_DURATION_MEDIUM);
        iconMovement.setInterpolator(EASE_INTERPOLATOR);
        iconMovement.addUpdateListener(animation -> {
            ViewCompat.offsetLeftAndRight(iconImg, ((Integer) animation.getAnimatedValue()) - iconImg.getLeft());
            ViewCompat.offsetLeftAndRight(completeImg,
                    ((Integer) animation.getAnimatedValue()) - completeImg.getLeft());
        });

        if (iconImg.getBackground() != null) {
            StartIconDrawable iconDrawable = (StartIconDrawable) iconImg.getBackground();
            iconMovement.addUpdateListener(animation -> {
                int color =
                        (int) EVALUATOR.evaluate(animation.getAnimatedFraction(), BLACK, worksWithService.brandColor);
                iconDrawable.setBackgroundColor(color);
            });
        }

        ValueAnimator iconElevation = ValueAnimator.ofFloat(ViewCompat.getElevation(iconImg),
                getResources().getDimension(R.dimen.ifttt_icon_elevation));
        iconElevation.setDuration(ANIM_DURATION_MEDIUM);
        iconElevation.setInterpolator(EASE_INTERPOLATOR);
        iconElevation.addUpdateListener(
                animation -> ViewCompat.setElevation(iconImg, (Float) animation.getAnimatedValue()));

        AnimatorSet completeSet = new AnimatorSet();

        ObjectAnimator fadeInConnectStateTxt = ObjectAnimator.ofFloat(connectStateTxt, "alpha", 0f, 1f);
        fadeInConnectStateTxt.setInterpolator(EASE_INTERPOLATOR);

        completeSet.playTogether(iconMovement, iconElevation, fadeInConnectStateTxt);

        ObjectAnimator fadeOutProgressRoot = ObjectAnimator.ofFloat(progressRoot, "alpha", 1f, 0f);
        fadeOutProgressRoot.setDuration(ANIM_DURATION_MEDIUM);
        fadeOutProgressRoot.setInterpolator(EASE_INTERPOLATOR);

        CharSequence text = hasConfig ? getResources().getString(R.string.ifttt_save_settings)
                : getResources().getString(R.string.ifttt_connecting_account);
        Animator checkMarkAnimator = getServiceAuthProcessingAnimator(text);
        iconMovement.setStartDelay(ICON_MOVEMENT_START_DELAY);
        fadeOutProgressRoot.setStartDelay(ICON_MOVEMENT_START_DELAY);
        completeSet.playTogether(checkMarkAnimator, iconMovement, fadeOutProgressRoot);

        completeSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                connectStateTxt.setAlpha(0f);
                connectStateTxt.setText(R.string.ifttt_connected);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                recordState(Enabled);

                setTextSwitcherText(helperTxt, manageConnection);
                helperTxt.setOnClickListener(new DebouncingOnClickListener() {
                    @Override
                    void doClick(View v) {
                        buttonApiHelper.redirectToPlayStore(getContext());
                    }
                });

                // After the connection has been authenticated, temporarily disable toggling feature until the new Connection
                // object has been set.
                if (connection.status != enabled) {
                    buttonRoot.setClickable(false);
                    iconImg.setClickable(false);
                }
            }
        });

        completeSet.start();
    }

    private void animateEmailValidation() {
        setTextSwitcherText(helperTxt, poweredByIfttt);

        if (isUserAuthenticated) {
            progressTxt.setText(
                    replaceKeyWithImage(connectStateTxt, getResources().getString(R.string.ifttt_signing_in_to_ifttt),
                            "IFTTT", ContextCompat.getDrawable(getContext(), R.drawable.ic_ifttt_logo_white)));
        } else {
            progressTxt.setText(R.string.ifttt_validating_email);
        }

        // Remove icon elevation when the progress bar is visible.
        ViewCompat.setElevation(iconImg, 0f);

        Animator showProgressText = getTextTransitionAnimator(progressTxt, Appear, null);

        ValueAnimator showProgress = ValueAnimator.ofFloat(0f, 0.5f).setDuration(ANIM_DURATION_LONG);
        showProgress.setInterpolator(LINEAR_INTERPOLATOR);
        showProgress.addUpdateListener(animation -> ((ProgressBackground) progressRoot.getBackground()).setProgress(
                (float) animation.getAnimatedValue()));

        ObjectAnimator fadeInProgressContainer = ObjectAnimator.ofFloat(progressRoot, "alpha", 0f, 1f);
        fadeInProgressContainer.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // After fading in progress bar, reset the icon's StartIconDrawable back to initial state.
                iconImg.setTranslationX(0f);
                if (iconImg.getBackground() != null) {
                    ((StartIconDrawable) iconImg.getBackground()).reset();
                    ((StartIconDrawable) iconImg.getBackground()).setBackgroundColor(worksWithService.brandColor);
                }
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeInProgressContainer, showProgress, showProgressText);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                emailEdt.setVisibility(GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
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

                    Animator fadeInCheckMark = getCheckMarkAnimator();

                    // Fade out progress bar.
                    ObjectAnimator fadeOutProgressRoot = ObjectAnimator.ofFloat(progressRoot, "alpha", 1f, 0f);
                    fadeOutProgressRoot.setInterpolator(EASE_INTERPOLATOR);

                    // Group changing progress text and completing progress bar animation together.
                    createAccountCompleteSet.playTogether(getTextTransitionAnimator(progressTxt, Change,
                            getResources().getString(R.string.ifttt_creating_account)), completeProgress);

                    // Group fading out progress bar and starting service connection state animation together.
                    createAccountCompleteSet.playTogether(fadeOutProgressRoot,
                            getStartServiceAuthAnimator(worksWithService));

                    // Play fading out progress bar and its bundled animations after the progress bar has been filled.
                    createAccountCompleteSet.playSequentially(completeProgress, fadeInCheckMark, fadeOutProgressRoot);
                    createAccountCompleteSet.start();
                } else {
                    completeProgress.setDuration(ANIM_DURATION_MEDIUM);
                    completeProgress.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
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

        set.start();
    }

    /**
     * Start the animation for Connection authentication.
     */
    private void animateToEmailField() {
        setTextSwitcherText(helperTxt, getResources().getString(R.string.ifttt_sign_in_to_ifttt_or_create_new_account));
        helperTxt.setClickable(false);

        // Fade out "Connect X" text.
        ObjectAnimator fadeOutConnect = ObjectAnimator.ofFloat(connectStateTxt, "alpha", 1f, 0f);
        fadeOutConnect.setDuration(ANIM_DURATION_MEDIUM);

        // Move service icon.
        ObjectAnimator slideIcon =
                ObjectAnimator.ofFloat(iconImg, "translationX", 0f, buttonRoot.getWidth() - iconImg.getWidth());
        slideIcon.setDuration(ANIM_DURATION_MEDIUM);

        // Fade in email EditText.
        ObjectAnimator fadeInEmailEdit = ObjectAnimator.ofFloat(emailEdt, "alpha", 0f, 1f);
        fadeInEmailEdit.setDuration(ANIM_DURATION_MEDIUM);
        fadeInEmailEdit.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Hide email field and disable it when the animation starts.
                emailEdt.setEnabled(false);
                emailEdt.setTextColor(Color.TRANSPARENT);
                emailEdt.setAlpha(0f);
                emailEdt.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Re-enable email field.
                emailEdt.setEnabled(true);
            }
        });

        // Fade in email text.
        ValueAnimator fadeInEmailText = ValueAnimator.ofFloat(0f, 1f);
        fadeInEmailText.addUpdateListener(animation -> {
            int textColor = (int) EVALUATOR.evaluate(animation.getAnimatedFraction(), Color.TRANSPARENT, Color.BLACK);
            emailEdt.setTextColor(textColor);
        });

        // Adjust icon elevation.
        float startButtonElevation =
                onDarkBackground ? getResources().getDimension(R.dimen.ifttt_start_icon_elevation_dark_mode) : 0f;
        ValueAnimator elevationChange = ValueAnimator.ofFloat(ViewCompat.getElevation(iconImg), startButtonElevation);
        elevationChange.addUpdateListener(
                animation -> ViewCompat.setElevation(iconImg, (Float) animation.getAnimatedValue()));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeOutConnect, fadeInEmailEdit, slideIcon, elevationChange);
        set.playSequentially(slideIcon, fadeInEmailText);

        // Morph service icon into the start button.
        if (iconImg.getBackground() != null) {
            Animator iconMorphing = ((StartIconDrawable) iconImg.getBackground()).getMorphAnimator();
            iconMorphing.setDuration(ANIM_DURATION_MEDIUM);
            set.playTogether(iconMorphing, fadeOutConnect);
        }

        set.setInterpolator(EASE_INTERPOLATOR);

        OnClickListener startAuthOnClickListener = new DebouncingOnClickListener() {
            @Override
            void doClick(View v) {
                if (ButtonUiHelper.isEmailInvalid(emailEdt.getText())) {
                    setTextSwitcherText(helperTxt, getResources().getString(R.string.ifttt_enter_valid_email));
                    return;
                }

                v.setClickable(false);
                // Dismiss keyboard if needed.
                InputMethodManager inputMethodManager =
                        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(emailEdt.getWindowToken(), 0);

                animateEmailValidation();

                String email = emailEdt.getText().toString();
                buttonApiHelper.prepareAuthentication(email);

                setTextSwitcherText(helperTxt, poweredByIfttt);
            }
        };

        // Only enable the OnClickListener after the animation has completed.
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                iconImg.setOnClickListener(startAuthOnClickListener);
                connectStateTxt.setClickable(false);
            }
        });

        set.start();
    }

    /**
     * Animate the button to a state for service connection.
     */
    private Animator getStartServiceAuthAnimator(Service service) {
        buttonRoot.setBackground(buildButtonBackground(getContext(), service.brandColor));
        setConnectStateText(connectStateTxt, getResources().getString(R.string.ifttt_sign_in_to, service.name),
                getResources().getString(R.string.ifttt_sign_in));
        buttonRoot.setOnClickListener(new DebouncingOnClickListener() {
            @Override
            void doClick(View v) {
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
        fadeInConnectText.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                setTextSwitcherText(helperTxt,
                        getResources().getString(R.string.ifttt_connect_services_description, service.name,
                                connection.getPrimaryService().name));
            }
        });

        helperTxt.setClickable(false);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(slideInConnectText, fadeInConnectText);
        return set;
    }

    private Animator getCheckMarkAnimator() {
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(ObjectAnimator.ofFloat(progressTxt, "alpha", 1f, 0f),
                ((CheckMarkDrawable) completeImg.getDrawable()).getAnimator());
        return set;
    }

    /**
     * Get an animator set that's used to run a "proceed and complete" animation, including a progress bar and a check
     * mark animation.
     *
     * @param text Text to be shown while the progress animation is running.
     * @return the Animator object that has timing and interpolator set up.
     */
    private Animator getServiceAuthProcessingAnimator(CharSequence text) {
        ObjectAnimator fadeInIconImg = ObjectAnimator.ofFloat(progressIconImg, "alpha", 0f, 1f);
        fadeInIconImg.setInterpolator(EASE_INTERPOLATOR);

        // It is possible that the progress bar is already visible in when this animation starts, for example, when
        // the user comes back from web knowing that all of the services have been authenticated. In this case, we will
        // reuse the current alpha value of the progressRoot, to avoid flashing the view.
        ObjectAnimator fadeInProgressContainer =
                ObjectAnimator.ofFloat(progressRoot, "alpha", progressRoot.getAlpha(), 1f);
        fadeInProgressContainer.setInterpolator(EASE_INTERPOLATOR);
        fadeInProgressContainer.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
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
        checkMarkAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                buttonRoot.setBackground(buildStateListButtonBackground(getContext(), Color.BLACK,
                        ContextCompat.getColor(getContext(), R.color.ifttt_background_touch_color)));
            }
        });

        ObjectAnimator fadeOutProgressTxt = ObjectAnimator.ofFloat(progressTxt, "alpha", 1f, 0f);
        fadeOutProgressTxt.setInterpolator(EASE_INTERPOLATOR);

        ObjectAnimator fadeOutServiceIcon = ObjectAnimator.ofFloat(progressIconImg, "alpha", 1f, 0f);
        fadeOutServiceIcon.setInterpolator(EASE_INTERPOLATOR);

        Animator textTransition = getTextTransitionAnimator(connectStateTxt, Appear, null);
        AnimatorSet set = new AnimatorSet();
        set.play(checkMarkAnimator).with(textTransition).with(fadeOutServiceIcon).with(fadeOutProgressTxt);
        set.playSequentially(fadeInProgressContainer, completeProgress, checkMarkAnimator);
        set.playTogether(fadeInProgressContainer, fadeInIconImg);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                ProgressBackground progressBackground = (ProgressBackground) progressRoot.getBackground();

                // Reset the progress background's progress and color.
                progressBackground.setProgress(0f);
                progressBackground.setColor(worksWithService.brandColor, getDarkerColor(worksWithService.brandColor));
                ImageLoader.get().load(IftttConnectButton.this, worksWithService.monochromeIconUrl, bitmap -> {
                    if (bitmap != null) {
                        progressIconImg.setImageBitmap(bitmap);
                    }
                });
            }
        });

        return set;
    }

    private Service findWorksWithService(Connection connection) {
        Service otherService = null;
        for (Service service : connection.services) {
            if (!service.isPrimary) {
                otherService = service;
                break;
            }
        }

        if (otherService == null) {
            throw new IllegalStateException("There is no primary service for this Connection.");
        }

        return otherService;
    }

    private void setConnectTextState(float progress, CharSequence enabledText, CharSequence disabledText) {
        if (progress <= FADE_OUT_PROGRESS) {
            float fadeOutProgress = progress / FADE_OUT_PROGRESS;
            connectStateTxt.setAlpha(1 - fadeOutProgress);
        } else if (progress >= FADE_IN_PROGRESS) {
            float fadeInProgress = (progress - FADE_IN_PROGRESS) / (1 - FADE_IN_PROGRESS);
            connectStateTxt.setAlpha(fadeInProgress);
        }

        if (progress > 0.5f && !enabledText.equals(connectStateTxt.getText())) {
            connectStateTxt.setText(enabledText);
        } else if (progress < 0.5f && !disabledText.equals(connectStateTxt.getText())) {
            connectStateTxt.setText(disabledText);
        }
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
        setConnection(connection);
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

        private CharSequence enabledText;
        private CharSequence disabledText;
        private boolean dragEnabled;

        IconDragHelperCallback() {
        }

        void setDragEnabled(boolean dragEnabled) {
            this.dragEnabled = dragEnabled;
        }

        void setTexts(CharSequence enabledText, CharSequence disabledText) {
            this.enabledText = enabledText;
            this.disabledText = disabledText;
        }

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            if (!dragEnabled) {
                return false;
            }

            return child == iconImg;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            // Only allow the iconImg to be dragged within the button.
            return Math.min(buttonRoot.getWidth() - iconImg.getWidth(), Math.max(0, left));
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            float progress = left / (float) (buttonRoot.getWidth() - iconImg.getWidth());
            setConnectTextState(progress, enabledText, disabledText);
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
            if ((releasedChild.getLeft() + iconImg.getWidth() / 2) / (float) buttonRoot.getWidth() <= 0.5f) {
                Runnable settlingAnimation = new Runnable() {
                    @Override
                    public void run() {
                        if (viewDragHelper.continueSettling(false)) {
                            post(this);
                        } else {
                            buttonApiHelper.disableConnection(connection.id, connectionResultCallback);
                        }
                    }
                };
                // Off state.
                if (viewDragHelper.settleCapturedViewAt(0, 0)) {
                    post(settlingAnimation);
                }

                dragEnabled = false;
            } else if (viewDragHelper.settleCapturedViewAt(buttonRoot.getWidth() - iconImg.getWidth(), 0)) {
                Runnable settlingAnimation = new Runnable() {
                    @Override
                    public void run() {
                        if (viewDragHelper.continueSettling(false)) {
                            post(this);
                        } else {
                            setTextSwitcherText(helperTxt, manageConnection);
                        }
                    }
                };
                // Back to on state.
                post(settlingAnimation);
            }
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
}
