package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.api.core.ApiFuture;
import com.google.cloud.ServiceOptions;
import com.google.cloud.bigquery.storage.v1.*;
import com.google.protobuf.Descriptors;
import org.json.JSONArray;
import org.json.JSONObject;
 
import java.io.IOException;
import java.time.Instant;



@SpringBootApplication
@RestController
public class Application {

    static class Self {
        public String href;
    }

    static class Links {
        public Self self;
    }

    static class PlayerState {
        public Integer x;
        public Integer y;
        public String direction;
        public Boolean wasHit;
        public Integer score;
    }

    static class Arena {
        public List<Integer> dims;
        public Map<String, PlayerState> state;
    }

    static class ArenaUpdate {
        public Links _links;
        public Arena arena;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    @GetMapping("/")
    public String index() {
        return "Let the battle begin!";
    }

    @PostMapping("/**")
    public String index(@RequestBody ArenaUpdate arenaUpdate) {
        writeCommittedStream.send(arenaUpdate.arena);

        String me = arenaUpdate._links.self.href;
        PlayerState myState = arenaUpdate.arena.state.get(me);
        if(wasHit(arenaUpdate)) {
            String[] commands = new String[] { "F", "R", "L", "T" };
            int i = new Random().nextInt(3);
            return commands[i];
        } else if (isAnyoneInRange(arenaUpdate)) {
            return "T";
        } else {
            String[] commands = new String[] { "F", "R", "L", "T" };
            int i = new Random().nextInt(3);
            return commands[i];
        }
    }

    boolean wasHit(ArenaUpdate arenaUpdate) {
        String me = arenaUpdate._links.self.href;
        PlayerState myState = arenaUpdate.arena.state.get(me);
        if(myState.wasHit) {
            System.out.print("I was hit. Escaping");    
            return true;
        }
        return false;
    }

    boolean isAnyoneInRange(ArenaUpdate arenaUpdate) {
        String me = arenaUpdate._links.self.href;
        PlayerState myState = arenaUpdate.arena.state.get(me);
        System.out.println("My position is x=" + myState.x +",y="+myState.y+" heading "+ myState.direction);
        if(myState.wasHit) {
            System.out.print("I was hit. Escaping");    
            return false;
        }
        
        switch (myState.direction) {
        case "N":
            System.out.println("Heading north");
            return anyOneInPosition(myState.x , myState.y + 1, arenaUpdate)
                    || anyOneInPosition(myState.x , myState.y + 2, arenaUpdate)
                    || anyOneInPosition(myState.x , myState.y + 3, arenaUpdate);
        case "S":
            System.out.println("Heading south");
            return anyOneInPosition(myState.x , myState.y- 1, arenaUpdate)
                    || anyOneInPosition(myState.x, myState.y-2 , arenaUpdate)
                    || anyOneInPosition(myState.x, myState.y-3, arenaUpdate);
        case "W":
            System.out.println("Heading west");
            return anyOneInPosition(myState.x -1 , myState.y , arenaUpdate)
                    || anyOneInPosition(myState.x -2 , myState.y , arenaUpdate)
                    || anyOneInPosition(myState.x -3, myState.y , arenaUpdate);
        case "E":
            System.out.println("Heading east");
            return anyOneInPosition(myState.x + 1, myState.y , arenaUpdate)
                    || anyOneInPosition(myState.x + 2, myState.y , arenaUpdate)
                    || anyOneInPosition(myState.x + 1, myState.y , arenaUpdate);

        default:
            System.out.println("Unsexpected direction " + myState.direction);
            return false;
        }
    }

    boolean anyOneInPosition(int x, int y, ArenaUpdate arenaUpdate) {
        boolean res =  arenaUpdate.arena.state.values().stream().anyMatch(p -> p.x == x && p.y == y);
        if(res) {
            System.out.println("Found someone at x="+x+", y="+ y);
        } 
        return res;
    }

    static class WriteCommittedStream {

        final JsonStreamWriter jsonStreamWriter;
    
        public WriteCommittedStream(String projectId, String datasetName, String tableName) throws IOException, Descriptors.DescriptorValidationException, InterruptedException {
    
          try (BigQueryWriteClient client = BigQueryWriteClient.create()) {
    
            WriteStream stream = WriteStream.newBuilder().setType(WriteStream.Type.COMMITTED).build();
            TableName parentTable = TableName.of(projectId, datasetName, tableName);
            CreateWriteStreamRequest createWriteStreamRequest =
                    CreateWriteStreamRequest.newBuilder()
                            .setParent(parentTable.toString())
                            .setWriteStream(stream)
                            .build();
    
            WriteStream writeStream = client.createWriteStream(createWriteStreamRequest);
    
            jsonStreamWriter = JsonStreamWriter.newBuilder(writeStream.getName(), writeStream.getTableSchema()).build();
          }
        }
    
        public ApiFuture<AppendRowsResponse> send(Arena arena) {
          Instant now = Instant.now();
          JSONArray jsonArray = new JSONArray();
    
          arena.state.forEach((url, playerState) -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("x", playerState.x);
            jsonObject.put("y", playerState.y);
            jsonObject.put("direction", playerState.direction);
            jsonObject.put("wasHit", playerState.wasHit);
            jsonObject.put("score", playerState.score);
            jsonObject.put("player", url);
            jsonObject.put("timestamp", now.getEpochSecond() * 1000 * 1000);
            jsonArray.put(jsonObject);
          });
    
          return jsonStreamWriter.append(jsonArray);
        }
    
      }
    
      final String projectId = ServiceOptions.getDefaultProjectId();
      final String datasetName = "snowball";
      final String tableName = "events";
    
      final WriteCommittedStream writeCommittedStream;
    
      public Application() throws Descriptors.DescriptorValidationException, IOException, InterruptedException {
        writeCommittedStream = new WriteCommittedStream(projectId, datasetName, tableName);
      }
    
}
