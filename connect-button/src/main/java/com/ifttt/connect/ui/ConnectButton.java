package com.ifttt.connect.ui;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.OnLifecycleEvent;
import com.ifttt.connect.BuildConfig;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.CredentialsProvider;
import com.ifttt.connect.ErrorResponse;
import com.ifttt.connect.Feature;
import com.ifttt.connect.R;
import com.ifttt.connect.UserTokenAsyncTask;
import com.ifttt.connect.api.PendingResult;
import java.util.List;

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
    private onConnectionStateChangeListener onConnectionStateChangeListener = null;

    private CredentialsProvider credentialsProvider;

    private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    public ConnectButton(@NonNull Context context) {
        this(context, null);
    }

    public ConnectButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConnectButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setClipToPadding(false);
        setClipChildren(false);
        setLayoutTransition(new LayoutTransition());

        lifecycleRegistry.markState(Lifecycle.State.CREATED);

        inflate(context, R.layout.view_ifttt_simple_connect_button, this);
        connectButton = findViewById(R.id.ifttt_connect_button);
        loadingView = findViewById(R.id.ifttt_loading_view);

        // Make sure the loading view has the same size as the connect button.
        loadingView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                loadingView.getViewTreeObserver().removeOnPreDrawListener(this);
                View buttonRoot = connectButton.findViewById(R.id.ifttt_button_root);
                ViewGroup.LayoutParams lp = loadingView.getLayoutParams();
                lp.width = buttonRoot.getWidth();
                lp.height = buttonRoot.getHeight();
                loadingView.setLayoutParams(lp);
                return false;
            }
        });
    }

    /*
     * Method to disable analytics tracking for the SDK. Analytics tracking is enabled by default,
     * Call this method before setting up the ConnectButton using{@link #ConnectButton.setup(Configuration) if you want to disable event tracking.
     * You only need to call this method once while setting up the first ConnectButton
     * Tracking will be disabled for all following instances of the ConnectButton.
     * */
    public static void disableTracking(Context context) {
        AnalyticsManager.getInstance(context).disableTracking();
    }

    /**
     * Connection enable interface for this class. It provides all of the necessary information for the
     * ConnectLocation library to handle different enabled state and set up location triggers
     */
    public interface onConnectionStateChangeListener {
        /**
         * Called when the connection is enabled.
         *
         * @param connectionFeatures Connection feature data associated with the Connection
         */
        void onConnectionEnabled(List<Feature> connectionFeatures);

        /**
         * Called when the connection is disabled.
         */
        void onConnectionDisabled();
    }

    /*
    * Method to set up state change callbacks for Location SDK
    * */
    public void setUpWithLocationTriggers(
            onConnectionStateChangeListener onConnectionStateChangeListener) {
        this.onConnectionStateChangeListener = onConnectionStateChangeListener;
    }

    /**
     * Set up the Connect Button to fetch the Connection data with the given id and set up the View to be able to do
     * authentication.
     *
     * @param configuration Configuration object that helps set up the Connect Button.
     */
    public void setup(Configuration configuration) {
        if (ButtonUiHelper.isEmailInvalid(configuration.suggestedUserEmail) && !ButtonUiHelper.isIftttInstalled(
                getContext().getPackageManager())) {
            connectButton.setVisibility(View.GONE);
            loadingView.setVisibility(View.GONE);
            Log.e(ConnectButton.class.getSimpleName(), configuration.suggestedUserEmail + " is invalid.");
            return;
        }

        connectButton.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.VISIBLE);

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
        UserTokenAsyncTask task = new UserTokenAsyncTask(credentialsProvider, clientToUse, () -> {
            if (configuration.connection != null) {
                if (configuration.listener != null) {
                    configuration.listener.onFetchConnectionSuccessful(configuration.connection);
                }

                connectButton.setConnection(configuration.connection);
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
     * Use this method if you want to change the default colors for rendering `ConnectButton` depending on the activity background
     * Default setting for this flag is false
     * @param onDarkBackground true for rendering ConnectButton on a dark background, false for light background
     */
    public void setOnDarkBackground(boolean onDarkBackground) {
        connectButton.setOnDarkBackground(onDarkBackground);
    }

    /**
     * Add a listener to be notified when the button's state has changed.
     *
     * @param listener {@link ButtonStateChangeListener} to be registered.
     */
    public void addButtonStateChangeListener(ButtonStateChangeListener listener) {
        connectButton.addButtonStateChangeListener(listener);
    }

    /**
     * Remove a previously registered listener.
     *
     * @param listener {@link ButtonStateChangeListener} to be removed.
     */
    public void removeButtonStateChangeListener(ButtonStateChangeListener listener) {
        connectButton.removeButtonStateChangeListener(listener);
    }

    /**
     * Given an {@link ConnectResult} from web redirect, refresh the UI of the button to reflect the current
     * state of the Connection authentication flow.
     *
     * @param result Authentication flow redirect result from the web view.
     */
    public void setConnectResult(@Nullable ConnectResult result) {
        if (credentialsProvider == null) {
            return;
        }

        if (result != null) {
            ButtonStateChangeListener listener = new ButtonStateChangeListener() {

                private final ConnectResult.NextStep nextStep = result.nextStep;

                @Override
                public void onStateChanged(ConnectButtonState currentState, ConnectButtonState previousState, List<Feature> connectionFeatures) {
                    connectButton.removeButtonStateChangeListener(this);
                    if (currentState == ConnectButtonState.Enabled && nextStep == ConnectResult.NextStep.Complete) {
                        if (result.userToken != null) {
                            API_CLIENT.setUserToken(result.userToken);
                            refreshConnection();
                        } else {
                            UserTokenAsyncTask task =
                                    new UserTokenAsyncTask(credentialsProvider, API_CLIENT, ConnectButton.this::refreshConnection);
                            task.execute();
                            lifecycleRegistry.addObserver(new AsyncTaskObserver(task));
                        }
                    } else if (currentState == ConnectButtonState.Disabled) {
                        if (onConnectionStateChangeListener != null) {
                            onConnectionStateChangeListener.onConnectionDisabled();
                        }
                    }
                }

                @Override
                public void onError(ErrorResponse errorResponse) {
                    connectButton.removeButtonStateChangeListener(this);
                }
            };

            connectButton.addButtonStateChangeListener(listener);
        }

        if (ViewCompat.isLaidOut(connectButton)) {
            connectButton.setConnectResult(result);
        } else {
            connectButton.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    connectButton.getViewTreeObserver().removeOnPreDrawListener(this);
                    connectButton.setConnectResult(result);
                    return false;
                }
            });
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
        Connection connection = connectButton.getConnection();
        PendingResult<Connection> pendingResult = API_CLIENT.api().showConnection(connection.id);
        pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
            @Override
            public void onSuccess(Connection result) {
                connectButton.setConnection(result);
                if (onConnectionStateChangeListener != null) {
                    onConnectionStateChangeListener.onConnectionEnabled(connection.features);
                }
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
             * @param connection {@link Connection} object.
             * @param suggestedUserEmail Email address string provided as the suggested email for the user. Must be a
             * valid email address.
             * @param credentialsProvider {@link CredentialsProvider} object that helps facilitate connection enable
             * flow from a ConnectButton.
             * @param connectRedirectUri Redirect {@link Uri} object that the ConnectButton is going to use to
             * redirect users back to your app after the connection enable flow is completed or failed.
             * @return The Builder object itself for chaining.
             */
            public static Builder withConnection(Connection connection, String suggestedUserEmail,
                    CredentialsProvider credentialsProvider, Uri connectRedirectUri) {
                Builder builder = new Builder(suggestedUserEmail, credentialsProvider, connectRedirectUri);
                builder.connection = connection;
                return builder;
            }

            /**
             * Factory method for creating a new Configuration builder.
             *
             * @param connectionId A Connection id that the {@link ConnectionApiClient} can use to fetch the
             * associated Connection object.
             * @param suggestedUserEmail Email address string provided as the suggested email for the user. Must be a
             * valid email address.
             * @param credentialsProvider {@link CredentialsProvider} object that helps facilitate connection enable
             * flow from a ConnectButton.
             * @param connectRedirectUri Redirect {@link Uri} object that the ConnectButton is going to use to
             * redirect users back to your app after the connection enable flow is completed or failed.
             * @return The Builder object itself for chaining.
             */
            public static Builder withConnectionId(String connectionId, String suggestedUserEmail,
                    CredentialsProvider credentialsProvider, Uri connectRedirectUri) {
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
             * @return The Builder object itself for chaining.
             */
            public Builder setOnFetchCompleteListener(OnFetchConnectionListener onFetchCompleteListener) {
                this.listener = onFetchCompleteListener;
                return this;
            }

            /**
             * @param connectionApiClient an optional {@link ConnectionApiClient} that will be used for the ConnectButton
             * instead of the default one.
             * @return The Builder object itself for chaining.
             */
            public Builder setConnectionApiClient(ConnectionApiClient connectionApiClient) {
                this.connectionApiClient = connectionApiClient;
                return this;
            }

            /**
             * @param inviteCode an optional string as the invite code, this is needed if your service is not yet
             * published on IFTTT Platform.
             * @return The Builder object itself for chaining.
             * @see ConnectionApiClient.Builder#setInviteCode(String)
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
