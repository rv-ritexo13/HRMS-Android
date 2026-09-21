package com.triotech.hrms.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainerView;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.triotech.hrms.R;
import com.triotech.hrms.core.util.SessionManager;
import com.triotech.hrms.databinding.ActivityMainBinding;
import com.triotech.hrms.ui.auth.LoginActivity;

/**
 * Single-Activity host for the Employee experience. All screens are Fragment
 * destinations in {@code nav_graph.xml}, switched via the bottom navigation bar.
 * Keeping navigation logic here (rather than spread across Fragments) is what
 * "basic navigation architecture" means for this app — feature Fragments stay
 * focused on their own screen.
 *
 * <p>Only reachable after a successful Employee login via {@link LoginActivity}.
 * The session check below is a defensive guard (e.g. the process was restarted
 * into this Activity directly by the OS after a logout elsewhere) rather than
 * the primary gate, which lives in {@code LoginActivity}.</p>
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = resolveNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        // Full-screen detail destinations hide the bottom bar.
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean fullScreen = id == R.id.payslipFragment
                    || id == R.id.editProfileFragment
                    || id == R.id.documentsFragment
                    || id == R.id.documentViewerFragment
                    || id == R.id.applyLeaveFragment
                    || id == R.id.leaveDetailFragment;
            binding.bottomNavigation.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
        });
    }

    private NavController resolveNavController() {
        FragmentContainerView navHost = binding.navHostFragment;
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(navHost.getId());
        if (fragment == null) {
            throw new IllegalStateException("NavHostFragment not found; check activity_main.xml");
        }
        return fragment.getNavController();
    }
}
