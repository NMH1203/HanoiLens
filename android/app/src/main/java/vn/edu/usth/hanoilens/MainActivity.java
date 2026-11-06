package vn.edu.usth.hanoilens;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import vn.edu.usth.hanoilens.databinding.ActivityMainBinding;
import vn.edu.usth.hanoilens.databinding.ItemBottomNavBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ItemBottomNavBinding[] navItems;
    private Menu navMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle window insets so floating bottom nav card stays above system navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.cardBottomNav, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            int baseMargin = (int) (20 * getResources().getDisplayMetrics().density);
            lp.bottomMargin = baseMargin + insets.bottom;
            view.setLayoutParams(lp);
            return windowInsets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            createBottomNavigation(navController);
            navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                    updateSelectedDestination(destination));
        }
    }

    private void createBottomNavigation(@NonNull NavController navController) {
        navMenu = new MenuBuilder(this);
        getMenuInflater().inflate(R.menu.bottom_nav_menu, navMenu);
        navItems = new ItemBottomNavBinding[navMenu.size()];
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < navMenu.size(); i++) {
            MenuItem menuItem = navMenu.getItem(i);
            final int destinationId = menuItem.getItemId();
            ItemBottomNavBinding item = ItemBottomNavBinding.inflate(
                    inflater, binding.bottomNavContainer, false);
            item.layoutNavItemRoot.setId(destinationId);
            item.tvNavItemLabel.setText(menuItem.getTitle());
            item.ivNavItemIcon.setImageDrawable(menuItem.getIcon());
            item.layoutNavItemRoot.setOnClickListener(view -> {
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() != destinationId) {
                    int startDestinationId = navController.getGraph().getStartDestinationId();
                    NavOptions options = new NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setRestoreState(true)
                            .setPopUpTo(startDestinationId, false, true)
                            .build();
                    navController.navigate(destinationId, null, options);
                }
            });
            navItems[i] = item;
            binding.bottomNavContainer.addView(item.getRoot());
        }
    }

    private void updateSelectedDestination(@NonNull NavDestination destination) {
        if (navItems == null) return;

        for (int i = 0; i < navMenu.size(); i++) {
            MenuItem menuItem = navMenu.getItem(i);
            boolean selected = destination.getId() == menuItem.getItemId();
            ItemBottomNavBinding item = navItems[i];
            item.layoutNavItemRoot.setSelected(selected);
            item.containerNavIconCircle.setBackground(circleBackground(selected));
            ImageViewCompat.setImageTintList(item.ivNavItemIcon, ContextCompat.getColorStateList(
                    this, selected ? R.color.nav_active_icon : R.color.nav_inactive_icon));
            item.tvNavItemLabel.setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.nav_active_coral : R.color.nav_inactive_text));
            item.tvNavItemLabel.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            String label = menuItem.getTitle().toString();
            item.layoutNavItemRoot.setContentDescription(selected
                    ? getString(R.string.nav_selected_accessibility, label) : label);
        }
    }

    @NonNull
    private GradientDrawable circleBackground(boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(ContextCompat.getColor(this,
                selected ? R.color.nav_active_coral : R.color.nav_inactive_icon_bg));
        return background;
    }
}
