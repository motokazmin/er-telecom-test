package com.er_telecom.test_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import org.tensorflow.lite.Interpreter;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.modeldownloader.CustomModel;
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions;
import com.google.firebase.ml.modeldownloader.DownloadType;
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.File;
import java.nio.FloatBuffer;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.editTextNumberX)
    TextView X;
    @BindView(R.id.editTextNumberY)
    TextView Y;
    @BindView(R.id.buttonPredict)
    Button predict;
    @BindView(R.id.Zresult)
    TextView prediction;

    private Interpreter interpreter = null;

    private FirebaseRemoteConfig mFirebaseRemoteConfig = null;
    private Boolean modelReady = false;
    private static final String TAG = "UnFunc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        predict.setOnClickListener( v -> onPredictClicked());
        initializeModelFromRemoteConfig();
    }

    private void onPredictClicked() {
        float x = Float.parseFloat(X.getText().toString());
        float y = Float.parseFloat(Y.getText().toString());
        float z = predict(x, y);
        prediction.setText(z == -1? "Что то пошло не так..." : Float.toString(z));
    }

    private void initializeModelFromRemoteConfig(){
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build();

        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener(
                new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            Boolean updated = task.getResult();
                            Log.d("UnFunc", "Config params updated: " + updated.toString());
                            if (updated == true)
                                loadModel();
                        }
                        else {
                            Log.d("UnFunc", "Config params is failed: " + task.getException());
                        }
                    }
                }
            );
    }

    public void loadModel() {
        CustomModelDownloadConditions conditions = new CustomModelDownloadConditions.Builder()
            .requireWifi()
            .build();

        FirebaseModelDownloader.getInstance()
            .getModel("FindFunc", DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.i(TAG, "onFailure exeption : " + e.toString());
                }
            })
            .addOnSuccessListener(new OnSuccessListener<CustomModel>() {
                @Override
                public void onSuccess(CustomModel model) {
                    Log.i(TAG, "onSuccess model : " + model.toString());
                    File modelFile = model.getFile();
                    if (modelFile != null) {
                        interpreter = new Interpreter(modelFile);
                        Log.i(TAG, "onSuccess interpreter has been created successful : " + modelFile.toString());
                    }
                }
            });
    }

    public float predict(float X, float Y) {
        if (interpreter == null) {
            return -1;
        }

        Float inputs = X - Y;
        FloatBuffer outputs = FloatBuffer.allocate(1);
        interpreter.run(inputs, outputs);
        Log.i(TAG, "predict: " + outputs.get(0));

        return outputs.get(0);
    }
}