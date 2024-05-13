package com.example.demo.Controller;

import com.example.demo.Model.AttemptInfo;
import com.example.demo.Model.QuestionDetail;
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

import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Controller
public class StudentAttemptsController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/student-attempts")
    public String showStudentAttemptsPage(@RequestParam(name = "studentId", required = false) String studentId,
                                          @RequestParam(name = "quizId", required = false) String quizId,
                                          Model model) {

        if (studentId != null && quizId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("quizId", quizId);
        }
        return "student-attempts";
    }

    @PostMapping("/student-attempts")
    @ResponseBody
    public List<AttemptInfo> getStudentAttempts(@RequestParam("studentId") String studentId,
                                                @RequestParam("quizId") String quizId) {

        // Replace with your Moodle token and domain
        String token = "3cc2d13c0b1ee13c59cb3757239b30e5";
        String domainName = "http://localhost/demo.ngockhanh.vn";
        String getAttemptsFunction = "mod_quiz_get_user_attempts";

        // List to store attempt information
        List<AttemptInfo> attemptList = new ArrayList<>();

        try {
            // Construct the URL to get attempts
            String getAttemptsUrl = domainName + "/webservice/rest/server.php" +
                    "?wstoken=" + token +
                    "&wsfunction=" + getAttemptsFunction +
                    "&moodlewsrestformat=json" +
                    "&quizid=" + quizId +
                    "&userid=" + studentId;

            // Call the API to get attempts
            String attemptsResponse = restTemplate.getForObject(getAttemptsUrl, String.class);

            // Parse JSON response to get attempts
            JSONObject responseJson = new JSONObject(attemptsResponse);
            JSONArray attemptsArray = responseJson.getJSONArray("attempts");

            // Check if there are attempts
            if (attemptsArray.length() > 0) {
                // Iterate through attempts
                for (int i = 0; i < attemptsArray.length(); i++) {
                    JSONObject attempt = attemptsArray.getJSONObject(i);
                    Integer attemptId = attempt.getInt("id");
                    Double score = attempt.getDouble("sumgrades");

                    // Create an AttemptInfo object
                    AttemptInfo attemptInfo = new AttemptInfo(attemptId, score);

                    // Get details for each attempt
                    List<QuestionDetail> questionDetails = getAttemptDetails(attemptId);
                    attemptInfo.setQuestionDetails(questionDetails);

                    // Add AttemptInfo object to the list
                    attemptList.add(attemptInfo);
                }
            } else {
                System.out.println("No attempts found for the student in this quiz.");
            }

        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("Error parsing JSON response: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e.getMessage());
        }

        // Return the list of AttemptInfo objects
        return attemptList;
    }

    // Method to get details for each attempt
// Method to get details for each attempt
    private List<QuestionDetail> getAttemptDetails(Integer attemptId) {
        List<QuestionDetail> questionDetails = new ArrayList<>();
        try {
            // Replace with your Moodle token and domain
            String token = "3cc2d13c0b1ee13c59cb3757239b30e5";
            String domainName = "http://localhost/demo.ngockhanh.vn";
            String getAttemptReviewFunction = "mod_quiz_get_attempt_review";

            // Construct the URL to get attempt review
            String getAttemptReviewUrl = domainName + "/webservice/rest/server.php" +
                    "?wstoken=" + token +
                    "&wsfunction=" + getAttemptReviewFunction +
                    "&moodlewsrestformat=json" +
                    "&attemptid=" + attemptId;

            // Call the API to get attempt review
            String attemptReviewResponse = restTemplate.getForObject(getAttemptReviewUrl, String.class);

            System.out.println(getAttemptReviewUrl);

            // Parse JSON response to get attempt review
            JSONObject reviewJson = new JSONObject(attemptReviewResponse);
            JSONArray questionArray = reviewJson.getJSONArray("questions");

            // Process question array to build QuestionDetail objects
            for (int i = 0; i < questionArray.length(); i++) {
                JSONObject questionJson = questionArray.getJSONObject(i);

                // Extract necessary information from questionJson and create QuestionDetail objects
                QuestionDetail questionDetail = new QuestionDetail();

                // Get HTML from JSON and clean it using Jsoup
                String questionHtml = questionJson.optString("html", ""); // Lấy trường HTML từ đối tượng JSON
                Document doc = Jsoup.parse(questionHtml);

                // Lấy các phần tử trong HTML và trích xuất thông tin
                String questionText = doc.select(".qtext").text(); // Lấy nội dung của câu hỏi
                String studentResponse = doc.select(".answer").text(); // Lấy câu trả lời của sinh viên
                String correctResponse = doc.select(".feedback").text(); // Lấy câu trả lời đúng

                // Thiết lập các giá trị đã lấy được vào đối tượng QuestionDetail
                questionDetail.setQuestionText(questionText);
                questionDetail.setStudentResponse(studentResponse);
                questionDetail.setCorrectResponse(correctResponse);

                // Thêm QuestionDetail vào danh sách
                questionDetails.add(questionDetail);
                System.out.println(questionDetails);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("Error parsing JSON response: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e.getMessage());
        }
        return questionDetails;
    }
}

