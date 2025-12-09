package com.zetrix.connectwallet.blockchain;

import com.zetrix.connectwallet.models.AccountResponse;
import com.zetrix.connectwallet.utils.ZetrixLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ChainSDK for interacting with Zetrix blockchain.
 * <p>
 * Provides methods to query blockchain data such as account nonces for transaction signing.
 * Supports both mainnet and testnet environments.
 * </p>
 * <p>
 * Ported from Flutter SDK's chain_sdk.dart (originally from chainSDK.js).
 * </p>
 * <p>
 * Usage:
 * <pre>
 * ChainSDK chainSdk = new ChainSDK();
 *
 * // Get nonce for mainnet
 * long nonce = chainSdk.getAccountNonce("zetrix-address", "1");
 *
 * // Get nonce for testnet
 * long nonce = chainSdk.getAccountNonce("zetrix-address", "2");
 * </pre>
 * </p>
 */
public class ChainSDK {

    private static final ZetrixLogger logger = ZetrixLogger.getLogger("ChainSDK");

    /**
     * Host URLs for different chain IDs.
     * - Chain ID "1": Mainnet
     * - Chain ID "2": Testnet
     */
    private static final Map<String, String> HOST_GROUP = new HashMap<>();

    static {
        HOST_GROUP.put("1", "https://wallet.zetrix.com");
        HOST_GROUP.put("2", "https://test-wallet.zetrix.com");
    }

    private final OkHttpClient httpClient;
    private final Map<String, ZetrixApiService> apiServices;

    /**
     * Create a new ChainSDK instance with default HTTP client configuration.
     */
    public ChainSDK() {
        this.httpClient = createHttpClient();
        this.apiServices = new HashMap<>();
    }

    /**
     * Create a new ChainSDK instance with custom HTTP client.
     *
     * @param httpClient custom OkHttpClient instance
     */
    public ChainSDK(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.apiServices = new HashMap<>();
    }

    /**
     * Create default HTTP client with timeout configuration.
     *
     * @return configured OkHttpClient
     */
    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Get or create API service for the specified chain ID.
     *
     * @param chainId the chain ID ("1" for mainnet, "2" for testnet)
     * @return ZetrixApiService instance
     * @throws IllegalArgumentException if chain ID is invalid
     */
    private ZetrixApiService getApiService(String chainId) {
        if (!apiServices.containsKey(chainId)) {
            String baseUrl = HOST_GROUP.get(chainId);
            if (baseUrl == null) {
                throw new IllegalArgumentException("Invalid chainId: " + chainId);
            }

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiServices.put(chainId, retrofit.create(ZetrixApiService.class));
        }

        return apiServices.get(chainId);
    }

    /**
     * Get account nonce for the given address and chain ID (synchronous).
     * <p>
     * Queries the blockchain for the account's current nonce value.
     * Returns 0 if:
     * - Account not found (error_code = 4)
     * - Nonce is null or missing
     * </p>
     *
     * @param address the Zetrix wallet address
     * @param chainId the chain ID ("1" for mainnet, "2" for testnet)
     * @return the account nonce, or 0 if not found
     * @throws IllegalArgumentException if chainId is invalid
     * @throws IOException if network error occurs
     * @throws RuntimeException if API call fails
     */
    public long getAccountNonce(String address, String chainId) throws IOException {
        logger.info("Getting account nonce for address: " + address + ", chainId: " + chainId);

        try {
            ZetrixApiService apiService = getApiService(chainId);
            Call<AccountResponse> call = apiService.getAccount(address);
            Response<AccountResponse> response = call.execute();

            if (!response.isSuccessful()) {
                throw new IOException("HTTP error: " + response.code() + " " + response.message());
            }

            AccountResponse accountResponse = response.body();
            if (accountResponse == null) {
                logger.warning("Empty response body for address: " + address);
                return 0;
            }

            // Error code 4 means account not found
            if (accountResponse.getErrorCode() == 4) {
                logger.info("Account not found (error_code=4), returning nonce 0");
                return 0;
            }

            // Extract nonce from result
            AccountResponse.AccountResult result = accountResponse.getResult();
            if (result == null || result.getNonce() == null) {
                logger.info("Nonce is null in response, returning 0");
                return 0;
            }

            long nonce = result.getNonce();
            logger.info("Successfully retrieved nonce: " + nonce);
            return nonce;

        } catch (IllegalArgumentException e) {
            logger.severe("Invalid chainId: " + chainId, e);
            throw e;
        } catch (IOException e) {
            logger.severe("Network error getting account nonce", e);
            throw e;
        } catch (Exception e) {
            logger.severe("Unexpected error getting account nonce", e);
            throw new RuntimeException("Failed to get account nonce", e);
        }
    }

    /**
     * Get account nonce for the given address and chain ID (asynchronous with callback).
     * <p>
     * Non-blocking version that executes the API call on a background thread.
     * </p>
     *
     * @param address the Zetrix wallet address
     * @param chainId the chain ID ("1" for mainnet, "2" for testnet)
     * @param callback callback to receive the result
     */
    public void getAccountNonce(String address, String chainId, NonceCallback callback) {
        new Thread(() -> {
            try {
                long nonce = getAccountNonce(address, chainId);
                callback.onSuccess(nonce);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    /**
     * Callback interface for asynchronous nonce retrieval.
     */
    public interface NonceCallback {
        /**
         * Called when nonce is successfully retrieved.
         *
         * @param nonce the account nonce
         */
        void onSuccess(long nonce);

        /**
         * Called when an error occurs.
         *
         * @param error the error that occurred
         */
        void onError(Exception error);
    }

    /**
     * Get the base URL for a given chain ID.
     *
     * @param chainId the chain ID
     * @return the base URL, or null if chain ID is invalid
     */
    public static String getHostUrl(String chainId) {
        return HOST_GROUP.get(chainId);
    }

    /**
     * Check if a chain ID is valid.
     *
     * @param chainId the chain ID to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidChainId(String chainId) {
        return HOST_GROUP.containsKey(chainId);
    }
}
