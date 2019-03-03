package mg.studio.weatherappdesign;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.text.ParseException;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!isNetworkConnected(this))
        {
            Toast.makeText(this, "No Internet connection.Please check your setting.", Toast.LENGTH_SHORT).show();
            return;
        }
        else
            new DownloadUpdate().execute();
    }

    public void btnClick(View view)
    {
        if (!isNetworkConnected(this))
        {
            Toast.makeText(this, "No Internet connection.Please check your setting.", Toast.LENGTH_SHORT).show();
            return;
        }
        new DownloadUpdate().execute();
    }

    public boolean isNetworkConnected(Context context) {
        if (context != null)
        {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null)
            {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
        }

    // This class is used to parse json file obtain from web weather forecast api.
    private class WeatherParser
    {
        private JsonParser mParser;
        private JsonObject mObject;
        private String mNowDate;

        private String [] mWeekDays = {"sat", "sun", "mon", "tue", "wed", "thr", "fri"};

        WeatherParser(String json)
        {
            mParser = new JsonParser();
            mObject = (JsonObject)mParser.parse(json);
            Calendar now = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            // Get current date.
            this.mNowDate = dateFormat.format(now.getTime());
        }

        public String getNowDate()
        {
            return this.mNowDate;
        }

        public String getCityName() throws JsonIOException, JsonSyntaxException
        {
            JsonObject city = mObject.get("city").getAsJsonObject();
            return city.get("name").getAsString();
        }

        public float[] getTemperatures() throws JsonIOException, JsonSyntaxException
        {

            JsonArray array = mObject.getAsJsonArray("list");
            float temp[] = new float[5];
            int dDay = 0;
            for (int i = 0; i < array.size(); i++)
            {
                JsonObject day = array.get(i).getAsJsonObject();
                dDay = daysBetween(this.mNowDate, day.get("dt_txt").getAsString());
                if (dDay < 5)
                    temp[dDay] = Float.valueOf(day.get("main").getAsJsonObject().get("temp").getAsString());
            }
            return temp;
        }

        public String[] getConditions() throws JsonIOException, JsonSyntaxException
        {
            JsonArray array = mObject.getAsJsonArray("list");
            String cond[] = new String[5];
            int dDay = 0;
            for (int i = 0; i < array.size(); i++)
            {
                JsonObject day = array.get(i).getAsJsonObject();
                dDay = daysBetween(mNowDate, day.get("dt_txt").getAsString());
                JsonArray weather = day.get("weather").getAsJsonArray();
                if (dDay < 5)
                    cond[dDay] = weather.get(0).getAsJsonObject().get("main").getAsString();
            }
            return cond;
        }

        public String[] getDaysOfWeek() throws JsonIOException, JsonSyntaxException, ParseException
        {
            JsonArray array = mObject.getAsJsonArray("list");
            String days[] = new String[5];

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance();
            JsonObject day = array.get(0).getAsJsonObject();
            c.setTime(format.parse(day.get("dt_txt").getAsString()));

            for (int i = 0; i < 5; i++)
            {
                days[i] = mWeekDays[c.get(Calendar.DAY_OF_WEEK)+i];
            }
            return days;
        }


        /**
         * Calculate how many days are there between 2 date.
         * @param startDay
         * @param endDay
         * @return
         */
        public int daysBetween(String startDay, String endDay) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date1 = null;
            Date date2 = null;
            try
            {
                date1 = sdf.parse(startDay);
                date2 = sdf.parse(endDay);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(date1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            long time1 = cal.getTimeInMillis();
            cal.setTime(date2);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            long time2 = cal.getTimeInMillis();
            long between_days = (time2 - time1) / (1000 * 3600 * 24);
            return Integer.parseInt(String.valueOf(between_days));
        }
    }

    private class DownloadUpdate extends AsyncTask<String, Void, String>
    {
        private Context mContext;
        DownloadUpdate()
        {
        }

        @Override
        protected String doInBackground(String... strings)
        {
            String stringUrl = "http://api.openweathermap.org/data/2.5/forecast?" +
                    "id=1814906&APPID=05e7afda3a0401675ccec840fb9b10f9&units=metric";
            HttpURLConnection urlConnection = null;
            BufferedReader reader;
            try
            {
                URL url = new URL(stringUrl);

                // Create the request to get the information from the server, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(80000);
                urlConnection.setReadTimeout(8000);
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream == null)
                {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                StringBuffer buffer = new StringBuffer();
                while ((line = reader.readLine()) != null) {
                    // Mainly needed for debugging
                    Log.d("TAG", line);
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                //The temperature
                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally
            {
                urlConnection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String temperature) {
            try
            {
                //Update the temperature displayed
                WeatherParser parser = new WeatherParser(temperature);
                ((TextView) findViewById(R.id.tv_location)).setText(parser.getCityName());
                ((TextView) findViewById(R.id.tv_date)).setText(parser.getNowDate());
                float temperatures[] = parser.getTemperatures();
                String conditions[] = parser.getConditions();
                String daysOfWeek[] = parser.getDaysOfWeek();
                ((TextView) findViewById(R.id.cond_forecast_0)).setText(conditions[0]);
                ((TextView) findViewById(R.id.cond_forecast_1)).setText(conditions[1]);
                ((TextView) findViewById(R.id.cond_forecast_2)).setText(conditions[2]);
                ((TextView) findViewById(R.id.cond_forecast_3)).setText(conditions[3]);
                ((TextView) findViewById(R.id.cond_forecast_4)).setText(conditions[4]);
                ((TextView) findViewById(R.id.temp_forecast_0)).setText(String.valueOf((int) temperatures[0]));
                ((TextView) findViewById(R.id.temp_forecast_1)).setText(String.valueOf((int) temperatures[1]) + "℃");
                ((TextView) findViewById(R.id.temp_forecast_2)).setText(String.valueOf((int) temperatures[2]) + "℃");
                ((TextView) findViewById(R.id.temp_forecast_3)).setText(String.valueOf((int) temperatures[3]) + "℃");
                ((TextView) findViewById(R.id.temp_forecast_4)).setText(String.valueOf((int) temperatures[4]) + "℃");
                ((TextView) findViewById(R.id.day_forecast_0)).setText(daysOfWeek[0]);
                ((TextView) findViewById(R.id.day_forecast_1)).setText(daysOfWeek[1]);
                ((TextView) findViewById(R.id.day_forecast_2)).setText(daysOfWeek[2]);
                ((TextView) findViewById(R.id.day_forecast_3)).setText(daysOfWeek[3]);
                ((TextView) findViewById(R.id.day_forecast_4)).setText(daysOfWeek[4]);
            }
            catch (JsonIOException e)
            {
                e.printStackTrace();
            }
            catch (JsonSyntaxException e)
            {
                e.printStackTrace();
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }

        }
    }
}
