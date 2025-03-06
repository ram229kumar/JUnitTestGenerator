package com.umich.junittestgenerator.controller;

import com.umich.junittestgenerator.dto.SourceCodeRequest;
import com.umich.junittestgenerator.service.TestGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-generator")
public class TestGenerationController {

    @Autowired
    private TestGeneratorService testGeneratorService;

    @PostMapping("/generate")
    public String generateTests(@RequestBody SourceCodeRequest request) {
        return testGeneratorService.generateTestCases(request.getSourceCode());
    }
}
