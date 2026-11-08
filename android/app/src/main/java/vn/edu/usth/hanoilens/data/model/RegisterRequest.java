package vn.edu.usth.hanoilens.data.model;

import com.google.gson.annotations.SerializedName;

/** Public account fields only; role and account status are assigned by the server. */
public class RegisterRequest {
    @SerializedName("displayName")
    private final String displayName;

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    public RegisterRequest(String displayName, String email, String password) {
        this.displayName = displayName;
        this.email = email;
        this.password = password;
    }
}
