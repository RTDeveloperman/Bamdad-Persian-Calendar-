package com.outlay.view.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.byagowi.persiancalendar.R;
import com.github.johnkil.print.PrintView;
import com.outlay.App;
import com.outlay.adapter.ExpenseAdapter;
import com.outlay.dao.Category;
import com.outlay.dao.Expense;
import com.outlay.presenter.ExpensesListPresenter;
import com.outlay.utils.DateUtils;
import com.outlay.utils.IconUtils;
import com.outlay.utils.ResourceUtils;
import com.outlay.view.Page;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import calendar.CivilDate;

/**
 * Created by Bogdan Melnychuk on 1/20/16.
 */
public class ExpensesListFragment extends BaseFragment {
    public static final String ARG_CATEGORY_ID = "_argCategoryId";
    public static final String ARG_DATE_FROM = "_argDateFrom";
    public static final String ARG_DATE_TO = "_argDateTo";


    @Bind(R.id.recyclerView)
    RecyclerView recyclerView;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.fab)
    FloatingActionButton fab;

    @Bind(R.id.noResults)
    View noResults;

    @Bind(R.id.filterCategoryIcon)
    PrintView filterCategoryIcon;

    @Bind(R.id.filterCategoryName)
    TextView filterCategoryName;

    @Bind(R.id.filterDateLabel)
    TextView filterDateLabel;

    @Inject
    ExpensesListPresenter presenter;

    private ExpenseAdapter adapter;
    private Date dateFrom;
    private Date dateTo;
    private Long categoryId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getComponent(getActivity()).inject(this);
        presenter.attachView(this);

        long dateFromMillis = getArguments().getLong(ARG_DATE_FROM);
        long dateToMillis = getArguments().getLong(ARG_DATE_TO);

        dateFrom = new Date(dateFromMillis);
        dateTo = new Date(dateToMillis);
        if (getArguments().containsKey(ARG_CATEGORY_ID)) {
            categoryId = getArguments().getLong(ARG_CATEGORY_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses_list_expense, null, false);
        ButterKnife.bind(this, view);
        enableToolbar(toolbar);
        setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.caption_expenses));
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setHasFixedSize(true);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        adapter = new ExpenseAdapter();
        adapter.setOnExpenseClickListener(expense -> Page.goToExpenseDetails(getActivity(), expense.getId()));
        recyclerView.setAdapter(adapter);
        fab.setImageDrawable(ResourceUtils.getMaterialToolbarIcon(getActivity(), R.string.ic_material_add));
        fab.setOnClickListener(v -> Page.goToExpenseDetails(getActivity(), null));

        if(categoryId != null) {
            Category category = presenter.getCategoryById(categoryId);
            IconUtils.loadCategoryIcon(category, filterCategoryIcon);
            filterCategoryName.setText(category.getTitle());
        } else {
            filterCategoryName.setVisibility(View.GONE);
            filterCategoryIcon.setVisibility(View.GONE);
        }
        filterDateLabel.setText(getDateLabel());
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.loadExpenses(dateFrom, dateTo, categoryId);
    }

    public void displayExpenses(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            noResults.setVisibility(View.VISIBLE);
        } else {
            noResults.setVisibility(View.GONE);
            adapter.setItems(expenses);
        }
    }

    private String getDateLabel() {

//        String dateFromStr = DateUtils.toShortString(dateFrom);
//        String dateToStr = DateUtils.toShortString(dateTo);
        String result = ReportFragment.ExpensesListFragmentDate;
//        if(!dateFromStr.equals(dateToStr)) {
//            result += " - " + dateToStr;
//        }
        return result;
    }
}
