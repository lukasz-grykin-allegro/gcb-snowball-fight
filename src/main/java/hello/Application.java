package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

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
        if (isAnyoneInRange(arenaUpdate)) {
            return "T";
        } else {
            String[] commands = new String[] { "F", "R", "L", "T" };
            int i = new Random().nextInt(3);
            return commands[i];
        }
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

}
