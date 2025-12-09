package com.zetrix.connectwallet.blockchain;

import com.zetrix.connectwallet.models.AccountResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit API interface for Zetrix blockchain endpoints.
 * <p>
 * Defines REST API methods for interacting with Zetrix wallet servers.
 * </p>
 */
public interface ZetrixApiService {

    /**
     * Get account information including nonce.
     * <p>
     * Endpoint: GET /getAccount?address={address}
     * </p>
     *
     * @param address the Zetrix wallet address to query
     * @return Call object for async execution
     */
    @GET("getAccount")
    Call<AccountResponse> getAccount(@Query("address") String address);
}
