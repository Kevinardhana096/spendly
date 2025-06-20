package com.example.spendly.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.spendly.R;
import com.example.spendly.activity.SetTotalBudgetActivity;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BudgetingEmptyStateFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BudgetingEmptyStateFragment extends Fragment {

    private Button btnStartBudgeting;

    public BudgetingEmptyStateFragment() {
        // Required empty public constructor
    }

    public static BudgetingEmptyStateFragment newInstance() {
        return new BudgetingEmptyStateFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_budgeting_empty_state, container, false);

        btnStartBudgeting = view.findViewById(R.id.btn_start_budgeting);
        btnStartBudgeting.setOnClickListener(v -> navigateToSetTotalBudget());

        return view;
    }

    private void navigateToSetTotalBudget() {
        Intent intent = new Intent(getActivity(), SetTotalBudgetActivity.class);
        startActivity(intent);
    }
}