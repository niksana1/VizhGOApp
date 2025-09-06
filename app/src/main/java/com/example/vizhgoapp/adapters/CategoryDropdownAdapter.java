package com.example.vizhgoapp.adapters;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.example.vizhgoapp.R;

public class CategoryDropdownAdapter {

    public interface OnCategorySelectedListener {
        void onCategorySelected(String categoryKey, int position);
    }

    public static void setupCategoryDropdown(Context context, AutoCompleteTextView dropdown, OnCategorySelectedListener listener) {

        String[] categoryKeys = context.getResources().getStringArray(R.array.categories);
        String[] displayNames = getDisplayNames(context, categoryKeys, "category_");

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, displayNames);
        dropdown.setAdapter(categoryAdapter);

        dropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (listener != null) {
                listener.onCategorySelected(categoryKeys[position], position);
            }
        });
    }

    public static void setupSubcategoryDropdown(Context context, AutoCompleteTextView subcategoryDropdown,
                                                String categoryKey, OnCategorySelectedListener listener) {

        int arrayId = context.getResources().getIdentifier(categoryKey + "_subcategories",
                "array", context.getPackageName());

        if (arrayId == 0) {
            Log.e("CategoryDropdown", "Could not find subcategory array for: " + categoryKey);
            return;
        }

        String[] subcategoryKeys = context.getResources().getStringArray(arrayId);
        String[] subcategoryDisplayNames = getDisplayNames(context, subcategoryKeys, "subcat_");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, subcategoryDisplayNames);
        subcategoryDropdown.setAdapter(adapter);

        subcategoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (listener != null) {
                listener.onCategorySelected(subcategoryKeys[position], position);
            }
        });
        //Clear previous selection
        subcategoryDropdown.setText("", false);
    }

    // Get the translated names for the categories and subcattegories
    private static String[] getDisplayNames(Context context, String[] keys, String prefix) {
        String[] displayNames = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            displayNames[i] = getDisplayName(context, keys[i], prefix);
        }
        return displayNames;
    }

    private static String getDisplayName(Context context, String key, String prefix) {
        int stringId = context.getResources().getIdentifier(prefix + key,
                "string", context.getPackageName());
        if (stringId != 0) {
            return context.getString(stringId);
        } else {
            Log.w("CategoryDropdown", "Translation not found for: " + prefix + key);
            return key; // Fallback to key if translation not found
        }
    }


}
