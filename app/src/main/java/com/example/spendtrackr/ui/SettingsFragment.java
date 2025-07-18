package com.example.spendtrackr.ui;

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
import com.example.spendtrackr.api.HealthResponse;
import com.example.spendtrackr.utils.SharedPrefHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {}

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkApiHealth(view.findViewById(R.id.apiStatusText));

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        EditText editBaseUrl = view.findViewById(R.id.editBaseUrl);
        EditText editApiKey = view.findViewById(R.id.editApiKey);
        Button buttonSave = view.findViewById(R.id.buttonSaveUrl);
        TextView apiStatusText = view.findViewById(R.id.apiStatusText);
        Button btnToggleApiKey = view.findViewById(R.id.btn_toggle_api_key);

        editBaseUrl.setText(SharedPrefHelper.getBaseUrl(requireContext()));
        editApiKey.setText(SharedPrefHelper.getApiKey(requireContext()));

        buttonSave.setOnClickListener(v -> {
            String newUrl = editBaseUrl.getText().toString().trim();
            String newApiKey = editApiKey.getText().toString().trim();
            SharedPrefHelper.setBaseUrl(requireContext(), newUrl);
            SharedPrefHelper.setApiKey(requireContext(), newApiKey);
            buttonSave.setEnabled(false);
            checkApiHealth(apiStatusText);
            buttonSave.setEnabled(true);
        });

        btnToggleApiKey.setOnClickListener(new View.OnClickListener() {
            private boolean isVisible = false;

            @Override
            public void onClick(View v) {
                if (isVisible) {
                    editApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
                    btnToggleApiKey.setText(R.string.api_button_show_api_key_text);
                } else {
                    editApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    btnToggleApiKey.setText(R.string.api_button_hide_api_key_text);
                }
                editApiKey.setSelection(editApiKey.getText().length());
                isVisible = !isVisible;
            }
        });
        return view;
    }

    private void checkApiHealth(TextView apiStatusText) {

        apiStatusText.setText(R.string.api_status_checking);
        apiStatusText.setTextColor(getResources().getColor(R.color.yellow, null));
        ApiService apiService = ApiClient.getApiService(requireContext());
        apiService.getHealth().enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(@NonNull Call<HealthResponse> call, @NonNull Response<HealthResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        "healthy".equalsIgnoreCase(response.body().getStatus())) {
                    apiStatusText.setText(R.string.api_status_healthy);
                    apiStatusText.setTextColor(getResources().getColor(R.color.green, null));

                } else {
                    apiStatusText.setText(R.string.api_status_unhealthy);
                    apiStatusText.setTextColor(getResources().getColor(R.color.red, null));

                }
            }

            @Override
            public void onFailure(@NonNull Call<HealthResponse> call, @NonNull Throwable t) {
                apiStatusText.setText(R.string.api_status_unhealthy);
            }
        });
    }

}
