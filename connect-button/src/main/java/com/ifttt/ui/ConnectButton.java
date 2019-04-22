package com.ifttt.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import com.ifttt.ConnectionApiClient;
import com.ifttt.ErrorResponse;
import com.ifttt.R;
import com.ifttt.api.PendingResult;

import static android.animation.ValueAnimator.INFINITE;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;
import static com.ifttt.ui.ButtonUiHelper.replaceKeyWithImage;

/**
 *
 */
public class ConnectButton extends FrameLayout implements LifecycleOwner {

    private static final long ANIM_DURATION = 1000L;
    private static ConnectionApiClient API_CLIENT;

    private final IftttConnectButton connectButton;
    private final TextView loadingView;
    private final TextSwitcher statusTextSwitcher;

    private CredentialsProvider credentialsProvider;
    private Connection connection;

    private final CharSequence worksWithIfttt;
    private final Drawable iftttLogo;

    private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    public ConnectButton(@NonNull Context context) {
        this(context, null);
    }

    public ConnectButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConnectButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
     * @param configuration Configuration object that helps set up the Connect Button.
     */
    public void setup(Configuration configuration) {
        ConnectionApiClient clientToUse;
        if (configuration.connectionApiClient == null) {
            if (API_CLIENT == null) {
                API_CLIENT = new ConnectionApiClient.Builder(getContext()).build();
            }

            clientToUse = API_CLIENT;
        } else {
            clientToUse = configuration.connectionApiClient;
        }

        credentialsProvider = configuration.credentialsProvider;
        connectButton.setup(configuration.suggestedUserEmail, clientToUse, configuration.connectRedirectUri,
                configuration.credentialsProvider);
        connectButton.resetFooterText();

        pulseLoading();
        UserTokenAsyncTask task = new UserTokenAsyncTask(credentialsProvider, () -> {
            if (configuration.connection != null) {
                connection = configuration.connection;
                if (configuration.listener != null) {
                    configuration.listener.onFetchConnectionSuccessful(connection);
                }

                connectButton.setConnection(connection);
                loadingView.setVisibility(GONE);
                statusTextSwitcher.setVisibility(GONE);
                ((Animator) loadingView.getTag()).cancel();
                return;
            }

            if (configuration.connectionId == null) {
                throw new IllegalStateException("Connection id cannot be null.");
            }

            PendingResult<Connection> pendingResult = API_CLIENT.api().showConnection(configuration.connectionId);
            pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
                @Override
                public void onSuccess(Connection result) {
                    connection = result;
                    if (configuration.listener != null) {
                        configuration.listener.onFetchConnectionSuccessful(result);
                    }

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
                        PendingResult<Connection> pendingResult =
                                API_CLIENT.api().showConnection(configuration.connectionId);
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
        if (connection == null || credentialsProvider == null) {
            return;
        }

        connectButton.setConnectResult(result);

        if (result.nextStep == ConnectResult.NextStep.Complete) {
            if (result.userToken != null) {
                API_CLIENT.setUserToken(result.userToken);
                refreshConnection();
            } else {
                UserTokenAsyncTask task = new UserTokenAsyncTask(credentialsProvider, this::refreshConnection);
                task.execute();
                lifecycleRegistry.addObserver(new AsyncTaskObserver(task));
            }
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

    private void refreshConnection() {
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
    public interface OnFetchConnectionListener {
        /**
         * Called when the request to fetch a {@link Connection} data is successful.
         *
         * @param connection Connection data object associated with the Connection ID passed to the View.
         */
        void onFetchConnectionSuccessful(Connection connection);
    }

    public static final class Configuration {

        private final String suggestedUserEmail;
        private final CredentialsProvider credentialsProvider;
        private final Uri connectRedirectUri;

        @Nullable private final ConnectionApiClient connectionApiClient;
        @Nullable private String connectionId;
        @Nullable private Connection connection;
        @Nullable private OnFetchConnectionListener listener;

        public static final class Builder {
            private final String suggestedUserEmail;
            private final CredentialsProvider credentialsProvider;
            private final Uri connectRedirectUri;
            @Nullable private ConnectionApiClient connectionApiClient;

            @Nullable private String connectionId;
            @Nullable private OnFetchConnectionListener listener;
            @Nullable private Connection connection;

            public static Builder withConnection(Connection connection, String suggestedUserEmail,
                    CredentialsProvider credentialsProvider, Uri connectRedirectUri) {
                if (ButtonUiHelper.isEmailInvalid(suggestedUserEmail)) {
                    throw new IllegalStateException(suggestedUserEmail + " is not a valid email address.");
                }

                Builder builder = new Builder(suggestedUserEmail, credentialsProvider, connectRedirectUri);
                builder.connection = connection;
                return builder;
            }

            public static Builder withConnectionId(String connectionId, String suggestedUserEmail,
                    CredentialsProvider credentialsProvider, Uri connectRedirectUri,
                    OnFetchConnectionListener onFetchConnectionListener) {
                if (ButtonUiHelper.isEmailInvalid(suggestedUserEmail)) {
                    throw new IllegalStateException(suggestedUserEmail + " is not a valid email address.");
                }

                Builder builder = new Builder(suggestedUserEmail, credentialsProvider, connectRedirectUri);
                builder.connectionId = connectionId;
                builder.listener = onFetchConnectionListener;
                return builder;
            }

            private Builder(String suggestedUserEmail, CredentialsProvider credentialsProvider,
                    Uri connectRedirectUri) {
                this.suggestedUserEmail = suggestedUserEmail;
                this.credentialsProvider = credentialsProvider;
                this.connectRedirectUri = connectRedirectUri;
            }

            public Builder setConnectionApiClient(ConnectionApiClient connectionApiClient) {
                this.connectionApiClient = connectionApiClient;
                return this;
            }

            public Configuration build() {
                if (connection == null && connectionId == null) {
                    throw new IllegalStateException("Either connection or connectionId must be non-null.");
                }

                Configuration configuration =
                        new Configuration(suggestedUserEmail, credentialsProvider, connectRedirectUri,
                                connectionApiClient);
                configuration.connection = connection;
                configuration.connectionId = connectionId;
                configuration.listener = listener;
                return configuration;
            }
        }

        private Configuration(String suggestedUserEmail, CredentialsProvider credentialsProvider,
                Uri connectRedirectUri, @Nullable ConnectionApiClient connectionApiClient) {
            this.suggestedUserEmail = suggestedUserEmail;
            this.credentialsProvider = credentialsProvider;
            this.connectRedirectUri = connectRedirectUri;
            this.connectionApiClient = connectionApiClient;
        }
    }

    private static final class UserTokenAsyncTask extends android.os.AsyncTask<Void, Void, String> {

        private interface UserTokenCallback {
            void onUserTokenSet();
        }

        private final CredentialsProvider callback;
        private final UserTokenCallback userTokenCallback;

        private UserTokenAsyncTask(CredentialsProvider callback, UserTokenCallback userTokenCallback) {
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
