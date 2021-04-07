package com.docker.sandbox.judge;

import com.docker.sandbox.amazons3.AmazonS3Service;
import com.docker.sandbox.compiler.CompilationResponse;
import com.docker.sandbox.judge.entities.CompilerDetails;
import com.docker.sandbox.submission.SubmissionRequest;
import com.docker.sandbox.testcase.TestCaseResponse;
import com.docker.sandbox.util.UnzipFile;
import com.docker.sandbox.verdict.Verdict;
import lombok.Setter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.concurrent.*;

@Component
@ConfigurationProperties(prefix = "completed.testcases")
@Setter
@RestController
public class JudgeController {

    private String exchange;
    private String routingKey;

    private final String PROJECT_DIRECTORY = System.getProperty("user.dir");
    private final String CURRENT_DIRECTORY =  System.getProperty("user.dir") +
            "/src/main/java/com/docker/sandbox/judge";
    private final String RUN_SANDBOX_SCRIPT_PATH = CURRENT_DIRECTORY + "/RunSandbox.sh";
    private final String EXECUTE_TEST_SCRIPT_PATH = CURRENT_DIRECTORY + "/ExecuteTest.sh";
    private final String COMPILE_SCRIPT_PATH = CURRENT_DIRECTORY + "/Compile.sh";
    private final String[][] COMPILERS = {
            {"CPP", "g++", ".cpp", "a.out"},
            {"C", "gcc", ".c", "a.out"},
            {"PYTHON", "python", ".py", ""},
    };
    private final int ACCEPTED_EXIT_STATUS = 0;
    private final int WRONG_ANSWER_EXIT_STATUS = 1;
    private final int TIMEOUT_EXIT_STATUS = 124;
    private final int RUNTIME_ERROR_EXIT_STATUS = 134;
    private final int MEMORY_LIMIT_EXCEEDED_EXIT_STATUS = 139;

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private AmazonS3Service amazonS3Service;

    public static void printResults(Process proc) throws IOException {
        try
        {
            InputStream stdin = proc.getInputStream();
            InputStreamReader isr1 = new InputStreamReader(stdin);
            BufferedReader br1 = new BufferedReader(isr1);
            String line1 = null;
            System.out.println("<OUTPUT>");
            while ( (line1 = br1.readLine()) != null)
                System.out.println(line1);
            System.out.println("</OUTPUT>");
            InputStream stderr = proc.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            System.out.println("<ERROR>");
            while ( (line = br.readLine()) != null)
                System.out.println(line);
            System.out.println("</ERROR>");
            int exitVal = proc.waitFor();
            System.out.println("Process exitValue: " + exitVal);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private CompilerDetails getCompilerDetails(String language) {
        CompilerDetails compilerDetails = new CompilerDetails();
        for(String[] compiler : COMPILERS) {
            if(compiler[0].equals(language)) {
                compilerDetails.setCompilerName(compiler[1]);
                compilerDetails.setExtension(compiler[2]);
                compilerDetails.setExecutable(compiler[3]);
            }
        }
        return compilerDetails;
    }

    private String getOutputOfProcess(Process process) {
        InputStream stdin = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(stdin);
        BufferedReader br = new BufferedReader(isr);
        String output = "";
        try {
            output = br.readLine();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    private int getNoOfTestCases(String testsFolderName) {
        int noOfTestCases = 0;
        try {
            String[] cmd = {
                "/bin/sh",
                "-c",
                "ls " + PROJECT_DIRECTORY + "/" + testsFolderName + "/input" + " " + "| grep 'input-.*.txt' | wc -l"
            };
            Process process = Runtime.getRuntime().exec(cmd);
            noOfTestCases = Integer.parseInt(getOutputOfProcess(process));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return noOfTestCases;
    }

    private boolean compileCode(SubmissionRequest submissionRequest,
                               CompilerDetails compilerDetails, String containerId) {
        String userName = submissionRequest.getUserName();
        boolean isCompilationSuccessful = false;
        try {
            Runtime.getRuntime().exec("chmod +x " + COMPILE_SCRIPT_PATH);
            String[] compileScript = {
                    "sh", COMPILE_SCRIPT_PATH,
                    compilerDetails.getCompilerName(),
                    "" + userName + compilerDetails.getExtension()
                    , containerId
                    , "/home/" + submissionRequest.getSubmissionId()
                    , compilerDetails.getExecutable()
            };
            Process process = Runtime.getRuntime().exec(compileScript);
            printResults(process);
            int exitStatus = process.waitFor();
            if(exitStatus == 0) {
                isCompilationSuccessful = true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return isCompilationSuccessful;
    }

    private Verdict executeTest(SubmissionRequest submissionRequest,
                                JudgeRequest judgeRequest,
                                CompilerDetails compilerDetails,
                                String containerId, String testCaseNo) {
        String userName = submissionRequest.getUserName();
        Verdict verdict = Verdict.MLE;
        try {
            Runtime.getRuntime().exec("chmod +x " + EXECUTE_TEST_SCRIPT_PATH);
            String[] executeTestScript = {"sh", EXECUTE_TEST_SCRIPT_PATH,
                    compilerDetails.getCompilerName(),
                    "" + userName + compilerDetails.getExtension()
                    , compilerDetails.getExecutable()
                    , containerId, testCaseNo
                    , "/home/" + submissionRequest.getSubmissionId()
                    , judgeRequest.getTimeLimit().toString()
                    , "" + judgeRequest.getMemoryLimit()
            };
            Process process = Runtime.getRuntime().exec(executeTestScript);
            printResults(process);
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
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return verdict;
    }

    private String createSandbox(SubmissionRequest submissionRequest) {
        String[] sandboxRunnerScript = {"sh", RUN_SANDBOX_SCRIPT_PATH,
                PROJECT_DIRECTORY + "/" + submissionRequest.getSubmissionId()
                , "/home/" + submissionRequest.getSubmissionId()
        };
        String containerId = "";
        try {
            Runtime.getRuntime().exec("chmod +x " + RUN_SANDBOX_SCRIPT_PATH);
            Process process = Runtime.getRuntime().exec(sandboxRunnerScript);
            process.waitFor();
            containerId = getOutputOfProcess(process);
        }  catch (Throwable t) {
            t.printStackTrace();
        }
        return containerId;
    }

    private String downloadTestCasesOfProblem(String problemId) {
        final byte[] data = amazonS3Service.downloadFile(problemId);
        String downloadedFilePath = PROJECT_DIRECTORY + "/" + problemId + ".zip";
        File downloadedFile = new File(downloadedFilePath);
        try {
            OutputStream os = new FileOutputStream(downloadedFile);
            os.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return downloadedFilePath;
    }

    private void copyCodeToFile(String code, String codeFilePath) {
        try {
            File codeFile = new File(codeFilePath);
            FileWriter myWriter = new FileWriter(codeFilePath);
            myWriter.write(code);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(value = "/judge", method = RequestMethod.POST)
    public String judgeSubmission(@RequestBody JudgeRequest judgeRequest) {
        SubmissionRequest submissionRequest = judgeRequest.getSubmissionRequest();
        String language = submissionRequest.getLanguageId().toString();
        CompilerDetails compilerDetails = getCompilerDetails(language);
        String testCaseZipFilePath = downloadTestCasesOfProblem(submissionRequest.getProblemId());
        UnzipFile.unzipFile(testCaseZipFilePath, PROJECT_DIRECTORY + "/" +
                submissionRequest.getSubmissionId());
        copyCodeToFile(submissionRequest.getCode(), PROJECT_DIRECTORY + "/" +
                submissionRequest.getSubmissionId() + "/" +
                submissionRequest.getUserName() + compilerDetails.getExtension());
        int noOfTestCases = getNoOfTestCases(submissionRequest.getSubmissionId());
        Verdict finalVerdict = Verdict.AC;
        try {
            String containerId = createSandbox(submissionRequest);
            if(!compilerDetails.getExecutable().equals("")) {
                boolean isCompilationSuccessful =
                        compileCode(submissionRequest, compilerDetails, containerId);
                if (isCompilationSuccessful) {
                    CompilationResponse compilationResponse = new CompilationResponse(true,
                            noOfTestCases, submissionRequest.getSubmissionId(), submissionRequest.getUserName());
                    template.convertAndSend(exchange, routingKey, compilationResponse);
                    ExecutorService testExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                    CompletionService<TestCaseResponse> testCompletionService = new
                            ExecutorCompletionService<TestCaseResponse> (testExecutorService);
                    for (int i = 0; i < noOfTestCases; i++) {
                        String testCaseNo;
                        if (i <= 9)
                            testCaseNo = "0" + i;
                        else
                            testCaseNo = "" + i;
                        TestExecutor testExecutor = new TestExecutor(submissionRequest,
                                judgeRequest, compilerDetails, containerId, testCaseNo);
                        testCompletionService.submit(testExecutor);
                    }
                    for (int i = 0; i < noOfTestCases; i++) {
                        Future<TestCaseResponse> testCaseResponseFuture = testCompletionService.take();
                        TestCaseResponse testCaseResponse = testCaseResponseFuture.get();
                        if(!testCaseResponse.getVerdict().equals(Verdict.AC))
                            finalVerdict = Verdict.WA;
                        template.convertAndSend(exchange, routingKey, testCaseResponse);
                    }
                } else {
                    finalVerdict = Verdict.CE;
                    CompilationResponse compilationResponse = new CompilationResponse(false,
                            noOfTestCases, submissionRequest.getSubmissionId(), submissionRequest.getUserName());
                    template.convertAndSend(exchange, routingKey, compilationResponse);
                }
            }
            Runtime.getRuntime().exec("docker rm -f " + containerId);
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
        }
        return finalVerdict.toString();
    }
}
