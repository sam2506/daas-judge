package com.docker.sandbox.submission.entities;

import com.docker.sandbox.language.Language;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SubmissionRequest {

    private String submissionId;
    public String problemId;
    public String userName;
    public String code;
    public Language languageId;
    public String contestId;
    public Date timestamp;
}
