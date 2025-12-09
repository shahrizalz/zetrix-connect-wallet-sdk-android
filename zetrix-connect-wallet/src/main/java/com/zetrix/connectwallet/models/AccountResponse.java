package com.zetrix.connectwallet.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for account information from Zetrix blockchain API.
 * <p>
 * Used by ChainSDK to parse responses from the /getAccount endpoint.
 * </p>
 */
public class AccountResponse {

    @SerializedName("error_code")
    private int errorCode;

    @SerializedName("error_desc")
    private String errorDesc;

    @SerializedName("result")
    private AccountResult result;

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public AccountResult getResult() {
        return result;
    }

    public void setResult(AccountResult result) {
        this.result = result;
    }

    /**
     * Inner class representing the account result data.
     */
    public static class AccountResult {
        @SerializedName("nonce")
        private Long nonce;

        @SerializedName("address")
        private String address;

        @SerializedName("balance")
        private String balance;

        public Long getNonce() {
            return nonce;
        }

        public void setNonce(Long nonce) {
            this.nonce = nonce;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getBalance() {
            return balance;
        }

        public void setBalance(String balance) {
            this.balance = balance;
        }
    }
}
