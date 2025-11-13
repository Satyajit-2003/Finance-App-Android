package com.example.spendtrackr.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.spendtrackr.R;
import com.example.spendtrackr.api.ApiService;
import com.example.spendtrackr.api.ApiClient;
import com.example.spendtrackr.api.AuthCheckResponse;
import com.example.spendtrackr.api.BaseResponse;
import com.example.spendtrackr.utils.SharedPrefHelper;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {}

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView apiStatusText = view.findViewById(R.id.serverStatusText);
        TextView authStatusText = view.findViewById(R.id.authStatusText);
        Button excelSheetButton = view.findViewById(R.id.excelSheetButton);
        checkApiHealth(apiStatusText, authStatusText, excelSheetButton);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        EditText editBaseUrl = view.findViewById(R.id.editBaseUrl);
        EditText editApiKey = view.findViewById(R.id.editApiKey);
        Button buttonSave = view.findViewById(R.id.buttonSaveUrl);
        TextView apiStatusText = view.findViewById(R.id.serverStatusText);
        TextView authStatusText = view.findViewById(R.id.authStatusText);
        Button excelSheetButton = view.findViewById(R.id.excelSheetButton);
        TextInputLayout apiKeyPlaceHolder = view.findViewById(R.id.apiKeyPlaceHolder);
        MaterialSwitch successNotificationToggle = view.findViewById(R.id.switchShowSuccessNotification);
        MaterialSwitch failureNotificationToggle = view.findViewById(R.id.switchShowFailureNotification);
        MaterialSwitch errorNotificationToggle = view.findViewById(R.id.switchShowErrorNotification);

        // Set initial states from SharedPreferences
        editBaseUrl.setText(SharedPrefHelper.getBaseUrl(requireContext()));
        editApiKey.setText(SharedPrefHelper.getApiKey(requireContext()));

        successNotificationToggle.setChecked(
                SharedPrefHelper.getShowSuccessNotification(requireContext())
        );
        failureNotificationToggle.setChecked(
                SharedPrefHelper.getShowFailureNotification(requireContext())
        );
        errorNotificationToggle.setChecked(
                SharedPrefHelper.getShowErrorNotification(requireContext())
        );


        successNotificationToggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                SharedPrefHelper.setShowSuccessNotification(requireContext(), isChecked)
        );

        failureNotificationToggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                SharedPrefHelper.setShowFailureNotification(requireContext(), isChecked)
        );

        errorNotificationToggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                SharedPrefHelper.setShowErrorNotification(requireContext(), isChecked)
        );


        buttonSave.setOnClickListener(v -> {
            String newUrl = editBaseUrl.getText().toString().trim();
            String newApiKey = editApiKey.getText().toString().trim();
            SharedPrefHelper.setBaseUrl(requireContext(), newUrl);
            SharedPrefHelper.setApiKey(requireContext(), newApiKey);
            buttonSave.setEnabled(false);
            checkApiHealth(apiStatusText, authStatusText, excelSheetButton);
            buttonSave.setEnabled(true);
        });

        apiKeyPlaceHolder.setEndIconOnClickListener(new View.OnClickListener() {
            private boolean isVisible = false;

            @Override
            public void onClick(View v) {
                if (isVisible) {
                    // Hide API key
                    editApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
                    apiKeyPlaceHolder.setEndIconDrawable(R.drawable.ic_show_password); // show "eye" icon
                } else {
                    // Show API key
                    editApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    apiKeyPlaceHolder.setEndIconDrawable(R.drawable.ic_hide_password); // show "eye-off" icon
                }

                // Move cursor to the end of the text after toggling visibility
                editApiKey.setSelection(editApiKey.getText().length());
                isVisible = !isVisible;
            }
        });

        return view;
    }

    private void checkApiHealth(TextView serverStatusCheck, TextView authStatusCheck, Button excelSheetButton) {
        serverStatusCheck.setText(R.string.server_status_checking);
        serverStatusCheck.setTextColor(getResources().getColor(R.color.yellow, null));
        authStatusCheck.setText(R.string.auth_status_checking);
        authStatusCheck.setTextColor(getResources().getColor(R.color.yellow, null));
        ApiService apiService = ApiClient.getApiService(requireContext());

        apiService.checkAuth().enqueue(new Callback<BaseResponse<AuthCheckResponse>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<AuthCheckResponse>> call,
                                   @NonNull Response<BaseResponse<AuthCheckResponse>> response) {

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    // 200 OK — API authenticated
                    serverStatusCheck.setText(R.string.server_status_healthy);
                    serverStatusCheck.setTextColor(getResources().getColor(R.color.green, null));
                    authStatusCheck.setText(R.string.auth_status_okay);
                    authStatusCheck.setTextColor(getResources().getColor(R.color.green, null));

                    String sheetUrl = response.body().data.getSheetUrl();
                    if (sheetUrl != null && !sheetUrl.isEmpty()) {
                        excelSheetButton.setEnabled(true);
                        excelSheetButton.setOnClickListener(v -> {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(sheetUrl));
                            startActivity(browserIntent);
                        });
                    } else {
                        excelSheetButton.setEnabled(false);
                    }
                } else if (response.code() == 403) {
                    // Invalid API Key
                    serverStatusCheck.setText(R.string.server_status_healthy);
                    serverStatusCheck.setTextColor(getResources().getColor(R.color.green, null));
                    authStatusCheck.setText(R.string.auth_status_bad);
                    authStatusCheck.setTextColor(getResources().getColor(R.color.red, null));
                    excelSheetButton.setEnabled(false);
                } else {
                    //  Server-side issue
                    serverStatusCheck.setText(R.string.server_status_error);
                    serverStatusCheck.setTextColor(getResources().getColor(R.color.red, null));
                    authStatusCheck.setText(R.string.auth_status_unknown);
                    authStatusCheck.setTextColor(getResources().getColor(R.color.yellow, null));
                    excelSheetButton.setEnabled(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<AuthCheckResponse>> call, @NonNull Throwable t) {
                // Timeout / Network error
                serverStatusCheck.setText(R.string.server_status_unhealthy);
                serverStatusCheck.setTextColor(getResources().getColor(R.color.red, null));
                authStatusCheck.setText(R.string.auth_status_unknown);
                authStatusCheck.setTextColor(getResources().getColor(R.color.yellow, null));
                excelSheetButton.setEnabled(false);
            }
        });
    }

}
