package firstblood;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import java.lang.Runtime;
import java.io.FileReader;

public class FirstBlood {

   private final String USER_AGENT = "Mozilla/5.0";
   private final String STEAM_API_KEY = "ABE6F1693EC796FAD97AC9C2EBE131C4";
   
   public class MatchHistory {
      
      @SerializedName("result")
      Result result;
      
      public class Result {
         @SerializedName("status")
         int status;
         
         @SerializedName("matches")
         ArrayList<MatchDetails> matches;
         
         public class MatchDetails {
            @SerializedName("match_id")
            long match_id;
            
            @SerializedName("match_seq_num")
            long match_seq_num;

            @SerializedName("start_time")
            long start_time;
            
            @SerializedName("lobby_type")
            long lobby_type;

            @SerializedName("players")
            ArrayList<PlayerDetails> players;
            
            public class PlayerDetails {
               @SerializedName("account_id")
               long account_id;

               @SerializedName("player_slot")
               int player_slot;
               
               @SerializedName("hero_id")
               int hero_id;
            }
         }         
      }
   }
   
   public BufferedReader loadURL(String urlString) throws Exception {
      URL url = new URL(urlString);
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent", USER_AGENT);
      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
         System.out.println("HTTP GET request has failed. Response Code : " + responseCode);
         return null;
      }

      return new BufferedReader(new InputStreamReader(connection.getInputStream()));
   }
   
   public String stringFromReader(BufferedReader reader) throws Exception {
      StringBuilder builder = new StringBuilder();
      for (String line = null; (line = reader.readLine()) != null;) {
          builder.append(line).append("\n");
      }
      return builder.toString();
   }
   
   public String downloadWebPage(String Url) throws Exception {
      String cmd = "e:/code/dota/firstblood/tools/wget.exe --no-check-certificate -O e:/temp/replayurl " + Url;
      Process p = Runtime.getRuntime().exec(cmd);
      p.waitFor();

      BufferedReader reader = new BufferedReader(new FileReader("e:/temp/replayurl"));
      String         line = null;
      StringBuilder  stringBuilder = new StringBuilder();
      String         ls = System.getProperty("line.separator");

      while ((line = reader.readLine()) != null) {
          stringBuilder.append(line);
          stringBuilder.append(ls);
      }
      reader.close();

      return stringBuilder.toString();
   }
   
   // retrieve at least minMatchesNum
   public ArrayList<MatchHistory.Result.MatchDetails> getMatchHistory() throws Exception {
      
      ArrayList<MatchHistory.Result.MatchDetails> result = new ArrayList<MatchHistory.Result.MatchDetails>();
      
      final int maxMatchesNum = 1000; // maximum number of matches to read, usually it seems to be around 500 
      long lastMatchId = 0;
      while (result.size() < maxMatchesNum) {
         String urlString = "https://api.steampowered.com/IDOTA2Match_570/GetMatchHistory/V001/?key=" + STEAM_API_KEY;
         if (lastMatchId != 0) urlString += "&start_at_match_id=" + lastMatchId;  
         
         BufferedReader reader = loadURL(urlString);
         if (reader == null) return result;
         
         Gson gson = new Gson();
         MatchHistory matchHistory = gson.fromJson(reader, MatchHistory.class);
         reader.close();
         
         if (matchHistory.result.matches.isEmpty()) return result; // no more matches available

         lastMatchId = matchHistory.result.matches.get(matchHistory.result.matches.size() - 1).match_id - 1; // starting match id for the next batch of matches
         result.addAll(matchHistory.result.matches);
      }
      
      return result;
   }
   
   public class MatchDetails {
      @SerializedName("result")
      Result result;
      
      public class Result {
         @SerializedName("cluster")
         long cluster;
      }
   }
   
   public MatchDetails getMatchDetails(long matchId) throws Exception {
      String urlString = "https://api.steampowered.com/IDOTA2Match_570/GetMatchDetails/V001/?key=" + STEAM_API_KEY + "&match_id=" + matchId;
      BufferedReader reader = loadURL(urlString);
      if (reader == null) return null;
      
      System.out.println("Match details: " + stringFromReader(reader));
      return null;
   }
   
   public String getReplayURL(long matchId) throws Exception {
      String urlString = "https://www.rjackson.me/tools/matchurls?matchid=" + matchId;
      String replaySaltPage = downloadWebPage(urlString);
      int start = replaySaltPage.indexOf("http://replay");
      if (start < 0) return "";
      int end = replaySaltPage.indexOf('\"', start + 1);
      if (end < 0) return "";
      return replaySaltPage.substring(start, end);
   }
   
   public void downloadReplay(String url) throws Exception {
      int start = url.lastIndexOf('/');
      if (start < 0) return;
      String fileName = url.substring(start);
      
      String cmd = "e:/code/dota/firstblood/tools/wget.exe -T 10 -O e:/temp/replays" + fileName + " " + url;
      Process p = Runtime.getRuntime().exec(cmd);
      p.waitFor();
   }
   
   public void run() throws Exception {
      ArrayList<MatchHistory.Result.MatchDetails> matches = getMatchHistory();
      System.out.println("Total number of matches = " + matches.size());
      if (matches.isEmpty()) return;
      for (MatchHistory.Result.MatchDetails match : matches) {
         String replayURL = getReplayURL(match.match_id);
         System.out.println("Getting replay for URL = " + replayURL);
         downloadReplay(replayURL);
      }
   }
}
