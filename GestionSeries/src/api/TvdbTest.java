package api;

public class TvdbTest {

	private static final String API_KEY = "D4858E1F5CC604F8";
	private static final String USER_KEY = "ECF8D275D64FAADF";
	private static final String USERNAME = "mr_flibble";

	public static void main(String[] args) {
		TvdbEndpoint tvdb = new TvdbEndpoint(API_KEY, USERNAME, USER_KEY);
		tvdb.login();
	}

}
