package game;

import java.util.List;

public class Quiz {

    private String name;
    private List<Question> questions; // perguntas do quiz

    public Quiz(String name, List<Question> questions) {
        this.name = name;
        this.questions = questions;
    }

    public String getName() {
        return name;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public int size() {
        if (questions == null)
            return 0;

        return questions.size();
    }

    @Override
    public String toString() {
        return name + " " + questions;
    }
}