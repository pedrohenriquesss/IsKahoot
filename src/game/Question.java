package game;

import java.util.List;

// representa uma pergunta do quiz
public class Question {

    private String question;
    private int correct;
    private int points;
    private List<String> options;
    private QuestionType type;

    public Question(String question, int correct, int points, List<String> options, QuestionType type) {
        this.question = question;
        this.correct = correct;
        this.points = points;
        this.options = options;
        this.type = type;
    }

    public String getQuestion() {
        return question;
    }

    public int getCorrect() {
        return correct;
    }

    public int getPoints() {
        return points;
    }

    public List<String> getOptions() {
        return options;
    }

    public QuestionType getType() {
        return type;
    }

    public boolean isCorrectOption(int option) {
        return option == correct;
    }
}