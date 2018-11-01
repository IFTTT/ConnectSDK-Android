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
import com.ifttt.Applet;
import com.ifttt.AuthenticationResult;
import com.ifttt.ErrorResponse;
import com.ifttt.IftttApiClient;
import com.ifttt.R;
import com.ifttt.Service;
import com.ifttt.api.PendingResult.ResultCallback;
import javax.annotation.Nullable;

import static android.graphics.Color.BLACK;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.lifecycle.Lifecycle.State.CREATED;
import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.lifecycle.Lifecycle.State.STARTED;
import static com.ifttt.Applet.Status.enabled;
import static com.ifttt.ui.ButtonUiHelper.TextTransitionType.Appear;
import static com.ifttt.ui.ButtonUiHelper.TextTransitionType.Change;
import static com.ifttt.ui.ButtonUiHelper.buildButtonBackground;
import static com.ifttt.ui.ButtonUiHelper.buildStateListButtonBackground;
import static com.ifttt.ui.ButtonUiHelper.getDarkerColor;
import static com.ifttt.ui.ButtonUiHelper.getTextTransitionAnimator;
import static com.ifttt.ui.ButtonUiHelper.replaceKeyWithImage;
import static com.ifttt.ui.ButtonUiHelper.setProgressBackgroundColor;
import static com.ifttt.ui.ButtonUiHelper.setProgressBackgroundProgress;
import static com.ifttt.ui.ButtonUiHelper.setTextSwitcherText;
import static com.ifttt.ui.IftttConnectButton.ButtonState.CreateAccount;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Enabled;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Initial;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Login;
import static com.ifttt.ui.IftttConnectButton.ButtonState.ServiceConnection;

/**
 *
 */
public final class IftttConnectButton extends LinearLayout implements LifecycleOwner {

    public enum ButtonState {
        /**
         * A button state for displaying an Applet in its initial state, the user has never authenticated this Applet
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
        ServiceConnection,

        /**
         * A button state for displaying an Applet that is enabled.
         */
        Enabled
    }

    private static final ErrorResponse INVALID_EMAIL = new ErrorResponse("invalid_email", "Invalid email address");
    private static final ErrorResponse CANCELED_AUTH = new ErrorResponse("canceled", "Authentication canceled");
    private static final ErrorResponse UNKNOWN_STATE = new ErrorResponse("unknown_state", "Cannot verify Button state");

    // Max length for the main text in the button. If the text plus service name is longer than this, we will only
    // render the text.
    private static final int MAX_LENGTH = 25;

    private static final float FADE_OUT_PROGRESS = 0.3f;
    private static final float FADE_IN_PROGRESS = 0.6f;

    // Start delay for moving icon animator to coordinate check mark animation.
    private static final long ICON_MOVEMENT_START_DELAY = 3400L;

    private static final long ANIM_DURATION_MEDIUM = 700L;
    private static final long ANIM_DURATION_LONG = 1500L;
    private static final LinearInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final FastOutSlowInInterpolator EASE_INTERPOLATOR = new FastOutSlowInInterpolator();

    private static final String AVENIR_DEMI = "avenir_next_ltpro_demi.otf";
    private static final String AVENIR_BOLD = "avenir_next_ltpro_bold.otf";

    private static final ArgbEvaluator EVALUATOR = new ArgbEvaluator();

    private final ResultCallback<Applet> appletResultCallback = new ResultCallback<Applet>() {
        @Override
        public void onSuccess(Applet result) {
            String connectText = getResources().getString(R.string.ifttt_connect_to, worksWithService.name);
            if (connectText.length() > MAX_LENGTH) {
                connectText = getResources().getString(R.string.ifttt_connect);
            }

            setTextSwitcherText(helperTxt, poweredByIfttt);

            Animator connectTextReset = getTextTransitionAnimator(connectStateTxt, Change, connectText);
            connectTextReset.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setApplet(result);
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
    private final CharSequence manageApplets;
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
    private Applet applet;
    private Service worksWithService;

    @Nullable private ButtonStateChangeListener buttonStateChangeListener;
    @Nullable private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;
    private ButtonApiHelper buttonApiHelper;

    // Toggle drag events.
    private ViewDragHelper viewDragHelper;
    private IconDragHelperCallback iconDragHelperCallback;

    private boolean onDarkBackground = false;

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

        Typeface demiTypeface = Typeface.createFromAsset(context.getAssets(), AVENIR_DEMI);
        Typeface boldTypeface = Typeface.createFromAsset(context.getAssets(), AVENIR_BOLD);

        emailEdt = findViewById(R.id.ifttt_email);
        emailEdt.setTypeface(demiTypeface);
        Drawable emailBackground = ButtonUiHelper.buildButtonBackground(context,
                ContextCompat.getColor(getContext(), R.color.ifttt_email_background_color));
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
            textView.setTypeface(demiTypeface);
            return textView;
        });
        helperTxt.setInAnimation(context, R.anim.ifttt_helper_text_in);
        helperTxt.setOutAnimation(context, R.anim.ifttt_helper_text_out);

        // Initialize SpannableString that replaces text with logo, using the current TextView in the TextSwitcher as
        // measurement, the CharSequence will only be used there.
        iftttLogo = ContextCompat.getDrawable(getContext(), R.drawable.ifttt_logo);
        poweredByIfttt = new SpannableString(replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                getResources().getString(R.string.ifttt_powered_by_ifttt), "IFTTT", iftttLogo));
        poweredByIfttt.setSpan(new AvenirTypefaceSpan(boldTypeface), 0, poweredByIfttt.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        manageApplets = replaceKeyWithImage((TextView) helperTxt.getCurrentView(),
                getResources().getString(R.string.ifttt_all_set), "IFTTT", iftttLogo);

        helperTxt.setOnClickListener(v -> getContext().startActivity(IftttAboutActivity.intent(context, applet)));

        buttonRoot = findViewById(R.id.ifttt_toggle_root);

        progressRoot = findViewById(R.id.ifttt_progress_container);
        if (SDK_INT >= KITKAT) {
            // Only use ProgressBackgroundDrawable on Android 19 or above.
            ProgressBackgroundDrawable progressRootBg = new ProgressBackgroundDrawable();
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
        return new SavedState(super.onSaveInstanceState(), buttonState, applet);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.superState);

        this.buttonState = savedState.buttonState;
        setApplet(savedState.applet);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    /**
     * Enable the Connect Button's Applet authentication and configuration features with an {@link IftttApiClient}
     * instance and a user email.
     *
     * Note:
     * - The redirect URI must be set for the {@link IftttApiClient} instance here.
     * - User email is a required parameter, the Button will crash if the value is not a valid email in DEBUG build.
     *
     * @param iftttApiClient IftttApiClient instance.
     * @param email This is used to pre-fill the email EditText when the user is doing Applet authentication.
     */
    public void setup(String email, IftttApiClient iftttApiClient, String redirectUrl,
            OAuthTokenProvider oAuthTokenProvider) {
        if (ButtonUiHelper.isEmailInvalid(email)) {
            // Crash in debug build to inform developers.
            throw new IllegalStateException(email
                    + " is not a valid email address, please make sure you pass in a valid user email string to set up IftttConnectButton.");
        }

        if (oAuthTokenProvider == null) {
            throw new IllegalStateException("OAuth token provider cannot be null.");
        }

        buttonApiHelper = new ButtonApiHelper(iftttApiClient, redirectUrl, oAuthTokenProvider, getLifecycle());
        isUserAuthenticated = iftttApiClient.isUserAuthenticated();
        emailEdt.setText(email);
    }

    /**
     * If the button is used in a dark background, set this flag to true so that the button can adapt the UI. This
     * method must be called before {@link #setApplet(Applet)} to apply the change.
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
     * Given an {@link AuthenticationResult} from web redirect, refresh the UI of the button to reflect the current
     * state of the Applet authentication flow.
     *
     * @param result Authentication flow redirect result from the web view.
     */
    public void setAuthenticationResult(AuthenticationResult result) {
        if (activityLifecycleCallbacks != null) {
            // Unregister existing ActivityLifecycleCallbacks and let the AuthenticationResult handle the button
            // state change.
            ((Activity) getContext()).getApplication().unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
            activityLifecycleCallbacks = null;
        }

        switch (result.nextStep) {
            case ServiceConnection:
                if (buttonState == Login) {
                    // If the previous state is Login, continue the progress to finish.
                    Animator completeEmailValidation = getCompleteEmailValidationAnimator();
                    completeEmailValidation.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Service nextService = findNextServiceToConnect(result);
                            if (nextService == null) {
                                recordError(new ErrorResponse("service_connection", "Failed connecting service"));
                                return;
                            }

                            worksWithService = nextService;

                            // Fade out progress bar.
                            ObjectAnimator fadeOutProgressRoot = ObjectAnimator.ofFloat(progressRoot, "alpha", 1f, 0f);
                            fadeOutProgressRoot.setInterpolator(EASE_INTERPOLATOR);

                            AnimatorSet set = new AnimatorSet();
                            set.playTogether(getServiceConnectionAnimator(worksWithService), fadeOutProgressRoot);
                            set.start();
                        }
                    });
                    completeEmailValidation.start();
                } else {
                    // If the previous state is service connection, finish it with a progress bar. Otherwise finish
                    // with only the check mark.
                    Animator animator = buttonState == ServiceConnection ? getServiceConnectingAnimator(
                            getResources().getString(R.string.ifttt_connecting_account)) : getCheckMarkAnimator();
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Service nextService = findNextServiceToConnect(result);
                            if (nextService == null || nextService == worksWithService) {
                                recordError(new ErrorResponse("service_connection",
                                        "Failed connecting service " + "" + worksWithService.id));
                                return;
                            }

                            worksWithService = nextService;
                            ObjectAnimator fadeOutProgressRoot = ObjectAnimator.ofFloat(progressRoot, "alpha", 1f, 0f);
                            fadeOutProgressRoot.setInterpolator(EASE_INTERPOLATOR);
                            Animator serviceConnectionAnimator = getServiceConnectionAnimator(worksWithService);
                            AnimatorSet set = new AnimatorSet();
                            set.playTogether(serviceConnectionAnimator, fadeOutProgressRoot);
                            set.start();
                        }
                    });
                    animator.start();
                }

                recordState(ServiceConnection);
                break;
            case Complete:
                Animator authenticatedAnimator = getAuthenticatedAnimator();
                if (buttonState == Login) {
                    // If the previous state is Login, continue the progress to finish.
                    AnimatorSet set = new AnimatorSet();
                    authenticatedAnimator.setStartDelay(ICON_MOVEMENT_START_DELAY);
                    set.playTogether(getCompleteEmailValidationAnimator(), authenticatedAnimator);
                    set.start();
                } else {
                    authenticatedAnimator.start();
                }

                break;
            default:
                // The authentication result doesn't contain any next step instruction.
                recordError(UNKNOWN_STATE);
        }
    }

    /**
     * Render the Connect Button to show the status of the Applet.
     *
     * @param applet Applet instance to be displayed.
     */
    public void setApplet(Applet applet) {
        this.applet = applet;
        worksWithService = findWorksWithService(applet);

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
                if (applet.status == enabled) {
                    setTextSwitcherText(helperTxt, manageApplets);
                } else {
                    setTextSwitcherText(helperTxt, poweredByIfttt);
                }
            }
        });
        fadeInButtonRoot.start();

        final Context context = getContext();
        if (applet.status != Applet.Status.enabled) {
            recordState(Initial);

            String signInText = getResources().getString(R.string.ifttt_connect_to, worksWithService.name);
            if (signInText.length() > MAX_LENGTH) {
                connectStateTxt.setText(R.string.ifttt_connect);
            } else {
                connectStateTxt.setText(signInText);
            }

            helperTxt.setClickable(true);
            iconDragHelperCallback.setDragEnabled(false);
        } else {
            recordState(Enabled);

            // Remove the email validation click listener.
            iconImg.setOnClickListener(null);

            CharSequence enabledText = getResources().getText(R.string.ifttt_connected);
            CharSequence disabledText = getResources().getText(R.string.ifttt_applet_off);
            iconDragHelperCallback.setDragEnabled(true);
            iconDragHelperCallback.setTexts(enabledText, disabledText);

            float progress = 1f;
            setConnectTextState(progress, enabledText, disabledText);

            helperTxt.setOnClickListener(v -> buttonApiHelper.redirectToPlayStore(context));
        }

        ImageLoader.get().load(this, worksWithService.monochromeIconUrl, bitmap -> {
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
                }
            });

            fadeInIconImg.start();
        });

        // Warm up icon image cache.
        for (Service service : applet.services) {
            if (service.id.equals(worksWithService.id)) {
                continue;
            }

            ImageLoader.get().fetch(getLifecycle(), service.monochromeIconUrl);
        }

        // Move the icon to the right if the Applet has already been authenticated and enabled.
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) iconImg.getLayoutParams();
        lp.gravity = applet.status == enabled ? Gravity.END : Gravity.START;
        iconImg.setLayoutParams(lp);

        if (SDK_INT < KITKAT) {
            // On Jelly Bean, the click events on the button will only take users out to the browser.
            buttonRoot.setOnClickListener(v -> buttonApiHelper.redirectToWebCompat(context, applet));
        } else if (applet.status == enabled) {
            buttonRoot.setOnClickListener(
                    v -> setTextSwitcherText(helperTxt, getResources().getString(R.string.slide_to_turn_off)));
        } else {
            buttonRoot.setOnClickListener(v -> {
                if (!isUserAuthenticated) {
                    animateToEmailField();
                } else {
                    animateEmailValidation();
                }
            });
        }
    }

    private Animator getAuthenticatedAnimator() {
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

        ValueAnimator iconElevation =
                ValueAnimator.ofFloat(0f, getResources().getDimension(R.dimen.ifttt_icon_elevation));
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
        if (progressRoot.getAlpha() == 1f) {
            completeSet.playTogether(iconMovement, fadeOutProgressRoot);
        } else {
            Animator checkMarkAnimator =
                    getServiceConnectingAnimator(getResources().getString(R.string.ifttt_connecting_account));
            iconMovement.setStartDelay(ICON_MOVEMENT_START_DELAY);
            fadeOutProgressRoot.setStartDelay(ICON_MOVEMENT_START_DELAY);
            completeSet.playTogether(checkMarkAnimator, iconMovement, fadeOutProgressRoot);
        }

        completeSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                connectStateTxt.setAlpha(0f);
                connectStateTxt.setText(R.string.ifttt_connected);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                recordState(Enabled);

                setTextSwitcherText(helperTxt, manageApplets);
                helperTxt.setOnClickListener(v -> buttonApiHelper.redirectToPlayStore(getContext()));

                // After the applet has been authenticated, temporarily disable toggling feature until the new Applet
                // object has been set.
                if (applet.status != enabled) {
                    buttonRoot.setClickable(false);
                    iconImg.setClickable(false);
                }
            }
        });

        return completeSet;
    }

    private void animateEmailValidation() {
        setTextSwitcherText(helperTxt, poweredByIfttt);

        if (isUserAuthenticated) {
            progressTxt.setText(
                    replaceKeyWithImage(connectStateTxt, getResources().getString(R.string.ifttt_signing_in_to_ifttt),
                            "IFTTT", ContextCompat.getDrawable(getContext(), R.drawable.ifttt_logo_white)));
        } else {
            progressTxt.setText(R.string.ifttt_validating_email);
        }

        Animator showProgressText = getTextTransitionAnimator(progressTxt, Appear, null);

        ValueAnimator showProgress = ValueAnimator.ofFloat(0f, 0.5f).setDuration(ANIM_DURATION_LONG);
        showProgress.setInterpolator(LINEAR_INTERPOLATOR);
        showProgress.addUpdateListener(animation -> setProgressBackgroundProgress(progressRoot.getBackground(),
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
                if (!buttonApiHelper.isAccountFound()) {
                    recordState(CreateAccount);
                    AnimatorSet createAccountCompleteSet = new AnimatorSet();

                    // Complete email validation progress bar.
                    Animator completeEmailValidationAnimator = getCompleteEmailValidationAnimator();

                    // Fade out progress bar.
                    ObjectAnimator fadeOutProgressRoot = ObjectAnimator.ofFloat(progressRoot, "alpha", 1f, 0f);
                    fadeOutProgressRoot.setInterpolator(EASE_INTERPOLATOR);

                    // Group changing progress text and completing progress bar animation together.
                    createAccountCompleteSet.playTogether(getTextTransitionAnimator(progressTxt, Change,
                            getResources().getString(R.string.ifttt_creating_account)),
                            completeEmailValidationAnimator);

                    // Group fading out progress bar and starting service connection state animation together.
                    createAccountCompleteSet.playTogether(fadeOutProgressRoot,
                            getServiceConnectionAnimator(worksWithService));

                    // Play fading out progress bar and its bundled animations after the progress bar has been filled.
                    createAccountCompleteSet.playSequentially(completeEmailValidationAnimator, fadeOutProgressRoot);
                    createAccountCompleteSet.start();
                } else {
                    recordState(Login);
                    buttonApiHelper.redirectToWeb(getContext(), applet, emailEdt.getText().toString(), buttonState);
                    monitorRedirect();
                }
            }
        });

        set.start();
    }

    private Animator getCompleteEmailValidationAnimator() {
        ValueAnimator.AnimatorUpdateListener updateListener =
                animation -> setProgressBackgroundProgress(progressRoot.getBackground(),
                        (float) animation.getAnimatedValue());

        ValueAnimator complete = ValueAnimator.ofFloat(0.5f, 1f).setDuration(ANIM_DURATION_LONG);
        complete.setInterpolator(LINEAR_INTERPOLATOR);
        complete.addUpdateListener(updateListener);

        ObjectAnimator fadeOutProgressText = ObjectAnimator.ofFloat(progressTxt, "alpha", 1f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(complete, fadeOutProgressText,
                ((CheckMarkDrawable) completeImg.getDrawable()).getAnimator());

        return set;
    }

    /**
     * Start the animation for Applet authentication.
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
                emailEdt.setAlpha(0f);
                emailEdt.setVisibility(VISIBLE);
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeOutConnect, fadeInEmailEdit, fadeOutConnect, slideIcon);

        // Morph service icon into the start button.
        if (iconImg.getBackground() != null) {
            Animator iconMorphing = ((StartIconDrawable) iconImg.getBackground()).getAnimator();
            iconMorphing.setDuration(ANIM_DURATION_MEDIUM);
            set.playTogether(iconMorphing, fadeOutConnect);
        }

        set.setInterpolator(EASE_INTERPOLATOR);

        OnClickListener startAuthOnClickListener = v -> {
            if (ButtonUiHelper.isEmailInvalid(emailEdt.getText())) {
                if (buttonStateChangeListener != null) {
                    buttonStateChangeListener.onError(INVALID_EMAIL);
                }

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
    private Animator getServiceConnectionAnimator(Service service) {
        buttonRoot.setBackground(buildButtonBackground(getContext(), service.brandColor));
        String connectText = getResources().getString(R.string.ifttt_sign_in_to, service.name);
        if (connectText.length() > MAX_LENGTH) {
            connectStateTxt.setText(R.string.ifttt_sign_in);
        } else {
            connectStateTxt.setText(connectText);
        }
        buttonRoot.setOnClickListener(v -> {
            buttonApiHelper.redirectToWeb(getContext(), applet, emailEdt.getText().toString(), buttonState);
            monitorRedirect();
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
                                applet.getPrimaryService().name));
            }
        });

        helperTxt.setClickable(false);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(slideInConnectText, fadeInConnectText);
        return set;
    }

    private Animator getCheckMarkAnimator() {
        CheckMarkDrawable drawable = (CheckMarkDrawable) completeImg.getDrawable();
        Animator checkMarkAnimator = drawable.getAnimator();

        ObjectAnimator fadeInProgressContainer = ObjectAnimator.ofFloat(progressRoot, "alpha", 0f, 1f);
        fadeInProgressContainer.setInterpolator(EASE_INTERPOLATOR);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(fadeInProgressContainer, checkMarkAnimator);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                setProgressBackgroundColor(progressRoot.getBackground(), worksWithService.brandColor,
                        worksWithService.brandColor);
            }
        });
        return set;
    }

    /**
     * Get an animator set that's used to run a "proceed and complete" animation, including a progress bar and a check
     * mark animation.
     *
     * @param text Text to be shown while the progress animation is running.
     * @return the Animator object that has timing and interpolator set up.
     */
    private Animator getServiceConnectingAnimator(CharSequence text) {
        ObjectAnimator fadeInIconImg = ObjectAnimator.ofFloat(progressIconImg, "alpha", 0f, 1f);
        fadeInIconImg.setInterpolator(EASE_INTERPOLATOR);

        ObjectAnimator fadeInProgressContainer = ObjectAnimator.ofFloat(progressRoot, "alpha", 0f, 1f);
        fadeInProgressContainer.setInterpolator(EASE_INTERPOLATOR);
        fadeInProgressContainer.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                progressTxt.setText(text);
                progressTxt.setAlpha(1f);
            }
        });

        ValueAnimator completeProgress = ValueAnimator.ofFloat(0f, 1f);
        completeProgress.addUpdateListener(animation -> setProgressBackgroundProgress(progressRoot.getBackground(),
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
                setProgressBackgroundColor(progressRoot.getBackground(), worksWithService.brandColor,
                        getDarkerColor(worksWithService.brandColor));
                ImageLoader.get().load(IftttConnectButton.this, worksWithService.monochromeIconUrl, bitmap -> {
                    if (bitmap != null) {
                        progressIconImg.setImageBitmap(bitmap);
                    }
                });
            }
        });

        return set;
    }

    private Service findWorksWithService(Applet applet) {
        Service otherService = null;
        for (Service service : applet.services) {
            if (!service.isPrimary) {
                otherService = service;
                break;
            }
        }

        if (otherService == null) {
            throw new IllegalStateException("There is no primary service for this Applet.");
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
        setApplet(applet);
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
                    recordError(CANCELED_AUTH);
                    activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                    activityLifecycleCallbacks = null;
                }
            }
        };
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    @Nullable
    private Service findNextServiceToConnect(AuthenticationResult result) {
        // Find the next service to connect.
        Service nextService = null;
        for (Service service : applet.services) {
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
                            buttonApiHelper.disableApplet(applet.id, appletResultCallback);
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
                            setTextSwitcherText(helperTxt, manageApplets);
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
        final Applet applet;

        SavedState(@Nullable Parcelable superState, ButtonState buttonState, Applet applet) {
            this.superState = superState;
            this.buttonState = buttonState;
            this.applet = applet;
        }

        protected SavedState(Parcel in) {
            superState = in.readParcelable(IftttConnectButton.class.getClassLoader());
            buttonState = (ButtonState) in.readSerializable();
            applet = in.readParcelable(Applet.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(superState, flags);
            dest.writeSerializable(buttonState);
            dest.writeParcelable(applet, flags);
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
