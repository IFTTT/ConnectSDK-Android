package com.ifttt.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.OnLifecycleEvent;
import com.ifttt.Connection;
import com.ifttt.ErrorResponse;
import com.ifttt.IftttApiClient;
import com.ifttt.R;
import com.ifttt.api.PendingResult;

import static android.animation.ValueAnimator.INFINITE;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;
import static com.ifttt.ui.ButtonUiHelper.replaceKeyWithImage;

/**
 * A wrapper of a {@link IftttConnectButton} that provides some default configurations. Users of this class only need to
 * provide a {@link Callback} instance, as well as the Connection id, and this view will handle both the loading state and
 * other UI states accordingly.
 */
public final class SimpleConnectButton extends FrameLayout implements LifecycleOwner {

    private static final long ANIM_DURATION = 1000L;
    private static IftttApiClient API_CLIENT;

    private final IftttConnectButton connectButton;
    private final TextView loadingView;
    private final TextSwitcher statusTextSwitcher;

    private Callback callback;
    private Connection connection;

    private final CharSequence worksWithIfttt;
    private final Drawable iftttLogo;

    private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    public SimpleConnectButton(@NonNull Context context) {
        this(context, null);
    }

    public SimpleConnectButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleConnectButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        lifecycleRegistry.markState(Lifecycle.State.CREATED);

        inflate(context, R.layout.view_ifttt_simple_connect_button, this);
        connectButton = findViewById(R.id.ifttt_connect_button);
        loadingView = findViewById(R.id.ifttt_loading_view);
        statusTextSwitcher = findViewById(R.id.ifttt_status_text);

        iftttLogo = ContextCompat.getDrawable(getContext(), R.drawable.ic_ifttt_logo_black).mutate();
        iftttLogo.setAlpha((int) (0.3f * 255));
        worksWithIfttt = new SpannableString(replaceKeyWithImage((TextView) statusTextSwitcher.getCurrentView(),
                context.getString(R.string.ifttt_powered_by_ifttt), "IFTTT", iftttLogo));
    }

    /**
     * Set up the Connect Button to fetch the Connection data with the given id and set up the View to be able to do
     * authentication.
     *
     * @param connectionId ID of the Connection that you want to show on this View.
     * @param email This is used to pre-fill the email EditText when the user is doing Connection authentication.
     * @param ownerServiceId The id of the service that this Connect Button is used for. To ensure the Connection flow
     * works with your IFTTT user token, you should make sure the Connection that you are embedding is owned by your
     * service.
     * @param redirectUri string that will be used when the Connection authentication flow is completed on web view, in
     * * order to return the result to the app.
     * @param callback A callback that supports Connection authentication as well as provide Connection data when the
     * fetch is successful.
     */
    public void setup(String connectionId, String email, String ownerServiceId, String redirectUri, Callback callback) {
        this.callback = callback;

        if (API_CLIENT == null) {
            API_CLIENT = new IftttApiClient.Builder(getContext()).build();
        }

        connectButton.setup(email, ownerServiceId, API_CLIENT, redirectUri, callback::getOAuthCode);
        connectButton.resetFooterText();

        pulseLoading();
        UserTokenAsyncTask task = new UserTokenAsyncTask(callback, () -> {
            PendingResult<Connection> pendingResult = API_CLIENT.api().showConnection(connectionId);
            pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
                @Override
                public void onSuccess(Connection result) {
                    connection = result;
                    callback.onFetchConnectionSuccessful(result);

                    connectButton.setConnection(result);
                    loadingView.setVisibility(GONE);
                    statusTextSwitcher.setVisibility(GONE);
                    ((Animator) loadingView.getTag()).cancel();
                }

                @Override
                public void onFailure(ErrorResponse errorResponse) {
                    CharSequence errorText =
                            HtmlCompat.fromHtml(getResources().getString(R.string.ifttt_error_fetching_connection),
                                    FROM_HTML_MODE_COMPACT);
                    SpannableString errorSpan = new SpannableString(errorText);
                    errorSpan.setSpan(
                            new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.ifttt_error_red)), 0,
                            errorText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    statusTextSwitcher.setText(errorSpan);
                    statusTextSwitcher.setOnClickListener(v -> {
                        PendingResult<Connection> pendingResult = API_CLIENT.api().showConnection(connectionId);
                        pendingResult.execute(this);
                        lifecycleRegistry.addObserver(new PendingResultLifecycleObserver<>(pendingResult));

                        statusTextSwitcher.setClickable(false);
                        statusTextSwitcher.setText(worksWithIfttt);
                    });
                }
            });

            lifecycleRegistry.addObserver(new PendingResultLifecycleObserver<>(pendingResult));
        });
        task.execute();
        lifecycleRegistry.addObserver(new AsyncTaskObserver(task));

        statusTextSwitcher.setCurrentText(worksWithIfttt);
    }

    /**
     * If the button is used in a dark background, set this flag to true so that the button can adapt the UI. This
     * method must be called before {@link IftttConnectButton#setConnection(Connection)} to apply the change.
     *
     * @param onDarkBackground True if the button is used in a dark background, false otherwise.
     */
    public void setOnDarkBackground(boolean onDarkBackground) {
        if (onDarkBackground) {
            loadingView.setBackgroundResource(R.drawable.ifttt_loading_background_dark);
        } else {
            loadingView.setBackgroundResource(R.drawable.button_background_default);
        }
        connectButton.setOnDarkBackground(onDarkBackground);

        TextView currentHelperTextView = (TextView) statusTextSwitcher.getCurrentView();
        TextView nextHelperTextView = (TextView) statusTextSwitcher.getNextView();

        if (onDarkBackground) {
            int semiTransparentWhite = ContextCompat.getColor(getContext(), R.color.ifttt_footer_text_white);
            currentHelperTextView.setTextColor(semiTransparentWhite);
            nextHelperTextView.setTextColor(semiTransparentWhite);

            // Tint the logo Drawable within the text to white.
            DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), Color.WHITE);
        } else {
            int semiTransparentBlack = ContextCompat.getColor(getContext(), R.color.ifttt_footer_text_black);
            currentHelperTextView.setTextColor(semiTransparentBlack);
            nextHelperTextView.setTextColor(semiTransparentBlack);

            // Tint the logo Drawable within the text to black.
            DrawableCompat.setTint(DrawableCompat.wrap(iftttLogo), Color.BLACK);
        }
    }

    /**
     * Set a listener to be notified when the button's state has changed.
     */
    public void setButtonStateChangeListener(ButtonStateChangeListener listener) {
        connectButton.setButtonStateChangeListener(listener);
    }

    /**
     * Given an {@link ConnectResult} from web redirect, refresh the UI of the button to reflect the current
     * state of the Connection authentication flow.
     *
     * @param result Authentication flow redirect result from the web view.
     */
    public void setConnectResult(ConnectResult result) {
        if (connection == null || callback == null) {
            return;
        }

        connectButton.setConnectResult(result);

        if (result.nextStep == ConnectResult.NextStep.Complete) {
            UserTokenAsyncTask task = new UserTokenAsyncTask(callback, () -> {
                PendingResult<Connection> pendingResult = API_CLIENT.api().showConnection(connection.id);
                pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
                    @Override
                    public void onSuccess(Connection result1) {
                        connectButton.setConnection(result1);
                    }

                    @Override
                    public void onFailure(ErrorResponse errorResponse) {
                        connectButton.setConnection(connection);
                    }
                });

                lifecycleRegistry.addObserver(new PendingResultLifecycleObserver<>(pendingResult));
            });

            task.execute();
            lifecycleRegistry.addObserver(new AsyncTaskObserver(task));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lifecycleRegistry.markState(Lifecycle.State.STARTED);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    private void pulseLoading() {
        ValueAnimator animator = ValueAnimator.ofInt(255, 200);
        animator.addUpdateListener(
                animation -> loadingView.setTextColor(Color.argb((int) animation.getAnimatedValue(), 255, 255, 255)));
        animator.setRepeatCount(INFINITE);
        animator.setDuration(ANIM_DURATION);
        animator.start();

        loadingView.setTag(animator);
    }

    /**
     * Configuration interface for this class. It provides all of the necessary information for the
     * {@link IftttConnectButton} to render UI, handle different states and perform API calls.
     */
    public interface Callback {
        /**
         * @return An IFTTT user token for this user. This token is going to be used to set up the {@link IftttApiClient}
         * to fetch Connection data with regard to this user.
         */
        @Nullable
        String getUserToken();

        /**
         * @return Your service's OAuth code. The OAuth code will be used to automatically authenticate user to your
         * service on IFTTT platform.
         * @see IftttConnectButton#setup(String, String, IftttApiClient, String, OAuthCodeProvider).
         */
        String getOAuthCode();

        /**
         * Called when the request to fetch a {@link Connection} data is successful.
         *
         * @param connection Connection data object associated with the Connection ID passed to the View.
         */
        void onFetchConnectionSuccessful(Connection connection);
    }

    private static final class UserTokenAsyncTask extends android.os.AsyncTask<Void, Void, String> {

        private interface UserTokenCallback {
            void onUserTokenSet();
        }

        private final SimpleConnectButton.Callback callback;
        private final UserTokenCallback userTokenCallback;

        private UserTokenAsyncTask(SimpleConnectButton.Callback callback, UserTokenCallback userTokenCallback) {
            this.callback = callback;
            this.userTokenCallback = userTokenCallback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return callback.getUserToken();
        }

        @Override
        protected void onPostExecute(String s) {
            API_CLIENT.setUserToken(s);
            userTokenCallback.onUserTokenSet();
        }
    }

    private final class AsyncTaskObserver implements LifecycleObserver {

        private final AsyncTask task;

        private AsyncTaskObserver(AsyncTask task) {
            this.task = task;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        void onStop() {
            task.cancel(true);
        }
    }
}
