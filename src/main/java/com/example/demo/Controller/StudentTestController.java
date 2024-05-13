package com.example.demo.Controller;

import com.example.demo.Model.QuizInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class StudentTestController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/student-tests")
    public String showStudentTestPage(@RequestParam(name = "courseId", required = false) String courseId,
                                      @RequestParam(name = "studentId", required = false) String studentId,
                                      Model model) {
        if (courseId != null && studentId != null) {
            model.addAttribute("courseId", courseId);
            model.addAttribute("studentId", studentId);
        }
        return "student-tests";
    }

    @PostMapping("/student-tests")
    @ResponseBody
    public List<QuizInfo> getStudentAttempts(@RequestParam("courseId") String courseId,
                                             @RequestParam("studentId") String studentId) {

        // Replace with your Moodle token and domain
        String token = "3cc2d13c0b1ee13c59cb3757239b30e5";
        String domainName = "http://localhost/demo.ngockhanh.vn";
        String getQuizzesFunction = "mod_quiz_get_quizzes_by_courses";
        String getAttemptsFunction = "mod_quiz_get_user_attempts";

        List<QuizInfo> quizList = new ArrayList<>(); // Danh sách lưu trữ thông tin của các bài kiểm tra
        try {
            // Construct the URL to get quizzes
            String getQuizzesUrl = domainName + "/webservice/rest/server.php" +
                    "?wstoken=" + token +
                    "&wsfunction=" + getQuizzesFunction +
                    "&moodlewsrestformat=json" +
                    "&courseids[0]=" + courseId;

            // Call the API to get quizzes
            String quizzesResponse = restTemplate.getForObject(getQuizzesUrl, String.class);
            System.out.println(quizzesResponse);

            // Parse JSON response to get quizzes
            JSONObject responseJson = new JSONObject(quizzesResponse);
            JSONArray quizzesArray = responseJson.getJSONArray("quizzes");

            // Check if there are quizzes
            if (quizzesArray.length() > 0) {
                // Iterate through quizzes
                for (int i = 0; i < quizzesArray.length(); i++) {
                    JSONObject quiz = quizzesArray.getJSONObject(i);
                    Integer quizId = quiz.getInt("id");
                    String quizName = quiz.getString("name");

                    // Create a QuizInfo object and add it to the list
                    QuizInfo quizInfo = new QuizInfo(quizId, quizName);
                    quizList.add(quizInfo);

                    System.out.println("Quiz ID: " + quizId + ", Quiz Name: " + quizName);
                }
            } else {
                System.out.println("No quizzes found.");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("Error parsing JSON response: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e.getMessage());
        }

        // Return the list of QuizInfo objects
        return quizList;
    }

}
