package es.upm.practica.common;

import java.io.Serializable;
import java.util.Map;

// PARA LA PERSONA QUE MODIFIQUE ESTA CLASE QUE NO QUITE EL MÉTODO GETMOOD() PLIS
public class MoodResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mood;
    private Map<String, Integer> scores;
    private String explanation;

    public MoodResult(String mood, Map<String, Integer> scores, String explanation) {
        this.mood = mood;
        this.scores = scores;
        this.explanation = explanation;
    }

    public String getMood() {
        return mood;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public String toString() {
        return "MoodResult{" +
                "mood='" + mood + '\'' +
                ", scores=" + scores +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}