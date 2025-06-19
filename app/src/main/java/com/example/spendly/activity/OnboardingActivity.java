package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.spendly.R;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private Button btnSkip;
    private Button btnNext;

    private static final int NUM_PAGES = 3;
    private ImageView[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);

        // Set up ViewPager with adapter
        OnboardingPagerAdapter pagerAdapter = new OnboardingPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Set up page dots indicator
        setupDots();

        // Set up page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDots(position);

                // Change button text on last page
                if (position == NUM_PAGES - 1) {
                    btnNext.setText("Get Started");
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText("Continue");
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        // Set up button click listeners
        btnSkip.setOnClickListener(v -> {
            // Skip onboarding and go to sign in
            startSignInActivity();
        });

        btnNext.setOnClickListener(v -> {
            // If on last page, start sign in activity
            if (viewPager.getCurrentItem() == NUM_PAGES - 1) {
                startSignInActivity();
            } else {
                // Otherwise go to next page
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        });
    }

    private void setupDots() {
        dots = new ImageView[NUM_PAGES];

        // Create dots and add to layout
        for (int i = 0; i < NUM_PAGES; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageResource(i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);

            // Set layout parameters with margins
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dotsLayout.addView(dots[i], params);
        }
    }

    private void updateDots(int currentPage) {
        for (int i = 0; i < NUM_PAGES; i++) {
            dots[i].setImageResource(i == currentPage ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    private void startSignInActivity() {
        Intent intent = new Intent(OnboardingActivity.this, SignInActivity.class);
        startActivity(intent);
        finish(); // Close onboarding activity
    }

    // Adapter for onboarding pages
    private static class OnboardingPagerAdapter extends FragmentStateAdapter {
        public OnboardingPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return OnboardingFragment.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

    // Fragment for individual onboarding pages
    public static class OnboardingFragment extends Fragment {
        private static final String ARG_POSITION = "position";

        public static OnboardingFragment newInstance(int position) {
            OnboardingFragment fragment = new OnboardingFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_POSITION, position);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int position = requireArguments().getInt(ARG_POSITION, 0);

            // Inflate appropriate layout based on position
            int layoutResId;
            switch (position) {
                case 0:
                    layoutResId = R.layout.item_onboarding_screen_1;
                    break;
                case 1:
                    layoutResId = R.layout.item_onboarding_screen_2;
                    break;
                case 2:
                default:
                    layoutResId = R.layout.item_onboarding_screen_3;
                    break;
            }

            View view = inflater.inflate(layoutResId, container, false);

            // Set title and description programmatically
            ImageView imageView = view.findViewById(R.id.iv_onboarding_image);
            TextView titleView = view.findViewById(R.id.tv_onboarding_title);
            TextView descView = view.findViewById(R.id.tv_onboarding_description);

            // Set content based on position
            switch (position) {
                case 0:
                    imageView.setImageResource(R.drawable.ic_onboarding_1);
                    titleView.setText("Track Your Money");
                    descView.setText("Easily record your income and expenses to understand where your money goes");
                    break;
                case 1:
                    imageView.setImageResource(R.drawable.ic_onboarding_2);
                    titleView.setText("Stay On Budget");
                    descView.setText("Set monthly limits and control your \nspending with smart budgeting tools");
                    break;
                case 2:
                    imageView.setImageResource(R.drawable.ic_onboarding_3);
                    titleView.setText("Save Your Money");
                    descView.setText("Start a saving habit today and \nsee your progress grow along the way");
                    break;
            }

            return view;
        }
    }
}

