import java.io.*;
import java.util.*;

import com.wrapper.spotify.Api;
import com.wrapper.spotify.exceptions.WebApiException;
import com.wrapper.spotify.methods.ArtistSearchRequest;
import com.wrapper.spotify.methods.TopTracksRequest;
import com.wrapper.spotify.methods.authentication.ClientCredentialsGrantRequest;
import com.wrapper.spotify.models.Artist;
import com.wrapper.spotify.models.ClientCredentials;
import com.wrapper.spotify.models.Page;
import com.wrapper.spotify.models.Track;

/**
 * @author aayush.saxena
 * @since 7/25/17
 */
public class SpotifyConnect {
    public static String CLIENT_ID = "0066307627c2459aaf0370e2c5949dc0";
    public static String CLIENT_SECRET = "a6038211d7304138a27f3edcdc0e9db5";
    public static Set<Artist> artistSet = new HashSet<Artist>();

    public static void main(String[] strings) throws IOException, WebApiException, InterruptedException {

        final Api api = Api.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .build();

        /* Create a request object. */
        final ClientCredentialsGrantRequest request = api.clientCredentialsGrant().build();

        /* Use the request object to make the request, either asynchronously (getAsync) or synchronously (get) */
        final ClientCredentials response = request.get();

        if(response != null) {
            System.out.println("Successfully retrieved an access token! " + response.getAccessToken());
            System.out.println("The access token expires in " + response.getExpiresIn() + " seconds");
            api.setAccessToken(response.getAccessToken());
        } else {
            System.out.println("Something happened");
        }

        processDocument();
        searchArtistsFromFile(api);
        checkArtistSet();
        findTopSongs(api);
    }

    public static void processDocument() throws IOException {
        File file = new File("festival.txt");
        try {
            Scanner sc = new Scanner(file);
            while(sc.hasNextLine()) {
                String line = sc.nextLine();
                processLine(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void processLine(String line) throws IOException {
        String[] words = line.split(" ");
        ArrayList<String> wordList = new ArrayList<String>();
        PrintWriter writer = new PrintWriter("festival-regex-processed.txt", "UTF-8");
        for(int i = 0; i < words.length; i++) {
            if(processWords(words[i])) {
                wordList.add(words[i]);
            }
        }
        for(int i = 0; i < wordList.size(); i++) {
            //System.out.println(wordList.get(i));
            writer.println(wordList.get(i));
        }
        writer.close();
    }

    public static boolean processWords(String word) {
        String checkLine = "abcdefghijklmnopqrstuvwxyz1234567890-!&";
        word = word.toLowerCase();
        boolean flag = true;
        for(int i = 0; i < word.length(); i++) {
            String letter = word.charAt(i) + "";
            if(!checkLine.contains(letter)) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    public static void searchArtistsFromFile(Api api) throws FileNotFoundException, IOException, WebApiException, InterruptedException {
        File file = new File("festival-regex-processed.txt");
        Scanner sc = new Scanner(file);
        int count = 0;
        String temp = "";
        while(sc.hasNextLine()) {
            String artist = sc.nextLine();
            Thread.sleep(200);
            searchArtists(api, artist);
            count++;
        }
    }

    public static void searchArtists(Api api, String artist) {
        final ArtistSearchRequest artistRequest = api.searchArtists(artist).limit(10).build();

        try {
            final Page<Artist> artistSearchResult = artistRequest.get();
            final List<Artist> artists = artistSearchResult.getItems();

            if(artists.size() > 0) {
                System.out.println("Found: " + artists.get(0).getName());
                artistSet.add(artists.get(0));
            }

        } catch (Exception e) {
            //System.out.println("Something went wrong! " + e.getMessage());
        }
    }

    public static void checkArtistSet() throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter nameWriter = new PrintWriter("festival-artists-name.txt", "UTF-8");
        PrintWriter idWriter = new PrintWriter("festival-artists-id.txt", "UTF-8");
        PrintWriter artistWriter = new PrintWriter("festival-artists-details.txt", "UTF-8");
        Set<String> artistName = new HashSet<String>();
        Set<String> artistId = new HashSet<String>();
        Set<String> artistDetail = new HashSet<String>();
        for(Artist artist: artistSet) {
            artistName.add(artist.getName());
            artistId.add(artist.getId());
            artistDetail.add(artist.getId() + ":" + artist.getName());
        }
        System.out.println("Here are the Artists that were found: ");
        for (String artist: artistName) {
            nameWriter.println(artist);
            System.out.println(artist);
        }

        for(String id: artistId) {
            idWriter.println(id);
        }

        for(String detail: artistDetail) {
            artistWriter.println(detail);
        }

        nameWriter.close();
        idWriter.close();
        artistWriter.close();
    }

    public static void findTopSongs(Api api) throws FileNotFoundException, WebApiException, IOException {
        File file = new File("festival-artists-details.txt");
        Scanner sc = new Scanner(file);
        Map<String, List<Track>> topSongs = new HashMap<String, List<Track>>();
        while(sc.hasNextLine()) {
            String artist = sc.nextLine();
            String[] details = artist.split(":");
            String id = details[0];
            String name = details[1];
            List<Track> topTracks = listTopSongs(api, id);
            topSongs.put(name, topTracks);
        }

        System.out.println("Here is a list of songs you should listen: ");
        for(String artist: topSongs.keySet()) {
            System.out.print(artist + " Top songs- [");
            for(Track track: topSongs.get(artist)) {
                System.out.print(track.getName());
                System.out.print(", ");
            }
            System.out.print("]");
            System.out.println();
        }
    }

    public static List<Track> listTopSongs(Api api, String artistId) throws WebApiException, IOException {
        final TopTracksRequest topTracksRequest = api.getTopTracksForArtist(artistId, "SE").build();
        final List<Track> trackList = topTracksRequest.get();
        return trackList;
    }
}
