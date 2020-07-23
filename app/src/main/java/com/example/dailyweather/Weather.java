package com.example.dailyweather;


public class Weather {

    WeatherResults weatherResults;

    public Weather(WeatherResults weatherResults){
        this.weatherResults = weatherResults;
    }

    public WeatherResults getWeatherResults() {
        return weatherResults;
    }
}
