package com.docker.sandbox.testcase;

import com.docker.sandbox.verdict.Verdict;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TestCaseResponse {
    int testCaseNo;
    Verdict verdict;
    String submissionId;
    String userName;
}
