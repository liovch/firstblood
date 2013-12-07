package firstblood;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;

public class FirstBlood {

   private final String USER_AGENT = "Mozilla/5.0";
   private final String STEAM_API_KEY = "ABE6F1693EC796FAD97AC9C2EBE131C4";
   
   public class MatchHistory {
      
      @SerializedName("result")
      public Result result;
      
      public class Result {
         @SerializedName("status")
         public int status;
         
         @SerializedName("matches")
         public ArrayList<MatchDetails> matches;
         
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
   
   // retrieve at least minMatchesNum
   public ArrayList<MatchHistory.Result.MatchDetails> getMatchHistory() throws Exception {
      
      ArrayList<MatchHistory.Result.MatchDetails> result = new ArrayList<MatchHistory.Result.MatchDetails>();
      
      final int maxMatchesNum = 1000; // maximum number of matches to read, usually it seems to be around 500 
      long lastMatchId = 0;
      while (result.size() < maxMatchesNum) {
         String urlString = "https://api.steampowered.com/IDOTA2Match_570/GetMatchHistory/V001/?key=" + STEAM_API_KEY;
         if (lastMatchId != 0) urlString += "&start_at_match_id=" + lastMatchId;  
         
         URL url = new URL(urlString);
         HttpURLConnection connection = (HttpURLConnection)url.openConnection();
         connection.setRequestMethod("GET");
         connection.setRequestProperty("User-Agent", USER_AGENT);
         int responseCode = connection.getResponseCode();
         if (responseCode != 200) {
            System.out.println("HTTP GET request has failed. Response Code : " + responseCode);
            return result;
         }

         BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
         Gson gson = new Gson();
         MatchHistory matchHistory = gson.fromJson(reader, MatchHistory.class);
         reader.close();
         
         // find last match id
         if (matchHistory.result.matches.isEmpty()) {
            System.out.println("Empty array of matches received");
            return result;
         }
         lastMatchId = matchHistory.result.matches.get(matchHistory.result.matches.size() - 1).match_id - 1; // starting match id for the next batch of matches
         result.addAll(matchHistory.result.matches);
      }
      
      return result;
   }
   
   public void run() throws Exception {
      ArrayList<MatchHistory.Result.MatchDetails> matches = getMatchHistory();
      System.out.println("Total number of matches = " + matches.size());
   }
  
}