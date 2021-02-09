package com.docker.sandbox.judge;


import com.docker.sandbox.submission.entities.SubmissionRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JudgeRequest {
    SubmissionRequest submissionRequest;
    Double timeLimit;
    int memoryLimit;
}
