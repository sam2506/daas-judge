package com.docker.sandbox.submission;

import com.docker.sandbox.verdict.Verdict;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SubmissionResponse {

    private String submissionId;
    private String userName;
    private String contestId;
    private Verdict verdict;
}
