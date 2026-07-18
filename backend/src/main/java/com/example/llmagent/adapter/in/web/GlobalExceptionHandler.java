package com.example.llmagent.adapter.in.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.llmagent.domain.agent.PromptTemplate;

/** 全域錯誤對映:缺範本變數 → 400(明確列出缺漏);not found 類 → 404。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PromptTemplate.MissingVariableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> missingVariables(PromptTemplate.MissingVariableException e) {
        return Map.of("error", e.getMessage(), "missing", e.missing());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> notFound(IllegalArgumentException e) {
        return Map.of("error", e.getMessage() == null ? "not found" : e.getMessage());
    }
}
