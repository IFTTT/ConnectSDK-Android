package com.ifttt.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.ifttt.api.PendingResult;

import static android.animation.ValueAnimator.INFINITE;

/**
 * A wrapper of a {@link IftttConnectButton} that provides some default configurations. Users of this class only need to
 * provide a {@link Config} instance, as well as the Connection id, and this view will handle both the loading state and
 * other UI states accordingly.
 */
public final class SimpleConnectButton extends FrameLayout implements LifecycleOwner {

    private static final long ANIM_DURATION = 1000L;
    private static IftttApiClient API_CLIENT;

    private final IftttConnectButton connectButton;
    private final TextView loadingView;

    private Config config;
    private Connection connection;

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
    }

    public void setup(String connectionId, String email, String ownerServiceId, Config config) {
        this.config = config;

        if (API_CLIENT == null) {
            API_CLIENT = new IftttApiClient.Builder(getContext()).build();
        }

        connectButton.setup(email, ownerServiceId, API_CLIENT, config.getRedirectUri(), config::getOAuthCode);

        pulseLoading();
        UserTokenAsyncTask task = new UserTokenAsyncTask(config, () -> {
            PendingResult<Connection> pendingResult = API_CLIENT.api().showConnection(connectionId);
            pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
                @Override
                public void onSuccess(Connection result) {
                    connection = result;

                    connectButton.setConnection(result);
                    connectButton.setVisibility(VISIBLE);
                    loadingView.setVisibility(GONE);
                    ((Animator) loadingView.getTag()).cancel();
                }

                @Override
                public void onFailure(ErrorResponse errorResponse) {
                    // TODO: Surface errors.
                }
            });

            lifecycleRegistry.addObserver(new PendingResultLifecycleObserver<>(pendingResult));
        });
        task.execute();
        lifecycleRegistry.addObserver(new AsyncTaskObserver(task));
    }

    /**
     * If the button is used in a dark background, set this flag to true so that the button can adapt the UI. This
     * method must be called before {@link IftttConnectButton#setConnection(Connection)} to apply the change.
     *
     * @param onDarkBackground True if the button is used in a dark background, false otherwise.
     */
    public void setOnDarkBackground(boolean onDarkBackground) {
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
        if (connection == null || config == null) {
            return;
        }

        connectButton.setConnectResult(result);

        if (result.nextStep == ConnectResult.NextStep.Complete) {
            UserTokenAsyncTask task = new UserTokenAsyncTask(config, () -> {
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
    public interface Config {
        /**
         * @return An IFTTT user token for this user. This token is going to be used to set up the {@link IftttApiClient}
         * to fetch Connection data with regard to this user.
         */
        @Nullable
        String getUserToken();

        /**
         * @return Your service's OAuth code. The OAuth code will be used to automatically authenticate user to your
         * service on IFTTT platform.
         *
         * @see IftttConnectButton#setup(String, String, IftttApiClient, String, OAuthCodeProvider).
         */
        String getOAuthCode();

        /**
         * @return URL string that will be used when the Connection authentication flow is completed on web view, in
         * order to return the result to the app.
         *
         * @see IftttConnectButton#setup(String, String, IftttApiClient, String, OAuthCodeProvider).
         */
        String getRedirectUri();
    }

    private static final class UserTokenAsyncTask extends android.os.AsyncTask<Void, Void, String> {

        private interface Callback {
            void onUserTokenSet();
        }

        private final Config config;
        private final Callback callback;

        private UserTokenAsyncTask(Config config, Callback callback) {
            this.config = config;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return config.getUserToken();
        }

        @Override
        protected void onPostExecute(String s) {
            API_CLIENT.setUserToken(s);
            callback.onUserTokenSet();
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
