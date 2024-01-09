package info.movito.themoviedbapi;

import info.movito.themoviedbapi.model.config.TokenAuthorisation;
import info.movito.themoviedbapi.model.config.TokenSession;
import info.movito.themoviedbapi.model.core.responses.TmdbResponseException;
import info.movito.themoviedbapi.tools.ApiUrl;
import info.movito.themoviedbapi.tools.MovieDbException;

/**
 * The movie database api for authentication. See the
 * <a href="https://developer.themoviedb.org/reference/authentication-how-do-i-generate-a-session-id">documentation</a> for more info.
 */
public class TmdbAuthentication extends AbstractTmdbApi {
    public static final String PARAM_REQUEST_TOKEN = "request_token";

    public static final String TMDB_METHOD_AUTH = "authentication";

    /**
     * Create a new TmdbAuthentication instance to call the authentication related TMDb API methods.
     */
    TmdbAuthentication(TmdbApi tmdbApi) {
        super(tmdbApi);
    }

    /**
     * This method is used to generate a valid request token for user based authentication.
     *
     * A request token is required in order to request a session id.
     *
     * You can generate any number of request tokens but they will expire after 60 minutes.
     *
     * As soon as a valid session id has been created the token will be destroyed.
     */

    public TokenAuthorisation getAuthorisationToken() throws TmdbResponseException {
        ApiUrl apiUrl = new ApiUrl(TMDB_METHOD_AUTH, "token/new");

        return mapJsonResult(apiUrl, TokenAuthorisation.class);
    }

    /**
     * This method is used to generate a session id for user based authentication.
     *
     * A session id is required in order to use any of the write methods.
     */
    public TokenSession getSessionToken(TokenAuthorisation token) throws TmdbResponseException {
        ApiUrl apiUrl = new ApiUrl(TMDB_METHOD_AUTH, "session/new");

        if (!token.getSuccess()) {
            logger.warn("Authorisation token was not successful!");
            throw new MovieDbException("Authorisation token was not successful!");
        }

        apiUrl.addPathParam(PARAM_REQUEST_TOKEN, token.getRequestToken());

        return mapJsonResult(apiUrl, TokenSession.class);
    }

    /**
     * Try to validate TokenAuthorisation with username and password.
     *
     * @param token A TokenAuthorisation previously generated by getAuthorisationToken
     * @param user  username
     * @param pwd   password
     * @return The validated TokenAuthorisation. The same as input with getSuccess()==true
     */
    public TokenAuthorisation getLoginToken(TokenAuthorisation token, String user, String pwd) throws TmdbResponseException {
        ApiUrl apiUrl = new ApiUrl(TMDB_METHOD_AUTH, "token/validate_with_login");

        apiUrl.addPathParam(PARAM_REQUEST_TOKEN, token.getRequestToken());
        apiUrl.addPathParam("username", user);
        apiUrl.addPathParam("password", pwd);

        return mapJsonResult(apiUrl, TokenAuthorisation.class);
    }

    /**
     * Does all the necessary username/password authentication
     * stuff in one go
     *
     * Generates a new valid TokenAuthorisation
     *
     * Validates the Token via username/password
     *
     * requests a new session id with the validated TokenAuthorisation
     * and returns a new TokenSession which one may want to transform
     * into SessionToken for APO calls that require a authorized user.
     *
     * @return validated TokenSession
     * @throws info.movito.themoviedbapi.tools.MovieDbException if the login failed
     */
    public TokenSession getSessionLogin(String username, String password) throws TmdbResponseException {
        TokenAuthorisation authToken = getAuthorisationToken();

        if (!authToken.getSuccess()) {
            throw new MovieDbException("Authorisation token was not successful!");
        }

        TokenAuthorisation loginToken = getLoginToken(authToken, username, password);

        if (!loginToken.getSuccess()) {
            throw new MovieDbException("User authentication failed:" + loginToken);
        }

        return getSessionToken(loginToken);
    }

    /**
     * This method is used to generate a guest session id.
     *
     * A guest session can be used to rate movies without having a registered TMDb user account.
     *
     * You should only generate a single guest session per user (or device) as you will be able to attach the ratings to
     * a TMDb user account in the future.
     *
     * There are also IP limits in place so you should always make sure it's the end user doing the guest session
     * actions.
     *
     * If a guest session is not used for the first time within 24 hours, it will be automatically discarded.
     */
    public TokenSession getGuestSessionToken() throws TmdbResponseException {
        ApiUrl apiUrl = new ApiUrl(TMDB_METHOD_AUTH, "guest_session/new");

        return mapJsonResult(apiUrl, TokenSession.class);
    }
}
