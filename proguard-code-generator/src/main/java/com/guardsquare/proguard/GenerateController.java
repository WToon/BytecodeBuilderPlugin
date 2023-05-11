package com.guardsquare.proguard;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.guardsquare.proguard.CodeUtil.getProGuardInstructions;

@RestController
public class GenerateController {
    @PostMapping("/generate")
    public String generate(@RequestBody String code) {
        System.out.println(code);
        return getProGuardInstructions(code);
    }
}
