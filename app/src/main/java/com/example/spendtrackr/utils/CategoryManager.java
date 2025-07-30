package com.example.spendtrackr.utils;

import com.example.spendtrackr.R;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CategoryManager {

    private static final Map<String, Integer> CATEGORY_COLOR_MAP;
    static {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Food Order", R.color.category_food_order);
        map.put("Office Cafeteria", R.color.category_office_cafeteria);
        map.put("Dining Out", R.color.category_dining_out);
        map.put("Groceries", R.color.category_groceries);
        map.put("Travel", R.color.category_travel);
        map.put("Shopping", R.color.category_shopping);
        map.put("Entertainment", R.color.category_entertainment);
        map.put("Utility Bills", R.color.category_utility_bills);
        map.put("Healthcare", R.color.category_healthcare);
        map.put("Investment", R.color.category_investment);
        map.put("Transfer", R.color.category_transfer);
        map.put("Subscription", R.color.category_subscription);
        map.put("Rent", R.color.category_rent);
        map.put("Loan Repayment", R.color.category_loan_repayment);
        map.put("CC Bill", R.color.category_cc_bill);
        map.put("Education", R.color.category_education);
        map.put("Other", R.color.category_other);
        CATEGORY_COLOR_MAP = Collections.unmodifiableMap(map);
    }

    public static boolean shouldIncludeInPieChart(String category) {
        return category != null &&
                !category.equalsIgnoreCase("CC Bill") &&
                !category.equalsIgnoreCase("Loan Repayment");
    }


    public static int getColorResForCategory(String category) {
        if (category == null) return android.R.color.white;
        if ("Select".equalsIgnoreCase(category)) return R.color.category_select;

        Integer colorRes = CATEGORY_COLOR_MAP.get(category);
        if (colorRes == null) {
            return R.color.category_other;
        }
        return colorRes;
    }


    public static Map<String, Integer> getCategoryColorMap() {
        return CATEGORY_COLOR_MAP;
    }
}
