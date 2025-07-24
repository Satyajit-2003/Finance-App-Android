package com.example.spendtrackr.utils;

import com.example.spendtrackr.R;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CategoryManager {

    private static final Map<String, Integer> CATEGORY_COLOR_MAP;
    static {
        Map<String, Integer> map = new HashMap<>();
        map.put("Food Order", R.color.category_food_order);
        map.put("Office Lunch", R.color.category_office_lunch);
        map.put("Dining Out", R.color.category_dining_out);
        map.put("Groceries", R.color.category_groceries);
        map.put("Retail", R.color.category_retail);
        map.put("Bill Payment", R.color.category_bill_payment);
        map.put("Subscription", R.color.category_subscription);
        map.put("Transportation", R.color.category_transportation);
        map.put("Shopping", R.color.category_shopping);
        map.put("Entertainment", R.color.category_entertainment);
        map.put("Utilities", R.color.category_utilities);
        map.put("Healthcare", R.color.category_healthcare);
        map.put("Education", R.color.category_education);
        map.put("Investment", R.color.category_investment);
        map.put("Transfer", R.color.category_transfer);
        map.put("Cash Withdrawal", R.color.category_cash_withdrawal);
        map.put("Other", R.color.category_other);
        map.put("Select", android.R.color.white); // default unassigned
        CATEGORY_COLOR_MAP = Collections.unmodifiableMap(map);
    }

    public static int getColorResForCategory(String category) {
        if (category == null) return android.R.color.white;

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
