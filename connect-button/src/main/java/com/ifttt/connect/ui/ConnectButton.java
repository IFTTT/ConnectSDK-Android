package com.ifttt.connect.ui;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
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
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import com.ifttt.connect.R;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import com.ifttt.connect.api.ErrorResponse;
import com.ifttt.connect.api.PendingResult;
import com.ifttt.connect.api.UserTokenProvider;

import static android.animation.ValueAnimator.INFINITE;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;

/**
 * The main UI component for the Connect Button SDK. This class handles both displaying {@link Connection} status for a
 * given user, as well as initiating a Connection enable flow.
 */
public class ConnectButton extends FrameLayout implements LifecycleOwner {

    private static final long ANIM_DURATION = 1000L;
    private static ConnectionApiClient API_CLIENT;

    private ConnectResultCredentialProvider connectResultCredentialProvider;

    private final BaseConnectButton connectButton;
    private final TextView loadingView;

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

        lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);

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

    /**
     * Method to disable analytics tracking for the SDK. Analytics tracking is enabled by default,
     * Call this method before setting up the ConnectButton using{@link ConnectButton#setup(Configuration) if you want
     * to disable event tracking.
     *
     * You only need to call this method once while setting up the first ConnectButton
     * Tracking will be disabled for all following instances of the ConnectButton.
     */
    public static void disableTracking(Context context) {
        AnalyticsManager.getInstance(context).disableTracking();
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
            connectResultCredentialProvider = new ConnectResultCredentialProvider(configuration.credentialsProvider);
            if (API_CLIENT == null) {
                ConnectionApiClient.Builder clientBuilder = new ConnectionApiClient.Builder(getContext(),
                    connectResultCredentialProvider
                );
                if (configuration.inviteCode != null) {
                    clientBuilder.setInviteCode(configuration.inviteCode);
                }
                API_CLIENT = clientBuilder.build();
            }

            clientToUse = API_CLIENT;
        } else {
            clientToUse = configuration.connectionApiClient;
        }

        connectButton.setup(configuration.suggestedUserEmail,
            clientToUse,
            configuration.connectRedirectUri,
            configuration.credentialsProvider,
            configuration.inviteCode,
            configuration.skipConnectionConfiguration
        );

        pulseLoading();

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

        PendingResult<Connection> pendingResult = clientToUse.api().showConnection(configuration.connectionId);
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
                String errorText = getResources().getString(R.string.error_internet_connection);
                SpannableString termRetry = new SpannableString(getResources().getString(R.string.retry));
                termRetry.setSpan(new UnderlineSpan(), 0, termRetry.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                SpannableString errorSpan = new SpannableString(errorText);
                TextUtils.concat(errorSpan, " ", termRetry);

                errorSpan.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(),
                    R.color.ifttt_error_red
                    )),
                    0,
                    errorText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                connectButton.setErrorMessage(errorSpan, v -> {
                    PendingResult<Connection> pendingResult = clientToUse.api()
                        .showConnection(configuration.connectionId);
                    pendingResult.execute(this);
                    lifecycleRegistry.addObserver(new PendingResultLifecycleObserver<>(pendingResult));
                });
            }
        });

        lifecycleRegistry.addObserver(new PendingResultLifecycleObserver<>(pendingResult));
    }

    /**
     * Use this method if you want to change the default colors for rendering `ConnectButton` depending on the activity background
     * Default setting for this flag is false
     *
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
        if (result != null && result.userToken != null) {
            connectResultCredentialProvider.userToken = result.userToken;
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
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    private void pulseLoading() {
        ValueAnimator animator = ValueAnimator.ofInt(255, 200);
        animator.addUpdateListener(animation -> loadingView.setTextColor(Color.argb((int) animation.getAnimatedValue(),
            255,
            255,
            255
        )));
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
        private final boolean skipConnectionConfiguration;

        @Nullable private final ConnectionApiClient connectionApiClient;
        @Nullable private String connectionId;
        @Nullable private Connection connection;
        @Nullable private OnFetchConnectionListener listener;
        @Nullable private String inviteCode;

        /**
         * Factory method to build a new {@link Configuration} instance. There are a few steps to be taken in order to
         * complete the instantiation, taking into account several use cases:
         * - Building a ConnectButton with a connection ID String. This setup will let ConnectButton fetch the
         * connection data automatically.
         * - Building a ConnectButton with a pre-processed {@link Connection} object. This setup can be used if you
         * prefer parsing or constructing Connection on your own.
         * - Building a ConnectButton with just the {@link CredentialsProvider}. This setup allows ConnectButton to
         * instantiate a global ConnectionApiClient automatically.
         * - Building a ConnectButton with a {@link ConnectionApiClient} instance. This setup can be used if
         * you want a better control on the ConnectionApiClient, e.g controlling the authentication mechanism, user
         * login, etc.
         *
         * @param suggestedUserEmail Email address string provided as the suggested email for the user. Must be a
         * valid email address.
         * @param connectRedirectUri Redirect {@link Uri} object that the ConnectButton is going to use to
         * redirect users back to your app after the connection enable flow is completed or failed.
         */
        public static ConnectionSetup newBuilder(String suggestedUserEmail, Uri connectRedirectUri) {
            return new Builder(suggestedUserEmail, connectRedirectUri);
        }

        /**
         * Builder class for constructing a Configuration object.
         */
        public static final class Builder implements ConnectionSetup, ApiClientSetup, ConfigurationSetup {
            private final String suggestedUserEmail;
            private CredentialsProvider credentialsProvider;
            private final Uri connectRedirectUri;
            @Nullable private ConnectionApiClient connectionApiClient;

            @Nullable private String connectionId;
            @Nullable private OnFetchConnectionListener listener;
            @Nullable private Connection connection;
            @Nullable private String inviteCode;
            private boolean skipConnectionConfiguration;

            private Builder(String suggestedUserEmail, Uri connectRedirectUri) {
                this.suggestedUserEmail = suggestedUserEmail;
                this.connectRedirectUri = connectRedirectUri;
            }

            @Override
            public ConfigurationSetup setOnFetchCompleteListener(OnFetchConnectionListener onFetchCompleteListener) {
                this.listener = onFetchCompleteListener;
                return this;
            }

            @Override
            public ConfigurationSetup skipConnectionConfiguration() {
                this.skipConnectionConfiguration = true;
                return this;
            }

            @Override
            public ConfigurationSetup withClient(ConnectionApiClient client, CredentialsProvider provider) {
                this.connectionApiClient = client;
                this.credentialsProvider = provider;
                return this;
            }

            @Override
            public ConfigurationSetup withCredentialProvider(CredentialsProvider credentialProvider) {
                this.credentialsProvider = credentialProvider;
                return this;
            }

            @Override
            public ApiClientSetup withConnectionId(String id) {
                this.connectionId = id;
                return this;
            }

            @Override
            public ApiClientSetup withConnection(Connection connection) {
                this.connection = connection;
                return this;
            }

            @Override
            public ApiClientSetup setInviteCode(String inviteCode) {
                this.inviteCode = inviteCode;
                return this;
            }

            @Override
            public Configuration build() {
                if (connection == null && connectionId == null) {
                    throw new IllegalStateException("Either connection or connectionId must be non-null.");
                }

                Configuration configuration = new Configuration(suggestedUserEmail,
                    credentialsProvider,
                    connectRedirectUri, skipConnectionConfiguration, connectionApiClient
                );
                configuration.connection = connection;
                configuration.connectionId = connectionId;
                configuration.listener = listener;
                configuration.inviteCode = inviteCode;
                return configuration;
            }
        }

        private Configuration(
            String suggestedUserEmail,
            CredentialsProvider credentialsProvider,
            Uri connectRedirectUri, boolean skipConnectionConfiguration, @Nullable ConnectionApiClient connectionApiClient
        ) {
            this.suggestedUserEmail = suggestedUserEmail;
            this.credentialsProvider = credentialsProvider;
            this.connectRedirectUri = connectRedirectUri;
            this.skipConnectionConfiguration = skipConnectionConfiguration;
            this.connectionApiClient = connectionApiClient;
        }

        /**
         * A {@link Configuration.Builder} step to provide connection data related setup.
         */
        public interface ConnectionSetup {
            ApiClientSetup withConnectionId(String id);

            ApiClientSetup withConnection(Connection connection);

            ApiClientSetup setInviteCode(String inviteCode);
        }

        /**
         * A {@link Configuration.Builder} step to provide ConnectionApiClient related setup.
         */
        public interface ApiClientSetup {
            ConfigurationSetup withClient(ConnectionApiClient client, CredentialsProvider provider);

            ConfigurationSetup withCredentialProvider(CredentialsProvider credentialProvider);
        }

        /**
         * A {@link Configuration.Builder} step to complete the construction of a {@link Configuration} object.
         */
        public interface ConfigurationSetup {
            ConfigurationSetup setOnFetchCompleteListener(OnFetchConnectionListener onFetchCompleteListener);

            /**
             * Set up the {@link ConnectButton} such that the Connection enable flow skips the configuration step, which
             * is previously required for users to configure the Connection with appropriate feature field values.
             *
             * Setting this means that the user will have a partially enabled Connection after the enable flow is
             * completed, and it is the developers' responsibility to update the Connection with required field values
             * via Connect API. The partially enabled Connection will NOT be functional until all required fields are
             * configured.
             */
            ConfigurationSetup skipConnectionConfiguration();

            Configuration build();
        }
    }

    private static final class ConnectResultCredentialProvider implements UserTokenProvider {

        private final CredentialsProvider delegate;
        String userToken;

        ConnectResultCredentialProvider(CredentialsProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getUserToken() {
            if (userToken != null) {
                return userToken;
            }

            return delegate.getUserToken();
        }
    }
}
