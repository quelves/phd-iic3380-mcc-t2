package jwtc.android.chess;

import android.content.UriMatcher;
import android.net.Uri;
import jwtc.chess.PGNProvider;

public class MyPGNProviderARM extends PGNProvider{

	static {
		AUTHORITY = "jwtc.android.chess.MyPGNProviderARM";
		CONTENT_URI = Uri.parse("content://"  + AUTHORITY + "/games");
		
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "games", GAMES);
        sUriMatcher.addURI(AUTHORITY, "games/#", GAMES_ID);
	}
}