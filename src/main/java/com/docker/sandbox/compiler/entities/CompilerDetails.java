package com.docker.sandbox.compiler.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CompilerDetails {
    String compilerName;
    String extension;
    String executable;
}
