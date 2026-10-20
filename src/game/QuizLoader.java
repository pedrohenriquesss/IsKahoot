package game;

import java.io.*;
import java.util.*;

import com.google.gson.Gson;

// lê o quiz a partir do ficheiro json
public class QuizLoader {

    public static Quiz loadQuiz(String filePath) throws IOException {
        Gson gson = new Gson();
        Quiz quiz;
        // converte json -> objeto Quiz
        try (FileReader reader = new FileReader(filePath)) {
            quiz = gson.fromJson(reader, Quiz.class);
        }

        // verifica se o quiz é válido
        if (quiz == null ||
                quiz.getQuestions() == null ||
                quiz.getQuestions().isEmpty()) {
            throw new IOException("Quiz inválido");
        }

        List<Question> questions = new ArrayList<>();
        // alterna as perguntas individuais e as de equipa
        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            Question q = quiz.getQuestions().get(i);
            QuestionType type = (i % 2 == 0) ? QuestionType.INDIVIDUAL : QuestionType.TEAM;
            questions.add(new Question(q.getQuestion(), q.getCorrect(), q.getPoints(), q.getOptions(), type));
        }
        return new Quiz(quiz.getName(), questions);
    }
    
}