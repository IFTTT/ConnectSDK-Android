package com.ifttt.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

/**
 * The main UI component for the Connect Button SDK. This class handles both displaying {@link Connection} status for a
 * given user, as well as initiating a Connection enable flow.
 */
public class ConnectButton extends FrameLayout implements LifecycleOwner {

    private static final long ANIM_DURATION = 1000L;
    private static ConnectionApiClient API_CLIENT;

    private final BaseConnectButton connectButton;
    private final TextView loadingView;

    private CredentialsProvider credentialsProvider;
    private Connection connection;

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
                ConnectionApiClient.Builder clientBuilder = new ConnectionApiClient.Builder(getContext());
                if (configuration.inviteCode != null) {
                    clientBuilder.setInviteCode(configuration.inviteCode);
                }
                API_CLIENT = clientBuilder.build();
            }

            clientToUse = API_CLIENT;
        } else {
            clientToUse = configuration.connectionApiClient;
        }

        credentialsProvider = configuration.credentialsProvider;
        connectButton.setup(configuration.suggestedUserEmail, clientToUse, configuration.connectRedirectUri,
                configuration.credentialsProvider, configuration.inviteCode);

        pulseLoading();
        UserTokenAsyncTask task = new UserTokenAsyncTask(credentialsProvider, () -> {
            if (configuration.connection != null) {
                connection = configuration.connection;
                if (configuration.listener != null) {
                    configuration.listener.onFetchConnectionSuccessful(connection);
                }

                connectButton.setConnection(connection);
                loadingView.setVisibility(GONE);
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

                    connectButton.setErrorMessage(errorSpan, v -> {
                        PendingResult<Connection> pendingResult =
                                API_CLIENT.api().showConnection(configuration.connectionId);
                        pendingResult.execute(this);
                        lifecycleRegistry.addObserver(new PendingResultLifecycleObserver<>(pendingResult));
                    });
                }
            });

            lifecycleRegistry.addObserver(new PendingResultLifecycleObserver<>(pendingResult));
        });
        task.execute();
        lifecycleRegistry.addObserver(new AsyncTaskObserver(task));
    }

    /**
     * If the button is used in a dark background, set this flag to true so that the button can adapt the UI. This
     * method must be called before {@link BaseConnectButton#setConnection(Connection)} to apply the change.
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
     * {@link BaseConnectButton} to render UI, handle different states and perform API calls.
     */
    public interface OnFetchConnectionListener {
        /**
         * Called when the request to fetch a {@link Connection} data is successful.
         *
         * @param connection Connection data object associated with the Connection ID passed to the View.
         */
        void onFetchConnectionSuccessful(Connection connection);
    }

    /**
     * Configuration for a {@link ConnectButton}, it encapsulates the information needed to set up a ConnectButton
     * instance, to enable it to
     * - display Connection status, and
     * - initiate Connection enable flow.
     */
    public static final class Configuration {

        private final String suggestedUserEmail;
        private final CredentialsProvider credentialsProvider;
        private final Uri connectRedirectUri;

        @Nullable private final ConnectionApiClient connectionApiClient;
        @Nullable private String connectionId;
        @Nullable private Connection connection;
        @Nullable private OnFetchConnectionListener listener;
        @Nullable private String inviteCode;

        /**
         * Builder class for constructing a Configuration object.
         */
        public static final class Builder {
            private final String suggestedUserEmail;
            private final CredentialsProvider credentialsProvider;
            private final Uri connectRedirectUri;
            @Nullable private ConnectionApiClient connectionApiClient;

            @Nullable private String connectionId;
            @Nullable private OnFetchConnectionListener listener;
            @Nullable private Connection connection;
            @Nullable private String inviteCode;

            /**
             * Factory method for creating a new Configuration builder.
             *
             * @param connection    {@link Connection} object.
             * @param suggestedUserEmail    Email address string provided as the suggested email for the user. Must be a
             * valid email address.
             * @param credentialsProvider   {@link CredentialsProvider} object that helps facilitate connection enable
             * flow from a ConnectButton.
             * @param connectRedirectUri    Redirect {@link Uri} object that the ConnectButton is going to use to
             * redirect users back to your app after the connection enable flow is completed or failed.
             *
             * @return The Builder object itself for chaining.
             */
            public static Builder withConnection(Connection connection, String suggestedUserEmail,
                    CredentialsProvider credentialsProvider, Uri connectRedirectUri) {
                if (ButtonUiHelper.isEmailInvalid(suggestedUserEmail)) {
                    throw new IllegalStateException(suggestedUserEmail + " is not a valid email address.");
                }

                Builder builder = new Builder(suggestedUserEmail, credentialsProvider, connectRedirectUri);
                builder.connection = connection;
                return builder;
            }

            /**
             * Factory method for creating a new Configuration builder.
             *
             * @param connectionId    A Connection id that the {@link ConnectionApiClient} can use to fetch the
             * associated Connection object.
             * @param suggestedUserEmail    Email address string provided as the suggested email for the user. Must be a
             * valid email address.
             * @param credentialsProvider   {@link CredentialsProvider} object that helps facilitate connection enable
             * flow from a ConnectButton.
             * @param connectRedirectUri    Redirect {@link Uri} object that the ConnectButton is going to use to
             * redirect users back to your app after the connection enable flow is completed or failed.
             *
             * @return The Builder object itself for chaining.
             */
            public static Builder withConnectionId(String connectionId, String suggestedUserEmail,
                    CredentialsProvider credentialsProvider, Uri connectRedirectUri) {
                if (ButtonUiHelper.isEmailInvalid(suggestedUserEmail)) {
                    throw new IllegalStateException(suggestedUserEmail + " is not a valid email address.");
                }

                Builder builder = new Builder(suggestedUserEmail, credentialsProvider, connectRedirectUri);
                builder.connectionId = connectionId;
                return builder;
            }

            private Builder(String suggestedUserEmail, CredentialsProvider credentialsProvider,
                    Uri connectRedirectUri) {
                this.suggestedUserEmail = suggestedUserEmail;
                this.credentialsProvider = credentialsProvider;
                this.connectRedirectUri = connectRedirectUri;
            }

            /**
             * @param onFetchCompleteListener an optional {@link OnFetchConnectionListener}.
             *
             * Note that this callback will not be invoked if the Configuration is built through
             * {@link #withConnection(Connection, String, CredentialsProvider, Uri)}.
             *
             * @return The Builder object itself for chaining.
             */
            public Builder setOnFetchCompleteListener(OnFetchConnectionListener onFetchCompleteListener) {
                this.listener = onFetchCompleteListener;
                return this;
            }

            /**
             * @param connectionApiClient an optional {@link ConnectionApiClient} that will be used for the ConnectButton
             * instead of the default one.
             *
             * @return The Builder object itself for chaining.
             */
            public Builder setConnectionApiClient(ConnectionApiClient connectionApiClient) {
                this.connectionApiClient = connectionApiClient;
                return this;
            }

            /**
             * @param inviteCode an optional string as the invite code, this is needed if your service is not yet
             * published on IFTTT Platform.
             *
             * @see ConnectionApiClient.Builder#setInviteCode(String)
             *
             * @return The Builder object itself for chaining.
             */
            public Builder setInviteCode(String inviteCode) {
                this.inviteCode = inviteCode;
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
                configuration.inviteCode = inviteCode;
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
