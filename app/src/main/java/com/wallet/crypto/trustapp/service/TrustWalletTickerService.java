package com.wallet.crypto.trustapp.service;

import com.google.gson.Gson;
import com.wallet.crypto.trustapp.entity.Ticker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;


public class TrustWalletTickerService implements TickerService {

    private final OkHttpClient httpClient;
    private final Gson gson;
    private ApiClient apiClient;


    private static String TRUST_API_URL = "https://api.trustwalletapp.com";
    static {
        try {
            URL url = new URL("http://jsondata.herc.one/api.properties");
            InputStream is = url.openStream();
            Properties properties = new Properties();
            properties.load(is);
            TRUST_API_URL = properties.getProperty("TRUST_API_URL");

        } catch(Exception ex) {
            //log.warn()
        }
    }

    public TrustWalletTickerService(
            OkHttpClient httpClient,
            Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
        buildApiClient(TRUST_API_URL);
    }

    private void buildApiClient(String baseUrl) {
        apiClient = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(ApiClient.class);
    }

    @Override
    public Observable<Ticker> fetchTickerPrice(String symbols) {
        return apiClient
                .fetchTickerPrice(symbols)
                .lift(apiError())
                .map(r -> r.response[0])
                .subscribeOn(Schedulers.io());
    }

    private static @NonNull
    <T> ApiErrorOperator<T> apiError() {
        return new ApiErrorOperator<>();
    }

    public interface ApiClient {
        @GET("prices?currency=USD&")
        Observable<Response<TrustResponse>> fetchTickerPrice(@Query("symbols") String symbols);
    }

    private static class TrustResponse {
        Ticker[] response;
    }

    private final static class ApiErrorOperator <T> implements ObservableOperator<T, Response<T>> {

        @Override
        public Observer<? super Response<T>> apply(Observer<? super T> observer) throws Exception {
            return new DisposableObserver<Response<T>>() {
                @Override
                public void onNext(Response<T> response) {
                    observer.onNext(response.body());
                    observer.onComplete();
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }

                @Override
                public void onComplete() {
                    observer.onComplete();
                }
            };
        }
    }
}
