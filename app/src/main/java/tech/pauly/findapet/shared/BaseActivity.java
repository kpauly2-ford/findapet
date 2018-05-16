package tech.pauly.findapet.shared;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import io.reactivex.disposables.CompositeDisposable;

public class BaseActivity extends AppCompatActivity {

    private CompositeDisposable lifecycleSubscriptions = new CompositeDisposable();

    @Override
    public void onResume() {
        subscribeToEventBus();
        super.onResume();
    }

    @Override
    public void onPause() {
        lifecycleSubscriptions.clear();
        super.onPause();
    }

    @Nullable
    protected CompositeDisposable registerViewEvents() {
        return null;
    }

    protected void activityEvent(ActivityEvent event) {
        startActivity(new Intent(this, event.getStartActivityClass()));
    }

    public void fragmentEvent(FragmentEvent event) {
        Fragment newFragment = Fragment.instantiate(this, event.getFragmentClass().getName());
        getSupportFragmentManager().beginTransaction()
                                   .replace(event.getContainerId(), newFragment)
                                   .commit();
    }

    private void subscribeToEventBus() {
        lifecycleSubscriptions.clear();
        CompositeDisposable viewEvents = registerViewEvents();
        if (viewEvents != null) {
            lifecycleSubscriptions.add(viewEvents);
        }
    }
}