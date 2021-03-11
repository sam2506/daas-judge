package com.docker.sandbox.judge;

import com.docker.sandbox.judge.entities.CompilerDetails;
import com.docker.sandbox.submission.SubmissionRequest;
import com.docker.sandbox.testcase.TestCaseResponse;
import com.docker.sandbox.verdict.Verdict;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.concurrent.Callable;

@NoArgsConstructor
@AllArgsConstructor
public class TestExecutor implements Callable<TestCaseResponse> {

    private SubmissionRequest submissionRequest;
    private JudgeRequest judgeRequest;
    private CompilerDetails compilerDetails;
    private String containerId;
    private String testCaseNo;

    @Override
    public TestCaseResponse call() throws Exception {
        final int ACCEPTED_EXIT_STATUS = 0;
        final int WRONG_ANSWER_EXIT_STATUS = 1;
        final int TIMEOUT_EXIT_STATUS = 124;
        final int RUNTIME_ERROR_EXIT_STATUS = 134;
        final String EXECUTE_TEST_SCRIPT_PATH = System.getProperty("user.dir") +
                "/src/main/java/com/docker/sandbox/judge" + "/ExecuteTest.sh";
        String userName = submissionRequest.getUserName();
        Verdict verdict = Verdict.MLE;
        TestCaseResponse testCaseResponse = new TestCaseResponse();
        try {
            Runtime.getRuntime().exec("chmod +x " + EXECUTE_TEST_SCRIPT_PATH);
            String[] executeTestScript = {"sh", EXECUTE_TEST_SCRIPT_PATH,
                    compilerDetails.getCompilerName(),
                    "" + userName + compilerDetails.getExtension()
                    , compilerDetails.getExecutable()
                    , containerId, testCaseNo
                    , "/home/" + submissionRequest.getSubmissionId()
                    , String.valueOf((int) Math.ceil(judgeRequest.getTimeLimit()))
                    , "" + judgeRequest.getMemoryLimit()
            };
            Process process = Runtime.getRuntime().exec(executeTestScript);
            JudgeController.printResults(process);
            int exitStatus = process.waitFor();
            if(exitStatus == ACCEPTED_EXIT_STATUS) {
                verdict = Verdict.AC;
            }
            if(exitStatus == WRONG_ANSWER_EXIT_STATUS) {
                verdict = Verdict.WA;
            }
            if(exitStatus == TIMEOUT_EXIT_STATUS) {
                verdict = Verdict.TLE;
            }
            if(exitStatus == RUNTIME_ERROR_EXIT_STATUS) {
                verdict = Verdict.RE;
            }
            testCaseResponse.setTestCaseNo(Integer.parseInt(testCaseNo));
            testCaseResponse.setVerdict(verdict);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return testCaseResponse;
    }
}
