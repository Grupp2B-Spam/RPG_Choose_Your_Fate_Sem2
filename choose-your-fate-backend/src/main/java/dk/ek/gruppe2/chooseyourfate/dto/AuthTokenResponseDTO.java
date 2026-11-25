package dk.ek.gruppe2.chooseyourfate.dto;

public class AuthTokenResponseDTO {

    private String token;
    private String tokenType;

    public AuthTokenResponseDTO() {
    }

    public AuthTokenResponseDTO(String token) {
        this.token = token;
        this.tokenType = "Bearer";
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
