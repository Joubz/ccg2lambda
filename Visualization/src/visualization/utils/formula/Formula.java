package visualization.utils.formula;

import visualization.utils.formula.node.Actor;
import visualization.utils.formula.node.BaseNode;
import visualization.utils.formula.node.Conjunction;
import visualization.utils.formula.node.Event;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a formula output by ccg2lambda.
 * Also encapsulates the base lambda and the sentence said lambda was generated from.
 *
 * @author Gaétan Basile
 */
public class Formula {
    /**
     * The pattern for recognizing a variable name.
     */
    private static final Pattern varNamePattern = Pattern.compile("_\\w+");
    /**
     * The pattern for recognizing a variable id.
     */
    private static final Pattern varIdPattern = Pattern.compile("(\\w+\\.)|(\\(\\w+,\\w+\\))|(\\(\\w+\\))");

    /**
     * The base formula this formula was created from.
     */
    private String lambda;
    /**
     * The sentence which is represented by this formula.
     */
    private String sentence;
    /**
     * The actors of this formula. They are the ones to initiate or to endure events.
     */
    private Map<String, Actor> actors = new HashMap<>();
    /**
     * The events of this formula. They represent actions and have effect on the actors.
     */
    private Map<String, Event> events = new HashMap<>();
    /**
     * The conjunctions of this formula. They link other items or provide additionnal info (time, place...)
     */
    private Map<String, Conjunction> conjunctions = new HashMap<>();

    /**
     * Private constructor for a formula, they must be created using the static parse method.
     * See {@link #parse(String, String)}
     */
    private Formula() {
    }

    /**
     * Private constructor for a formula. Initializes the lambda and sentence fields.
     *
     * @param lambda   the formula generated by ccg2lambda
     * @param sentence the base sentence the lambda was generated from
     */
    private Formula(String lambda, String sentence) {
        this.lambda = lambda;
        this.sentence = sentence;
    }

    /**
     * Simplifies the lambda. Replaces some chars and deletes tautologies.
     *
     * @param lambda the lambda to simplify
     * @return the simplified lambda
     */
    public static String simplifyLambda(String lambda) {
        String correctedLambda = lambda.replace("&amp;", "&");
        StringBuilder simpLambda = new StringBuilder();
        Scanner sc = new Scanner(correctedLambda);
        sc.useDelimiter("\\s*& TrueP\\s*|\\s*TrueP &\\s*");
        String part;
        do {
            part = sc.next();
            simpLambda.append(' ').append(part);
        } while (sc.hasNext());
        return simpLambda.toString();
    }

    /**
     * Parses the lambda and creates the actors and the events.
     */
    public static Formula parse(String lambda, String sentence) {
        Formula newFormula = new Formula(lambda, sentence);

        String token;
        String varId;
        String varName;

        int eventNumber = 0;
        int conjunctionNumber = 0;

        Scanner sc = new Scanner(lambda);
        sc.useDelimiter("&");
        do {
            token = sc.next();
            // if the token is a &, do not do anything
            if (!"&".equals(token)) {
                // if the token is an actor declaration, add it to the actors map
                if (token.matches(".*exists \\w+\\..*")) {
                    Matcher m = varIdPattern.matcher(token);
                    Matcher n = varNamePattern.matcher(token);
                    if (m.find() && n.find()) {
                        varId = m.group();
                        varId = varId.substring(0, varId.length() - 1);
                        varName = n.group().substring(1);

                        newFormula.actors.put(varId, new Actor(varId, varName));
                    }
                }
                // if the token is an event, add it to the events map
                else if (token.matches(".*Prog\\(.*")) {
                    Matcher m = varNamePattern.matcher(token);
                    Matcher n = varIdPattern.matcher(token);
                    String actorId;

                    if (m.find() && n.find()) {
                        varName = m.group().substring(1);
                        varId = "e" + eventNumber;
                        actorId = n.group();
                        actorId = actorId.substring(1, actorId.length() - 1);
                        // if the actorId starts with "_" then it is a proper noun and we have to search for it in the map
                        if (actorId.startsWith("_")) {
                            actorId = newFormula.getActorByName(actorId.substring(1));
                        }
                        eventNumber++;

                        newFormula.events.put(varId, new Event(varId, varName, newFormula.actors.get(actorId)));
                    }
                }
                // if it is not anything else, then it is a conjunction, we need to add it to the conjunctions map
                else {
                    String joinedId;
                    Matcher m = varNamePattern.matcher(token);
                    Matcher n = varIdPattern.matcher(token);

                    if (m.find() && n.find()) {
                        varName = m.group().substring(1);
                        varId = "c" + conjunctionNumber;
                        conjunctionNumber++;

                        joinedId = n.group();
                        joinedId = joinedId.substring(1, joinedId.length() - 1);
                        String[] ids = joinedId.split(",");
                        List<Actor> joined = new ArrayList<>();
                        for (String id : ids) {
                            if (!(id.startsWith("e") || id.startsWith("c"))) {
                                // if the id stats with "_" then it is a proper noun and we have to search for it in the map
                                if (id.startsWith("_")) {
                                    id = newFormula.getActorByName(id.substring(1));
                                }
                                // else we just add it to the list of actors joined
                                joined.add(newFormula.actors.get(id));
                            }
                        }
                        newFormula.conjunctions.put(varId, new Conjunction(varId, varName, joined.toArray(new BaseNode[0])));
                    }
                }
            }
        } while (sc.hasNext());
        return newFormula;
    }

    /**
     * Get the key to an actor by searching for its name.
     *
     * @param name the name of the actor we search for.
     * @return the key if found; null otherwise
     */
    private String getActorByName(String name) {
        for (Map.Entry<String, Actor> entry : actors.entrySet()) {
            if (entry.getValue().getName().equals(name)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Get the base lambda for this formula.
     *
     * @return a String containing the base lambda
     */
    public String getLambda() {
        return lambda;
    }

    /**
     * Get the sentence this formula was made from.
     *
     * @return a String containing the base sentence
     */
    public String getSentence() {
        return sentence;
    }

    /**
     * Get the actors of the formula.
     *
     * @return a map containing all the actors by id->value
     */
    public Map<String, Actor> getActors() {
        return actors;
    }

    /**
     * Get the events of the formula.
     *
     * @return a map containing all the events by actor_executing_the_event->value
     */
    public Map<String, Event> getEvents() {
        return events;
    }

    /**
     * Get the conjunctions of the formula.
     *
     * @return a map containing all conjunctions by actors_and_events_whom_this_applies->value
     */
    public Map<String, Conjunction> getConjunctions() {
        return conjunctions;
    }
}