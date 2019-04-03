package mp3app;

import com.mpatric.mp3agic.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

public class Main {

	public static void main(String[] args) throws IOException, 
												  UnsupportedTagException, 
												  InvalidDataException, 
												  SQLException {

		// Handling command line arguments
		if (args.length !=1) {
			throw new IllegalArgumentException(
					"Specify a directory containing MP3s to use this program."
			);
		}
		
		String folder = args[0];
		
		// Get path for folder and check that it exists
		Path mp3Folder = Paths.get(folder);
	
		if (!Files.exists(mp3Folder)) {
			throw new IllegalArgumentException(
					"The directory you specified, " + mp3Folder + ", does " +
					"not exist."
			);
		}
		
		// Create a linked list that holds the path of each mp3 file
		LinkedList<Path> mp3Paths = new LinkedList<Path>();
		
		// Glob out the mp3s and add them to the list
		try (DirectoryStream<Path> stream = 
				Files.newDirectoryStream(mp3Folder, "*.{mp3}")) {
			for (Path entry : stream) {
				mp3Paths.add(entry);
			}
		} catch (IOException ex) {
			System.err.println(ex);
		}
		
		
		Song [] songList = new Song[mp3Paths.size()];
		int index = 0;
		// Use the mp3agic library to do stuff
		for (Path mp3FilePath : mp3Paths) {
			Mp3File mp3file = new Mp3File(mp3FilePath);
			if (mp3file.hasId3v1Tag()) {
				ID3v1 songTags = mp3file.getId3v1Tag();
				HashMap<String, String> dict = new HashMap<String, String>();
				dict.put("Title", songTags.getTitle());
				dict.put("Artist", songTags.getArtist());
				dict.put("Album", songTags.getAlbum());
				dict.put("Year", songTags.getYear());
				dict.put("Genre", songTags.getGenreDescription());
				// I recognize that the whole hash map thing was unnecessary 
				// here and I could have just gone from the songTags to the 
				// object creation, but this is a learning experience and I'm
				// trying to learn the syntax for as many things as possible
				songList[index] = new Song(dict.get("Title"),
								 		   dict.get("Artist"),
								 		   dict.get("Album"),
								 		   dict.get("Year"),
								 		   dict.get("Genre")
				);
				index++;
			} else {
				ID3v1 songTags = new ID3v1Tag();
				mp3file.setId3v1Tag(songTags);	
			}
			
			if (mp3file.hasId3v2Tag()) {
				// ID3v2 deeperTags = mp3file.getId3v2Tag();
				// System.out.println(deeperTags.getLyrics());
			} 
		}
		
		// Print everything from our song objects in the console
		for (int i = 0; i < songList.length; i++) {
			System.out.println(songList[i].getTitle());
			System.out.println(songList[i].getArtist());
			System.out.println(songList[i].getYear());
			System.out.println(songList[i].getAlbum());
			System.out.println(songList[i].getGenre());
			System.out.println();
		}
		
		// Now we're getting into DB/SQL stuff using the H2 library
		try (Connection conn = DriverManager.getConnection(
				"jdbc:h2:~/mydatabase;AUTO_SERVER=TRUE;" + 
				"INIT=runscript from './create.sql'")) {
			String insertTableSQL = "INSERT INTO SONGS" + 
									"(TITLE, ARTIST, YEAR, ALBUM, GENRE)" +
									"VALUES (?, ?, ?, ?, ?)";
			PreparedStatement ps = conn.prepareStatement(insertTableSQL);
			
			for (Song song : songList) {
				ps.setString(1, song.getTitle());
				ps.setString(2, song.getArtist());
				ps.setString(3, song.getYear());
				ps.setString(4, song.getAlbum());
				ps.setString(5, song.getGenre());
			}
			
			int[] updates = ps.executeBatch();
			conn.commit();
			System.out.println("Inserted " + updates.length + " records.");
			// TODO: why is updates.length 0?
        }
		
		
		
		
	}

}
