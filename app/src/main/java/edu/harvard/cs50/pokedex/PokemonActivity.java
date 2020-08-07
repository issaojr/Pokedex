package edu.harvard.cs50.pokedex;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

/**
 * Class related to individual (selected) pokemon activity
 */
public class PokemonActivity extends AppCompatActivity {
    private TextView nameTextView;
    private TextView numberTextView;
    private TextView type1TextView;
    private TextView type2TextView;
    private String url;
    private RequestQueue requestQueue;
    private Button catchButton;
    private boolean catched;
    private SharedPreferences sharedPreferences;
    private ImageView pokemonImage;
    private TextView descriptionTextView;
    private int id;

    /**
     * Initializes all objects
     *
     * @param savedInstanceState <-- saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        url = getIntent().getStringExtra("url");
        nameTextView = findViewById(R.id.pokemon_name);
        numberTextView = findViewById(R.id.pokemon_number);
        type1TextView = findViewById(R.id.pokemon_type1);
        type2TextView = findViewById(R.id.pokemon_type2);
        catchButton = findViewById(R.id.catch_button);
        pokemonImage = findViewById(R.id.pk_image);
        descriptionTextView = findViewById(R.id.pokemon_description);

        sharedPreferences = getSharedPreferences("MyPREFERENCES", Context.MODE_PRIVATE);
        load();
    }

    /**
     * Main function of this method is to load a selected pokemon
     * Secondly is made a request to obtain a descriptive text about each pokemon
     */
    public void load() {

        type1TextView.setText("");
        type2TextView.setText("");

        // Url of pokemon texts
        final String urlDescription = "https://pokeapi.co/api/v2/pokemon-species/";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    id = response.getInt("id");

                    // calls a method to realize a second request
                    loadDescription(urlDescription + id);

                    nameTextView.setText(response.getString("name"));
                    numberTextView.setText(String.format(Locale.US, "#%03d", id));

                    // catch button is initialized
                    catched = sharedPreferences.getBoolean(numberTextView.getText().toString(), false);
                    setButtonText(catched);

                    // getting data from the response json object
                    JSONArray typeEntries = response.getJSONArray("types");
                    for (int i = 0; i < typeEntries.length(); i++) {
                        JSONObject typeEntry = typeEntries.getJSONObject(i);
                        int slot = typeEntry.getInt("slot");
                        String type = typeEntry.getJSONObject("type").getString("name");

                        if (slot == 1) {
                            type1TextView.setText(type);
                        } else if (slot == 2) {
                            type2TextView.setText(type);
                        }
                    }
                    JSONObject spriteEntries = response.getJSONObject("sprites");
                    String imgUrl = spriteEntries.getString("front_default");

                    new DownloadSpriteTask().execute(imgUrl);

                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon json error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon details error", error);
            }
        });
        requestQueue.add(request);
    }

    /**
     * A description is requested through this method
     *
     * @param url <-- API url for description text requests
     */
    public void loadDescription(final String url) {

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray textEntries = response.getJSONArray("flavor_text_entries");
                    JSONObject textEntry = textEntries.getJSONObject(0);
                    // getting a little english text
                    descriptionTextView.setText(textEntry.getString("flavor_text"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon details error", error);
            }
        });
        requestQueue.add(request);
    }

    /**
     * Inverts catched variable state
     *
     * @param view <-- application view
     */
    public void toggleCatch(View view) {
        catched = !catched;
        saveState(numberTextView.getText().toString(), catched);
        setButtonText(catched);
    }

    /**
     * saves new state on internal memory
     *
     * @param key   <-- key for persist state
     * @param value <-- boolean value that represents pokemon state (catched/released)
     */
    private void saveState(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * changes button label
     *
     * @param c <-- boolean to set button label (release if true or catch! if false)
     */
    private void setButtonText(boolean c) {
        if (c) {
            catchButton.setText(R.string.release_label);
        } else {
            catchButton.setText(R.string.catch_label);
        }
    }

    /**
     * This async task is responsible for downloading a pokemon icon
     * and for extract bitmap
     */
    @SuppressLint("StaticFieldLeak")
    private class DownloadSpriteTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                return BitmapFactory.decodeStream(url.openStream());
            } catch (IOException e) {
                Log.e("cs50", "Download sprite error", e);
                return null;
            }
        }

        /**
         * Bitmap is set to imageview placeholder
         *
         * @param bitmap <-- pokemon icon requested from API
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            pokemonImage.setImageBitmap(bitmap);
        }
    }
}
