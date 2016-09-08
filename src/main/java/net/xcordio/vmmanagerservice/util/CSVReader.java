package net.xcordio.vmmanagerservice.util;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads CSV data line-by-line from given {@link Reader}.
 * @author xcordio
 */
public class CSVReader {
	
	public final Reader reader;
	protected boolean crLFskipped = true;
	
	public CSVReader(Reader reader) {
		if (reader == null) throw new NullPointerException();
		this.reader = reader;
	}
	
	public static final int FIRSTCHAR = 0;
	public static final int UNUQOTED = 1;
	public static final int QUOTED = 2;
	public static final int CLOSED_QUOTE = 3;
	
	public List<String> nextCsv() throws IOException {
		List<String> values = new ArrayList<String>();
		StringBuilder bd = new StringBuilder();
		/* 0 - expecting value (quoted or not)
		 * 1 - no-quote parsing (expecting chars or comma)
		 * 2 - opened quote
		 * 3 - closed quote (expecting comma or quote again) */
		int state = FIRSTCHAR;
		for (;;) {
			int r = reader.read();
			if (r < 0) {
				if (bd.length() > 0 || values.size() > 0) {
					values.add(bd.toString());
				}
				return values.isEmpty() ? null : values;
			}
			//
			switch (state) {
			case FIRSTCHAR: // for first character of value only, may be quote or not
				// we skip line all line breaks for previous CSV line
				if (r == '\r' || r == '\n') {
					if (!crLFskipped) {
						if (r == '\n') crLFskipped = true;
						continue;
					}
					// end of line w/o any values
					if (bd.length() > 0 || values.size() > 0) {
						values.add(bd.toString());
					}
					crLFskipped = r == '\n';
					return values;
				}
				// checking if value is quoted
				if (r == '\"') {
					state = QUOTED;
					continue;
				}
				// checking for comma at first char
				if (r == ',') break;
				// nothing like that: then we have unquoted value
				state = UNUQOTED;
				bd.append((char) r);
				continue;
			case UNUQOTED: // for other that first character, and first char wasn't quote
				// end of quote
				if (r == ',') break;
				if (r == '\r' || r == '\n') {
					values.add(bd.toString());
					crLFskipped = r == '\n';
					return values;
				}
				bd.append((char) r);
				continue;
			case QUOTED: // for other than first character, when "inside quotes"
				if (r == '\"') {
					state = CLOSED_QUOTE;
					continue;
				}
				// not touching \r nor \n here: they're part of value
				else bd.append((char) r);
				continue;
			case CLOSED_QUOTE: // for other than first character, when "outside quotes"
				if (r == '\"') {
					bd.append('\"');
					state = QUOTED;
					continue;
				}
				if (r == ',') break;
				if (r == '\r' || r == '\n') {
					values.add(bd.toString());
					crLFskipped = r == '\n';
					return values;
				}
				throw new IOException("corrupted csv format: one of '\"', ',', '\\r', '\\n' is expected after closed quote, but got character code " + r + " char: " + (char) r);
			default: 
				throw new RuntimeException("assertion failed: unknown state");
			}
			values.add(bd.toString());
			bd.setLength(0);
			state = FIRSTCHAR;
		}
	}
}
