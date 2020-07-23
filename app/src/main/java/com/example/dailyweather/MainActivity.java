package com.example.dailyweather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    //main container layout...used for background image change...
    private ConstraintLayout mConstraintLayout;

    //layouts for changing city
    private LinearLayout mLayoutSearch, mLayoutChangeCity;
    private Button mButtonSearch;
    private EditText mEdSearch;

    //animation for weather
    private LottieAnimationView mLottieWeather;

    private TextView tv_sunrise;
    private TextView tv_sunset;
    private TextView tv_humidity;
    private TextView tv_updated_date;
    private TextView tv_wind;
    private TextView tv_temperatureStatus;
    private TextView tv_currentCity;
    private TextView tv_temperature;
    private TextView tv_temperatureFeelsLike;
    private TextView tv_pressure;
    private TextView tv_windDirection;
    private TextView tv_changeCity;
    private ImageView mImageViewabout;
    private boolean checkPermisson;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;



    //progressbar to show status when loadind
    private ProgressBar mProgressBar;

    //this section is for location tracking usinn GPS and granting permissions
    LocationTrack locationTrack;
    private ArrayList permissionsToRequest;
    private ArrayList permissionsRejected = new ArrayList();
    private ArrayList permissions = new ArrayList();
    private final static int ALL_PERMISSIONS_RESULT = 101;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();
        LottieAnimationView mTempLottie = findViewById(R.id.mLottieTemp);
        mLottieWeather = findViewById(R.id.mLottieWeather);
        mConstraintLayout = findViewById(R.id.mConstraintLayout);
        mLayoutChangeCity = findViewById(R.id.layoutChangeCity);
        mLayoutSearch = findViewById(R.id.layoutSearch);
        mButtonSearch = findViewById(R.id.mBtnSearch);
        mEdSearch = findViewById(R.id.mEdSearch);
        mProgressBar = findViewById(R.id.mProgressBar);
        mImageViewabout = findViewById(R.id.mImageViewabout);
        tv_currentCity = (TextView) findViewById(R.id.tv_currentCity);
        tv_temperature = (TextView) findViewById(R.id.tv_temperature);
        tv_temperatureStatus = (TextView) findViewById(R.id.tv_temperatureStatus);
        tv_humidity = (TextView) findViewById(R.id.tv_humidity);
        tv_pressure = (TextView) findViewById(R.id.tv_pressure);
        tv_wind = (TextView) findViewById(R.id.tv_wind);
        tv_windDirection = findViewById(R.id.tv_windDir);
        tv_sunrise = (TextView) findViewById(R.id.tv_sunrise);
        tv_sunset = (TextView) findViewById(R.id.tv_sunset);
        tv_changeCity = findViewById(R.id.tv_changeCity);
        tv_updated_date = findViewById(R.id.tv_lastUpdate);
        tv_temperatureFeelsLike = findViewById(R.id.tv_tempFeelsLike);
        recyclerView = (RecyclerView) findViewById(R.id.weather_daily_list);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
//       // recyclerView.setAdapter(adapter);





        //changing city
        tv_changeCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //animating layout for changing city
                //checck anim folder.. animations are there

                ViewGroup dateOneDay = (ViewGroup) findViewById(R.id.layoutSearch);
                Animation rightTOleft = AnimationUtils.loadAnimation(MainActivity.this, R.anim.left_to_right);

                if (mLayoutSearch.getVisibility() == View.GONE) {
                    mLayoutSearch.setVisibility(View.VISIBLE);
                    mLayoutChangeCity.setVisibility(View.GONE);
                    dateOneDay.startAnimation(rightTOleft);
                    mButtonSearch.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            searchForCity(mEdSearch.getText().toString().trim());
                            mLayoutSearch.setVisibility(View.GONE);
                            mLayoutChangeCity.setVisibility(View.VISIBLE);
                            // hideKeyboard(v);
                        }
                    });

                    //Changing keyboard focus after user enters city
                    mEdSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (!hasFocus) {
                                hideKeyboard(v);
                            }
                        }
                    });
                }
            }
        });




        //adding runtime permissions
        //and declared in MANIFEST as well

        permissions.add(ACCESS_FINE_LOCATION);
        permissions.add(ACCESS_COARSE_LOCATION);
        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0)
                requestPermissions((String[]) permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }



    }

    private ArrayList findUnAskedPermissions(ArrayList wanted) {
        ArrayList result = new ArrayList();
        for (Object perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }
        return result;
    }

    //for the first time when user installs app and grant permission following code is executed
    //as soon as permissions are granted,current lat lng are passed to retrieve weather
    private boolean hasPermission(Object permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                if (MainActivity.this.checkSelfPermission((String) permission) == PackageManager.PERMISSION_GRANTED) {


                    locationTrack = new LocationTrack(MainActivity.this);
                    if (locationTrack.canGetLocation()) {
                        double longitude = locationTrack.getLongitude();
                        double latitude = locationTrack.getLatitude();
                        HashMap<String, String> param = new HashMap<String, String>();
                        param.put("lat", "" + latitude);
                        param.put("lon", "" + longitude);
                        param.put("appid", getString(R.string.open_weather_maps_app_id));
                        getApiData(param); //lat long and API id is passed as parameters in  getApiData(param) using HAHMAP
                        //Toast.makeText(this, ""+latitude +"\n"+longitude, Toast.LENGTH_SHORT).show();
                    } else {
                        locationTrack.showSettingsAlert();
                    }

                }
                return (MainActivity.this.checkSelfPermission((String) permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (Object perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }
                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale((String) permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions((String[]) permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.app.AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationTrack.stopListener();
    }








    public void getApiData(final HashMap<String, String> parameter) {

        mProgressBar.setVisibility(View.VISIBLE);
        mConstraintLayout.setAlpha((float) 0.5);
        final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather?lat=" + parameter.get("lat") + "&lon=" + parameter.get("lon") + "&appid=d8445df189a8badf6d75de516b9a1063";
        StringRequest jsonArrayRequest = new StringRequest(Request.Method.GET, WEATHER_URL, new Response.Listener<String>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(String response) {
                try {
                   // Toast.makeText(MainActivity.this, "" + response, Toast.LENGTH_SHORT).show();
                    JSONObject json = new JSONObject(response);

                    JSONObject weatherDetails = json.getJSONArray("weather").getJSONObject(0);
                    JSONObject main = json.getJSONObject("main");
                    JSONObject sys = json.getJSONObject("sys");
                    JSONObject windObj = json.getJSONObject("wind");
                    JSONObject coordObj = json.getJSONObject("coord");

                    double latitude = coordObj.getDouble("lat");
                    double longitude = coordObj.getDouble("lon");
//                    Toast.makeText(MainActivity.this, ""+latitude +"\n"+longitude, Toast.LENGTH_SHORT).show();
                    double temperature = main.getDouble("temp");
                    double feels_like = main.getDouble("feels_like");
                    double min_temp = main.getDouble("temp_min");
                    double max_temp = main.getDouble("temp_max");
                    int humidityVal = main.getInt("humidity");
                    int pressureVal = main.getInt("pressure");
                    long sunriseVal = sys.getLong("sunrise") * 1000;
                    long sunsetVal = sys.getLong("sunset") * 1000;
                    String weatherType = weatherDetails.getString("description");
                    String weatherMain = weatherDetails.getString("main");
                    float windSpeed = windObj.getLong("speed");
                    float windDir = windObj.getLong("deg");
                    int id = weatherDetails.getInt("id");

                    Geocoder geocoder;
                    List<Address> addresses = null;
                    geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

                    try {
                        addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String cityA = null;
                    if (addresses != null && addresses.size()>0) {
                        cityA = addresses.get(0).getLocality();
                        if (cityA==null)
                        {
                            // Toast.makeText(MainActivity.this, "Can not determine city", Toast.LENGTH_SHORT).show();
                            tv_currentCity.setText("Unknown City");
                        }else {
                            tv_currentCity.setText(cityA + ", " + json.getJSONObject("sys").getString("country"));
                        }
                    }

                    tv_wind.setText(mps_to_kmph(windSpeed) + "km/hr");
                    tv_windDirection.setText(headingToString2(windDir));
                    String hPa = "hPa";
                    tv_temperatureStatus.setText(weatherType);
                    tv_humidity.setText(humidityVal + " %");
                    tv_pressure.setText(pressureVal + " " + hPa);


                    /////Temperature conversion
                    double c = temperature;
                    c = c - 273;
                    String[] arr = String.valueOf(c).split("\\.");
                    int[] intArr = new int[1];
                    intArr[0] = Integer.parseInt(arr[0]); // 1
                    tv_temperature.setText(intArr[0] + " °C");
//////////////////////////////////////////FEELS LIKE CONVERSION....//////////////
                    double cc = feels_like;
                    cc = cc - 273;
                    String[] arrA = String.valueOf(cc).split("\\.");
                    int[] intArrA = new int[1];
                    intArrA[0] = Integer.parseInt(arrA[0]); // 1
                    tv_temperatureFeelsLike.setText("Feels Like " + intArrA[0] + " °C");
                    /////////////////////////////////////////

                    //////////////////////////////////////////FEELS LIKE CONVERSION....//////////////
                    double ccMin = min_temp;
                    ccMin = ccMin - 273;
                    String[] arrMin = String.valueOf(ccMin).split("\\.");
                    int[] intArrMIn = new int[1];
                    intArrMIn[0] = Integer.parseInt(arrMin[0]); // 1
                    //temp_min.setText("MIN: " + intArrMIn[0] + " °C");
                    /////////////////////////////////////////

                    //////////////////////////////////////////FEELS LIKE CONVERSION....//////////////
                    double ccMax = max_temp;
                    ccMax = ccMax - 273;
                    String[] arrMax = String.valueOf(ccMax).split("\\.");
                    int[] intArrMax = new int[1];
                    intArrMax[0] = Integer.parseInt(arrMax[0]); // 1
                    // temp_max.setText("MAx: " + intArrMax[0] + " °C");
                    /////////////////////////////////////////

                    Date date = new Date();
                    SimpleDateFormat displayFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm");
                    SimpleDateFormat parseFormat = new SimpleDateFormat();
                    try {
                        date = parseFormat.parse(String.valueOf(json.getLong("dt") * 1000));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    String updatedOn = displayFormat.format(date);

                    tv_updated_date.setText("Last update: " + updatedOn);
                    Date dfsunrise = new java.util.Date(sunriseVal);
                    String sunriseValue = new SimpleDateFormat("HH:mm").format(dfsunrise);
                    tv_sunrise.setText(sunriseValue + "");

                    Date dfsunset = new java.util.Date(sunsetVal);
                    String sunsetValue = new SimpleDateFormat("HH:mm").format(dfsunset);
                    tv_sunset.setText(sunsetValue);

                    // setWeatherIcon(id, sunriseVal, sunsetVal);
                    setWeatherAnimation(id, sunriseVal, sunsetVal);


                    getFiveDaysApiData(cityA);

                    mProgressBar.setVisibility(View.INVISIBLE);
                    mConstraintLayout.setAlpha((float) 1.0);
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG", error.toString());
            }
        });
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(jsonArrayRequest);
    }

    //Function to get wind Direction
    public static String headingToString2(double x) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[(int) Math.round((((double) x % 360) / 45))];
    }

    //Function to convert mps_to_kmph
    float mps_to_kmph(float mps) {
        return (float) (3.6 * mps);
    }

    private void setWeatherAnimation(int actualId, long sunrise, long sunset) {
        //https://openweathermap.org/weather-conditions
        //refer this link
        int id = actualId / 100;
        long currentTime = new Date().getTime();
        String icon = "";
        if (actualId == 800) {
            if (currentTime >= sunrise && currentTime < sunset) {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient);
                mLottieWeather.setAnimation("clearday.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            } else {

                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient_new);
                mLottieWeather.setAnimation("clcearsky_night.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            }
        } else if (actualId > 800 && actualId <= 803) {

            if (currentTime >= sunrise && currentTime < sunset) {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient);
                mLottieWeather.setAnimation("partialclouds_day.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            } else {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient_new);
                mLottieWeather.setAnimation("partialclouds_night.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            }
        } else if (actualId == 804) {
            if (currentTime >= sunrise && currentTime < sunset) {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient);
                mLottieWeather.setAnimation("cloudy.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            } else {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient_new);
                mLottieWeather.setAnimation("cloudy.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            }
        } else if (actualId >= 701 && actualId <= 781) {

            if (currentTime >= sunrise && currentTime < sunset) {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient);
                mLottieWeather.setAnimation("mist.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            } else {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient_new);
                mLottieWeather.setAnimation("mist.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            }
        } else if (actualId >= 600 && actualId <= 622) {

            if (currentTime >= sunrise && currentTime < sunset) {

                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient);
                mLottieWeather.setAnimation("snow.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            } else {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient_new);
                mLottieWeather.setAnimation("snow.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            }
        } else if (actualId >= 500 && actualId <= 504) {

            if (currentTime >= sunrise && currentTime < sunset) {

                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient);
                mLottieWeather.setAnimation("partialrain_day.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            } else {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient_new);
                mLottieWeather.setAnimation("partialrain_night.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            }
        } else if (actualId >= 511 && actualId <= 531) {

            if (currentTime >= sunrise && currentTime < sunset) {

                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient);
                mLottieWeather.setAnimation("rain.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            } else {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient_new);
                mLottieWeather.setAnimation("rain.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            }
        } else if (actualId >= 300 && actualId <= 321) {

            if (currentTime >= sunrise && currentTime < sunset) {

                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient);
                mLottieWeather.setAnimation("drizzle.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            } else {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient_new);
                mLottieWeather.setAnimation("drizzle.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            }
        } else if (actualId >= 200 && actualId <= 232) {

            if (currentTime >= sunrise && currentTime < sunset) {

                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient);
                mLottieWeather.setAnimation("thunderstorm.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            } else {
                mConstraintLayout.setBackgroundResource(R.drawable.bg_gradient_new);
                mLottieWeather.setAnimation("thunderstorm.json");
                mLottieWeather.playAnimation();
                mLottieWeather.loop(true);
            }
        }

    }


    private void setWeatherIcon(int actualId, long sunrise, long sunset) {
        //https://openweathermap.org/weather-conditions
        //refer this link
        int id = actualId / 100;
        String icon = "";
        if (actualId == 800) {
            long currentTime = new Date().getTime();
            if (currentTime >= sunrise && currentTime < sunset) {
                icon = getString(R.string.weather_sunny);
            } else {
                icon = getString(R.string.weather_clear_night);
            }
        } else {
            switch (id) {
                case 2:
                    icon = getString(R.string.weather_thunder);
                    break;
                case 3:
                    icon = getString(R.string.weather_drizzle);
                    break;
                case 7:
                    icon = getString(R.string.weather_foggy);
                    break;
                case 8:
                    icon = getString(R.string.weather_cloudy);
                    break;
                case 6:
                    icon = getString(R.string.weather_snowy);
                    break;
                case 5:
                    icon = getString(R.string.weather_rainy);
                    break;
            }
        }
        //weatherIconFromAPI.setText(icon);
    }
    //it returns data... so i willl work on this....
    public void getFiveDaysApiData(final String city) {
        final JSONObject data = null;
        //   Toast.makeText(getActivity(), ""+parameter.get("lat")+parameter.get("lon"), Toast.LENGTH_SHORT).show();
        String apiUrl = "http://api.openweathermap.org/data/2.5/forecast?q=" + city + "&appid=d8445df189a8badf6d75de516b9a1063&units=metric";
        final List<weatherObject> daysOfTheWeek = new ArrayList<weatherObject>();
        //final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather?lat=" + parameter.get("lat") + "&lon=" + parameter.get("lon") + "&appid=d8445df189a8badf6d75de516b9a1063";
        StringRequest jsonArrayRequest = new StringRequest(Request.Method.GET, apiUrl, new Response.Listener<String>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(String response) {
                try {
                   /// Toast.makeText(MainActivity.this, "" + response, Toast.LENGTH_SHORT).show();
                    JSONObject json = new JSONObject(response);
                    //  city.setText(json.getString("name").toUpperCase(Locale.UK) + ", " + json.getJSONObject("sys").getString("country"));

                    GsonBuilder builder = new GsonBuilder();
                    Gson gson = builder.create();
                    Forecast forecast = gson.fromJson(response, Forecast.class);
                    if (null == forecast) {
                        Toast.makeText(getApplicationContext(), "Nothing was returned", Toast.LENGTH_LONG).show();
                    } else {
                      //  Toast.makeText(getApplicationContext(), "Response Good", Toast.LENGTH_LONG).show();
                        int[] everyday = new int[]{0, 0, 0, 0, 0, 0, 0};
                        List<FiveWeathers> weatherInfo = forecast.getList();
                        for (int i = 0; i < weatherInfo.size(); i++) {
                            String time = weatherInfo.get(i).getDt_txt();
                            String shortDay = convertTimeToDay(time);

                            String temp = weatherInfo.get(i).getMain().getTemp();
                            String tempMin = weatherInfo.get(i).getMain().getTemp_min();
                            if (convertTimeToDay(time).equals("Mon") && everyday[0] < 1) {
                                daysOfTheWeek.add(new weatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[0] = 1;
                            }
                            if (convertTimeToDay(time).equals("Tue") && everyday[1] < 1) {
                                daysOfTheWeek.add(new weatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[1] = 1;
                            }
                            if (convertTimeToDay(time).equals("Wed") && everyday[2] < 1) {
                                daysOfTheWeek.add(new weatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[2] = 1;
                            }
                            if (convertTimeToDay(time).equals("Thu") && everyday[3] < 1) {
                                daysOfTheWeek.add(new weatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[3] = 1;
                            }
                            if (convertTimeToDay(time).equals("Fri") && everyday[4] < 1) {
                                daysOfTheWeek.add(new weatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[4] = 1;
                            }
                            if (convertTimeToDay(time).equals("Sat") && everyday[5] < 1) {
                                daysOfTheWeek.add(new weatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[5] = 1;
                            }
                            if (convertTimeToDay(time).equals("Sun") && everyday[6] < 1) {
                                daysOfTheWeek.add(new weatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[6] = 1;
                            }
                            recyclerViewAdapter = new RecyclerViewAdapter(MainActivity.this, daysOfTheWeek);
                            recyclerView.setAdapter(recyclerViewAdapter);
                        }
                    }

                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG", error.toString());
            }
        });
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(jsonArrayRequest);

    }
    private String convertTimeToDay(String time){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:SSSS", Locale.getDefault());
        String days = "";
        try {
            Date date = format.parse(time);
            System.out.println("Our time " + date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            days = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault());
            System.out.println("Our time " + days);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return days;
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    //Function used to search city
    //
    public void searchForCity(final String city) {

        // final WeatherTask weatherTask = new WeatherTask();
        //weatherTask.execute(new String[]{city + "&appid=d8445df189a8badf6d75de516b9a1063" + "&units=metric"});
        new Thread() {
            public void run() {
                //RemoteFetch class takes city name and search for city
                final JSONObject json = RemoteFetch.getJSON(MainActivity.this, city);
                if (json == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.place_not_found), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            getWeatherbyCity(json);
                        }
                    });
                }
            }
        }.start();
    }



    //this functions gets city and then retrieve its weather
    //called in above function
    private void getWeatherbyCity(JSONObject json) {
        mProgressBar.setVisibility(View.VISIBLE);
        mConstraintLayout.setAlpha((float) 0.5);
        try {
            //All the necessary parsing from API is here.... when searched on basis of city
            tv_currentCity.setText(json.getString("name").toUpperCase(Locale.getDefault()) + ", " + json.getJSONObject("sys").getString("country"));
            JSONObject weatherDetails = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");
            JSONObject sys = json.getJSONObject("sys");
            JSONObject windObj = json.getJSONObject("wind");

            double temperature = main.getDouble("temp");
            double feels_like = main.getDouble("feels_like");
            double min_temp = main.getDouble("temp_min");
            double max_temp = main.getDouble("temp_max");
            int humidityVal = main.getInt("humidity");
            int pressureVal = main.getInt("pressure");
            long sunriseVal = sys.getLong("sunrise") * 1000;
            long sunsetVal = sys.getLong("sunset") * 1000;
            String weatherType = weatherDetails.getString("description");
            String weatherMain = weatherDetails.getString("main");
            float windSpeed = windObj.getLong("speed");
            float windDir = windObj.getLong("deg");
            int id = weatherDetails.getInt("id");


            tv_wind.setText(mps_to_kmph(windSpeed) + "km/hr");
            tv_windDirection.setText(headingToString2(windDir));
            tv_temperatureStatus.setText(weatherType);
            tv_humidity.setText(humidityVal + "%");
            tv_pressure.setText(pressureVal + " hPa");

            DecimalFormat decimalFormat = new DecimalFormat("#.#");
            String tempFormat = decimalFormat.format(temperature);
            tv_temperature.setText(tempFormat + " ℃");

            //feels like
            String tempFormatFeelsLike = decimalFormat.format(feels_like);
            tv_temperatureFeelsLike.setText("Feels Like " + tempFormatFeelsLike + " ℃");

            Date date = new Date();
            SimpleDateFormat displayFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm");
            SimpleDateFormat parseFormat = new SimpleDateFormat();

            try {
                date = parseFormat.parse(String.valueOf(json.getLong("dt") * 1000));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String updatedOn = displayFormat.format(date);
            tv_updated_date.setText("Last update: " + updatedOn);

            Date dfsunrise = new java.util.Date(sunriseVal);
            String sunriseValue = new SimpleDateFormat("HH:mm").format(dfsunrise);
            tv_sunrise.setText(sunriseValue + "");

            Date dfsunset = new java.util.Date(sunsetVal);
            String sunsetValue = new SimpleDateFormat("HH:mm").format(dfsunset);
            tv_sunset.setText(sunsetValue);

            // Toast.makeText(this, ""+id, Toast.LENGTH_SHORT).show();
            //setWeatherIcon(id, sunriseVal, sunsetVal) sets weather icon into a text View...as disscussed in onCreate
            //setWeatherIcon(id, sunriseVal, sunsetVal);

            //setting weather animation....
            setWeatherAnimation(id, sunriseVal, sunsetVal);

            mConstraintLayout.setAlpha((float) 1.0);
            mProgressBar.setVisibility(View.INVISIBLE);

        } catch (Exception e) {
            Log.e("SimpleWeather", "One or more fields not found in the JSON data");
        }
    }



}
