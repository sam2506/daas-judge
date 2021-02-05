package com.docker.sandbox.compiler;


import com.docker.sandbox.output.entities.Output;
import com.docker.sandbox.submission.entities.SubmissionRequest;
import com.docker.sandbox.test.entities.Test;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CompileRequest {
    SubmissionRequest submissionRequest;
    Double timeLimit;
    int memoryLimit;
}
